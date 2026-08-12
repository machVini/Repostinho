package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.data.model.Movement
import com.mach.apps.repostinho.data.model.MovementType
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

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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

            if (movement.participantCount > 0) {
                val people = if (movement.participantCount == 1) "1 pessoa"
                else "${movement.participantCount} pessoas"
                val weight = formatWeight(movement.totalWeight)
                Text(
                    text = "Rateado entre $people · $weight " +
                        if (movement.totalWeight == 1.0) "peso" else "pesos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (myWeight != null) {
                Text(
                    text = "Você entrou com ${formatWeight(myWeight)} de peso",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
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
