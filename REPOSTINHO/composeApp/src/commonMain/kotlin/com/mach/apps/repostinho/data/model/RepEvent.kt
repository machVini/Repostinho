package com.mach.apps.repostinho.data.model

import kotlinx.serialization.Serializable

/**
 * Data sem hora e sem fuso.
 *
 * Continua sendo três inteiros mesmo com o `kotlinx-datetime` já no projeto: é o formato
 * que trafega no JSON do `banco-api`, e um aniversário não tem fuso — o dia 20/02 é 20/02
 * em qualquer lugar. A conversão para `LocalDate` acontece só na hora de comparar com hoje.
 */
@Serializable
data class RepDate(val day: Int, val month: Int, val year: Int) : Comparable<RepDate> {
    override fun compareTo(other: RepDate): Int = compareValuesBy(
        this, other, { it.year }, { it.month }, { it.day }
    )
}

/**
 * A que parte da vida da rep o evento pertence.
 *
 * É o que dá cor ao card: sem categoria, uma agenda com aluguel, aniversário e festa no
 * mesmo tom obriga a ler o nome de cada linha para saber do que se trata.
 */
@Serializable
enum class EventCategory(val label: String) {
    ANIVERSARIO("Aniversários"),
    ROLE("Rolês"),
    CONTA("Vencimento de contas"),
    ARU("ARU")
}

/** De quanto em quanto tempo o evento volta. */
@Serializable
enum class Recurrence {
    /** Data única: acontece uma vez e sai da agenda. */
    NENHUMA,

    /** Todo mês no mesmo dia — o aluguel. */
    MENSAL,

    /** Todo ano no mesmo dia e mês — aniversários. */
    ANUAL
}

/**
 * Um evento da rep. Eventos de um dia só deixam [end] igual a [start].
 *
 * Em eventos recorrentes, [start] é a primeira ocorrência: o ano dela não limita nada, só
 * diz de quando em diante a data vale.
 */
@Serializable
data class RepEvent(
    val id: String,
    val name: String,
    val start: RepDate,
    val end: RepDate = start,
    val category: EventCategory = EventCategory.ROLE,
    val recurrence: Recurrence = Recurrence.NENHUMA,
    /**
     * Merece se destacar na lista — hoje só o aniversário da rep.
     *
     * É sinalizador, e não uma quinta categoria, porque o aniversário da rep continua
     * sendo um aniversário: ele muda de cor, não de gaveta.
     */
    val isHighlight: Boolean = false,
    /**
     * Veio da tela, e não da agenda fixa do app.
     *
     * Só o que foi adicionado por alguém pode ser apagado por alguém: as datas que o app
     * já traz de fábrica não somem por um toque errado.
     */
    val isCustom: Boolean = false
) {
    val isMultiDay: Boolean get() = start != end
}

/**
 * Uma ocorrência de um evento numa data concreta.
 *
 * O aluguel é um evento só, mas aparece cinco vezes entre agosto e dezembro. Quem vai
 * para a tela é isto, não o [RepEvent].
 */
data class EventOccurrence(
    val event: RepEvent,
    val start: RepDate,
    val end: RepDate
) {
    val isMultiDay: Boolean get() = start != end
}
