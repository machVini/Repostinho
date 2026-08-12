package com.mach.apps.repostinho.data.model

import kotlinx.serialization.Serializable

/**
 * Data sem hora e sem fuso.
 *
 * O app não tem `kotlinx-datetime`, e a agenda da rep não precisa: são datas de calendário
 * que ninguém converte nem compara com "agora". Três inteiros resolvem sem dependência nova.
 */
@Serializable
data class RepDate(val day: Int, val month: Int, val year: Int)

/** Um evento da rep. Eventos de um dia só deixam [end] igual a [start]. */
@Serializable
data class RepEvent(
    val id: String,
    val name: String,
    val start: RepDate,
    val end: RepDate = start
) {
    val isMultiDay: Boolean get() = start != end
}
