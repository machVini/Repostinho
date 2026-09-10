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

/** 07/08/2026 é sexta-feira — a âncora do rodízio. */
private val ANCHOR = LocalDate(2026, 8, 7)
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
    fun aSemanaSoViraNaSexta() {
        // Quinta ainda é a semana da sexta anterior; a sexta seguinte já é a próxima.
        assertEquals(0, ChoreRotation.weekIndex(RUNNING, LocalDate(2026, 8, 13)))
        assertEquals(1, ChoreRotation.weekIndex(RUNNING, LocalDate(2026, 8, 14)))
    }

    @Test
    fun aAncoraAntigaDaQuartaContinuaValendoOMesmo() {
        // Quem já tem o app guardou 12/08 em disco, e essa data continua chegando aqui
        // depois da troca para sexta. Como a conta passa pela sexta anterior — 07/08 —,
        // as duas âncoras dão a mesma semana: ninguém troca de tarefa por causa da
        // mudança de dia, só pela virada passar a acontecer antes.
        val antiga = RotationState(anchor = LocalDate(2026, 8, 12))

        listOf(
            LocalDate(2026, 8, 7),
            LocalDate(2026, 8, 13),
            LocalDate(2026, 8, 14),
            LocalDate(2026, 9, 10),
            LocalDate(2026, 12, 25)
        ).forEach { dia ->
            assertEquals(
                ChoreRotation.weekIndex(RUNNING, dia),
                ChoreRotation.weekIndex(antiga, dia),
                "semana diferente em $dia"
            )
        }
    }

    @Test
    fun ancoraAtrasadaNaoVoltaAoInicio() {
        // Relógio do aparelho atrasado: a divisão truncada devolveria 0 aqui, e a escala
        // saltaria de volta para a da âncora.
        assertEquals(-1, ChoreRotation.weekIndex(RUNNING, LocalDate(2026, 8, 6)))
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
        val paused = ChoreRotation.pause(RUNNING, LocalDate(2026, 8, 14))

        assertEquals(1, ChoreRotation.weekIndex(paused, LocalDate(2026, 8, 14)))
        // Um mês depois, ainda a mesma semana.
        assertEquals(1, ChoreRotation.weekIndex(paused, LocalDate(2026, 9, 11)))
    }

    @Test
    fun retomarContinuaDeOndeParouEmVezDeSaltar() {
        // Pausa na semana 1 e volta um mês depois: a próxima escala é a 2, não a 5 — as
        // semanas de férias não podem livrar ninguém da louça.
        val paused = ChoreRotation.pause(RUNNING, LocalDate(2026, 8, 14))
        val resumeDay = LocalDate(2026, 9, 11)
        val resumed = ChoreRotation.resume(paused, resumeDay)

        assertEquals(1, ChoreRotation.weekIndex(resumed, resumeDay))
        assertEquals(2, ChoreRotation.weekIndex(resumed, LocalDate(2026, 9, 18)))
    }

    @Test
    fun retomarNoMeioDaSemanaNaoAdiantaAVirada() {
        // Pausou na sexta, retomou no domingo: a virada continua sendo na sexta seguinte.
        val paused = ChoreRotation.pause(RUNNING, LocalDate(2026, 8, 14))
        val resumed = ChoreRotation.resume(paused, LocalDate(2026, 8, 16))

        assertEquals(1, ChoreRotation.weekIndex(resumed, LocalDate(2026, 8, 16)))
        assertEquals(1, ChoreRotation.weekIndex(resumed, LocalDate(2026, 8, 20)))
        assertEquals(2, ChoreRotation.weekIndex(resumed, LocalDate(2026, 8, 21)))
    }

    @Test
    fun pausarDuasVezesNaoDeslocaAEscala() {
        val once = ChoreRotation.pause(RUNNING, LocalDate(2026, 8, 14))
        val twice = ChoreRotation.pause(once, LocalDate(2026, 9, 11))

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
    fun oIntervaloVaiDaSextaAQuintaSeguinte() {
        // A semana que a rep combinou: 7 (sexta) a 13 (quinta) de agosto.
        assertEquals("7 a 13 de agosto", ChoreRotation.weekRangeLabel(LocalDate(2026, 8, 8)))
        // Qualquer dia da mesma semana devolve o mesmo intervalo.
        assertEquals("7 a 13 de agosto", ChoreRotation.weekRangeLabel(LocalDate(2026, 8, 13)))
        // A sexta seguinte já é a próxima.
        assertEquals("14 a 20 de agosto", ChoreRotation.weekRangeLabel(LocalDate(2026, 8, 14)))
    }

    @Test
    fun oMesApareceDosDoisLadosQuandoASemanaViraOMes() {
        assertEquals(
            "28 de agosto a 3 de setembro",
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
 * A virada da sexta ao meio-dia.
 *
 * O dia inteiro de sexta é o caso interessante: de manhã ele ainda pertence à semana que
 * está acabando, e a partir do meio-dia à que começa. Errar isso troca a escala na frente
 * de quem ainda ia fazer a tarefa.
 */
class TurnTimeTest {

    /** 14/08/2026, a sexta seguinte à âncora. */
    private fun sexta(hour: Int, minute: Int) =
        LocalDateTime(2026, 8, 14, hour, minute)

    @Test
    fun sextaDeManhaAindaEAsemanaAnterior() {
        assertEquals(LocalDate(2026, 8, 13), ChoreRotation.rotationDate(sexta(9, 0)))
        assertEquals(0, ChoreRotation.weekIndex(RUNNING, ChoreRotation.rotationDate(sexta(9, 0))))
    }

    @Test
    fun umMinutoAntesDaViradaNaoVira() {
        // O caso que a escala antiga tinha de graça e esta precisa acertar na mão.
        assertEquals(LocalDate(2026, 8, 13), ChoreRotation.rotationDate(sexta(11, 59)))
        assertEquals(0, ChoreRotation.weekIndex(RUNNING, ChoreRotation.rotationDate(sexta(11, 59))))
    }

    @Test
    fun naHoraCravadaJaEAsemanaNova() {
        // Meio-dia pertence à semana que começa: o intervalo é fechado no início.
        assertEquals(LocalDate(2026, 8, 14), ChoreRotation.rotationDate(sexta(12, 0)))
        assertEquals(1, ChoreRotation.weekIndex(RUNNING, ChoreRotation.rotationDate(sexta(12, 0))))
    }

    @Test
    fun aMeiaNoiteDaSextaNaoViraMais() {
        // Era aqui que a escala trocava antes de existir hora de virada.
        assertEquals(LocalDate(2026, 8, 13), ChoreRotation.rotationDate(sexta(0, 0)))
        assertEquals(0, ChoreRotation.weekIndex(RUNNING, ChoreRotation.rotationDate(sexta(0, 0))))
    }

    @Test
    fun osOutrosDiasNaoTemHora() {
        // Só a sexta olha o relógio; nos demais a data é o próprio dia, de madrugada a
        // madrugada.
        val sabado = LocalDateTime(2026, 8, 15, 0, 1)
        val quinta = LocalDateTime(2026, 8, 20, 23, 59)

        assertEquals(LocalDate(2026, 8, 15), ChoreRotation.rotationDate(sabado))
        assertEquals(LocalDate(2026, 8, 20), ChoreRotation.rotationDate(quinta))
        assertEquals(1, ChoreRotation.weekIndex(RUNNING, ChoreRotation.rotationDate(sabado)))
        assertEquals(1, ChoreRotation.weekIndex(RUNNING, ChoreRotation.rotationDate(quinta)))
    }

    @Test
    fun oRotuloAcompanhaAvirada() {
        // O intervalo na tela é o mesmo da escala: sexta de manhã ainda mostra a semana
        // que está acabando.
        assertEquals(
            "7 a 13 de agosto",
            ChoreRotation.weekRangeLabel(ChoreRotation.rotationDate(sexta(10, 0)))
        )
        assertEquals(
            "14 a 20 de agosto",
            ChoreRotation.weekRangeLabel(ChoreRotation.rotationDate(sexta(13, 0)))
        )
    }
}
