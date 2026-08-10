package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.data.model.Resident
import com.mach.apps.repostinho.data.model.RoomType

@Composable
fun ResidentsScreen(
    state: BankUiState,
    onSaveResident: (Resident) -> Unit,
    onRemoveResident: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item { NewResidentCard(onSaveResident) }

        item {
            Text(
                text = "Moradores",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        items(state.residents) { resident ->
            ResidentRow(resident, onSaveResident, onRemoveResident)
        }
    }
}

@Composable
private fun NewResidentCard(onSaveResident: (Resident) -> Unit) {
    var name by remember { mutableStateOf("") }
    var roomType by remember { mutableStateOf(RoomType.INDIVIDUAL) }
    var isModerator by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Novo morador", fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            RoomTypeSelector(selected = roomType, onSelect = { roomType = it })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isModerator, onCheckedChange = { isModerator = it })
                Text("Responsável pelo financeiro")
            }

            Button(
                onClick = {
                    onSaveResident(
                        Resident(name = name, roomType = roomType, isModerator = isModerator)
                    )
                    name = ""
                    roomType = RoomType.INDIVIDUAL
                    isModerator = false
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Cadastrar")
            }
        }
    }
}

@Composable
private fun ResidentRow(
    resident: Resident,
    onSaveResident: (Resident) -> Unit,
    onRemoveResident: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resident.name,
                    fontWeight = FontWeight.Bold,
                    color = if (resident.isActive) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = buildString {
                        append(resident.roomType.label)
                        if (resident.isModerator) append(" · financeiro")
                        if (!resident.isActive) append(" · inativo")
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (resident.isActive) {
                TextButton(onClick = { onRemoveResident(resident.id) }) { Text("Remover") }
            } else {
                TextButton(onClick = { onSaveResident(resident.copy(isActive = true)) }) {
                    Text("Reativar")
                }
            }
        }
    }
}

@Composable
private fun RoomTypeSelector(selected: RoomType, onSelect: (RoomType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Quarto")
                Text(selected.label)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RoomType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
