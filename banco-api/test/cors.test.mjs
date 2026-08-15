/*
 * CORS é o que separa "o app nativo funciona" de "o PWA funciona".
 *
 * O preflight é o caso perigoso: ele chega sem credencial, e a verificação de token roda
 * antes do roteamento. Se ele levar 401, o navegador cancela a chamada real e o app web
 * fica sem dados — sem erro visível no servidor, porque a requisição de verdade nem sai.
 */
import { test } from "node:test";
import assert from "node:assert/strict";

import worker from "../src/index.js";

const ORIGEM = "https://repostinho.pages.dev";

function preflight(origin = ORIGEM) {
  return new Request("https://banco.exemplo.workers.dev/banco", {
    method: "OPTIONS",
    headers: {
      origin,
      "access-control-request-method": "GET",
      "access-control-request-headers": "authorization",
    },
  });
}

test("preflight passa sem credencial", async () => {
  const res = await worker.fetch(preflight(), {}, {});

  assert.equal(res.status, 204, "preflight não pode exigir token");
  assert.equal(res.headers.get("access-control-allow-origin"), ORIGEM);
  assert.match(res.headers.get("access-control-allow-headers"), /authorization/);
});

test("cabeçalho pedido pelo cliente é liberado", async () => {
  // O Coil manda `cache-control` ao buscar a foto do morador. Com lista fixa, o navegador
  // barrava a resposta inteira e o perfil ficava sem foto.
  const req = new Request("https://banco.exemplo.workers.dev/foto/vk", {
    method: "OPTIONS",
    headers: {
      origin: ORIGEM,
      "access-control-request-method": "GET",
      "access-control-request-headers": "authorization, cache-control",
    },
  });
  const res = await worker.fetch(req, {}, {});

  const permitidos = res.headers.get("access-control-allow-headers");
  assert.match(permitidos, /cache-control/);
  assert.match(permitidos, /authorization/);
});

test("origem fora da lista não recebe liberação", async () => {
  const env = { ALLOWED_ORIGINS: "https://repostinho.pages.dev" };
  const res = await worker.fetch(preflight("https://site-aleatorio.com"), env, {});

  assert.equal(res.headers.get("access-control-allow-origin"), null);
});

test("origem na lista recebe liberação", async () => {
  const env = { ALLOWED_ORIGINS: "https://outro.com, https://repostinho.pages.dev" };
  const res = await worker.fetch(preflight(), env, {});

  assert.equal(res.headers.get("access-control-allow-origin"), ORIGEM);
});

test("resposta de erro também carrega os cabeçalhos", async () => {
  // Sem CORS no 401, o navegador esconde o status e o app web não consegue nem
  // distinguir "não autorizado" de "servidor fora do ar".
  const req = new Request("https://banco.exemplo.workers.dev/banco", {
    method: "GET",
    headers: { origin: ORIGEM },
  });
  const res = await worker.fetch(req, {}, {});

  assert.equal(res.status, 401);
  assert.equal(res.headers.get("access-control-allow-origin"), ORIGEM);
});

test("requisição sem origem não ganha cabeçalho", async () => {
  // App nativo não manda Origin; devolver o cabeçalho ali seria ruído.
  const req = new Request("https://banco.exemplo.workers.dev/banco", { method: "GET" });
  const res = await worker.fetch(req, {}, {});

  assert.equal(res.headers.get("access-control-allow-origin"), null);
});
