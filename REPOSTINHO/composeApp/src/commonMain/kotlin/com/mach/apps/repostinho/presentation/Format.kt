package com.mach.apps.repostinho.presentation

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
