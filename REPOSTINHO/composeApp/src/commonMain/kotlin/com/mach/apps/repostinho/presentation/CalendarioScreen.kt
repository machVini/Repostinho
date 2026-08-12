package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.data.model.RepEvent

@Composable
fun CalendarioScreen(
    events: List<RepEvent>,
    modifier: Modifier = Modifier
) {
    // A ordenação é da tela, não do repositório: a lista da agenda é curta e faz mais
    // sentido lida em ordem de data do que na ordem em que foi cadastrada.
    val ordered = events.sortedWith(
        compareBy({ it.start.year }, { it.start.month }, { it.start.day })
    )

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "Eventos da rep",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "As datas do ano, em ordem.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(ordered) { event ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selo de data à esquerda, no formato de folhinha de calendário.
                    Column(
                        modifier = Modifier.width(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = event.start.day.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = monthAbbrev(event.start.month),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatEventPeriod(event),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "A agenda está fixa por enquanto — ainda não dá para cadastrar " +
                    "evento pelo app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}
