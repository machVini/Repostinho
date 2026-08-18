package com.mach.apps.repostinho.data.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * O estado do rodízio: onde ele começou e se está andando.
 *
 * São só dois campos porque o resto é calculado. Guardar a escala da semana atual em
 * disco abriria a chance de ela discordar da data — e aí não haveria como saber qual das
 * duas está certa.
 */
@Serializable
data class RotationState(
    /**
     * A quarta-feira que conta como semana 0.
     *
     * Ela se move quando o rodízio é retomado depois de uma pausa: é assim que as semanas
     * paradas somem da conta em vez de serem puladas de uma vez.
     */
    val anchor: LocalDate,
    /** Índice congelado enquanto o rodízio está pausado; `null` quando está andando. */
    val pausedAtWeek: Int? = null
) {
    val isPaused: Boolean get() = pausedAtWeek != null
}

/**
 * O rodízio das tarefas da rep.
 *
 * A escala não é "rodada" por ninguém: ela é função da data. Um job semanal que
 * reescrevesse as duplas erraria de três jeitos — não rodando (celular desligado),
 * rodando duas vezes, ou rodando só num aparelho. Calculando, uma quarta-feira que passou
 * com o app fechado não tem consequência nenhuma: basta abrir depois.
 *
 * A virada é na quarta à tarde porque é quando a rep troca a escala.
 */
object ChoreRotation {

    /** O dia em que a escala vira. */
    val TURN_DAY = DayOfWeek.WEDNESDAY

    /**
     * A hora da virada, no fuso da rep.
     *
     * A escala trocava à meia-noite, mas a rep troca no meio da quarta à tarde. Meia-noite
     * tirava a tarefa de quem ainda ia fazê-la de manhã e entregava a nova para quem
     * pegava a casa antes de ela ter sido passada a limpo — as duas pontas erradas ao
     * mesmo tempo.
     */
    val TURN_TIME = LocalTime(14, 30)

    /**
     * A quarta em que a escala do código começou a valer.
     *
     * Mudar esta data reembaralha quem faz o quê, então ela é histórica: fica como está.
     */
    val DEFAULT_ANCHOR = LocalDate(2026, 8, 12)

    /**
     * A data que manda no rodízio no instante [now].
     *
     * Quarta antes das 14h30 ainda é a semana que está acabando; das 14h30 em diante, a
     * que começa. Nos outros dias é o próprio dia, e por isso o resto do cálculo continua
     * em [LocalDate]: é uma data por semana, e levar a hora para dentro de [weekIndex]
     * obrigaria toda chamada a carregar um relógio.
     */
    fun rotationDate(now: LocalDateTime): LocalDate =
        if (now.dayOfWeek == TURN_DAY && now.time < TURN_TIME) now.date.minusDays(1)
        else now.date

    /**
     * Qual semana do rodízio [today] cai, contando de [state].
     *
     * Pausado, a resposta é sempre a mesma, independentemente de quantas quartas passem.
     */
    fun weekIndex(state: RotationState, today: LocalDate): Int =
        state.pausedAtWeek ?: weeksSinceAnchor(state.anchor, today)

    /** Congela a escala como ela está hoje. */
    fun pause(state: RotationState, today: LocalDate): RotationState =
        if (state.isPaused) state
        else state.copy(pausedAtWeek = weeksSinceAnchor(state.anchor, today))

    /**
     * Volta a andar a partir da escala congelada.
     *
     * A âncora recua tantas semanas quanto o índice congelado, de forma que hoje continue
     * sendo a mesma escala e a próxima quarta seja a seguinte. Sem isso, retomar depois de
     * um mês de férias saltaria quatro semanas de uma vez, e quem estava devendo a louça
     * escaparia dela.
     */
    fun resume(state: RotationState, today: LocalDate): RotationState {
        val frozen = state.pausedAtWeek ?: return state
        val currentTurn = turnDayOnOrBefore(today)
        return RotationState(
            anchor = currentTurn.minusWeeks(frozen),
            pausedAtWeek = null
        )
    }

    /**
     * A escala de uma semana: cada tarefa com o grupo que a pegou.
     *
     * As tarefas ficam na ordem em que foram declaradas e os grupos deslizam por cima. Na
     * semana 0 cada grupo pega a tarefa de mesma posição; a cada semana, todo grupo desce
     * uma casa na lista e quem estava na última volta para a primeira.
     */
    fun assign(
        chores: List<Chore>,
        groups: List<ChoreGroup>,
        week: Int,
        doneChoreIds: Set<String> = emptySet()
    ): List<ChoreTask> {
        if (groups.isEmpty()) {
            return chores.map {
                ChoreTask(it.id, it.name, emptyList(), isRest = it.isRest)
            }
        }

        return chores.mapIndexed { index, chore ->
            val group = groups[(index - week).mod(groups.size)]
            ChoreTask(
                id = chore.id,
                name = chore.name,
                assigneeIds = group.memberIds,
                // Folga não se marca, então nunca chega marcada na tela mesmo que o id
                // esteja no conjunto — uma tarefa que virou folga no rodízio seguinte não
                // pode aparecer com o traço de feita da semana passada.
                done = !chore.isRest && chore.id in doneChoreIds,
                isRest = chore.isRest
            )
        }
    }

    /**
     * O intervalo da semana em que [date] cai: "12 a 18 de agosto".
     *
     * Sai da data, e não de um texto fixo, porque um rótulo escrito à mão fica errado na
     * quarta seguinte — e um rótulo errado é pior do que rótulo nenhum numa escala que
     * define quem lava a louça.
     *
     * O mês só aparece dos dois lados quando a semana vira o mês: "26 de agosto a 1 de
     * setembro".
     */
    fun weekRangeLabel(date: LocalDate): String {
        val start = turnDayOnOrBefore(date)
        val end = LocalDate.fromEpochDays(start.toEpochDays() + 6)

        return if (start.month == end.month) {
            "${start.day} a ${end.day} de ${monthName(end)}"
        } else {
            "${start.day} de ${monthName(start)} a ${end.day} de ${monthName(end)}"
        }
    }

    private fun monthName(date: LocalDate): String = MONTHS[date.month.ordinal]

    private val MONTHS = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    )

    /** A quarta-feira de [date], ou a própria data se ela já for quarta. */
    fun turnDayOnOrBefore(date: LocalDate): LocalDate {
        val diff = (date.dayOfWeek.ordinal - TURN_DAY.ordinal).mod(7)
        return date.minusDays(diff)
    }

    /**
     * Quantas viradas de quarta-feira separam [anchor] de [today].
     *
     * Divisão para baixo, e não truncada: com uma âncora à frente da data — relógio do
     * aparelho atrasado, por exemplo — o truncamento devolveria 0 para qualquer atraso
     * menor que uma semana e a escala pularia de volta ao início.
     */
    private fun weeksSinceAnchor(anchor: LocalDate, today: LocalDate): Int {
        val days = today.toEpochDays() - turnDayOnOrBefore(anchor).toEpochDays()
        return days.floorDiv(7).toInt()
    }
}

private fun LocalDate.minusDays(days: Int): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() - days)

private fun LocalDate.minusWeeks(weeks: Int): LocalDate = minusDays(weeks * 7)
