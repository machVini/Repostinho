package com.mach.apps.repostinho

import com.mach.apps.repostinho.data.model.BankSettings
import com.mach.apps.repostinho.data.model.Resident
import com.mach.apps.repostinho.data.model.RoomType
import com.mach.apps.repostinho.data.model.Transaction
import com.mach.apps.repostinho.data.model.TransactionType
import com.mach.apps.repostinho.domain.BalanceCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BalanceCalculatorTest {

    private val calculator = BalanceCalculator()

    private val ana = Resident("ana", "Ana", RoomType.INDIVIDUAL)
    private val bruno = Resident("bruno", "Bruno", RoomType.INDIVIDUAL)
    private val residents = listOf(ana, bruno)

    /** Dois quartos individuais e R$ 1.000 de fixo: R$ 500 para cada um. */
    private val settings = BankSettings(monthlyFixedTotalCents = 100_000L)

    private fun expense(payerId: String, cents: Long, weights: Map<String, Double>) =
        Transaction(
            description = "Pizza",
            type = TransactionType.PRIVADO,
            payerId = payerId,
            totalValueCents = cents,
            weights = weights
        )

    @Test
    fun `fixo e rateado igualmente entre quartos iguais`() {
        assertEquals(50_000L, calculator.fixedShare(settings, residents, ana.id))
        assertEquals(50_000L, calculator.fixedShare(settings, residents, bruno.id))
    }

    @Test
    fun `pizza paga para outro morador desce o saldo de quem pagou e sobe o de quem consumiu`() {
        // Ana paga R$ 50 de pizza só para o Bruno.
        val transactions = listOf(expense(ana.id, 5_000L, mapOf(bruno.id to 1.0)))

        // Ana devia 500, pagou 50 por outro: agora deve 450.
        assertEquals(45_000L, calculator.debtOf(transactions, settings, residents, ana.id))
        // Bruno devia 500 e consumiu 50: agora deve 550.
        assertEquals(55_000L, calculator.debtOf(transactions, settings, residents, bruno.id))
    }

    @Test
    fun `lancar o pagamento zera a divida do morador`() {
        val pizza = expense(ana.id, 5_000L, mapOf(bruno.id to 1.0))
        val pagamento = Transaction(
            description = "Pagamento",
            type = TransactionType.ENTRADA,
            payerId = bruno.id,
            totalValueCents = 55_000L,
            weights = emptyMap()
        )

        val debt = calculator.debtOf(listOf(pizza, pagamento), settings, residents, bruno.id)
        assertEquals(0L, debt)
    }

    @Test
    fun `despesa coletiva e rateada pelos pesos do lancamento`() {
        // Ana paga R$ 90 de mercado dividido 2 para ela e 1 para o Bruno.
        val transactions = listOf(
            expense(ana.id, 9_000L, mapOf(ana.id to 2.0, bruno.id to 1.0))
        )

        // Ana: -500 fixo + 90 pago - 60 da parte dela = -470.
        assertEquals(47_000L, calculator.debtOf(transactions, settings, residents, ana.id))
        // Bruno: -500 fixo - 30 da parte dele = -530.
        assertEquals(53_000L, calculator.debtOf(transactions, settings, residents, bruno.id))
    }

    @Test
    fun `quarto triplo paga menos que individual no rateio do fixo`() {
        val carla = Resident("carla", "Carla", RoomType.TRIPLO_MAIOR)
        val todos = listOf(ana, carla)

        val anaShare = calculator.fixedShare(settings, todos, ana.id)
        val carlaShare = calculator.fixedShare(settings, todos, carla.id)

        // Pesos da rep: individual 1,10 e triplo maior 0,88 -> R$ 1.000 divididos por 1,98.
        assertEquals(55_556L, anaShare)
        assertEquals(44_444L, carlaShare)
        assertEquals(100_000L, anaShare + carlaShare)
    }

    @Test
    fun `morador inativo sai do rateio do fixo`() {
        val inativo = bruno.copy(isActive = false)
        val todos = listOf(ana, inativo)

        // Sobrou só a Ana ativa: ela absorve o fixo inteiro.
        assertEquals(100_000L, calculator.fixedShare(settings, todos, ana.id))
        assertEquals(0L, calculator.fixedShare(settings, todos, inativo.id))
    }

    // --- rateio em centavos inteiros ---

    @Test
    fun `divisao que nao fecha redonda nao perde nem inventa centavo`() {
        // R$ 10 entre três: 3,34 + 3,33 + 3,33. O centavo que sobra tem que ir para alguém.
        val shares = calculator.distribute(
            1_000L,
            mapOf("a" to 1.0, "b" to 1.0, "c" to 1.0)
        )

        assertEquals(1_000L, shares.values.sum())
        assertEquals(listOf(334L, 333L, 333L), shares.values.sortedDescending())
    }

    @Test
    fun `sobra de centavos vai para quem tem a maior fracao truncada`() {
        // 100 centavos com pesos 1 e 2: 33,33 e 66,66 -> a sobra é do peso maior.
        val shares = calculator.distribute(100L, mapOf("pequeno" to 1.0, "grande" to 2.0))

        assertEquals(33L, shares.getValue("pequeno"))
        assertEquals(67L, shares.getValue("grande"))
        assertEquals(100L, shares.values.sum())
    }

    @Test
    fun `rateio nao depende da ordem do mapa`() {
        val direta = calculator.distribute(1_000L, mapOf("a" to 1.0, "b" to 1.0, "c" to 1.0))
        val invertida = calculator.distribute(1_000L, mapOf("c" to 1.0, "b" to 1.0, "a" to 1.0))

        assertEquals(direta, invertida)
    }

    @Test
    fun `rateio real da rep fecha exatamente no total do mes`() {
        val rep = listOf(
            Resident("i1", "I1", RoomType.INDIVIDUAL),
            Resident("i2", "I2", RoomType.INDIVIDUAL),
            Resident("i3", "I3", RoomType.INDIVIDUAL),
            Resident("dm1", "DM1", RoomType.DUPLO_MAIOR),
            Resident("dm2", "DM2", RoomType.DUPLO_MAIOR),
            Resident("dm3", "DM3", RoomType.DUPLO_MAIOR),
            Resident("dm4", "DM4", RoomType.DUPLO_MAIOR),
            Resident("dn1", "DN1", RoomType.DUPLO_MENOR),
            Resident("dn2", "DN2", RoomType.DUPLO_MENOR),
            Resident("tm1", "TM1", RoomType.TRIPLO_MAIOR),
            Resident("tm2", "TM2", RoomType.TRIPLO_MAIOR),
            Resident("tm3", "TM3", RoomType.TRIPLO_MAIOR),
            Resident("tn1", "TN1", RoomType.TRIPLO_MENOR),
            Resident("tn2", "TN2", RoomType.TRIPLO_MENOR),
            Resident("tn3", "TN3", RoomType.TRIPLO_MENOR)
        )
        val real = BankSettings(monthlyFixedTotalCents = 750_000L)

        val shares = calculator.fixedShares(real, rep)

        // O rateio não pode criar nem sumir com dinheiro: tem que bater no centavo.
        assertEquals(750_000L, shares.values.sum())
        assertEquals(57_935L, shares.getValue("i1"))
        assertEquals(52_669L, shares.getValue("dm1"))
        assertEquals(42_135L, shares.getValue("tn1"))
        // Individual paga mais que triplo menor.
        assertTrue(shares.getValue("i1") > shares.getValue("tn1"))
    }

    @Test
    fun `mil lancamentos nao acumulam erro de centavo`() {
        // O ponto de trocar Double por Long: repetir uma divisão feia mil vezes não pode
        // deslocar o saldo nem um centavo.
        val transactions = (1..1_000).map {
            expense(ana.id, 1_000L, mapOf(ana.id to 1.0, bruno.id to 2.0))
        }

        // Cada lançamento: Ana +1000 de crédito -333 da parte dela; Bruno -667.
        assertEquals(667_000L, calculator.calculateUserBalance(transactions, ana.id))
        assertEquals(-667_000L, calculator.calculateUserBalance(transactions, bruno.id))
    }
}
