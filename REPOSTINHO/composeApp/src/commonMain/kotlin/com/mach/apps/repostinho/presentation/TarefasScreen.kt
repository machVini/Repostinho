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

@Composable
fun TarefasScreen(
    tasks: List<ChoreTask>,
    residents: List<Resident>,
    currentResidentId: String,
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
                text = "As duplas estão fixas por enquanto — o rodízio automático ainda " +
                    "não foi implementado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}
