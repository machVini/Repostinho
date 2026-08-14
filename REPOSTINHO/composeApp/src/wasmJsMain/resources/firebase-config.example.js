/*
 * Copie para firebase-config.js e preencha com os valores do SEU projeto.
 *
 * Onde achar: console do Firebase -> Configurações do projeto -> Seus aplicativos ->
 * adicionar app Web (</>). O bloco `firebaseConfig` que ele mostra é exatamente isto.
 *
 * O firebase-config.js está no .gitignore. Estas chaves não são segredo — o Firebase
 * expõe a apiKey no navegador de propósito, e quem protege os dados são as regras do
 * projeto e o token exigido pelo banco-api — mas mantê-las fora do repositório evita que
 * o projeto de vocês vire alvo de tráfego alheio.
 *
 * Não esqueça: em Authentication -> Settings -> Authorized domains, adicione o domínio
 * onde o PWA vai rodar. Sem isso o login é recusado mesmo com a config correta.
 */
globalThis.repostinhoFirebaseConfig = {
  apiKey: "SUA_API_KEY",
  authDomain: "seu-projeto.firebaseapp.com",
  projectId: "seu-projeto",
  storageBucket: "seu-projeto.appspot.com",
  messagingSenderId: "000000000000",
  appId: "1:000000000000:web:0000000000000000000000",
};
