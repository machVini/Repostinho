/*
 * Service worker do Repostinho.
 *
 * Existe por dois motivos: sem ele o iOS não oferece "Adicionar à Tela de Início" como
 * app, e sem ele os ~4,7 MB de Wasm seriam rebaixados a cada abertura.
 *
 * Estratégia: o binário (Wasm, JS, ícones) é imutável e vai de cache primeiro; o
 * `banco-api` nunca é cacheado aqui, porque saldo velho servido como novo é pior do que
 * uma tela de erro — e o app já guarda a última resposta boa por conta própria.
 */
const CACHE = "repostinho-v1";

const SHELL = [
  "./",
  "./index.html",
  "./manifest.json",
  "./icon-192.png",
  "./icon-512.png",
  "./apple-touch-icon.png",
];

self.addEventListener("install", (event) => {
  // `addAll` falha inteiro se um arquivo faltar; o shell é curto e estável de propósito.
  event.waitUntil(
    caches.open(CACHE).then((cache) => cache.addAll(SHELL)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((nomes) => Promise.all(nomes.filter((n) => n !== CACHE).map((n) => caches.delete(n))))
      .then(() => self.clients.claim())
  );
});

function guardar(req, res) {
  if (res.ok && res.type === "basic") {
    const copia = res.clone();
    caches.open(CACHE).then((cache) => cache.put(req, copia));
  }
  return res;
}

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;

  const url = new URL(req.url);

  // Só cuidamos do que é servido junto com o app. Chamadas ao banco-api e ao Firebase
  // passam direto para a rede.
  if (url.origin !== self.location.origin) return;

  /*
   * Duas políticas, e a distinção não é detalhe.
   *
   * Os `.wasm` têm o hash do conteúdo no nome: build novo gera nome novo, então servir do
   * cache nunca devolve versão velha — e são eles os ~4,7 MB que não se quer rebaixar.
   *
   * O resto (index.html, composeApp.js) tem nome fixo. Cache primeiro ali significaria que
   * a primeira atualização publicada jamais chegaria em quem já abriu o app uma vez: o
   * nome do cache é constante, e o `activate` só apaga caches de nome diferente.
   */
  if (url.pathname.endsWith(".wasm")) {
    event.respondWith(
      caches.match(req).then((hit) => hit || fetch(req).then((res) => guardar(req, res)))
    );
    return;
  }

  // Rede primeiro, cache como rede reserva: atualiza sempre que dá, e abre offline.
  event.respondWith(
    fetch(req)
      .then((res) => guardar(req, res))
      .catch(() => caches.match(req).then((hit) => hit || Promise.reject(new Error("offline"))))
  );
});
