package com.mach.apps.repostinho.data.model

import kotlinx.serialization.Serializable

/**
 * O rodízio das tarefas da rep.
 *
 * A escala já foi função da data: bastava a quarta-feira chegar para ela virar sozinha, e
 * dois aparelhos chegavam no mesmo resultado sem combinar nada. Agora ela é um número
 * guardado no `banco-api`, que só anda quando alguém com permissão roda — a rep pediu
 * para decidir a hora de virar, em vez de o calendário decidir.
 *
 * O que sobrou aqui é a parte que nunca dependeu de data: dado um número de semana, quem
 * pega qual tarefa.
 */
object ChoreRotation {

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
}
