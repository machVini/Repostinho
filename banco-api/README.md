# banco-api

Endpoint que converte a planilha do banco da rep (`Banco atual.xlsm`, no OneDrive) em
JSON, para o app consumir a cada abertura.

O app não lê a planilha direto porque `.xlsm` é um ZIP de XMLs, e o Kotlin/Native (alvo
iOS) não tem `inflate` nem biblioteca de xlsx. Em JavaScript o SheetJS resolve, então a
conversão fica aqui.

## Deploy

```bash
cd banco-api
npm install
wrangler secret put SHEET_URL   # link de download direto da planilha
wrangler secret put API_TOKEN   # qualquer string longa; a mesma vai no app
wrangler deploy
```

O Worker se chama `banco`, então a URL fica `https://banco.<subdominio>.workers.dev`.
Secrets são por Worker: **renomear o Worker exige configurá-los de novo**, porque para a
Cloudflare passa a ser outro Worker.

O `SHEET_URL` é o link de download direto do compartilhamento, no formato:

```
https://<tenant>-my.sharepoint.com/personal/<usuario>/_layouts/15/download.aspx?share=<token>
```

## Contrato

`GET /banco`, com o header `x-rep-token`. Responde:

```json
{
  "generatedAt": "2026-08-12T01:15:00.000Z",
  "balances":  [ { "name": "VK", "previousCents": -31269, "expensesCents": 76979,
                   "paymentsCents": 66788, "finalCents": -41460, "isFormer": false } ],
  "movements": [ { "id": "320", "description": "Aluguel", "type": "ENTRADA",
                   "payer": "Massa", "valueCents": 64900, "weights": {},
                   "totalWeight": 0 } ],
  "caixinha":  [ { "label": "Total (saldo real)", "initialCents": 879613,
                   "variationCents": 1394, "finalCents": 881007, "isTotal": true } ]
}
```

Os nomes dos campos são os das `data class` do app, então o JSON desserializa direto
nos modelos. Mudar um nome aqui quebra o app.

Respostas são cacheadas por 5 minutos na borda.

## Limites

O `API_TOKEN` é **obstáculo, não autenticação**: ele está no binário do app e pode ser
extraído por quem se dispuser. Ele impede que o endpoint fique aberto a quem topar com a
URL, e mantém o link do SharePoint fora do app. Proteger de verdade exigiria login por
morador — outro projeto.

## Se a planilha mudar de forma

As posições estão no topo do `src/index.js`: nomes das abas, faixas de linhas de saldos e
caixinha, e as colunas de participantes. Aba renomeada faz o endpoint responder 502 em vez
de devolver lista vazia — melhor falhar alto do que mostrar saldo zerado.
