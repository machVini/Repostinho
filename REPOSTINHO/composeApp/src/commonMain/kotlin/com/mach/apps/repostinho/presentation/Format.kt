package com.mach.apps.repostinho.presentation

import com.mach.apps.repostinho.data.model.RepEvent
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
 * Nomes de mês escritos à mão porque não há `kotlinx-datetime` no projeto e o
 * `java.time` não existe no alvo iOS.
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
 */
fun formatEventPeriod(event: RepEvent): String = when {
    !event.isMultiDay -> "${event.start.day} de ${monthName(event.start.month)}"

    event.start.month == event.end.month ->
        "${event.start.day} a ${event.end.day} de ${monthName(event.start.month)}"

    else -> "${event.start.day} de ${monthName(event.start.month)} a " +
        "${event.end.day} de ${monthName(event.end.month)}"
}
