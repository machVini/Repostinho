package com.mach.apps.repostinho.data.model

import kotlinx.serialization.Serializable

/**
 * Uma tarefa da escala, sem dono.
 *
 * Quem faz depende da semana: as tarefas ficam paradas numa ordem e são os grupos que
 * andam por cima delas. Guardar o responsável aqui era o que obrigava a reescrever a
 * escala toda quarta.
 */
@Serializable
data class Chore(
    val id: String,
    val name: String,
    /** Folga não é tarefa: aparece na escala, mas não tem o que marcar como feito. */
    val isRest: Boolean = false
)

/** Uma dupla ou trio que anda junto pelo rodízio. */
@Serializable
data class ChoreGroup(
    val id: String,
    val memberIds: List<String>
)

/**
 * Uma tarefa já com o grupo da semana — o que a tela consome.
 *
 * É resultado de cálculo, não estado guardado: sai de [Chore] + [ChoreGroup] + o índice
 * da semana. Duas aberturas do app na mesma semana produzem exatamente a mesma escala.
 */
@Serializable
data class ChoreTask(
    val id: String,
    val name: String,
    val assigneeIds: List<String>,
    val done: Boolean = false,
    val isRest: Boolean = false
)
