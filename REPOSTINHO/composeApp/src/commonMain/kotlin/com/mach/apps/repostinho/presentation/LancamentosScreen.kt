package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.data.model.Movement
import com.mach.apps.repostinho.data.model.MovementType
import com.mach.apps.repostinho.ui.MenuMaxHeight
import com.mach.apps.repostinho.ui.RepIcons
import com.mach.apps.repostinho.ui.accentColor
import com.mach.apps.repostinho.ui.positiveColor
import com.mach.apps.repostinho.ui.rememberMenuToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LancamentosScreen(
    movements: List<Movement>,
    residentNames: List<String>,
    currentMemberName: String,
    modifier: Modifier = Modifier
) {
    // null = sem filtro. É o único estado que não corresponde a um morador de verdade,
    // então não pode ser um nome vazio — colidiria se algum dia existisse um assim.
    var filterName by remember { mutableStateOf<String?>(null) }
    val menu = rememberMenuToggle()
    var lancando by remember { mutableStateOf(false) }

    // O lançamento sai daqui para o Forms, que é quem de fato escreve na planilha.
    val uriHandler = LocalUriHandler.current

    val filtered = remember(movements, filterName) {
        filterName?.let { name -> movements.filter { it.involves(name) } } ?: movements
    }
    // Mais recente primeiro: na planilha os lançamentos são acrescentados no fim.
    val ordered = filtered.asReversed()

    // A planilha entrega os nomes na ordem das colunas dela, que não ajuda quem procura
    // alguém numa lista de 25.
    val otherNames = remember(residentNames, currentMemberName) {
        residentNames.filter { it != currentMemberName }.sortedByNome()
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "Lançamentos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = filterSubtitle(filtered.size, filterName, currentMemberName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterName == null,
                        onClick = { filterName = null },
                        label = { Text("Todos") }
                    )
                    FilterChip(
                        selected = filterName == currentMemberName,
                        onClick = { filterName = currentMemberName },
                        label = { Text("Minhas") }
                    )

                    // Só aparece se houver outro morador para filtrar: sem a planilha
                    // carregada, a lista fica vazia e o chip não teria o que abrir.
                    if (otherNames.isNotEmpty()) {
                        Box {
                            val isOther = filterName != null && filterName != currentMemberName
                            FilterChip(
                                selected = isOther,
                                onClick = { menu.onAnchorClick() },
                                label = { Text(if (isOther) filterName!! else "Morador…") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (menu.expanded) RepIcons.ExpandLess
                                        else RepIcons.ExpandMore,
                                        contentDescription = if (menu.expanded) "Fechar a lista"
                                        else "Escolher morador"
                                    )
                                }
                            )
                            DropdownMenu(
                                expanded = menu.expanded,
                                onDismissRequest = { menu.dismiss() },
                                // Sem limite, 25 nomes cobrem a lista de lançamentos toda.
                                modifier = Modifier.heightIn(max = MenuMaxHeight)
                            ) {
                                otherNames.forEach { name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            filterName = name
                                            menu.select()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { lancando = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("Novo lançamento")
            }
        }

        // Só com filtro: sem ele, lista vazia é planilha não carregada, e quem explica
        // isso é o aviso de sincronização no topo da tela.
        if (ordered.isEmpty() && filterName != null) {
            item {
                val who = if (filterName == currentMemberName) "você" else filterName
                Text(
                    text = "Nenhum lançamento envolvendo $who.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }

        items(ordered) { movement ->
            MovementCard(movement, currentMemberName)
        }

        item {
            Text(
                text = "Quem grava o lançamento é o formulário, não o app — a planilha " +
                    "segue sendo a fonte da verdade.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }

    if (lancando) {
        LancamentoDialog(
            participants = residentNames,
            currentMemberName = currentMemberName,
            onDismiss = { lancando = false },
            onOpenForm = { uriHandler.openUri(it) }
        )
    }
}

private fun filterSubtitle(count: Int, filterName: String?, currentMemberName: String): String {
    val suffix = when (filterName) {
        null -> "no banco atual."
        currentMemberName -> "envolvendo você."
        else -> "envolvendo $filterName."
    }
    return "$count lançamentos $suffix"
}

@Composable
private fun MovementCard(movement: Movement, currentMemberName: String) {
    val isEntry = movement.type == MovementType.ENTRADA
    val myWeight = movement.weights[currentMemberName]
    val expandable = movement.participantCount > 0
    var expanded by remember(movement.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            // Entrada não tem rateio: sem participantes, não há o que abrir, e um card
            // que responde ao toque sem mostrar nada frustra mais do que um inerte.
            .then(
                if (expandable) Modifier.clickable { expanded = !expanded } else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(movement.description, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${label(movement.type)} · ${movement.payer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatBrl(movement.valueCents),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    // Entrada é dinheiro chegando; o resto é gasto a ratear.
                    color = if (isEntry) positiveColor() else MaterialTheme.colorScheme.onSurface
                )
            }

            if (expandable) {
                val people = if (movement.participantCount == 1) "1 pessoa"
                else "${movement.participantCount} pessoas"
                val weight = formatWeight(movement.totalWeight)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rateado entre $people · $weight " +
                            if (movement.totalWeight == 1.0) "peso" else "pesos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded) RepIcons.ExpandLess
                        else RepIcons.ExpandMore,
                        contentDescription = if (expanded) "Esconder o rateio"
                        else "Ver o rateio",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (myWeight != null && !expanded) {
                // Fechado, esta linha é o resumo do que interessa ao morador. Aberto, ela
                // repetiria o que a lista já mostra destacado.
                Text(
                    text = "Você entrou com ${formatWeight(myWeight)} de peso",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor(),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (expanded) {
                SplitBreakdown(movement, currentMemberName)
            }
        }
    }
}

/**
 * Quem entrou no lançamento, com peso e quanto deve.
 *
 * Ordenado do maior para o menor valor: em rateio com pesos diferentes, a primeira
 * pergunta costuma ser quem pagou mais.
 */
@Composable
private fun SplitBreakdown(movement: Movement, currentMemberName: String) {
    val shares = movement.sharesInCents()
    val ordered = shares.entries.sortedWith(
        compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key }
    )

    Column(modifier = Modifier.padding(top = 12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        ordered.forEach { (name, cents) ->
            val isMine = name == currentMemberName
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isMine) FontWeight.Bold else FontWeight.Normal,
                    color = if (isMine) accentColor() else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatWeight(movement.weights.getValue(name)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formatBrl(cents),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isMine) FontWeight.Bold else FontWeight.Normal,
                    color = if (isMine) accentColor() else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

private fun label(type: MovementType): String = when (type) {
    MovementType.COLETIVO -> "Coletivo"
    MovementType.PRIVADO -> "Privado"
    MovementType.ENTRADA -> "Entrada"
    MovementType.SAIDA -> "Saída"
}
