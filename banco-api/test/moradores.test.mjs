/*
 * Quem pode reescrever a lista de moradores.
 *
 * A lista é a lista de permitidos do app: o login casa o email autenticado com um
 * morador ativo. Então gravá-la é dar e tirar acesso, e isso é do administrador, não
 * de quem simplesmente está logado.
 *
 * O caso perigoso é o morador legítimo: ele tem token do Firebase válido, passa pela
 * porta da frente do Worker e chegava no `POST` sem mais nenhuma checagem. O teste
 * assina um token de verdade — par de chaves próprio, JWKS servido por um `fetch`
 * trocado — porque só assim o `verificarTokenFirebase` devolve um `uid` e o cenário
 * fica igual ao de produção.
 *
 * Uso:  node --test test/moradores.test.mjs
 */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { webcrypto as crypto } from "node:crypto";

import worker from "../src/index.js";

const PROJECT_ID = "repostinho-teste";
const KID = "chave-de-teste";
const API_TOKEN = "token-de-administrador";

const fetchOriginal = globalThis.fetch;
let tokenDeMorador;

function base64Url(bytes) {
  return Buffer.from(bytes).toString("base64url");
}

async function assinarToken(privateKey, corpo) {
  const cabecalho = base64Url(JSON.stringify({ alg: "RS256", kid: KID, typ: "JWT" }));
  const carga = base64Url(JSON.stringify(corpo));
  const assinatura = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    privateKey,
    new TextEncoder().encode(`${cabecalho}.${carga}`)
  );
  return `${cabecalho}.${carga}.${base64Url(new Uint8Array(assinatura))}`;
}

