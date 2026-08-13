package com.mach.apps.repostinho

import com.mach.apps.repostinho.data.model.EventCategory
import com.mach.apps.repostinho.data.model.EventSchedule
import com.mach.apps.repostinho.data.model.Recurrence
import com.mach.apps.repostinho.data.model.RepDate
import com.mach.apps.repostinho.data.model.RepEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 13 de agosto de 2026 — o "hoje" de referência dos testes. */
private val HOJE = RepDate(13, 8, 2026)

/**
 * Nenhum evento da agenda usa recorrência mensal hoje, mas o `banco-api` aceita `MENSAL` e
 * a expansão precisa continuar correta para quem cadastrar um por lá.
 */
private val MENSAL_DIA_18 = RepEvent(
    id = "mensal",
    name = "Evento mensal",
    start = RepDate(18, 1, 2026),
    recurrence = Recurrence.MENSAL
)

private val NIVER_REP = RepEvent(
    id = "niver-rep",
    name = "Aniversário da Rep",
    start = RepDate(3, 8, 2023),
    category = EventCategory.ANIVERSARIO,
    recurrence = Recurrence.ANUAL
)

private val INTERREPS = RepEvent(
    id = "interreps",
    name = "InterReps",
    start = RepDate(19, 11, 2026),
    end = RepDate(22, 11, 2026),
    category = EventCategory.ARU
)

private fun datesOf(vararg events: RepEvent, today: RepDate = HOJE): List<RepDate> =
    EventSchedule.occurrencesUntilEndOfYear(events.toList(), today).map { it.start }

class EventScheduleTest {

    @Test
    fun umEventoMensalAbreUmaLinhaPorMesQueFalta() {
        // De 13/08 até o fim do ano sobram cinco dias 18.
        assertEquals(
            listOf(
                RepDate(18, 8, 2026),
                RepDate(18, 9, 2026),
                RepDate(18, 10, 2026),
                RepDate(18, 11, 2026),
                RepDate(18, 12, 2026)
            ),
            datesOf(MENSAL_DIA_18)
        )
    }

    @Test
    fun oQuePassouSaiDaAgendaSozinho() {
        // O aniversário da rep foi dia 3; hoje é 13. Não aparece mais este ano.
        assertTrue(datesOf(NIVER_REP).isEmpty())
    }

    @Test
    fun umAnuarioQueAindaVemApareceUmaVezSo() {
        // Mesma data, mas visto de julho: ainda está por vir.
        assertEquals(
            listOf(RepDate(3, 8, 2026)),
            datesOf(NIVER_REP, today = RepDate(1, 7, 2026))
        )
    }

    @Test
    fun aJanelaParaNoFimDoAno() {
        // Um evento de janeiro que vem não entra: a agenda é só do ano corrente.
        val janeiro = RepEvent("x", "Ano que vem", RepDate(5, 1, 2027))
        assertTrue(datesOf(janeiro).isEmpty())
    }

    @Test
    fun eventoDeVariosDiasFicaNaListaEnquantoAcontece() {
        // No dia 20, o InterReps (19 a 22) ainda está rolando e não pode sumir.
        assertEquals(
            listOf(RepDate(19, 11, 2026)),
            datesOf(INTERREPS, today = RepDate(20, 11, 2026))
        )
        // No dia 23 já acabou.
        assertTrue(datesOf(INTERREPS, today = RepDate(23, 11, 2026)).isEmpty())
    }

    @Test
    fun aListaSaiEmOrdemDeData() {
        val ordered = datesOf(INTERREPS, MENSAL_DIA_18, NIVER_REP)
        assertEquals(ordered.sortedWith(compareBy { it }), ordered)
    }

    @Test
    fun diaTrintaEUmCaiNoUltimoDiaDosMesesCurtos() {
        val conta = RepEvent(
            id = "conta",
            name = "Conta",
            start = RepDate(31, 1, 2026),
            recurrence = Recurrence.MENSAL
        )
        val meses = datesOf(conta, today = RepDate(1, 9, 2026))

        // Setembro e novembro têm 30 dias: a conta cai no dia 30, em vez de sumir do mês.
        assertEquals(
            listOf(
                RepDate(30, 9, 2026),
                RepDate(31, 10, 2026),
                RepDate(30, 11, 2026),
                RepDate(31, 12, 2026)
            ),
            meses
        )
    }

    @Test
    fun aDuracaoSobreviveAoRepetir() {
        val rolePorTresDias = RepEvent(
            id = "role",
            name = "Rolê",
            start = RepDate(10, 1, 2026),
            end = RepDate(12, 1, 2026),
            recurrence = Recurrence.MENSAL
        )
        val primeira = EventSchedule
            .occurrencesUntilEndOfYear(listOf(rolePorTresDias), RepDate(1, 9, 2026))
            .first()

        assertEquals(RepDate(10, 9, 2026), primeira.start)
        assertEquals(RepDate(12, 9, 2026), primeira.end)
    }

    @Test
    fun fevereiroDeAnoBissextoTemVinteENove() {
        val niver = RepEvent(
            id = "bissexto",
            name = "Niver bissexto",
            start = RepDate(29, 2, 2024),
            recurrence = Recurrence.ANUAL
        )
        // 2028 é bissexto: a data existe e fica no dia 29.
        assertEquals(
            listOf(RepDate(29, 2, 2028)),
            datesOf(niver, today = RepDate(1, 1, 2028))
        )
        // 2027 não é: cai no dia 28 em vez de sumir da agenda.
        assertEquals(
            listOf(RepDate(28, 2, 2027)),
            datesOf(niver, today = RepDate(1, 1, 2027))
        )
    }
}
