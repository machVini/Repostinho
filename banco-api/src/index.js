import * as XLSX from "xlsx";
import { verificarTokenFirebase, tokenDoCabecalho } from "./auth.js";

/*
 * Converte a planilha do banco da rep em JSON para o app.
 *
 * O app não faz esse trabalho porque um .xlsm é um ZIP de XMLs, e o Kotlin/Native
 * (alvo iOS) não tem `inflate` nem biblioteca madura de xlsx. Em JavaScript o SheetJS
 * resolve, então a conversão vive aqui.
 *
 * A URL da planilha fica em SHEET_URL, como secret: assim o link do SharePoint não
 * viaja no binário do app nem circula por aí.
 */

const CACHE_SECONDS = 300;

/** Suba isto quando a forma da resposta mudar; invalida o cache de borda. */
const CACHE_VERSION = "3";

/** Quantas atas o card da Home mostra. */
const ATAS_COUNT = 3;

/** Quantos arquivos ler antes de ordenar por data de reunião. */
const ATAS_SCAN = 50;

/**
 * Mesmo cache do banco. Já foi de 30 minutos, mas isso fazia o "puxar para atualizar"
 * não trazer uma ata recém-adicionada — o gesto existe justamente para não esperar.
 */
const ATAS_CACHE_SECONDS = CACHE_SECONDS;

/**
 * Prefixo das chaves das tarefas feitas no KV.
 *
 * A semana entra na chave, então a virada de quarta-feira zera as marcações sozinha: a
 * semana seguinte é outra chave, que ainda não existe. Nada precisa ser apagado.
 */
const TAREFAS_PREFIX = "feitas:v1";

/**
 * Por quanto tempo uma semana de marcações sobrevive no KV.
 *
 * Passada a semana, elas não têm mais leitor. Sessenta dias dão folga para o rodízio ser
 * pausado e retomado sem perder o que já estava marcado.
 */
const TAREFAS_TTL_SECONDS = 60 * 24 * 60 * 60;

/**
 * Onde a agenda adicionada pela rep fica.
 *
 * Sem TTL, ao contrário das tarefas: um evento cadastrado vale até alguém apagar. Mora no
 * mesmo KV das tarefas porque são duas listas pequenas do mesmo app — um namespace por
 * chave só multiplicaria configuração.
 */
const EVENTOS_KEY = "eventos:v1";

/**
 * Onde ficam os moradores.
 *
 * Saíram do código do app para cá porque foto, aniversário e data de entrada mudam sem
 * que ninguém queira publicar versão nova só por isso.
 */
const MORADORES_KEY = "moradores:v1";

/**
 * Prefixo das fotos dos moradores no KV.
 *
 * O lugar certo para binário seria o R2, mas ele precisa ser habilitado no Dashboard e
 * costuma pedir cartão. São quinze fotos de poucos KB, lidas o tempo todo e trocadas
 * quase nunca — cabem aqui sem drama. O app só conhece a URL, então mudar para R2 depois
 * não encosta no Kotlin.
 */
const FOTOS_PREFIX = "foto:v1";

/**
 * Teto por foto.
 *
 * O KV aceita 25 MiB, mas uma foto de celular sem redimensionar chega a 5 MB e o app
 * baixaria isso a cada perfil aberto. Recusar na entrada é mais gentil do que descobrir
 * pela conta de banda.
 */
const FOTO_MAX_BYTES = 2 * 1024 * 1024;

const FOTO_TIPOS = ["image/jpeg", "image/png", "image/webp"];

/** Os tipos de quarto que o app sabe desenhar. */
const ROOM_TYPES = [
  "INDIVIDUAL",
  "DUPLO_MAIOR",
  "DUPLO_MENOR",
  "TRIPLO_MAIOR",
  "TRIPLO_MENOR",
];

