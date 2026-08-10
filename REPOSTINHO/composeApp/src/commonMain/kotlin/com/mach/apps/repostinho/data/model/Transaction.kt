package com.mach.apps.repostinho.data.model

import kotlinx.serialization.Serializable
import kotlin.time.Clock


@Serializable
data class Transaction(
    val id: String = "",
    val description: String, // Campo 1 do seu Forms
    val type: TransactionType, // Campo 2 (Privado, Coletivo, etc)
    val payerId: String, // Campo 3 (ID do morador que pagou)
    /** Em centavos. Dinheiro em Double acumula resto ao longo dos lançamentos. */
    val totalValueCents: Long, // Campo 4
    val weights: Map<String, Double>, // IDs dos moradores e seus respectivos pesos (Campos 5 a 28)
    val timestamp: Long = Clock.System.now().epochSeconds,
    val isClosed: Boolean = false // Define se já entrou em um "fechamento"
)

@Serializable
enum class TransactionType {
    PRIVADO, COLETIVO, SAIDA, ENTRADA
}