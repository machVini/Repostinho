import * as XLSX from "xlsx";

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

function json(body, status, extraHeaders = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", ...extraHeaders },
  });
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (url.pathname !== "/banco") {
      return json({ error: "not found" }, 404);
    }

    // Token compartilhado. Isto é obstáculo, não autenticação: ele está no binário do
    // app e pode ser extraído. Serve para o endpoint não ficar aberto a quem topar com
    // a URL — segurança de verdade exigiria login por morador.
    if (env.API_TOKEN && request.headers.get("x-rep-token") !== env.API_TOKEN) {
      return json({ error: "unauthorized" }, 401);
    }

    const cache = caches.default;
    const cached = await cache.match(request);
    if (cached) return cached;

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
      generatedAtLabel: new Intl.DateTimeFormat("pt-BR", {
        timeZone: "America/Sao_Paulo",
        day: "2-digit",
        month: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      }).format(now),
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
    ctx.waitUntil(cache.put(request, response.clone()));
    return response;
  },
};
