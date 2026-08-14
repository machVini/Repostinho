# Publicar o PWA

O terceiro alvo do app: o mesmo Compose que roda no Android e no iOS, compilado para
WebAssembly e servido como Progressive Web App. Existe para os iPhones da rep não
dependerem dos US$ 99 anuais do programa da Apple.

O que segue é o passo a passo de uma publicação. Faça na ordem — o Firebase precisa
conhecer o domínio antes de o login funcionar, e o Worker precisa conhecer a origem antes
de o app conseguir buscar dados.

## 1. Registrar o app Web no Firebase

O Android e o iOS usam o SDK do GitLive, que **não publica artefato Wasm**. Na web o login
passa pelo SDK JavaScript, e ele precisa da configuração do projeto.

No console do Firebase: *Configurações do projeto → Seus aplicativos → adicionar app Web
(`</>`)*. Copie o bloco `firebaseConfig` que aparece.

```bash
cd REPOSTINHO/composeApp/src/wasmJsMain/resources
cp firebase-config.example.js firebase-config.js
# preencha firebase-config.js com os valores do console
```

`firebase-config.js` está no `.gitignore`. Essas chaves não são segredo — o Firebase expõe
a `apiKey` no navegador de propósito, e quem protege os dados são as regras do projeto e o
token exigido pelo `banco-api` — mas não têm por que morar no repositório.

## 2. Gerar o build

```bash
cd REPOSTINHO
./gradlew :composeApp:wasmJsBrowserDistribution
```

Sai em `composeApp/build/dist/wasmJs/productionExecutable`: dois `.wasm`, o `composeApp.js`,
o `index.html`, os ícones, o `manifest.json` e o `service-worker.js`.

São ~15 MB em disco e **~4,7 MB comprimidos** na rede. O grosso é o Skia, motor gráfico que
o Compose carrega para desenhar — é custo fixo, não cresce com o tamanho do app.

> **Se você editar qualquer arquivo de `wasmJsMain`**, apague os caches antes de rebuildar.
> O Kotlin 2.3.20 tem um bug na compilação incremental do Wasm e derruba o compilador com
> `ArrayIndexOutOfBoundsException`. Não afeta o app publicado, só o ciclo de
> desenvolvimento.

## 3. Publicar no Firebase Hosting

Mesma conta do login, e os domínios que o Firebase gera (`*.web.app` e `*.firebaseapp.com`)
**já vêm autorizados no Authentication** — o que dispensa o passo mais fácil de esquecer,
autorizar o domínio na mão.

O [`firebase.json`](../firebase.json) na raiz já aponta para a pasta do build e define os
cabeçalhos de cache. Da raiz do repositório:

```bash
npm install -g firebase-tools     # só na primeira vez
firebase login
firebase use --add                # escolha o projeto do Repostinho
firebase deploy --only hosting
```

A URL sai como `https://<seu-projeto>.web.app`.

Sobre os cabeçalhos configurados: os `.wasm` têm o hash do conteúdo no nome, então vão como
`immutable` por um ano — build novo gera nome novo. Já o `index.html`, o `composeApp.js` e o
`service-worker.js` têm nome fixo e vão com `must-revalidate`. Se fossem cacheados junto, a
primeira atualização publicada nunca chegaria em quem já tivesse aberto o app.

Não há regra de *rewrite* de propósito: o app não tem rotas de URL, e um catch-all faria
arquivo faltando responder `index.html` com status 200 — o service worker acabaria
guardando HTML no lugar de um ícone.

## 4. Liberar a origem no Worker

O `banco-api` valida o token **antes** de rotear, e um preflight CORS chega sem credencial
nenhuma. O tratamento já está no código, mas a origem precisa ser autorizada:

```bash
cd banco-api
wrangler secret put ALLOWED_ORIGINS
# cole as duas, separadas por vírgula:
# https://seu-projeto.web.app,https://seu-projeto.firebaseapp.com
wrangler deploy
```

O Firebase serve o site nos dois domínios, e o navegador manda como origem exatamente o que
está na barra de endereço — então vale liberar ambos, ou o app funciona por um e falha pelo
outro.

Deixar `ALLOWED_ORIGINS` em branco libera qualquer origem. Serve para desenvolvimento, mas
em produção vale restringir: com token viajando no cabeçalho, um `*` deixa qualquer site
pedir dados em nome de quem estiver logado.

## 5. Instalar no celular

Não há loja. Cada morador abre a URL e adiciona à tela de início:

- **iPhone**: Safari → botão Compartilhar → *Adicionar à Tela de Início*
- **Android**: Chrome → menu → *Instalar aplicativo*

Precisa ser o **Safari** no iPhone. Outros navegadores no iOS usam o motor do Safari, mas
não oferecem a instalação.

## O que esperar

**Piso de iOS 18.2.** O Compose para Web precisa de WasmGC, que o Safari só passou a
suportar nessa versão. iPhone mais antigo não abre.

**O Safari apaga o armazenamento** de sites pouco usados depois de alguns dias. A sessão
mora ali, então quem ficar semanas sem abrir vai logar de novo. Nenhum dado se perde: tudo
é cache do `banco-api`.

**A tela é um canvas.** Não dá para selecionar texto e leitor de tela funciona mal. É o que
se troca por reaproveitar a UI inteira em vez de reescrevê-la em HTML.

**A primeira abertura baixa os ~4,7 MB.** Depois disso o service worker serve o binário do
cache, e abrir fica instantâneo.
