package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun TransactionFormScreen(viewModel: DashboardViewModel) {
    var description by remember { mutableStateOf("") }
    var totalValue by remember { mutableStateOf("") }
    val residents by viewModel.residents.collectAsState() // Lista vinda do Banco
    val weights = remember { mutableStateMapOf<String, Double>() }

    Column {
        TextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") })
        TextField(value = totalValue, onValueChange = { totalValue = it }, label = { Text("Valor (R$)") })

        LazyColumn {
            items(residents) { resident ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(resident.name, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = weights[resident.id]?.toString() ?: "0",
                        onValueChange = { newValue ->
                            weights[resident.id] = newValue.toDoubleOrNull() ?: 0.0
                        },
                        modifier = Modifier.width(60.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }

        Button(onClick = {
            viewModel.save(description, totalValue.toDouble(), weights.toMap())
        }) {
            Text("Lançar no Banco")
        }
    }
}