package com.mach.apps.repostinho

import com.mach.apps.repostinho.data.model.Movement
import com.mach.apps.repostinho.data.model.MovementType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A regra que importa é a soma: o que o card mostra por pessoa tem que fechar com o valor
 * do lançamento, que está logo acima na mesma tela.
 */
class MovementSharesTest {

    private fun movement(
        valueCents: Long,
        weights: Map<String, Double>,
        totalWeight: Double = weights.values.sum()
    ) = Movement(
        id = "t",
        description = "teste",
        type = MovementType.PRIVADO,
        payer = "VK",
        valueCents = valueCents,
        weights = weights,
        totalWeight = totalWeight
    )

    @Test
    fun divisaoExataFechaComOTotal() {
        val shares = movement(30000L, mapOf("a" to 1.0, "b" to 1.0, "c" to 1.0)).sharesInCents()
        assertEquals(mapOf("a" to 10000L, "b" to 10000L, "c" to 10000L), shares)
    }

    /** R$ 100 entre três daria 33,333… — alguém precisa receber o centavo que sobra. */
    @Test
    fun divisaoComRestoNaoPerdeCentavo() {
        val shares = movement(10000L, mapOf("a" to 1.0, "b" to 1.0, "c" to 1.0)).sharesInCents()
        assertEquals(10000L, shares.values.sum())
        assertTrue(shares.values.all { it == 3333L || it == 3334L })
        assertEquals(1, shares.values.count { it == 3334L })
    }

    @Test
    fun pesosDiferentesFechamComOTotal() {
        val shares = movement(
            valueCents = 704888L,
            weights = mapOf(
                "Lameu" to 0.8, "Leozin" to 1.1, "Pico" to 1.1, "LL" to 1.1,
                "Du" to 1.0, "Michel" to 1.0, "Peter" to 1.0, "Gab" to 0.95,
                "Gu" to 0.95, "Mixirica" to 0.88, "Massa" to 0.88, "VK" to 1.0,
                "Cansado" to 0.88, "Prazer" to 0.8, "Picasso" to 0.8
            ),
            totalWeight = 14.24
        ).sharesInCents()

        assertEquals(704888L, shares.values.sum())
        // Peso maior nunca paga menos que peso menor.
        assertTrue(shares.getValue("Pico") > shares.getValue("Lameu"))
    }

    /** A planilha tem estornos, como a "Correção energia Julho" de -R$ 650. */
    @Test
    fun valorNegativoFechaComOTotal() {
        val shares = movement(
            valueCents = -65000L,
            weights = (1..15).associate { "p$it" to 1.0 }
        ).sharesInCents()

        assertEquals(-65000L, shares.values.sum())
    }

    @Test
    fun participanteUnicoRecebeTudo() {
        val shares = movement(9893L, mapOf("Pico" to 1.0)).sharesInCents()
        assertEquals(mapOf("Pico" to 9893L), shares)
    }

    /** Entradas não têm rateio; não pode virar divisão por zero. */
    @Test
    fun semParticipantesDevolveVazio() {
        val shares = movement(64900L, emptyMap(), totalWeight = 0.0).sharesInCents()
        assertTrue(shares.isEmpty())
    }
}
