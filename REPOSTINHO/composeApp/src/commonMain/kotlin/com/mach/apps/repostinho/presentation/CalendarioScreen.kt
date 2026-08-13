package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.data.model.EventCategory
import com.mach.apps.repostinho.data.model.EventOccurrence
import com.mach.apps.repostinho.data.model.RepDate
import com.mach.apps.repostinho.data.model.RepEvent
import com.mach.apps.repostinho.ui.RepIcons
import com.mach.apps.repostinho.ui.accentColor
import com.mach.apps.repostinho.ui.eventCategoryColor

@Composable
fun CalendarioScreen(
    occurrences: List<EventOccurrence>,
    isShared: Boolean,
    /** O ano da janela da agenda — é nele que um evento novo é cadastrado. */
    year: Int,
    onAddEvent: (RepEvent) -> Unit,
    onRemoveEvent: (eventId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var adding by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<RepEvent?>(null) }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "Eventos da rep",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "O que ainda vem este ano, em ordem.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isShared) {
                    Text(
                        text = "Sem conexão — os eventos cadastrados pela rep podem estar " +
                            "desatualizados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            Button(
                onClick = { adding = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("Adicionar evento")
            }
        }

        if (occurrences.isEmpty()) {
            item {
                Text(
                    text = "Nada marcado daqui até o fim do ano.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        items(occurrences) { occurrence ->
            EventCard(
                occurrence = occurrence,
                onRemove = { removing = occurrence.event }
            )
        }

        item {
            CategoryLegend(modifier = Modifier.padding(vertical = 16.dp))
        }
    }

    if (adding) {
        AddEventDialog(
            year = year,
            onDismiss = { adding = false },
            onConfirm = { event ->
                adding = false
                onAddEvent(event)
            }
        )
    }

    removing?.let { event ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Apagar \"${event.name}\"?") },
            text = { Text("Ele sai da agenda de todos os moradores.") },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    onRemoveEvent(event.id)
                }) {
                    Text("Apagar")
                }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun EventCard(
    occurrence: EventOccurrence,
    onRemove: () -> Unit
) {
    val event = occurrence.event
    val categoryColor = eventCategoryColor(event.category)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        // O aniversário da rep se destaca pelo card inteiro, e não por mais uma cor de
        // texto: numa lista onde cada categoria já tem a sua, mais uma cor sumiria no
        // meio. Pintado, ele é o único que muda de forma.
        colors = if (event.isHighlight) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(
                start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selo de data à esquerda, no formato de folhinha de calendário.
            Column(
                modifier = Modifier.width(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = occurrence.start.day.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (event.isHighlight) accentColor() else categoryColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = monthAbbrev(occurrence.start.month),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (event.isHighlight) accentColor() else categoryColor,
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
                    text = formatOccurrencePeriod(occurrence),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CategoryTag(
                    category = event.category,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // Só o que a rep cadastrou pode ser apagado — as datas fixas do app não têm
            // botão, para ninguém tirar o InterReps da agenda sem querer.
            if (event.isCustom) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = RepIcons.Delete,
                        contentDescription = "Apagar ${event.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Bolinha da cor da categoria com o nome ao lado. */
@Composable
private fun CategoryTag(
    category: EventCategory,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(eventCategoryColor(category), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall,
            color = eventCategoryColor(category),
            fontWeight = FontWeight.Bold
        )
    }
}

/** A legenda fecha a lista: sem ela, a cor do selo é enigma até abrir um card de cada tipo. */
@Composable
private fun CategoryLegend(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Categorias",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        EventCategory.entries.forEach { category ->
            CategoryTag(category = category, modifier = Modifier.padding(top = 6.dp))
        }
        Text(
            text = "A agenda fixa (aniversários e ARU) vem no app. O que for " +
                "adicionado aqui aparece para a rep inteira.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

/**
 * Cadastro de evento.
 *
 * Sem seletor de data nativo: o Material 3 tem um, mas ele é enorme para três números que
 * o morador já sabe de cabeça. Três campos curtos são mais rápidos do que navegar um mês
 * de cada vez até novembro.
 */
@Composable
private fun AddEventDialog(
    year: Int,
    onDismiss: () -> Unit,
    onConfirm: (RepEvent) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(EventCategory.ROLE) }

    val dayValue = day.toIntOrNull()
    val monthValue = month.toIntOrNull()
    val valid = name.isNotBlank() &&
        dayValue != null && dayValue in 1..31 &&
        monthValue != null && monthValue in 1..12

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo evento") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = day,
                        onValueChange = { if (it.length <= 2) day = it },
                        label = { Text("Dia") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { if (it.length <= 2) month = it },
                        label = { Text("Mês") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Categoria",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
                // Uma linha por categoria: lado a lado, os nomes quebrariam no meio.
                EventCategory.entries.forEach { option ->
                    FilterChip(
                        selected = option == category,
                        onClick = { category = option },
                        label = { Text(option.label) },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onConfirm(
                        RepEvent(
                            // O id carrega o instante do cadastro para dois moradores não
                            // criarem o mesmo id ao adicionar eventos de nomes parecidos.
                            id = "custom-${name.lowercase().filter { it.isLetterOrDigit() }}" +
                                "-${dayValue}${monthValue}",
                            name = name.trim(),
                            start = RepDate(dayValue!!, monthValue!!, year),
                            category = category,
                            isCustom = true
                        )
                    )
                }
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
