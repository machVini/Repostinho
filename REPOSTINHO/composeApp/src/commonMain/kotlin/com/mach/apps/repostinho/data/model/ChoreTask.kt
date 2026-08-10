package com.mach.apps.repostinho.data.model

import kotlinx.serialization.Serializable

/** Uma tarefa da escala semanal e a dupla ou trio responsável por ela. */
@Serializable
data class ChoreTask(
    val id: String,
    val name: String,
    val assigneeIds: List<String>,
    val done: Boolean = false,
    /** Folga não é tarefa: aparece na escala, mas não tem o que marcar como feito. */
    val isRest: Boolean = false
)
