/*
 * Tokens do Firebase assinados aqui dentro, para o teste exercitar o caminho real.
 *
 * Sem isto, `verificarTokenFirebase` devolveria `null` e os testes de permissão passariam
 * pelo motivo errado: recusando todo mundo, inclusive quem deveria entrar.
 */
import { webcrypto as crypto } from "node:crypto";

const JWKS_URL =
  "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

function base64Url(bytes) {
  return Buffer.from(bytes).toString("base64url");
}

/**
 * Gera um par de chaves, serve o JWKS por um `fetch` trocado e devolve um assinador.
 *
 * Chame `restaurar()` no fim do arquivo de teste: o `fetch` é global, e deixá-lo trocado
 * vaza para qualquer teste que rode depois.
 */
export async function tokensDeTeste(projectId, kid = "chave-de-teste") {
  const par = await crypto.subtle.generateKey(
    {
      name: "RSASSA-PKCS1-v1_5",
      modulusLength: 2048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: "SHA-256",
    },
    true,
    ["sign", "verify"]
  );

  const jwk = await crypto.subtle.exportKey("jwk", par.publicKey);
  const fetchOriginal = globalThis.fetch;
  globalThis.fetch = async (url) => {
    if (String(url) !== JWKS_URL) return fetchOriginal(url);
    return new Response(
      JSON.stringify({ keys: [{ ...jwk, kid, alg: "RS256", use: "sig" }] }),
      { headers: { "content-type": "application/json", "cache-control": "max-age=3600" } }
    );
  };

  const assinar = async (extra = {}) => {
    const agora = Math.floor(Date.now() / 1000);
    const corpo = {
      aud: projectId,
      iss: `https://securetoken.google.com/${projectId}`,
      sub: "uid-de-teste",
      iat: agora,
      exp: agora + 3600,
      ...extra,
    };
    const cabecalho = base64Url(JSON.stringify({ alg: "RS256", kid, typ: "JWT" }));
    const carga = base64Url(JSON.stringify(corpo));
    const assinatura = await crypto.subtle.sign(
      "RSASSA-PKCS1-v1_5",
      par.privateKey,
      new TextEncoder().encode(`${cabecalho}.${carga}`)
    );
    return `${cabecalho}.${carga}.${base64Url(new Uint8Array(assinatura))}`;
  };

  return { assinar, restaurar: () => (globalThis.fetch = fetchOriginal) };
}
