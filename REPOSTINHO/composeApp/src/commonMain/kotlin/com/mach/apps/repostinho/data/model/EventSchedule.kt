package com.mach.apps.repostinho.data.model

/**
 * Transforma os eventos da rep na lista de datas que a agenda mostra.
 *
 * A janela é sempre de hoje até 31 de dezembro do ano corrente: a agenda responde "o que
 * ainda vem este ano", e não "tudo que já cadastramos". Um evento que passou sai da lista
 * sozinho, sem ninguém precisar limpar nada.
 */
object EventSchedule {

    /** Dias de cada mês. Fevereiro é tratado à parte por causa do ano bissexto. */
    private val DAYS_IN_MONTH = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    /**
     * As ocorrências de [events] entre [today] e o fim do ano de [today], em ordem.
     *
     * Um evento de vários dias entra se ele ainda não terminou: o InterReps continua na
     * lista durante os quatro dias em que está acontecendo, e não some no primeiro.
     */
    fun occurrencesUntilEndOfYear(
        events: List<RepEvent>,
        today: RepDate
    ): List<EventOccurrence> {
        val lastDay = RepDate(31, 12, today.year)

        return events
            // Uma data impossível — mês 22, de um dedo escorregado no cadastro fixo — não
            // pode levar a agenda junto. O `catch` do ViewModel transformaria a exceção
            // numa lista vazia, e a tela ficaria em branco sem dizer por quê; melhor
            // perder o evento torto e mostrar os outros treze.
            .filter { it.start.isReal() && it.end.isReal() }
            .flatMap { occurrencesOf(it, today, lastDay) }
            .sortedWith(compareBy({ it.start }, { it.event.name }))
    }

    /** Existe no calendário: mês de 1 a 12 e dia dentro do que aquele mês tem. */
    private fun RepDate.isReal(): Boolean =
        month in 1..12 && day in 1..daysInMonth(month, year)

    private fun occurrencesOf(
        event: RepEvent,
        from: RepDate,
        to: RepDate
    ): List<EventOccurrence> = when (event.recurrence) {
        // Já passou ou é do ano que vem: nos dois casos fica fora desta janela.
        Recurrence.NENHUMA ->
            if (event.end >= from && event.start <= to) {
                listOf(EventOccurrence(event, event.start, event.end))
            } else {
                emptyList()
            }

        Recurrence.MENSAL -> (1..12).mapNotNull { month ->
            occurrenceOn(event, day = event.start.day, month = month, year = to.year, from, to)
        }

        Recurrence.ANUAL -> listOfNotNull(
            occurrenceOn(
                event,
                day = event.start.day,
                month = event.start.month,
                year = to.year,
                from,
                to
            )
        )
    }

    /**
     * A ocorrência naquele dia, se ela couber na janela.
     *
     * A duração do evento original é preservada: um rolê de três dias que se repetisse
     * continuaria com três dias em cada volta.
     */
    private fun occurrenceOn(
        event: RepEvent,
        day: Int,
        month: Int,
        year: Int,
        from: RepDate,
        to: RepDate
    ): EventOccurrence? {
        // Dia 31 num mês de 30 cai no último dia do mês, em vez de sumir da agenda.
        val start = RepDate(day.coerceAtMost(daysInMonth(month, year)), month, year)
        if (start < from || start > to) return null

        val length = daysBetween(event.start, event.end)
        return EventOccurrence(event, start, addDays(start, length))
    }

    private fun daysInMonth(month: Int, year: Int): Int =
        if (month == 2 && isLeapYear(year)) 29 else DAYS_IN_MONTH[month - 1]

    private fun isLeapYear(year: Int): Boolean =
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

    /** Só para durações curtas dentro do mesmo ano, que é o caso da agenda da rep. */
    private fun daysBetween(start: RepDate, end: RepDate): Int =
        dayOfYear(end) - dayOfYear(start)

    private fun dayOfYear(date: RepDate): Int =
        (1 until date.month).sumOf { daysInMonth(it, date.year) } + date.day

    private fun addDays(date: RepDate, days: Int): RepDate {
        var day = date.day + days
        var month = date.month
        var year = date.year

        while (day > daysInMonth(month, year)) {
            day -= daysInMonth(month, year)
            month++
            if (month > 12) {
                month = 1
                year++
            }
        }
        return RepDate(day, month, year)
    }
}
