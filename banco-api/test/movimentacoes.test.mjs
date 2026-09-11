/*
 * O rateio que sai da planilha.
 *
 * O bug que originou estes testes: a coluna "Total de pesos" fica no meio das colunas de
 * gente, e entrou no rateio como se fosse um morador. Junto com isso, a célula lida como
 * total havia virado outra coisa quando as colunas andaram — e o app passou a mostrar,
 * para cada pessoa, o peso convertido em reais.
 *
 * Uso:  node --test test/movimentacoes.test.mjs
 */
import { test } from "node:test";
import assert from "node:assert/strict";

import { readMovements } from "../src/index.js";

/** Monta uma planilha de mentira no formato que o SheetJS entrega. */
function planilha(cabecalhos, linhas) {
  const sheet = { "!ref": `A1:AE${linhas.length + 1}` };
  const por = (linha, coluna, valor) => {
    if (valor === undefined) return;
    const letra = (c) =>
      c <= 26
        ? String.fromCharCode(64 + c)
        : String.fromCharCode(64 + Math.floor((c - 1) / 26)) +
          String.fromCharCode(65 + ((c - 1) % 26));
    sheet[`${letra(coluna)}${linha}`] = { v: valor };
  };

  // Cabeçalho das colunas de pessoas, a partir de F (6).
  cabecalhos.forEach((nome, i) => por(1, 6 + i, nome));

  linhas.forEach((l, i) => {
    const linha = i + 2;
    por(linha, 1, l.id);
    por(linha, 2, l.description);
    por(linha, 3, l.type);
    por(linha, 4, l.payer);
    por(linha, 5, l.value);
    (l.pesos ?? []).forEach((p, j) => por(linha, 6 + j, p));
    por(linha, 31, l.coluna31);
  });
  return sheet;
}

test("a coluna de total não vira morador", async () => {
  const sheet = planilha(
    ["Lameu", "LL", "Total de pesos"],
    [{ id: 1, description: "Samba", type: "PRIVADO", payer: "LL", value: 48, pesos: [0, 1, 1], coluna31: 48 }]
  );

  const [m] = readMovements(sheet);

  assert.deepEqual(Object.keys(m.weights), ["LL"]);
  assert.equal(m.weights["Total de pesos"], undefined);
});

test("o total é a soma dos pesos enviados", async () => {
  // A célula da coluna 31 era lida como total e hoje traz o valor por peso; somar o que
  // vai junto é o que faz as partes fecharem no valor do lançamento.
  const sheet = planilha(
    ["Lameu", "LL", "Du", "Total de pesos"],
    [{ id: 2, description: "Mercado", type: "COLETIVO", payer: "Du", value: 90, pesos: [1, 1, 1, 3], coluna31: 30 }]
  );

  const [m] = readMovements(sheet);

  assert.equal(m.totalWeight, 3);
  assert.equal(m.valueCents, 9000);
});

test("peso fracionado não vaza resto de ponto flutuante", async () => {
  // Aluguel rateado por tipo de quarto: 0.85 + 1.15 + 1 dá 3.0000000000000004 na conta
  // crua, e esse número iria inteiro para a tela.
  const sheet = planilha(
    ["Lameu", "LL", "Du", "Total de pesos"],
    [{ id: 3, description: "Aluguel", type: "COLETIVO", payer: "Caix. Déb/PIX", value: 300, pesos: [0.85, 1.15, 1, 3] }]
  );

  const [m] = readMovements(sheet);

  assert.equal(m.totalWeight, 3);
});

test("lançamento sem rateio fica com total zero", async () => {
  // Entrada e saída não têm participante; dividir por zero não pode virar NaN na tela.
  const sheet = planilha(
    ["Lameu", "Total de pesos"],
    [{ id: 4, description: "Reforço", type: "ENTRADA", payer: "Ext. (PIX)", value: 400 }]
  );

  const [m] = readMovements(sheet);

  assert.deepEqual(m.weights, {});
  assert.equal(m.totalWeight, 0);
});
