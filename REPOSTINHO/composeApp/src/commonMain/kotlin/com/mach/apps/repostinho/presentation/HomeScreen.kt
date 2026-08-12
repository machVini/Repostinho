package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.ui.LocalDarkTheme
import com.mach.apps.repostinho.ui.RepIcons
import com.mach.apps.repostinho.data.model.MeetingNotes
import com.mach.apps.repostinho.data.model.ChoreTask
import org.jetbrains.compose.resources.painterResource
import repostinho.composeapp.generated.resources.Res
import repostinho.composeapp.generated.resources.logo_peito
import repostinho.composeapp.generated.resources.logo_rep
import com.mach.apps.repostinho.ui.positiveColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: BankUiState,
    sheet: SheetUiState,
    notes: MeetingNotes,
    tasks: List<ChoreTask>,
    onGoToBanco: () -> Unit,
    onGoToTarefas: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val myTask = tasks.firstOrNull { state.currentResidentId in it.assigneeIds }

    PullToRefreshBox(
        isRefreshing = sheet.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        HomeContent(state, sheet, notes, tasks, myTask, onGoToBanco, onGoToTarefas)
    }
}

@Composable
private fun HomeContent(
    state: BankUiState,
    sheet: SheetUiState,
    notes: MeetingNotes,
    tasks: List<ChoreTask>,
    myTask: ChoreTask?,
    onGoToBanco: () -> Unit,
    onGoToTarefas: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item { Greeting() }

        item { MyBalanceCard(sheet, onGoToBanco) }
        item { MyTaskCard(myTask, tasks, onGoToTarefas) }
        item { MeetingNotesCard(notes) }
        item { RepSummaryCard(sheet) }
    }
}

/**
 * Saudação com os dois brasões da rep à direita.
 *
 * O texto leva `weight` para os logos nunca serem empurrados para fora: num aparelho
 * estreito quem cede é o nome, não a marca.
 */
@Composable
private fun Greeting() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Olá, ${SheetUiState.CURRENT_MEMBER_NAME}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "República Postinho · desde 2023",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Image(
            painter = painterResource(Res.drawable.logo_rep),
            contentDescription = "Brasão da República Postinho",
            modifier = Modifier.size(LOGO_SIZE)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Image(
            painter = painterResource(Res.drawable.logo_peito),
            contentDescription = "Logo secundário da rep",
            // O secundário é traço azul-marinho chapado: no fundo escuro ele quase some.
            // Como é de uma cor só, tingir resolve sem precisar de um segundo arquivo.
            colorFilter = if (LocalDarkTheme.current) {
                ColorFilter.tint(MaterialTheme.colorScheme.secondary)
            } else {
                null
            },
            modifier = Modifier.size(LOGO_SIZE)
        )
    }
}

private val LOGO_SIZE = 34.dp

@Composable
private fun MyBalanceCard(sheet: SheetUiState, onGoToBanco: () -> Unit) {
    val balance = sheet.myBalanceCents ?: 0L
    // Escolha da sessão: fechar o app volta a mostrar. Guardar isso exigiria persistir
    // preferência, que o app ainda não tem.
    var hidden by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Meu saldo", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { hidden = !hidden }) {
                    Icon(
                        imageVector = if (hidden) RepIcons.EyeOpen else RepIcons.EyeClosed,
                        contentDescription = if (hidden) "Mostrar o saldo"
                        else "Esconder o saldo"
                    )
                }
            }

            Text(
                text = if (hidden) MASK else formatBrl(balance),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                // Escondido, a cor entregaria o sinal do saldo — some junto com o valor.
                color = when {
                    hidden -> MaterialTheme.colorScheme.onPrimaryContainer
                    balance < 0 -> MaterialTheme.colorScheme.error
                    else -> positiveColor()
                }
            )
            TextButton(onClick = onGoToBanco) {
                Text("Ver o banco", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private const val MASK = "R$ ••••••"

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

/**
 * As últimas atas da pasta do Drive.
 *
 * O app não lê o conteúdo: cada linha abre o arquivo no Drive. Quem pode abrir é decidido
 * pelo compartilhamento da pasta, não aqui.
 */
@Composable
private fun MeetingNotesCard(notes: MeetingNotes) {
    val uriHandler = LocalUriHandler.current

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Atas de reunião", fontWeight = FontWeight.Bold)

            if (notes.files.isEmpty()) {
                Text(
                    text = "Nenhuma ata encontrada na pasta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            notes.files.forEach { note ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(note.url) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = RepIcons.OpenExternal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sem a pasta não há para onde levar — o botão sumiria em vez de abrir nada.
            if (notes.folderUrl.isNotBlank()) {
                TextButton(onClick = { uriHandler.openUri(notes.folderUrl) }) {
                    Text("Ver mais", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RepSummaryCard(sheet: SheetUiState) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("A rep", fontWeight = FontWeight.Bold)

            SummaryLine("Moradores ativos", sheet.activeMembers.size.toString())
            SummaryLine("Caixinha", formatBrl(sheet.caixinhaTotalCents ?: 0L))
            SummaryLine("Total a receber", formatBrl(sheet.totalOwedCents))
            SummaryLine("Lançamentos", sheet.movements.size.toString())
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // O rótulo cede espaço; o valor nunca quebra. Sem o peso, "Total a receber" e
        // um número de cinco dígitos colidiam em tela estreita.
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
