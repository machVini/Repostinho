package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.data.model.ChoreTask
import com.mach.apps.repostinho.data.model.Resident
import com.mach.apps.repostinho.data.repository.RotationStatus
import com.mach.apps.repostinho.ui.accentColor

@Composable
fun TarefasScreen(
    tasks: List<ChoreTask>,
    residents: List<Resident>,
    currentResidentId: String,
    rotation: RotationStatus,
    onSetDone: (taskId: String, done: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val nameById = residents.associate { it.id to it.name }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "Escala da semana",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Você só marca a sua. As outras são consulta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            RotationCard(rotation = rotation)
        }

        items(tasks) { task ->
            val isMine = currentResidentId in task.assigneeIds
            val checkable = isMine && !task.isRest
            val names = task.assigneeIds.mapNotNull { nameById[it] }.joinToString(" · ")

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = if (isMine) {
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    CardDefaults.cardColors()
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.name, fontWeight = FontWeight.Bold)
                        Text(
                            text = names,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (isMine) {
                            Text(
                                text = if (task.isRest) "Sua semana" else "Sua tarefa",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    // Folga não tem o que marcar, então nem checkbox desabilitado aparece.
                    if (!task.isRest) {
                        Checkbox(
                            checked = task.done,
                            onCheckedChange = { onSetDone(task.id, it) },
                            enabled = checkable
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "A escala vira sozinha toda quarta-feira. Quem está em cada dupla " +
                    "ainda é fixo no app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

/**
 * De quando até quando vale esta escala.
 *
 * O intervalo é a primeira pergunta de quem abre a tela ("essa é a desta semana ou a da
 * passada?"), então ele vem antes da lista, não num rodapé.
 *
 * O controle de pausa existe no repositório e está testado, mas não aparece aqui por
 * enquanto: pausar só valeria neste aparelho, e uma escala congelada num celular e
 * girando nos outros é pior do que não poder pausar.
 */
@Composable
private fun RotationCard(rotation: RotationStatus) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Semana ${rotation.rangeLabel}",
                fontWeight = FontWeight.Bold,
                color = accentColor()
            )
            Text(
                text = if (rotation.isShared) {
                    "O que você marcar aparece para a rep."
                } else {
                    "Sem conexão — o que você marcar fica só neste aparelho."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (rotation.isShared) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}