before(async () => {
  const par = await crypto.subtle.generateKey(
    { name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
    true,
    ["sign", "verify"]
  );

  const jwk = await crypto.subtle.exportKey("jwk", par.publicKey);
  globalThis.fetch = async () =>
    new Response(JSON.stringify({ keys: [{ ...jwk, kid: KID, alg: "RS256", use: "sig" }] }), {
      headers: { "content-type": "application/json", "cache-control": "max-age=3600" },
    });

  const agora = Math.floor(Date.now() / 1000);
  tokenDeMorador = await assinarToken(par.privateKey, {
    aud: PROJECT_ID,
    iss: `https://securetoken.google.com/${PROJECT_ID}`,
    sub: "uid-do-peter",
    iat: agora,
    exp: agora + 3600,
  });
});

after(() => {
  globalThis.fetch = fetchOriginal;
});

/** KV de mentira que lembra o que gravaram, para o teste ver se gravaram. */
function kvFalso(inicial) {
  const dados = new Map(inicial ? [["moradores:v1", JSON.stringify(inicial)]] : []);
  return {
    escritas: 0,
    async get(key, options) {
      const bruto = dados.get(key) ?? null;
      if (bruto === null) return null;
      return options?.type === "json" ? JSON.parse(bruto) : bruto;
    },
    async getWithMetadata(key) {
      return { value: dados.get(key) ?? null, metadata: { contentType: "image/jpeg" } };
    },
    async put(key, value) {
      this.escritas++;
      dados.set(key, value);
    },
  };
}

const LISTA = [{ id: "peter", name: "Peter", isActive: true, email: "peter@exemplo.com" }];
const INVASAO = [{ id: "peter", name: "Peter", isActive: true, email: "invasor@exemplo.com" }];

function requisicao(headers, body) {
  return new Request("https://banco.exemplo.workers.dev/moradores", {
    method: body ? "POST" : "GET",
    headers: { ...headers, ...(body ? { "content-type": "application/json" } : {}) },
    body: body ? JSON.stringify({ residents: body }) : undefined,
  });
}

test("morador logado não reescreve a lista", async () => {
  const TAREFAS = kvFalso(LISTA);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(
    requisicao({ authorization: `Bearer ${tokenDeMorador}` }, INVASAO),
    env,
    {}
  );

  assert.equal(res.status, 403);
  // O status sozinho não bastaria: recusar depois de ter gravado seria o mesmo furo.
  assert.equal(TAREFAS.escritas, 0);
});

test("morador logado continua lendo a lista", async () => {
  const env = { TAREFAS: kvFalso(LISTA), API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(
    requisicao({ authorization: `Bearer ${tokenDeMorador}` }),
    env,
    {}
  );

  assert.equal(res.status, 200);
  const { residents } = await res.json();
  assert.equal(residents[0].email, "peter@exemplo.com");
});

test("administrador grava", async () => {
  const TAREFAS = kvFalso(LISTA);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(
    requisicao({ "x-rep-token": API_TOKEN }, INVASAO),
    env,
    {}
  );

  assert.equal(res.status, 200);
  assert.equal(TAREFAS.escritas, 1);
});

test("sem credencial nenhuma para no 401, antes do roteamento", async () => {
  const env = { TAREFAS: kvFalso(LISTA), API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(requisicao({}, INVASAO), env, {});

  assert.equal(res.status, 401);
});

function requisicaoDeFoto(headers, body) {
  return new Request("https://banco.exemplo.workers.dev/foto/peter", {
    method: body ? "PUT" : "GET",
    headers: { ...headers, ...(body ? { "content-type": "image/jpeg" } : {}) },
    body,
  });
}

test("morador logado não troca a foto de outro", async () => {
  // O app só lê fotos — nenhuma tela envia uma. Quem publica é o cadastro por linha de
  // comando, então exigir administrador aqui não tira nada de ninguém.
  const TAREFAS = kvFalso(LISTA);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(
    requisicaoDeFoto({ authorization: `Bearer ${tokenDeMorador}` }, new Uint8Array([1, 2, 3])),
    env,
    {}
  );

  assert.equal(res.status, 403);
  assert.equal(TAREFAS.escritas, 0);
});

test("administrador troca a foto", async () => {
  const TAREFAS = kvFalso(LISTA);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(
    requisicaoDeFoto({ "x-rep-token": API_TOKEN }, new Uint8Array([1, 2, 3])),
    env,
    {}
  );

  assert.equal(res.status, 200);
  assert.equal(TAREFAS.escritas, 1);
});

/*
 * A ficha médica na volta do KV.
 *
 * O `validResident` descarta campo que ele não conhece — foi assim que a ficha ficou de
 * fora até existir este código. O teste existe para que ela não volte a sumir em silêncio
 * numa mudança futura: uma ficha perdida só é notada na emergência.
 */
const COM_FICHA = [
  {
    id: "peter",
    name: "Peter",
    isActive: true,
    medical: {
      bloodType: "  O+ ",
      drugAllergies: "Nenhuma conhecida",
      otherAllergies: "  ",
      conditions: "Rinite alérgica",
      medications: "Levotiroxina 112mcg",
      medicationLocation: "Caixa azul na pia do banheiro",
      contacts: [
        { name: " Luciana ", relationship: "Mãe", phone: "(19) 99226-2146" },
        { name: "   ", relationship: "Amiga", phone: "(11) 96192-7998" },
        { name: "Adara", relationship: null, phone: null },
      ],
      notes: "Toma o remédio todo dia",
      inventado: "some daqui",
    },
  },
];

test("a ficha médica sobrevive à gravação, limpa", async () => {
  const TAREFAS = kvFalso(LISTA);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(requisicao({ "x-rep-token": API_TOKEN }, COM_FICHA), env, {});
  assert.equal(res.status, 200);

  const { residents } = await res.json();
  const ficha = residents[0].medical;

  assert.equal(ficha.bloodType, "O+");
  assert.equal(ficha.otherAllergies, null);
  assert.equal(ficha.medicationLocation, "Caixa azul na pia do banheiro");
  assert.equal(ficha.inventado, undefined);
});

test("contato sem nome não entra", async () => {
  // Telefone sem dono não diz para quem se está ligando, e numa emergência isso é pior
  // do que um contato a menos.
  const TAREFAS = kvFalso(LISTA);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(requisicao({ "x-rep-token": API_TOKEN }, COM_FICHA), env, {});
  const { residents } = await res.json();
  const contacts = residents[0].medical.contacts;

  assert.equal(contacts.length, 2);
  assert.deepEqual(contacts.map((c) => c.name), ["Luciana", "Adara"]);
  // Contato só com nome continua valendo: alguém para procurar já é alguma coisa.
  assert.equal(contacts[1].phone, null);
});

test("ficha vazia vira nenhuma ficha", async () => {
  // "Preencheu tudo em branco" e "não preencheu" precisam chegar iguais na tela.
  const vazia = [
    { id: "du", name: "Du", isActive: true, medical: { bloodType: "   ", contacts: [] } },
  ];
  const TAREFAS = kvFalso(LISTA);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(requisicao({ "x-rep-token": API_TOKEN }, vazia), env, {});
  const { residents } = await res.json();

  assert.equal(residents[0].medical, null);
});

test("morador sem ficha continua válido", async () => {
  // Ninguém é obrigado a preencher, e a ausência não pode invalidar o cadastro inteiro.
  const TAREFAS = kvFalso(LISTA);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(requisicao({ "x-rep-token": API_TOKEN }, LISTA), env, {});
  const { residents } = await res.json();

  assert.equal(res.status, 200);
  assert.equal(residents[0].medical, null);
});
