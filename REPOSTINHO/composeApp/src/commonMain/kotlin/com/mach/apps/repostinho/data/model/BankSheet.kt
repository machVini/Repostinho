package com.mach.apps.repostinho.data.model

import kotlinx.serialization.Serializable

/*
 * Os três modelos abaixo espelham as abas da planilha do banco da rep, uma para uma.
 *
 * Eles não recalculam nada: a planilha é a fonte da verdade, e o app mostra o que ela já
 * fechou. Recalcular abriria espaço para o app e a planilha discordarem em centavos, e aí
 * ninguém sabe qual dos dois está certo.
 */

/** Uma linha da aba `Saldos_pessoas`. */
@Serializable
data class MemberBalance(
    val name: String,
    val previousCents: Long,
    val expensesCents: Long,
    val paymentsCents: Long,
    /** Negativo = deve ao banco, como na planilha. */
    val finalCents: Long,
    /** A planilha separa ex-moradores e agregados numa segunda tabela. */
    val isFormer: Boolean = false
)

/** Uma linha da aba `Movimentações`. */
@Serializable
data class Movement(
    val id: String,
    val description: String,
    val type: TransactionType,
    /**
     * Nem sempre é morador: a planilha também lança em nome dos caixas
     * (`Caix. Déb/PIX`, `Ext. (PIX)`), então aqui é texto livre, não um id.
     */
    val payer: String,
    val valueCents: Long,
    /** Nome do participante para o peso dele no rateio. Vazio nas entradas. */
    val weights: Map<String, Double> = emptyMap(),
    val totalWeight: Double = 0.0
) {
    val participantCount: Int get() = weights.size
}

/** Uma linha da aba `Saldos_caixinha`. */
@Serializable
data class CaixinhaLine(
    val label: String,
    val initialCents: Long,
    val variationCents: Long,
    val finalCents: Long,
    /** O "Total (saldo real)" fecha a tabela e aparece destacado. */
    val isTotal: Boolean = false
)
