package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.mach.apps.repostinho.data.repository.RotatingChoreRepository
import com.mach.apps.repostinho.data.repository.RotationStatus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import com.mach.apps.repostinho.ui.accentColor

@Composable
fun TarefasScreen(
    tasks: List<ChoreTask>,
    residents: List<Resident>,
    currentResidentId: String,
    rotation: RotationStatus,
    /** Mostra o botão de virar a escala. Quem confere de verdade é o Worker. */
    canRotate: Boolean,
    rotationAction: RotationAction,
    onSetDone: (taskId: String, done: Boolean) -> Unit,
    onRotate: () -> Unit,
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
            RotationCard(
                rotation = rotation,
                canRotate = canRotate,
                action = rotationAction,
                onRotate = onRotate
            )
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
                text = "A escala vira sozinha toda sexta-feira ao meio-dia. Quem está " +
                    "em cada dupla ainda é fixo no app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

/**
 * Desde quando vale esta escala, e o botão de virá-la.
 *
 * A escala não vira mais sozinha: ela anda quando alguém da rep roda. Por isso o cartão
 * diz **desde quando** a atual está valendo, em vez do intervalo de datas que existia
 * quando o calendário mandava — um intervalo ali agora prometeria uma virada que não vai
 * acontecer.
 *
 * O botão aparece só para quem pode. Esconder não é segurança: quem confere é o Worker,
 * pelo email dentro do token. Aqui é só para não oferecer a quem vai levar um "não".
 */
@Composable
private fun RotationCard(
    rotation: RotationStatus,
    canRotate: Boolean,
    action: RotationAction,
    onRotate: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = rotation.rotatedAt?.let { "Escala desde ${diaEMes(it)}" }
                    ?: "Escala atual",
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

            if (canRotate) {
                Button(
                    onClick = onRotate,
                    // Enquanto a chamada está no ar o botão fica desligado: dois toques
                    // seriam duas viradas, e a rep pularia uma escala inteira.
                    enabled = !action.isBusy,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text(if (action.isBusy) "Virando…" else "Virar a escala")
                }
                Text(
                    text = action.error ?: "Todo mundo passa para a próxima tarefa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (action.error != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/**
 * "16 de outubro", a partir do instante ISO que o servidor manda.
 *
 * Data torta não pode derrubar a tela de tarefas: se o texto não for o esperado, o cartão
 * volta a dizer só "Escala atual".
 */
@OptIn(ExperimentalTime::class)
private fun diaEMes(iso: String): String? {
    val data = runCatching { Instant.parse(iso).toLocalDateTime(RotatingChoreRepository.CAMPINAS) }
        .getOrNull() ?: return null
    return "${data.day} de ${monthName(data.month.ordinal + 1)}"
}
