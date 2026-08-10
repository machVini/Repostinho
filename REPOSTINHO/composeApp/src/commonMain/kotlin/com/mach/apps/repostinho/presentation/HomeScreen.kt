package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.data.model.ChoreTask
import com.mach.apps.repostinho.ui.positiveColor

@Composable
fun HomeScreen(
    state: BankUiState,
    tasks: List<ChoreTask>,
    onGoToBanco: () -> Unit,
    onGoToTarefas: () -> Unit,
    modifier: Modifier = Modifier
) {
    val myTask = tasks.firstOrNull { state.currentResidentId in it.assigneeIds }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                Text(
                    text = "Olá, ${state.currentResident?.name ?: ""}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "República Postinho · desde 2023",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { MyBalanceCard(state, onGoToBanco) }
        item { MyTaskCard(myTask, tasks, onGoToTarefas) }
        item { MeetingNotesCard() }
        item { RepSummaryCard(state) }
    }
}

@Composable
private fun MyBalanceCard(state: BankUiState, onGoToBanco: () -> Unit) {
    val balance = state.currentBalanceCents ?: 0L

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Meu saldo", style = MaterialTheme.typography.titleMedium)
            Text(
                text = formatBrl(balance),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (balance < 0) MaterialTheme.colorScheme.error else positiveColor()
            )
            TextButton(onClick = onGoToBanco) {
                Text("Ver o banco", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MyTaskCard(
    myTask: ChoreTask?,
    tasks: List<ChoreTask>,
    onGoToTarefas: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Minha tarefa da semana", style = MaterialTheme.typography.titleMedium)
            Text(
                text = myTask?.name ?: "Sem tarefa",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    myTask == null -> "Você não está escalado nesta semana."
                    myTask.isRest -> "Semana de folga, nada a marcar."
                    myTask.done -> "Já marcada como feita."
                    else -> "Ainda não marcada."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            // A folga não conta como tarefa no placar da rep.
            val chores = tasks.filter { !it.isRest }
            Text(
                text = "${chores.count { it.done }} de ${chores.size} tarefas feitas na rep",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            TextButton(onClick = onGoToTarefas) {
                Text("Ver a escala", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Atas ainda não existem como funcionalidade: o card é só o lugar reservado para elas. */
@Composable
private fun MeetingNotesCard() {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Atas de reunião", fontWeight = FontWeight.Bold)
            Text(
                text = "Ainda não implementado.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Aqui entrariam as decisões das reuniões da rep, com data e " +
                    "quem estava presente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun RepSummaryCard(state: BankUiState) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("A rep", fontWeight = FontWeight.Bold)

            SummaryLine("Moradores ativos", state.activeResidents.size.toString())
            SummaryLine("Aluguel + contas", formatBrl(state.settings.monthlyFixedTotalCents))
            SummaryLine("Total a receber", formatBrl(state.totalOwedCents))
            SummaryLine("Lançamentos", state.transactionCount.toString())
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
