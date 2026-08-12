package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.data.model.ChoreTask
import com.mach.apps.repostinho.ui.positiveColor

@Composable
fun PerfilScreen(
    state: BankUiState,
    /** Saldo da planilha, não recalculado aqui — mesmo número que a Home e a aba Saldos. */
    myBalanceCents: Long?,
    tasks: List<ChoreTask>,
    modifier: Modifier = Modifier
) {
    val resident = state.currentResident
    val myTask = tasks.firstOrNull { state.currentResidentId in it.assigneeIds }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = resident?.name ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = resident?.roomType?.label ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Dados", fontWeight = FontWeight.Bold)
                    InfoLine("Aniversário", resident?.birthDate ?: "—")
                    InfoLine("Entrou na rep", resident?.joinedAt ?: "—")
                    InfoLine("Quarto", resident?.roomType?.label ?: "—")
                    InfoLine(
                        "Papel",
                        if (resident?.isModerator == true) "Responsável pelo financeiro"
                        else "Morador"
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Na rep", fontWeight = FontWeight.Bold)

                    myBalanceCents?.let { balance ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Saldo",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatBrl(balance),
                                fontWeight = FontWeight.Bold,
                                color = if (balance < 0) MaterialTheme.colorScheme.error
                                else positiveColor()
                            )
                        }
                    }
                    InfoLine("Tarefa da semana", myTask?.name ?: "Sem tarefa")
                    if (myTask != null && !myTask.isRest) {
                        InfoLine("Tarefa feita", if (myTask.done) "Sim" else "Ainda não")
                    }
                }
            }
        }

        item {
            Text(
                text = "Sessão fixa no VK — ainda não há login.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(value, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
