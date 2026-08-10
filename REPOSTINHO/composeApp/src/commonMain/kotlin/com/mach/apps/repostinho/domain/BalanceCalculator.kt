package com.mach.apps.repostinho.domain

import com.mach.apps.repostinho.data.model.BankSettings
import com.mach.apps.repostinho.data.model.Resident
import com.mach.apps.repostinho.data.model.Transaction
import kotlin.math.floor

/**
 * Todo o dinheiro aqui é `Long` de centavos. Os pesos seguem `Double`, porque são razões
 * (1,10 / 0,88) e não valores — mas nenhuma divisão sai daqui sem virar centavo inteiro.
 */
class BalanceCalculator {

    /**
     * Divide [totalCents] entre [weights] sem perder nem criar centavos.
     *
     * Cada um recebe o piso da sua parte e a sobra é distribuída um centavo por vez, começando
     * por quem ficou com a maior fração truncada. Empate desempata pelo id, para o resultado
     * não depender da ordem em que o mapa foi montado.
     */
    fun distribute(totalCents: Long, weights: Map<String, Double>): Map<String, Long> {
        val positive = weights.filterValues { it > 0.0 }
        val totalWeight = positive.values.sum()

        if (positive.isEmpty() || totalWeight <= 0.0 || totalCents <= 0L) {
            return positive.mapValues { 0L }
        }

        val exact = positive.mapValues { (_, weight) -> totalCents * weight / totalWeight }
        val shares = exact.mapValues { (_, value) -> floor(value).toLong() }.toMutableMap()

        // A sobra é sempre menor que a quantidade de pessoas, então uma passada resolve.
        var remainder = totalCents - shares.values.sum()
        val byLargestFraction = exact.keys.sortedWith(
            compareByDescending<String> { exact.getValue(it) - shares.getValue(it) }.thenBy { it }
        )

        for (id in byLargestFraction) {
            if (remainder <= 0L) break
            shares[id] = shares.getValue(id) + 1
            remainder--
        }
        return shares
    }

    /** Quanto do [transaction] cabe a cada morador que entrou no rateio. */
    fun sharesOf(transaction: Transaction): Map<String, Long> =
        distribute(transaction.totalValueCents, transaction.weights)

    fun shareOf(transaction: Transaction, residentId: String): Long =
        sharesOf(transaction)[residentId] ?: 0L

    /** Aluguel + contas fixas rateados entre os ativos pelo peso do quarto de cada um. */
    fun fixedShares(settings: BankSettings, residents: List<Resident>): Map<String, Long> {
        val weights = residents.filter { it.isActive }
            .associate { it.id to settings.weightOf(it.roomType) }
        return distribute(settings.monthlyFixedTotalCents, weights)
    }

    fun fixedShare(
        settings: BankSettings,
        residents: List<Resident>,
        residentId: String
    ): Long = fixedShares(settings, residents)[residentId] ?: 0L

    fun calculateUserBalance(transactions: List<Transaction>, userId: String): Long {
        var balance = 0L
        transactions.forEach { tx ->
            // Se o usuário foi o pagador, ele recebe um CRÉDITO do valor total
            if (tx.payerId == userId) {
                balance += tx.totalValueCents
            }
            // Independente de ser pagador, ele subtrai a parte que DEVE (DÉBITO)
            balance -= shareOf(tx, userId)
        }
        return balance
    }

    /**
     * Saldo do morador no banco da rep.
     *
     * Negativo = deve ao banco. Positivo = pagou mais do que devia.
     * Pagamentos entram como lançamento normal (o morador como pagador, sem rateio),
     * então zerar a dívida é só lançar o pagamento.
     */
    fun balanceOf(
        transactions: List<Transaction>,
        settings: BankSettings,
        residents: List<Resident>,
        residentId: String
    ): Long = calculateUserBalance(transactions, residentId) -
        fixedShare(settings, residents, residentId)

    /** Quanto o morador deve hoje. Zero se estiver em dia ou com crédito. */
    fun debtOf(
        transactions: List<Transaction>,
        settings: BankSettings,
        residents: List<Resident>,
        residentId: String
    ): Long = maxOf(0L, -balanceOf(transactions, settings, residents, residentId))
}
