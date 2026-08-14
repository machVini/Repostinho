package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mach.apps.repostinho.data.model.ChoreTask
import com.mach.apps.repostinho.data.model.Resident
import com.mach.apps.repostinho.ui.positiveColor

@Composable
fun PerfilScreen(
    state: BankUiState,
    /** Saldo da planilha, não recalculado aqui — mesmo número que a Home e a aba Saldos. */
    myBalanceCents: Long?,
    tasks: List<ChoreTask>,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var saindo by remember { mutableStateOf(false) }
    val resident = state.currentResident
    val myTask = tasks.firstOrNull { state.currentResidentId in it.assigneeIds }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            // Sem card em volta: a foto é o retrato de quem está usando o app, e uma
            // moldura ao redor dela a deixaria parecendo mais um dado numa lista.
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ResidentPhoto(
                    photoUrl = resident?.photoUrl,
                    name = resident?.name.orEmpty(),
                    size = 160.dp
                )
                Text(
                    text = resident?.name ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Dados", fontWeight = FontWeight.Bold)
                    InfoLine("Aniversário", formatBirthday(resident) ?: "—")
                    InfoLine("Entrou na rep", formatJoined(resident) ?: "—")
                    InfoLine("Quarto", resident?.roomType?.label ?: "—")
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
                text = resident?.email ?: "Sem email cadastrado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            OutlinedButton(
                onClick = { saindo = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Text("Sair")
            }
        }
    }

    if (saindo) {
        AlertDialog(
            onDismissRequest = { saindo = false },
            title = { Text("Sair da conta?") },
            text = { Text("Você vai precisar entrar de novo com email e senha.") },
            confirmButton = {
                TextButton(onClick = {
                    saindo = false
                    onSignOut()
                }) {
                    Text("Sair")
                }
            },
            dismissButton = {
                TextButton(onClick = { saindo = false }) { Text("Cancelar") }
            }
        )
    }
}

/** "Março de 2026". O mês vem com inicial maiúscula por ser o começo da frase. */
private fun formatJoined(resident: Resident?): String? {
    val month = resident?.joinedMonth ?: return null
    val year = resident.joinedYear ?: return null
    return "${monthName(month).replaceFirstChar { it.uppercase() }} de $year"
}

/** "20 de fevereiro". Sem ano: a idade de ninguém precisa aparecer no perfil. */
private fun formatBirthday(resident: Resident?): String? {
    val day = resident?.birthDay ?: return null
    val month = resident.birthMonth ?: return null
    return "$day de ${monthName(month)}"
}

/**
 * Foto do morador, ou a inicial dele num círculo.
 *
 * O monograma não é enfeite: a foto vem por URL e pode faltar, demorar ou falhar, e um
 * buraco no topo do perfil parece tela quebrada.
 */
@Composable
private fun ResidentPhoto(photoUrl: String?, name: String, size: Dp) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            // A inicial acompanha o círculo: num monograma de 160dp, uma letra de
            // tamanho fixo ficaria perdida no meio.
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Foto de $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
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
