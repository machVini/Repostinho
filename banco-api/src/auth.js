/*
 * Verificação do token do Firebase.
 *
 * Existe para o app parar de carregar o segredo compartilhado dentro do binário. Com o
 * `x-rep-token` embutido, qualquer um que extraísse o APK lia saldos, emails e fotos da
 * rep inteira — e distribuir o app para quinze pessoas espalha esse segredo por quinze
 * aparelhos. Agora o app manda o token do morador logado, que só existe para quem tem
 * conta, e o segredo compartilhado fica sendo chave de administração, na mão de uma
 * pessoa só.
 *
 * O Firebase assina os tokens com RS256 e publica as chaves públicas em JWK, que é o
 * formato que o WebCrypto importa direto — o outro endpoint devolve certificado X.509 e
 * exigiria parsear ASN.1 aqui dentro.
 */

const JWKS_URL =
  "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

/**
 * Cache das chaves públicas entre requisições.
 *
 * O Worker fica vivo entre chamadas, então isso evita uma ida ao Google por request. O
 * Google rotaciona as chaves e manda o prazo no `cache-control`; respeitá-lo é o que
 * impede o token novo de ser recusado por causa de uma chave velha guardada aqui.
 */
let chavesCache = { porKid: null, expiraEm: 0 };

async function chavesDoGoogle() {
  const agora = Date.now();
  if (chavesCache.porKid && agora < chavesCache.expiraEm) return chavesCache.porKid;

  const resposta = await fetch(JWKS_URL);
  if (!resposta.ok) throw new Error(`JWKS respondeu ${resposta.status}`);

  const { keys } = await resposta.json();
  const porKid = {};
  for (const chave of keys ?? []) porKid[chave.kid] = chave;

  const maxAge = /max-age=(\d+)/.exec(resposta.headers.get("cache-control") ?? "");
  // Uma hora de piso se o cabeçalho não vier: sem prazo nenhum, cada request buscaria de
  // novo; prazo longo demais recusaria tokens legítimos depois de uma rotação.
  const segundos = maxAge ? Number(maxAge[1]) : 3600;

  chavesCache = { porKid, expiraEm: agora + segundos * 1000 };
  return porKid;
}

function base64UrlParaBytes(texto) {
  const base64 = texto.replace(/-/g, "+").replace(/_/g, "/");
  const preenchido = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
  const binario = atob(preenchido);
  return Uint8Array.from(binario, (c) => c.charCodeAt(0));
}

function base64UrlParaJson(texto) {
  return JSON.parse(new TextDecoder().decode(base64UrlParaBytes(texto)));
}

/**
 * Devolve o `uid` do morador quando o token é válido, ou `null`.
 *
 * Recusa em silêncio de propósito: distinguir "token expirado" de "assinatura errada" na
 * resposta só ajudaria quem está tentando forjar um.
 */
export async function verificarTokenFirebase(token, projectId) {
  if (!token || !projectId) return null;

  const partes = token.split(".");
  if (partes.length !== 3) return null;

  let cabecalho;
  let corpo;
  try {
    cabecalho = base64UrlParaJson(partes[0]);
    corpo = base64UrlParaJson(partes[1]);
  } catch {
    return null;
  }

  if (cabecalho.alg !== "RS256" || !cabecalho.kid) return null;

  // Conferido antes da assinatura por ser barato: token de outro projeto Firebase é
  // legitimamente assinado pelo Google e passaria na verificação criptográfica.
  const agora = Math.floor(Date.now() / 1000);
  if (corpo.aud !== projectId) return null;
  if (corpo.iss !== `https://securetoken.google.com/${projectId}`) return null;
  if (typeof corpo.sub !== "string" || corpo.sub.length === 0) return null;
  if (typeof corpo.exp !== "number" || corpo.exp <= agora) return null;
  if (typeof corpo.iat !== "number" || corpo.iat > agora + 300) return null;

  let chave;
  try {
    const porKid = await chavesDoGoogle();
    const jwk = porKid[cabecalho.kid];
    if (!jwk) return null;

    chave = await crypto.subtle.importKey(
      "jwk",
      jwk,
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["verify"]
    );
  } catch {
    // Google fora do ar não pode virar "token inválido" silencioso lá em cima, mas
    // também não há o que liberar sem conseguir verificar.
    return null;
  }

  const assinado = new TextEncoder().encode(`${partes[0]}.${partes[1]}`);
  const assinatura = base64UrlParaBytes(partes[2]);

  const ok = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5",
    chave,
    assinatura,
    assinado
  );
  return ok ? corpo.sub : null;
}

/** O `Authorization: Bearer <token>`, se vier. */
export function tokenDoCabecalho(request) {
  const bruto = request.headers.get("authorization") ?? "";
  return bruto.startsWith("Bearer ") ? bruto.slice("Bearer ".length).trim() : null;
}
