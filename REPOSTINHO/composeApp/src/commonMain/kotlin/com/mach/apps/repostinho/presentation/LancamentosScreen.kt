package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.data.model.Movement
import com.mach.apps.repostinho.data.model.MovementType
import com.mach.apps.repostinho.ui.RepIcons
import com.mach.apps.repostinho.ui.accentColor
import com.mach.apps.repostinho.ui.positiveColor

@Composable
fun LancamentosScreen(
    movements: List<Movement>,
    currentMemberName: String,
    modifier: Modifier = Modifier
) {
    // Mais recente primeiro: na planilha os lançamentos são acrescentados no fim.
    val ordered = movements.asReversed()

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "Lançamentos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${movements.size} lançamentos no banco atual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(ordered) { movement ->
            MovementCard(movement, currentMemberName)
        }

        item {
            Text(
                text = "Os lançamentos continuam sendo feitos na planilha — o app só mostra.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
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