/** O que o app sabe desenhar. Categoria desconhecida vira ROLE em vez de derrubar a tela. */
const EVENT_CATEGORIES = ["ANIVERSARIO", "REP", "ROLE", "ARU"];
const RECURRENCES = ["NENHUMA", "MENSAL", "ANUAL"];

/** Nomes das abas na planilha. Mudou lá, muda aqui. */
const SHEET_BALANCES = "Saldos_pessoas";
const SHEET_MOVEMENTS = "Movimentações";
const SHEET_CAIXINHA = "Saldos_caixinha";

/** Linhas que a planilha usa em cada bloco (1-based, como no Excel). */
const BALANCES_CURRENT = [2, 18];
const BALANCES_FORMER = [23, 30];
const CAIXINHA_ROWS = [2, 8];
const MOVEMENT_FIRST_COL = 6; // F
const MOVEMENT_LAST_COL = 30; // AD

const TYPES = { COLETIVO: "COLETIVO", PRIVADO: "PRIVADO", ENTRADA: "ENTRADA", SAIDA: "SAIDA" };

/** Dinheiro em centavos: Double acumula resto ao longo das somas. */
function cents(value) {
  if (typeof value !== "number" || !isFinite(value)) return 0;
  return Math.round(value * 100);
}

function cell(sheet, row, col) {
  const ref = XLSX.utils.encode_cell({ r: row - 1, c: col - 1 });
  const c = sheet[ref];
  return c ? c.v : undefined;
}

function text(value) {
  return value === undefined || value === null ? "" : String(value).trim();
}

/**
 * Pesos com 2 casas.
 *
 * O total de pesos é fórmula no Excel, e vem com o resto do binário: o aluguel de junho
 * chega como 14.240000000000002. Sem isto, o número vaza para a tela do app.
 */
function weight(value) {
  return Math.round(value * 100) / 100;
}

export function readBalances(sheet, [first, last], isFormer) {
  const out = [];
  for (let row = first; row <= last; row++) {
    const name = text(cell(sheet, row, 1));
    if (!name) continue;
    out.push({
      name,
      previousCents: cents(cell(sheet, row, 2)),
      expensesCents: cents(cell(sheet, row, 3)),
      paymentsCents: cents(cell(sheet, row, 4)),
      finalCents: cents(cell(sheet, row, 5)),
      isFormer,
    });
  }
  return out;
}

export function readMovements(sheet) {
  const range = XLSX.utils.decode_range(sheet["!ref"]);
  const lastRow = range.e.r + 1;

  // Cabeçalho: cada coluna de F a AD é uma pessoa.
  const people = {};
  for (let col = MOVEMENT_FIRST_COL; col <= MOVEMENT_LAST_COL; col++) {
    const name = text(cell(sheet, 1, col));
    if (name) people[col] = name;
  }

  const out = [];
  for (let row = 2; row <= lastRow; row++) {
    const description = text(cell(sheet, row, 2));
    const rawType = text(cell(sheet, row, 3)).toUpperCase();
    const value = cell(sheet, row, 5);
    // Linha sem descrição, sem tipo ou sem valor é sobra da tabela, não lançamento.
    if (!description || !rawType || typeof value !== "number") continue;

    const weights = {};
    for (const [col, name] of Object.entries(people)) {
      const w = cell(sheet, row, Number(col));
      if (typeof w === "number" && w !== 0) weights[name] = weight(w);
    }

    const totalWeight = cell(sheet, row, 31);
    const forms = cell(sheet, row, 1);

    out.push({
      id: forms ? String(forms) : `row${row}`,
      description,
      type: TYPES[rawType] ?? "PRIVADO",
      payer: text(cell(sheet, row, 4)) || "-",
      valueCents: cents(value),
      weights,
      totalWeight: typeof totalWeight === "number" ? weight(totalWeight) : 0,
    });
  }
  return out;
}

