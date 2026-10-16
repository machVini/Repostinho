/*
 * Quem vira a escala das tarefas.
 *
 * A escala deixou de sair da data e virou estado compartilhado: ela só anda quando alguém
 * manda. Por isso "quem" deixou de ser detalhe de tela — esconder o botão é combinado,
 * não permissão. Quem decide é o email assinado dentro do token do Firebase, casado com
 * o `canRotateChores` do cadastro.
 *
 * Uso:  node --test test/rodizio.test.mjs
 */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";

import worker from "../src/index.js";
import { tokensDeTeste } from "./token.mjs";

const PROJECT_ID = "repostinho-teste";
const API_TOKEN = "token-de-administrador";

let assinar;
let restaurar;

before(async () => {
  ({ assinar, restaurar } = await tokensDeTeste(PROJECT_ID));
});

after(() => restaurar());

const MORADORES = [
  { id: "vk", name: "VK", isActive: true, email: "vk@example.com", canRotateChores: true },
  { id: "gab", name: "Gab", isActive: true, email: "gab@example.com" },
  { id: "saiu", name: "Picasso", isActive: false, email: "ex@example.com", canRotateChores: true },
];

function kvFalso(semana) {
  const dados = new Map([["moradores:v1", JSON.stringify(MORADORES)]]);
  if (semana !== undefined) {
    dados.set("rodizio:v1", JSON.stringify({ week: semana, rotatedAt: "2026-10-16T03:00:00.000Z" }));
  }
  return {
    escritas: 0,
    async get(key, options) {
      const bruto = dados.get(key) ?? null;
      if (bruto === null) return null;
      return options?.type === "json" ? JSON.parse(bruto) : bruto;
    },
    async put(key, value) {
      this.escritas++;
      dados.set(key, value);
    },
  };
}

function req(headers, body) {
  return new Request("https://banco.exemplo.workers.dev/rodizio", {
    method: body ? "POST" : "GET",
    headers: { ...headers, ...(body ? { "content-type": "application/json" } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
}

const comoMorador = async (email) => ({ authorization: `Bearer ${await assinar({ email })}` });

test("qualquer morador lê a semana", async () => {
  const env = { TAREFAS: kvFalso(8), API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(req(await comoMorador("gab@example.com")), env, {});
  const corpo = await res.json();

  assert.equal(res.status, 200);
  assert.equal(corpo.week, 8);
});

test("sem nada gravado, a escala é a primeira", async () => {
  // A rep que ainda não rodou nenhuma vez não pode ver erro na tela de tarefas.
  const env = { TAREFAS: kvFalso(), API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const corpo = await (await worker.fetch(req(await comoMorador("gab@example.com")), env, {})).json();

  assert.equal(corpo.week, 0);
  assert.equal(corpo.rotatedAt, null);
});

test("quem tem a permissão vira a escala", async () => {
  const TAREFAS = kvFalso(8);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(req(await comoMorador("vk@example.com"), { delta: 1 }), env, {});
  const corpo = await res.json();

  assert.equal(res.status, 200);
  assert.equal(corpo.week, 9);
  assert.equal(TAREFAS.escritas, 1);
  assert.ok(corpo.rotatedAt, "a data de quando rodou precisa ficar gravada");
});

test("morador sem a permissão não vira", async () => {
  // O caso que a tela sozinha não resolveria: o botão não aparece para ele, mas a
  // chamada existe e qualquer um logado poderia fazê-la.
  const TAREFAS = kvFalso(8);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(req(await comoMorador("gab@example.com"), { delta: 1 }), env, {});

  assert.equal(res.status, 403);
  assert.equal(TAREFAS.escritas, 0);
});

test("ex-morador com a marca antiga não vira", async () => {
  // A permissão não sobrevive à saída da rep, mesmo que ninguém lembre de tirá-la.
  const TAREFAS = kvFalso(8);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(req(await comoMorador("ex@example.com"), { delta: 1 }), env, {});

  assert.equal(res.status, 403);
  assert.equal(TAREFAS.escritas, 0);
});

test("token sem email não vira", async () => {
  // Conta de autenticação sem email não casa com morador nenhum, e sem casar não há
  // permissão que valha.
  const TAREFAS = kvFalso(8);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };
  const semEmail = { authorization: `Bearer ${await assinar({})}` };

  const res = await worker.fetch(req(semEmail, { delta: 1 }), env, {});

  assert.equal(res.status, 403);
  assert.equal(TAREFAS.escritas, 0);
});

test("o administrador vira pela linha de comando", async () => {
  const TAREFAS = kvFalso(8);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(req({ "x-rep-token": API_TOKEN }, { delta: 1 }), env, {});

  assert.equal(res.status, 200);
  assert.equal((await res.json()).week, 9);
});

test("desfazer volta uma escala, e nunca abaixo da primeira", async () => {
  const env = { TAREFAS: kvFalso(1), API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const uma = await (await worker.fetch(req(await comoMorador("vk@example.com"), { delta: -1 }), env, {})).json();
  assert.equal(uma.week, 0);

  const outra = await (await worker.fetch(req(await comoMorador("vk@example.com"), { delta: -1 }), env, {})).json();
  assert.equal(outra.week, 0, "desfazer na primeira escala não pode ir para antes dela");
});

test("delta inventado é recusado", async () => {
  // Número absoluto abriria espaço para o app mandar uma semana velha e a escala andar
  // para trás sem ninguém pedir.
  const TAREFAS = kvFalso(8);
  const env = { TAREFAS, API_TOKEN, FIREBASE_PROJECT_ID: PROJECT_ID };

  const res = await worker.fetch(req(await comoMorador("vk@example.com"), { delta: 7 }), env, {});

  assert.equal(res.status, 400);
  assert.equal(TAREFAS.escritas, 0);
});
