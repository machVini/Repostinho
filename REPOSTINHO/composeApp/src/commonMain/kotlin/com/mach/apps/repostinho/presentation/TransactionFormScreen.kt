package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun TransactionFormScreen(
    state: BankUiState,
    onSaveExpense: (description: String, payerId: String, totalCents: Long, weights: Map<String, Double>) -> Unit,
    modifier: Modifier = Modifier
) {
    var description by remember { mutableStateOf("") }
    var totalValue by remember { mutableStateOf("") }
    var payerId by remember { mutableStateOf("") }

    // Texto cru por morador: guardar Double aqui atrapalharia digitar "1,5" (o "1," se perderia).
    val weightInputs = remember { mutableStateMapOf<String, String>() }

    val activeResidents = state.activeResidents
    val payer = activeResidents.firstOrNull { it.id == payerId }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }

        item {
            OutlinedTextField(
                value = totalValue,
                onValueChange = { totalValue = it },
                label = { Text("Valor (R$)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }

        item {
            PayerSelector(
                residents = activeResidents,
                selectedName = payer?.name,
                onSelect = { payerId = it }
            )
        }

        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text("Quem divide", fontWeight = FontWeight.Bold)
                Text(
                    text = "Peso de cada um. Deixe 0 para quem não entra no rateio.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        items(activeResidents) { resident ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(resident.name, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = weightInputs[resident.id] ?: "1",
                    onValueChange = { weightInputs[resident.id] = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(90.dp)
                )
            }
        }

        item {
            Button(
                onClick = {
                    val weights = activeResidents.associate { resident ->
                        resident.id to ((weightInputs[resident.id] ?: "1")
                            .replace(',', '.')
                            .toDoubleOrNull() ?: 0.0)
                    }
                    onSaveExpense(
                        description,
                        payerId,
                        parseBrlToCents(totalValue) ?: 0L,
                        weights
                    )
                    description = ""
                    totalValue = ""
                    weightInputs.clear()
                },
                enabled = payerId.isNotBlank() &&
                    description.isNotBlank() &&
                    (parseBrlToCents(totalValue) ?: 0L) > 0L,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Text("Lançar no banco")
            }
        }
    }
}

@Composable
private fun PayerSelector(
    residents: List<com.mach.apps.repostinho.data.model.Resident>,
    selectedName: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Quem pagou")
                Text(selectedName ?: "Selecionar")
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            residents.forEach { resident ->
                DropdownMenuItem(
                    text = { Text(resident.name) },
                    onClick = {
                        onSelect(resident.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