export function readCaixinha(sheet) {
  const out = [];
  for (let row = CAIXINHA_ROWS[0]; row <= CAIXINHA_ROWS[1]; row++) {
    const label = text(cell(sheet, row, 1));
    if (!label) continue;
    out.push({
      label,
      initialCents: cents(cell(sheet, row, 2)),
      variationCents: cents(cell(sheet, row, 3)),
      finalCents: cents(cell(sheet, row, 4)),
      isTotal: label.includes("Total"),
    });
  }
  return out;
}

/**
 * "12/08/2026 02:52", no fuso de Campinas.
 *
 * Data e hora são formatadas separadamente porque o `pt-BR` junta as duas com vírgula
 * quando pedidas de uma vez.
 */
function formatLabel(date) {
  const zone = { timeZone: "America/Sao_Paulo" };
  const day = new Intl.DateTimeFormat("pt-BR", {
    ...zone,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
  const time = new Intl.DateTimeFormat("pt-BR", {
    ...zone,
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date);
  return `${day} ${time}`;
}

const MONTHS_PT = [
  "janeiro", "fevereiro", "março", "abril", "maio", "junho",
  "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
];

/** Tira acento para casar "março" escrito de qualquer jeito. */
function fold(text) {
  return text.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
}

/**
 * Data da reunião a partir do nome do arquivo, em epoch ms.
 *
 * A data pode estar em qualquer posição do nome, porque na prática as atas aparecem como
 * "11/08/2026", "Ata Repostinho 18/08/2026" e "18 de agosto de 2026". Devolve `null`
 * quando não há data reconhecível — aí a ordenação cai para a data de criação no Drive.
 */
function meetingDate(name) {
  const raw = String(name ?? "");
  const text = fold(raw);

  // ISO primeiro: em "2026-08-18" o regex de barra não casaria, mas o de dia/mês/ano
  // poderia confundir se o nome misturasse os dois formatos.
  const iso = /(\d{4})-(\d{1,2})-(\d{1,2})/.exec(raw);
  if (iso) return utc(iso[1], iso[2], iso[3]);

  const slashed = /(\d{1,2})[/.](\d{1,2})[/.](\d{4})/.exec(raw);
  if (slashed) return utc(slashed[3], slashed[2], slashed[1]);

  const written = /(\d{1,2})\s+de\s+([a-z]+)\s+de\s+(\d{4})/.exec(text);
  if (written) {
    const month = MONTHS_PT.findIndex((m) => fold(m) === written[2]);
    if (month >= 0) return utc(written[3], month + 1, written[1]);
  }

  return null;
}

function utc(year, month, day) {
  const at = Date.UTC(Number(year), Number(month) - 1, Number(day));
  return Number.isNaN(at) ? null : at;
}

/** Ordena da reunião mais recente para a mais antiga e corta em [count]. */
export function orderAtas(files, count) {
  return files
    .map((file) => ({
      file,
      // `Date.parse` devolve NaN em data inválida, e NaN quebraria a comparação.
      at: meetingDate(file.name ?? "") ?? (Date.parse(file.createdTime ?? "") || 0),
    }))
    .sort((a, b) => b.at - a.at)
    .slice(0, count)
    .map(({ file }) => file);
}

/**
 * Lista as atas de reunião na pasta do Drive, da reunião mais recente para a mais antiga.
 *
 * A ordem sai do nome do arquivo, não da data de criação no Drive: as atas se chamam
 * "dd/MM/yyyy", e subir uma ata antiga depois não deve colocá-la no topo do card. Quando
 * o nome não é uma data, vale a data de criação.
 *
 * Por isso a busca traz mais arquivos do que o card mostra — a ordenação é feita aqui,
 * depois de ler os nomes.
 */
async function handleAtas(request, env, ctx) {
  if (!env.DRIVE_FOLDER_ID || !env.DRIVE_API_KEY) {
    return json({ error: "DRIVE_FOLDER_ID ou DRIVE_API_KEY não configuradas" }, 500);
  }

  const cache = caches.default;
  const key = cacheKey(request, env.DRIVE_FOLDER_ID);
  if (!wantsFresh(request)) {
    const cached = await cache.match(key);
    if (cached) return cached;
  }

  const query = new URLSearchParams({
    q: `'${env.DRIVE_FOLDER_ID}' in parents and trashed = false`,
    orderBy: "createdTime desc",
    pageSize: String(ATAS_SCAN),
    fields: "files(id,name,webViewLink,createdTime)",
    key: env.DRIVE_API_KEY,
  });

  let listing;
  try {
    const upstream = await fetch(`https://www.googleapis.com/drive/v3/files?${query}`);
    if (!upstream.ok) {
      const detail = await upstream.text();
      // 403 aqui costuma ser pasta não compartilhada como "qualquer pessoa com o link":
      // a chave de API não enxerga o que é restrito a pessoas específicas.
      return json({ error: `Drive respondeu ${upstream.status}`, detail }, 502);
    }
    listing = await upstream.json();
  } catch (e) {
    return json({ error: `falha ao listar a pasta: ${e.message}` }, 502);
  }

  // Pasta sem compartilhamento público devolve 200 com lista vazia, e não 403 — sem esta
  // checagem, "não tenho permissão" chegaria na tela como "não há atas".
  if ((listing.files ?? []).length === 0) {
    const meta = await fetch(
      `https://www.googleapis.com/drive/v3/files/${env.DRIVE_FOLDER_ID}` +
        `?fields=id&key=${env.DRIVE_API_KEY}`
    );
    if (!meta.ok) {
      return json(
        {
          error: "pasta inacessível pela chave de API",
          detail:
            "confira se ela está compartilhada como 'qualquer pessoa com o link' e se " +
            "o DRIVE_FOLDER_ID está certo",
        },
        502
      );
    }
  }

  const payload = {
    folderUrl: `https://drive.google.com/drive/folders/${env.DRIVE_FOLDER_ID}`,
    files: orderAtas(listing.files ?? [], ATAS_COUNT).map((file) => ({
      id: file.id,
      name: file.name,
      // webViewLink abre no app do Drive quando ele está instalado, e no navegador
      // quando não está.
      url: file.webViewLink ?? `https://drive.google.com/file/d/${file.id}/view`,
    })),
  };

  const response = json(payload, 200, {
    "cache-control": `public, max-age=${ATAS_CACHE_SECONDS}`,
  });
  ctx.waitUntil(cache.put(key, response.clone()));
  return response;
}

/**
 * As tarefas marcadas como feitas na semana, compartilhadas entre os moradores.
 *
 * Quem calcula a semana é o app, não o Worker: a regra do rodízio (quando vira, se está
 * pausado) mora no Kotlin, e duplicá-la aqui criaria duas versões da mesma conta para
 * discordarem. O custo é que um aparelho com a data errada marca na semana errada.
 *
 * Só isto é compartilhado por enquanto — a escala em si continua fixa no app.
 */
async function handleTarefas(request, env) {
  if (!env.TAREFAS) {
    return json({ error: "KV TAREFAS não configurado" }, 500);
  }

  const url = new URL(request.url);

  if (request.method === "GET") {
    const week = weekParam(url.searchParams.get("semana"));
    if (week === null) return json({ error: "semana inválida" }, 400);
    return tarefasResponse(env, week, await readDone(env, week));
  }

  if (request.method !== "POST") {
    return json({ error: "method not allowed" }, 405);
  }

  let body;
  try {
    body = await request.json();
  } catch {
    return json({ error: "corpo não é JSON" }, 400);
  }

  const week = weekParam(body?.week);
  const choreId = typeof body?.choreId === "string" ? body.choreId.trim() : "";
  if (week === null || !choreId) {
    return json({ error: "week e choreId são obrigatórios" }, 400);
  }

  // Ler-alterar-gravar: o KV não tem operação atômica, então duas pessoas marcando no
  // mesmo segundo podem perder uma das marcas. Numa rep de 15 pessoas o desencontro é
  // raro e o conserto é remarcar; resolver de verdade exigiria um Durable Object.
  const current = new Set(await readDone(env, week));
  if (body.done === false) current.delete(choreId);
  else current.add(choreId);

  const updated = [...current].sort();
  await env.TAREFAS.put(`${TAREFAS_PREFIX}:${week}`, JSON.stringify(updated), {
    expirationTtl: TAREFAS_TTL_SECONDS,
  });

  return tarefasResponse(env, week, updated);
}

async function readDone(env, week) {
  const raw = await env.TAREFAS.get(`${TAREFAS_PREFIX}:${week}`, { type: "json" });
  return Array.isArray(raw) ? raw.filter((id) => typeof id === "string") : [];
}

/** Marcação é estado vivo: cachear na borda mostraria a caixa desmarcada depois do toque. */
function tarefasResponse(env, week, doneChoreIds) {
  return json({ week, doneChoreIds }, 200, { "cache-control": "no-store" });
}

/**
 * A semana é inteira e pode ser negativa — âncora à frente, aparelho com data atrasada.
 *
 * Ausente é erro, e não zero: `Number(null)` é 0, então sem esta guarda um app que
 * esquecesse o parâmetro leria e gravaria na semana da âncora sem reclamar de nada.
 */
function weekParam(value) {
  if (value === null || value === undefined || value === "") return null;
  const week = Number(value);
  return Number.isInteger(week) ? week : null;
}

/**
 * Os eventos que a rep cadastrou pelo app.
 *
 * Só os adicionados pela tela moram aqui. A agenda fixa (aluguel, aniversários, InterReps)
 * vem embutida no app: ela vale mesmo sem rede e não faz sentido alguém poder apagá-la de
 * um toque. O app junta as duas listas.
 */
async function handleEventos(request, env) {
  if (!env.TAREFAS) {
    return json({ error: "KV TAREFAS não configurado" }, 500);
  }

  if (request.method === "GET") {
    return eventosResponse(await readEventos(env));
  }

  if (request.method !== "POST") {
    return json({ error: "method not allowed" }, 405);
  }

  let body;
  try {
    body = await request.json();
  } catch {
    return json({ error: "corpo não é JSON" }, 400);
  }

  const current = await readEventos(env);
  let updated;

  if (body?.remove) {
    const id = String(body.remove);
    updated = current.filter((event) => event.id !== id);
    if (updated.length === current.length) {
      return json({ error: "evento não encontrado" }, 404);
    }
  } else {
    const event = validEvent(body?.event);
    if (!event) return json({ error: "evento inválido" }, 400);
    // Mesmo id sobrescreve: reenviar depois de um timeout corrige em vez de duplicar.
    updated = [...current.filter((it) => it.id !== event.id), event];
  }

  await env.TAREFAS.put(EVENTOS_KEY, JSON.stringify(updated));
  return eventosResponse(updated);
}

async function readEventos(env) {
  const raw = await env.TAREFAS.get(EVENTOS_KEY, { type: "json" });
  if (!Array.isArray(raw)) return [];
  return raw.map(validEvent).filter(Boolean);
}

/**
 * Aceita só o que o app consegue desenhar.
 *
 * Um campo torto gravado aqui viraria exceção de desserialização no Kotlin, e o app não
 * abriria a agenda — melhor recusar na entrada do que derrubar a tela de todo mundo.
 */
function validEvent(raw) {
  if (!raw || typeof raw !== "object") return null;

  const id = typeof raw.id === "string" ? raw.id.trim() : "";
  const name = typeof raw.name === "string" ? raw.name.trim() : "";
  const start = validDate(raw.start);
  if (!id || !name || !start) return null;

  const end = validDate(raw.end) ?? start;
  const category = EVENT_CATEGORIES.includes(raw.category) ? raw.category : "ROLE";
  const recurrence = RECURRENCES.includes(raw.recurrence) ? raw.recurrence : "NENHUMA";

  // isCustom é sempre true: tudo que está no KV veio da tela, e é justamente isso que
  // autoriza apagar. Um cliente não pode se declarar fixo para virar inapagável.
  return { id, name, start, end, category, recurrence, isHighlight: false, isCustom: true };
}

function validDate(raw) {
  if (!raw || typeof raw !== "object") return null;
  const { day, month, year } = raw;
  const ok =
    Number.isInteger(day) && day >= 1 && day <= 31 &&
    Number.isInteger(month) && month >= 1 && month <= 12 &&
    Number.isInteger(year) && year >= 2000 && year <= 2100;
  return ok ? { day, month, year } : null;
}

/** Agenda é estado vivo: cachear na borda esconderia o evento recém-criado. */
function eventosResponse(events) {
  return json({ events }, 200, { "cache-control": "no-store" });
}

/**
 * A foto de um morador: `/foto/<id>`.
 *
 * Servida pelo Worker, e não por um bucket público, para ficar atrás do mesmo token do
 * resto. Foto de gente numa URL pública e adivinhável (`.../vk.jpg`) seria a única coisa
 * do app aberta para quem passasse por ali.
 */
async function handleFoto(request, env, id) {
  if (!env.TAREFAS) {
    return json({ error: "KV TAREFAS não configurado" }, 500);
  }
  if (!id) return json({ error: "faltou o id do morador" }, 400);

  const key = `${FOTOS_PREFIX}:${id}`;

  if (request.method === "GET") {
    const { value, metadata } = await env.TAREFAS.getWithMetadata(key, {
      type: "arrayBuffer",
    });
    if (!value) return json({ error: "sem foto" }, 404);

    return new Response(value, {
      headers: {
        "content-type": metadata?.contentType ?? "image/jpeg",
        // Sem cache: trocar a foto e continuar vendo a antiga é a primeira reclamação, e
        // são quinze imagens pequenas pedidas por uma tela só. Se um dia pesar, o
        // caminho é versionar a URL em vez de voltar a cachear às cegas.
        "cache-control": "no-store",
      },
    });
  }

  if (request.method !== "PUT") {
    return json({ error: "method not allowed" }, 405);
  }

  const contentType = (request.headers.get("content-type") ?? "").split(";")[0].trim();
  if (!FOTO_TIPOS.includes(contentType)) {
    return json({ error: `content-type deve ser um de ${FOTO_TIPOS.join(", ")}` }, 415);
  }

  const bytes = await request.arrayBuffer();
  if (bytes.byteLength === 0) return json({ error: "corpo vazio" }, 400);
  if (bytes.byteLength > FOTO_MAX_BYTES) {
    return json(
      { error: `foto tem ${bytes.byteLength} bytes; o limite é ${FOTO_MAX_BYTES}` },
      413
    );
  }

  await env.TAREFAS.put(key, bytes, { metadata: { contentType } });
  return json({ id, bytes: bytes.byteLength, contentType }, 200);
}

/**
 * Os moradores da rep.
 *
 * O app traz uma lista embutida para a primeira abertura sem rede; esta é a que manda
 * quando existe. `POST` grava a lista inteira de uma vez — são 15 pessoas que mudam
 * duas vezes por ano, e mandar tudo evita a pergunta de o que fazer com quem sumiu.
 */
async function handleMoradores(request, env) {
  if (!env.TAREFAS) {
    return json({ error: "KV TAREFAS não configurado" }, 500);
  }

  if (request.method === "GET") {
    return moradoresResponse(await readMoradores(env));
  }

  if (request.method !== "POST") {
    return json({ error: "method not allowed" }, 405);
  }

  let body;
  try {
    body = await request.json();
  } catch {
    return json({ error: "corpo não é JSON" }, 400);
  }

  if (!Array.isArray(body?.residents)) {
    return json({ error: "residents deve ser uma lista" }, 400);
  }

  const residents = body.residents.map(validResident).filter(Boolean);
  if (residents.length !== body.residents.length) {
    return json({ error: "algum morador está inválido" }, 400);
  }

  await env.TAREFAS.put(MORADORES_KEY, JSON.stringify(residents));
  return moradoresResponse(residents);
}

async function readMoradores(env) {
  const raw = await env.TAREFAS.get(MORADORES_KEY, { type: "json" });
  if (!Array.isArray(raw)) return [];
  return raw.map(validResident).filter(Boolean);
}

/**
 * Aceita só o que o app consegue desenhar.
 *
 * Campo torto aqui viraria exceção de desserialização no Kotlin, e o app abriria sem
 * morador nenhum — sem nome, sem tarefa, sem saldo próprio.
 */
function validResident(raw) {
  if (!raw || typeof raw !== "object") return null;

  const id = typeof raw.id === "string" ? raw.id.trim() : "";
  const name = typeof raw.name === "string" ? raw.name.trim() : "";
  if (!id || !name) return null;

  const roomType = ROOM_TYPES.includes(raw.roomType) ? raw.roomType : "INDIVIDUAL";
  const birthDay = intInRange(raw.birthDay, 1, 31);
  const birthMonth = intInRange(raw.birthMonth, 1, 12);
  const joinedMonth = intInRange(raw.joinedMonth, 1, 12);
  const joinedYear = intInRange(raw.joinedYear, 2000, 2100);

  return {
    id,
    name,
    roomType,
    isModerator: raw.isModerator === true,
    // Ausente é morador ativo: só quem saiu é marcado, e esquecer o campo não pode
    // apagar alguém da escala.
    isActive: raw.isActive !== false,
    // Dia sem mês (ou o contrário) não vira aniversário: o Calendário precisa dos dois.
    birthDay: birthDay !== null && birthMonth !== null ? birthDay : null,
    birthMonth: birthDay !== null && birthMonth !== null ? birthMonth : null,
    // Mês e ano de entrada. Um sem o outro não vira nada exibível, então caem juntos.
    joinedMonth: joinedMonth !== null && joinedYear !== null ? joinedMonth : null,
    joinedYear: joinedMonth !== null && joinedYear !== null ? joinedYear : null,
    photoUrl: typeof raw.photoUrl === "string" ? raw.photoUrl.trim() || null : null,
    // Minúsculo sempre: é por ele que o login casa a conta com o morador, e "VK@x.com"
    // e "vk@x.com" são o mesmo endereço para o provedor de autenticação.
    email: typeof raw.email === "string"
      ? raw.email.trim().toLowerCase() || null
      : null,
    // O nome dele na planilha, quando diferente. Sem isso o app procura a coluna pelo
    // nome que exibe, não acha, e a pessoa vê saldo vazio como se não devesse nada.
    sheetName: typeof raw.sheetName === "string" ? raw.sheetName.trim() || null : null,
  };
}

function intInRange(value, min, max) {
  return Number.isInteger(value) && value >= min && value <= max ? value : null;
}

/** Lista viva: cachear na borda esconderia a foto que alguém acabou de trocar. */
function moradoresResponse(residents) {
  return json({ residents }, 200, { "cache-control": "no-store" });
}

/**
 * Chave de cache que muda quando a resposta deveria mudar.
 *
 * `wrangler deploy` não limpa o cache de borda: uma entrada gravada antes continua sendo
 * servida até o TTL vencer, mesmo com código ou secret novos. Isso já fez o app mostrar a
 * pasta errada por meia hora depois de um deploy correto.
 *
 * [parts] leva o que, mudando, invalida a resposta — o id da pasta, por exemplo. Para
 * mudança de código, suba [CACHE_VERSION].
 */
function cacheKey(request, ...parts) {
  const url = new URL(request.url);
  // Só caminho e versão: parâmetros da chamada (como `fresh`) não podem multiplicar
  // entradas no cache.
  url.search = "";
  url.searchParams.set("v", [CACHE_VERSION, ...parts].join("."));
  return new Request(url.toString(), { method: "GET" });
}

/**
 * O morador puxou a lista para baixo e quer o estado de agora.
 *
 * Sem isto, puxar dentro da janela de cache devolvia o mesmo payload com o mesmo horário,
 * e o gesto parecia não ter feito nada. A abertura normal do app segue usando o cache.
 */
function wantsFresh(request) {
  return new URL(request.url).searchParams.get("fresh") === "1";
}

function json(body, status, extraHeaders = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", ...extraHeaders },
  });
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    /*
     * Duas credenciais, com papéis diferentes.
     *
     * O app manda o token do Firebase do morador logado — ele não carrega mais segredo
     * nenhum embutido, então extrair o APK não dá acesso a nada. O `x-rep-token` deixou
     * de viajar no binário e virou chave de administração, usada só nos cadastros por
     * linha de comando.
     */
    const uid = await verificarTokenFirebase(tokenDoCabecalho(request), env.FIREBASE_PROJECT_ID);
    const admin = env.API_TOKEN && request.headers.get("x-rep-token") === env.API_TOKEN;
    if (!uid && !admin) {
      return json({ error: "unauthorized" }, 401);
    }

    if (url.pathname === "/atas") return handleAtas(request, env, ctx);
    if (url.pathname === "/tarefas") return handleTarefas(request, env);
    if (url.pathname === "/eventos") return handleEventos(request, env);
    if (url.pathname === "/moradores") return handleMoradores(request, env);
    if (url.pathname.startsWith("/foto/")) {
      return handleFoto(request, env, decodeURIComponent(url.pathname.slice("/foto/".length)));
    }
    if (url.pathname !== "/banco") {
      return json({ error: "not found" }, 404);
    }

    const cache = caches.default;
    const key = cacheKey(request);
    if (!wantsFresh(request)) {
      const cached = await cache.match(key);
      if (cached) return cached;
    }

    if (!env.SHEET_URL) {
      return json({ error: "SHEET_URL não configurada" }, 500);
    }

    let workbook;
    try {
      const upstream = await fetch(env.SHEET_URL, {
        headers: { "user-agent": "repostinho-banco-api" },
      });
      if (!upstream.ok) {
        return json({ error: `planilha respondeu ${upstream.status}` }, 502);
      }
      const bytes = await upstream.arrayBuffer();
      workbook = XLSX.read(bytes, { type: "array" });
    } catch (e) {
      return json({ error: `falha ao baixar a planilha: ${e.message}` }, 502);
    }

    const balancesSheet = workbook.Sheets[SHEET_BALANCES];
    const movementsSheet = workbook.Sheets[SHEET_MOVEMENTS];
    const caixinhaSheet = workbook.Sheets[SHEET_CAIXINHA];

    // Falhar alto: uma aba renomeada não pode virar saldo vazio na tela.
    const missing = [
      [SHEET_BALANCES, balancesSheet],
      [SHEET_MOVEMENTS, movementsSheet],
      [SHEET_CAIXINHA, caixinhaSheet],
    ]
      .filter(([, sheet]) => !sheet)
      .map(([name]) => name);

    if (missing.length > 0) {
      return json({ error: `abas não encontradas: ${missing.join(", ")}` }, 502);
    }

    const now = new Date();
    const payload = {
      generatedAt: now.toISOString(),
      // Formatado aqui porque o app não tem biblioteca de data: converter UTC para o
      // fuso de Campinas no Kotlin/Native custaria uma dependência inteira.
      generatedAtLabel: formatLabel(now),
      balances: [
        ...readBalances(balancesSheet, BALANCES_CURRENT, false),
        ...readBalances(balancesSheet, BALANCES_FORMER, true),
      ],
      movements: readMovements(movementsSheet),
      caixinha: readCaixinha(caixinhaSheet),
    };

    const response = json(payload, 200, {
      "cache-control": `public, max-age=${CACHE_SECONDS}`,
    });
    ctx.waitUntil(cache.put(key, response.clone()));
    return response;
  },
};
