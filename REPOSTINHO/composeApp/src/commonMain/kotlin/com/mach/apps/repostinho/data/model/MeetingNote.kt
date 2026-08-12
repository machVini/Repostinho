package com.mach.apps.repostinho.data.model

import kotlinx.serialization.Serializable

/**
 * Uma ata de reunião guardada no Drive da rep.
 *
 * O app não lê o conteúdo: [url] abre o arquivo no app do Drive, ou no navegador quando
 * ele não está instalado. Quem controla quem pode abrir é o compartilhamento da pasta,
 * não o app.
 */
@Serializable
data class MeetingNote(
    val id: String,
    val name: String,
    val url: String
)

/** A pasta e as últimas atas dela. */
@Serializable
data class MeetingNotes(
    val folderUrl: String = "",
    val files: List<MeetingNote> = emptyList()
)
