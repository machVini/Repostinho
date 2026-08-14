package com.mach.apps.repostinho.presentation

import com.mach.apps.repostinho.data.model.EventOccurrence
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Dinheiro no app é sempre `Long` de centavos. `Double` acumula resto ao longo de somas
 * de lançamentos e vira diferença visível na tela.
 */
fun formatBrl(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val absolute = abs(cents)
    val reais = groupThousands(absolute / 100)
    val centavos = (absolute % 100).toString().padStart(2, '0')
    return "${sign}R$ $reais,$centavos"
}

private fun groupThousands(value: Long): String {
    val digits = value.toString()
    return buildString {
        digits.forEachIndexed { index, char ->
            if (index > 0 && (digits.length - index) % 3 == 0) append('.')
            append(char)
        }
    }
}

/**
 * Lê o que o morador digitou e devolve centavos.
 *
 * Aceita "1234", "12,50", "12.50" e "1.234,56". Quando vírgula e ponto aparecem juntos,
 * o ponto é separador de milhar; sozinho, é decimal.
 */
fun parseBrlToCents(input: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    val normalized = when {
        trimmed.contains(',') && trimmed.contains('.') ->
            trimmed.replace(".", "").replace(',', '.')
        trimmed.contains(',') -> trimmed.replace(',', '.')
        else -> trimmed
    }

    val value = normalized.toDoubleOrNull() ?: return null
    return (value * 100).roundToLong()
}

/*
 * Acentos que aparecem em português, dobrados para a letra sem acento.
 *
 * Escrito à mão porque o Kotlin comum não tem `Normalizer` e o alvo iOS não tem
 * `java.text` — é a mesma razão de os nomes de mês estarem logo abaixo.
 */
private val SEM_ACENTO = mapOf(
    'á' to 'a', 'à' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a',
    'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
    'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
    'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
    'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
    'ç' to 'c'
)

/**
 * Ordem alfabética de gente, ignorando acento e caixa.
 *
 * `sorted()` compara code points, e isso não é a ordem que alguém procura numa lista:
 * "LL" viria antes de "Lameu" só por ser maiúsculo, e um nome acentuado cairia depois
 * do Z se o acento estivesse na primeira letra.
 */
fun List<String>.sortedByNome(): List<String> = sortedBy { name ->
    buildString { name.lowercase().forEach { append(SEM_ACENTO[it] ?: it) } }
}

/**
 * Lê um peso digitado pelo morador. Aceita "1", "0,5" e "0.5".
 *
 * A vírgula é o que o teclado brasileiro oferece, mas o formulário do banco só entende
 * ponto — a conversão tem que acontecer aqui, não na cabeça de quem digita.
 */
fun parseWeight(input: String): Double? {
    val value = input.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return if (value > 0.0) value else null
}

/**
 * Peso de rateio: "1", "0,88", "14,24".
 *
 * Sai sem casas quando é inteiro, porque a maioria dos lançamentos usa peso 1 e "1,00"
 * em toda linha vira ruído.
 */
fun formatWeight(weight: Double): String {
    val rounded = (weight * 100).roundToLong()
    if (rounded % 100L == 0L) return (rounded / 100L).toString()
    val decimals = (rounded % 100L).toString().padStart(2, '0').trimEnd('0')
    return "${rounded / 100L},$decimals"
}

/*
 * Nomes de mês escritos à mão: o `kotlinx-datetime` entrou no projeto pelo rodízio, mas
 * ele não traz nomes localizados, e o `java.time` não existe no alvo iOS.
 */
private val MONTHS = listOf(
    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
)

/** Índice do mês vem 1-based; fora da faixa devolve vazio em vez de estourar. */
private fun monthName(month: Int): String = MONTHS.getOrElse(month - 1) { "" }

fun monthAbbrev(month: Int): String = monthName(month).take(3).uppercase()

/**
 * "15 de agosto" para um dia só, "19 a 22 de novembro" quando o evento se estende dentro
 * do mesmo mês, e "30 de novembro a 2 de dezembro" quando atravessa a virada.
 *
 * Recebe a ocorrência, e não o evento: um recorrente é um cadastro só, mas cada volta tem
 * a sua data, e é a data da vez que a linha precisa mostrar.
 */
fun formatOccurrencePeriod(occurrence: EventOccurrence): String = when {
    !occurrence.isMultiDay ->
        "${occurrence.start.day} de ${monthName(occurrence.start.month)}"

    occurrence.start.month == occurrence.end.month ->
        "${occurrence.start.day} a ${occurrence.end.day} de " +
            monthName(occurrence.start.month)

    else -> "${occurrence.start.day} de ${monthName(occurrence.start.month)} a " +
        "${occurrence.end.day} de ${monthName(occurrence.end.month)}"
}
