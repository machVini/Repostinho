package com.mach.apps.repostinho

import com.mach.apps.repostinho.data.model.Chore
import com.mach.apps.repostinho.data.model.ChoreGroup
import com.mach.apps.repostinho.data.model.ChoreRotation
import com.mach.apps.repostinho.data.model.RotationState
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 12/08/2026 é quarta-feira — a âncora do rodízio. */
private val ANCHOR = LocalDate(2026, 8, 12)
private val RUNNING = RotationState(anchor = ANCHOR)

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

class ChoreRotationTest {

    @Test
    fun semanaZeroNaPropriaAncora() {
        assertEquals(0, ChoreRotation.weekIndex(RUNNING, ANCHOR))
    }

    @Test
    fun aSemanaSoViraNaQuarta() {
        // Terça ainda é a semana da quarta anterior; a quarta seguinte já é a próxima.
        assertEquals(0, ChoreRotation.weekIndex(RUNNING, LocalDate(2026, 8, 18)))
        assertEquals(1, ChoreRotation.weekIndex(RUNNING, LocalDate(2026, 8, 19)))
    }

    @Test
    fun ancoraAtrasadaNaoVoltaAoInicio() {
        // Relógio do aparelho atrasado: a divisão truncada devolveria 0 aqui, e a escala
        // saltaria de volta para a da âncora.
        assertEquals(-1, ChoreRotation.weekIndex(RUNNING, LocalDate(2026, 8, 11)))
    }

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
    fun pausadoAEscalaNaoAndaComOTempo() {
        val paused = ChoreRotation.pause(RUNNING, LocalDate(2026, 8, 19))

        assertEquals(1, ChoreRotation.weekIndex(paused, LocalDate(2026, 8, 19)))
        // Um mês depois, ainda a mesma semana.
        assertEquals(1, ChoreRotation.weekIndex(paused, LocalDate(2026, 9, 16)))
    }

    @Test
    fun retomarContinuaDeOndeParouEmVezDeSaltar() {
        // Pausa na semana 1 e volta um mês depois: a próxima escala é a 2, não a 5 — as
        // semanas de férias não podem livrar ninguém da louça.
        val paused = ChoreRotation.pause(RUNNING, LocalDate(2026, 8, 19))
        val resumeDay = LocalDate(2026, 9, 16)
        val resumed = ChoreRotation.resume(paused, resumeDay)

        assertEquals(1, ChoreRotation.weekIndex(resumed, resumeDay))
        assertEquals(2, ChoreRotation.weekIndex(resumed, LocalDate(2026, 9, 23)))
    }

    @Test
    fun retomarNoMeioDaSemanaNaoAdiantaAVirada() {
        // Pausou na quarta, retomou na sexta: a virada continua sendo na quarta seguinte.
        val paused = ChoreRotation.pause(RUNNING, LocalDate(2026, 8, 19))
        val resumed = ChoreRotation.resume(paused, LocalDate(2026, 8, 21))

        assertEquals(1, ChoreRotation.weekIndex(resumed, LocalDate(2026, 8, 21)))
        assertEquals(1, ChoreRotation.weekIndex(resumed, LocalDate(2026, 8, 25)))
        assertEquals(2, ChoreRotation.weekIndex(resumed, LocalDate(2026, 8, 26)))
    }

    @Test
    fun pausarDuasVezesNaoDeslocaAEscala() {
        val once = ChoreRotation.pause(RUNNING, LocalDate(2026, 8, 19))
        val twice = ChoreRotation.pause(once, LocalDate(2026, 9, 16))

        assertEquals(once, twice)
    }

    @Test
    fun aEscalaDaSemanaEhSempreAMesma() {
        // A propriedade que dispensa sincronizar: dois aparelhos, mesma data, mesma escala.
        val umAparelho = ChoreRotation.assign(CHORES, GROUPS, week = 7)
        val outroAparelho = ChoreRotation.assign(CHORES, GROUPS, week = 7)

        assertEquals(umAparelho, outroAparelho)
    }

    @Test
    fun oIntervaloVaiDaQuartaATercaSeguinte() {
        // A semana que a rep combinou: 12 (quarta) a 18 (terça) de agosto.
        assertEquals("12 a 18 de agosto", ChoreRotation.weekRangeLabel(LocalDate(2026, 8, 13)))
        // Qualquer dia da mesma semana devolve o mesmo intervalo.
        assertEquals("12 a 18 de agosto", ChoreRotation.weekRangeLabel(LocalDate(2026, 8, 18)))
        // A quarta seguinte já é a próxima.
        assertEquals("19 a 25 de agosto", ChoreRotation.weekRangeLabel(LocalDate(2026, 8, 19)))
    }

    @Test
    fun oMesApareceDosDoisLadosQuandoASemanaViraOMes() {
        assertEquals(
            "26 de agosto a 1 de setembro",
            ChoreRotation.weekRangeLabel(LocalDate(2026, 8, 30))
        )
    }

    @Test
    fun semGruposAEscalaAparecevazia() {
        // Acontece se a lista de grupos vier vazia; a tela precisa continuar de pé.
        val tasks = ChoreRotation.assign(CHORES, groups = emptyList(), week = 3)

        assertEquals(CHORES.size, tasks.size)
        assertTrue(tasks.all { it.assigneeIds.isEmpty() })
    }
}
