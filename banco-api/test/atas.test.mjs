/*
 * Confere a ordenação das atas, que sai do nome do arquivo e não da data de criação.
 *
 * Uso:  node test/atas.test.mjs
 */
import { orderAtas } from "../src/index.js";

let failures = 0;

function check(label, actual, expected) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected);
  if (!ok) failures++;
  console.log(`${ok ? "ok  " : "FALHA"} ${label}${ok ? "" : `: ${actual} != ${expected}`}`);
}

const names = (files) => orderAtas(files, 3).map((f) => f.name);

// Caso real da pasta: os documentos foram criados fora da ordem das reuniões.
check(
  "ordena pela data no nome, não pela criação",
  names([
    { name: "23/06/2026", createdTime: "2026-08-11T10:00:00Z" },
    { name: "07/07/2026", createdTime: "2026-08-11T09:00:00Z" },
    { name: "11/08/2026", createdTime: "2026-08-11T08:00:00Z" },
  ]),
  ["11/08/2026", "07/07/2026", "23/06/2026"]
);

check(
  "corta em três",
  names([
    { name: "01/01/2026", createdTime: "2026-01-01T00:00:00Z" },
    { name: "02/02/2026", createdTime: "2026-02-01T00:00:00Z" },
    { name: "03/03/2026", createdTime: "2026-03-01T00:00:00Z" },
    { name: "04/04/2026", createdTime: "2026-04-01T00:00:00Z" },
  ]),
  ["04/04/2026", "03/03/2026", "02/02/2026"]
);

// Vira o ano: comparar texto colocaria 12/2025 na frente de 01/2026.
check(
  "atravessa a virada do ano",
  names([
    { name: "15/12/2025", createdTime: "2025-12-15T00:00:00Z" },
    { name: "10/01/2026", createdTime: "2026-01-10T00:00:00Z" },
  ]),
  ["10/01/2026", "15/12/2025"]
);

check(
  "nome que não é data cai para a data de criação",
  names([
    { name: "Ata da mudança", createdTime: "2026-09-01T00:00:00Z" },
    { name: "11/08/2026", createdTime: "2026-08-11T00:00:00Z" },
  ]),
  ["Ata da mudança", "11/08/2026"]
);

// Os formatos que as atas podem ter na prática.
check(
  "data no meio do nome",
  names([
    { name: "Ata Repostinho 18/08/2026", createdTime: "2020-01-01T00:00:00Z" },
    { name: "Ata Repostinho 04/08/2026", createdTime: "2020-01-01T00:00:00Z" },
  ]),
  ["Ata Repostinho 18/08/2026", "Ata Repostinho 04/08/2026"]
);

check(
  "data por extenso, com e sem acento",
  names([
    { name: "18 de agosto de 2026", createdTime: "2020-01-01T00:00:00Z" },
    { name: "3 de marco de 2026", createdTime: "2020-01-01T00:00:00Z" },
    { name: "10 de março de 2026", createdTime: "2020-01-01T00:00:00Z" },
  ]),
  ["18 de agosto de 2026", "10 de março de 2026", "3 de marco de 2026"]
);

check(
  "formatos misturados na mesma pasta",
  names([
    { name: "23/06/2026", createdTime: "2020-01-01T00:00:00Z" },
    { name: "Ata Repostinho 11/08/2026", createdTime: "2020-01-01T00:00:00Z" },
    { name: "7 de julho de 2026", createdTime: "2020-01-01T00:00:00Z" },
  ]),
  ["Ata Repostinho 11/08/2026", "7 de julho de 2026", "23/06/2026"]
);

check(
  "dia e mês com um dígito",
  names([
    { name: "Ata 1/9/2026", createdTime: "2020-01-01T00:00:00Z" },
    { name: "Ata 9/1/2026", createdTime: "2020-01-01T00:00:00Z" },
  ]),
  ["Ata 1/9/2026", "Ata 9/1/2026"]
);

check(
  "formato ISO no nome",
  names([
    { name: "2026-08-18 ata", createdTime: "2020-01-01T00:00:00Z" },
    { name: "2026-07-07 ata", createdTime: "2020-01-01T00:00:00Z" },
  ]),
  ["2026-08-18 ata", "2026-07-07 ata"]
);

check("pasta vazia", names([]), []);

console.log(failures === 0 ? "\nTodos os testes passaram." : `\n${failures} falha(s).`);
process.exit(failures === 0 ? 0 : 1);
