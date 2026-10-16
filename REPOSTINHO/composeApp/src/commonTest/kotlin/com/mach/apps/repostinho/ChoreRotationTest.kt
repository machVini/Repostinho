package com.mach.apps.repostinho

import com.mach.apps.repostinho.data.model.Chore
import com.mach.apps.repostinho.data.model.ChoreGroup
import com.mach.apps.repostinho.data.model.ChoreRotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val CHORES = listOf(
    Chore("panos", "Panos"),
    Chore("louca", "Louça"),
    Chore("folga", "Folga", isRest = true)
)

private val GROUPS = listOf(
    ChoreGroup("g1", listOf("a")),
    ChoreGroup("g2", listOf("b")),
    ChoreGroup("g3", listOf("c"))
)

private fun assigneeOf(week: Int, choreId: String): String =
    ChoreRotation.assign(CHORES, GROUPS, week)
        .first { it.id == choreId }
        .assigneeIds
        .single()

/**
 * O que a escala faz com um número de semana.
 *
 * Os testes de virada por data saíram junto com o código deles: a escala não anda mais
 * sozinha, ela anda quando alguém roda. O que continua valendo é isto — dado o número,
 * quem pega o quê, sempre igual em qualquer aparelho.
 */
class ChoreRotationTest {

    @Test
    fun cadaGrupoDesceUmaCasaPorSemana() {
        assertEquals("a", assigneeOf(week = 0, choreId = "panos"))
        assertEquals("c", assigneeOf(week = 1, choreId = "panos"))
        assertEquals("b", assigneeOf(week = 2, choreId = "panos"))
        // Com três grupos, a terceira virada fecha a volta.
        assertEquals("a", assigneeOf(week = 3, choreId = "panos"))
    }

    @Test
    fun aFolgaTambemGira() {
        assertEquals("c", assigneeOf(week = 0, choreId = "folga"))
        assertEquals("b", assigneeOf(week = 1, choreId = "folga"))
    }

    @Test
    fun folgaNuncaChegaMarcadaComoFeita() {
        // O id continua no conjunto de feitas, mas a tarefa virou folga no rodízio.
        val tasks = ChoreRotation.assign(CHORES, GROUPS, week = 0, doneChoreIds = setOf("folga"))
        assertTrue(tasks.first { it.id == "folga" }.done.not())
    }

    @Test
    fun aEscalaDaSemanaEhSempreAMesma() {
        // A propriedade que dispensa sincronizar: dois aparelhos, mesma data, mesma escala.
        val umAparelho = ChoreRotation.assign(CHORES, GROUPS, week = 7)
        val outroAparelho = ChoreRotation.assign(CHORES, GROUPS, week = 7)

        assertEquals(umAparelho, outroAparelho)
    }

    @Test
    fun semGruposAEscalaAparecevazia() {
        // Acontece se a lista de grupos vier vazia; a tela precisa continuar de pé.
        val tasks = ChoreRotation.assign(CHORES, groups = emptyList(), week = 3)

        assertEquals(CHORES.size, tasks.size)
        assertTrue(tasks.all { it.assigneeIds.isEmpty() })
    }
}
