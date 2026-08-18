package com.mach.apps.repostinho

import com.mach.apps.repostinho.data.model.Chore
import com.mach.apps.repostinho.data.model.ChoreGroup
import com.mach.apps.repostinho.data.model.ChoreRotation
import com.mach.apps.repostinho.data.model.RotationState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
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

/**
 * A virada da quarta às 14h30.
 *
 * O dia inteiro de quarta é o caso interessante: de manhã ele ainda pertence à semana que
 * está acabando, e a partir das 14h30 à que começa. Errar isso troca a escala na frente de
 * quem ainda ia fazer a tarefa.
 */
class TurnTimeTest {

    /** 19/08/2026, a quarta seguinte à âncora. */
    private fun quarta(hour: Int, minute: Int) =
        LocalDateTime(2026, 8, 19, hour, minute)

    @Test
    fun quartaDeManhaAindaEAsemanaAnterior() {
        assertEquals(LocalDate(2026, 8, 18), ChoreRotation.rotationDate(quarta(9, 0)))
        assertEquals(0, ChoreRotation.weekIndex(RUNNING, ChoreRotation.rotationDate(quarta(9, 0))))
    }

    @Test
    fun umMinutoAntesDaViradaNaoVira() {
        // O caso que a escala antiga tinha de graça e esta precisa acertar na mão.
        assertEquals(LocalDate(2026, 8, 18), ChoreRotation.rotationDate(quarta(14, 29)))
        assertEquals(0, ChoreRotation.weekIndex(RUNNING, ChoreRotation.rotationDate(quarta(14, 29))))
    }

    @Test
    fun naHoraCravadaJaEAsemanaNova() {
        // 14h30 pertence à semana que começa: o intervalo é fechado no início.
        assertEquals(LocalDate(2026, 8, 19), ChoreRotation.rotationDate(quarta(14, 30)))
        assertEquals(1, ChoreRotation.weekIndex(RUNNING, ChoreRotation.rotationDate(quarta(14, 30))))
    }

    @Test
    fun aMeiaNoiteDaQuartaNaoViraMais() {
        // Era aqui que a escala trocava antes.
        assertEquals(LocalDate(2026, 8, 18), ChoreRotation.rotationDate(quarta(0, 0)))
        assertEquals(0, ChoreRotation.weekIndex(RUNNING, ChoreRotation.rotationDate(quarta(0, 0))))
    }

    @Test
    fun osOutrosDiasNaoTemHora() {
        // Só a quarta olha o relógio; nos demais a data é o próprio dia, de madrugada a
        // madrugada.
        val quinta = LocalDateTime(2026, 8, 20, 0, 1)
        val terca = LocalDateTime(2026, 8, 25, 23, 59)

        assertEquals(LocalDate(2026, 8, 20), ChoreRotation.rotationDate(quinta))
        assertEquals(LocalDate(2026, 8, 25), ChoreRotation.rotationDate(terca))
        assertEquals(1, ChoreRotation.weekIndex(RUNNING, ChoreRotation.rotationDate(quinta)))
        assertEquals(1, ChoreRotation.weekIndex(RUNNING, ChoreRotation.rotationDate(terca)))
    }

    @Test
    fun oRotuloAcompanhaAvirada() {
        // O intervalo na tela é o mesmo da escala: quarta de manhã ainda mostra a semana
        // que está acabando.
        assertEquals(
            "12 a 18 de agosto",
            ChoreRotation.weekRangeLabel(ChoreRotation.rotationDate(quarta(10, 0)))
        )
        assertEquals(
            "19 a 25 de agosto",
            ChoreRotation.weekRangeLabel(ChoreRotation.rotationDate(quarta(15, 0)))
        )
    }
}
