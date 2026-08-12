/*
 * Roda a conversão contra um .xlsm de verdade e confere os números.
 *
 * Uso:  node test/convert.test.mjs <caminho-do-xlsm>
 *
 * Os valores esperados vieram da planilha em 11/08/2026; se ela mudou, atualize-os ou
 * passe --smoke para só checar a forma.
 */
import * as XLSX from "xlsx";
import { readFileSync } from "node:fs";
import { readBalances, readMovements, readCaixinha } from "../src/index.js";

const path = process.argv[2];
if (!path) {
  console.error("uso: node test/convert.test.mjs <caminho-do-xlsm>");
  process.exit(2);
}

const wb = XLSX.read(readFileSync(path), { type: "buffer" });
const balances = [
  ...readBalances(wb.Sheets["Saldos_pessoas"], [2, 18], false),
  ...readBalances(wb.Sheets["Saldos_pessoas"], [23, 30], true),
];
const movements = readMovements(wb.Sheets["Movimentações"]);
const caixinha = readCaixinha(wb.Sheets["Saldos_caixinha"]);

let failures = 0;

function check(label, actual, expected) {
  const ok = actual === expected;
  if (!ok) failures++;
  console.log(`${ok ? "ok  " : "FALHA"} ${label}: ${actual}${ok ? "" : ` (esperado ${expected})`}`);
}

check("moradores + ex-moradores", balances.length, 25);
check("lançamentos", movements.length, 47);
check("linhas da caixinha", caixinha.length, 7);

const vk = balances.find((b) => b.name === "VK");
check("saldo final do VK", vk?.finalCents, -41460);
check("saldo anterior do VK", vk?.previousCents, -31269);

const total = caixinha.find((c) => c.isTotal);
check("total da caixinha", total?.finalCents, 881007);
check("rótulo do total", total?.label, "Total (saldo real)");

const former = balances.filter((b) => b.isFormer);
check("ex-moradores", former.length, 8);

// Um lançamento coletivo com pesos fracionados: é onde o arredondamento apareceria.
const aluguel = movements.find((m) => m.description.startsWith("Aluguel junho"));
check("aluguel de junho em centavos", aluguel?.valueCents, 704888);
check("peso do Lameu no aluguel", aluguel?.weights["Lameu"], 0.8);
check("total de pesos do aluguel", aluguel?.totalWeight, 14.24);

// Entradas não têm rateio.
const entrada = movements.find((m) => m.type === "ENTRADA");
check("entrada sem participantes", Object.keys(entrada?.weights ?? {}).length, 0);

console.log(failures === 0 ? "\nTodos os testes passaram." : `\n${failures} falha(s).`);
process.exit(failures === 0 ? 0 : 1);
