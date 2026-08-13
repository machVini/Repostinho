package com.mach.apps.repostinho.data.model

import kotlin.math.floor
import kotlinx.serialization.Serializable

/*
 * Os três modelos abaixo espelham as abas da planilha do banco da rep, uma para uma.
 *
 * Eles não recalculam nada: a planilha é a fonte da verdade, e o app mostra o que ela já
 * fechou. Recalcular abriria espaço para o app e a planilha discordarem em centavos, e aí
 * ninguém sabe qual dos dois está certo.
 */

/**
 * A coluna "Tipo" da planilha.
 *
 * Os nomes das constantes são o que trafega no JSON do `banco-api`; renomear uma exige
 * mexer no Worker junto.
 */
@Serializable
enum class MovementType {
    PRIVADO, COLETIVO, SAIDA, ENTRADA
}

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
    val type: MovementType,
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

    /** Pagou ou entrou no rateio — as duas formas de um lançamento dizer respeito a alguém. */
    fun involves(name: String): Boolean = payer == name || weights.containsKey(name)

    /**
     * Quanto cada participante deve neste lançamento, em centavos.
     *
     * Reparte pelo maior resto: distribui a parte inteira de cada um e depois entrega os
     * centavos que sobraram a quem ficou com o maior resto. Arredondar cada parte
     * isoladamente deixaria a soma um ou dois centavos longe do valor do lançamento — e
     * numa tela onde o total está logo acima, isso vira pergunta.
     *
     * Funciona com valor negativo (a planilha tem estornos): o resto fracionário continua
     * entre 0 e 1 mesmo quando o piso é mais negativo que o valor exato.
     */
    fun sharesInCents(): Map<String, Long> {
        if (weights.isEmpty() || totalWeight <= 0.0) return emptyMap()

        val exact = weights.mapValues { (_, weight) -> valueCents * weight / totalWeight }
        val shares = exact.mapValues { (_, value) -> floor(value).toLong() }.toMutableMap()

        var leftover = valueCents - shares.values.sum()
        // Quem tem o maior resto recebe o centavo antes de quem tem o menor.
        val byRemainder = exact.entries.sortedByDescending { it.value - floor(it.value) }

        for (entry in byRemainder) {
            if (leftover <= 0L) break
            shares[entry.key] = shares.getValue(entry.key) + 1L
            leftover--
        }
        return shares
    }
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
