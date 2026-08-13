package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.data.model.MovementType
import com.mach.apps.repostinho.data.remote.LancamentoDraft
import com.mach.apps.repostinho.data.remote.LancamentoForm

/**
 * Monta um lançamento e abre o formulário do banco já preenchido.
 *
 * O app não envia nada: quem envia é o morador, no Forms, depois de conferir. A planilha
 * continua sendo a única fonte da verdade — este dialog só evita digitar 26 campos de peso
 * no celular, que é o que realmente dói no formulário de hoje.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LancamentoDialog(
    /** Os nomes que a planilha conhece, vindos do `/banco`. */
    participants: List<String>,
    currentMemberName: String,
    onDismiss: () -> Unit,
    onOpenForm: (url: String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MovementType.COLETIVO) }
    var payer by remember { mutableStateOf(currentMemberName) }

    // Só oferece quem tem campo no Forms: um peso para alguém de fora não teria onde ser
    // preenchido, e o lançamento sairia errado sem ninguém perceber.
    val elegiveis = remember(participants) {
        participants.filter { it in LancamentoForm.nomesConhecidos }
    }
    val selecionados = remember { mutableStateMapOf<String, Boolean>() }

    // Coletivo é "todo mundo com peso 1", que é o caso mais comum e o mais chato de
    // digitar. Entrar na tela já com todos marcados poupa 26 toques.
    remember(type, elegiveis) {
        if (type == MovementType.COLETIVO) {
            elegiveis.forEach { selecionados[it] = true }
        }
        true
    }

    val cents = parseBrlToCents(value)
    val marcados = elegiveis.filter { selecionados[it] == true }
    // Entrada não tem rateio: é dinheiro chegando, e a planilha espera peso nenhum.
    val precisaDeParticipante = type != MovementType.ENTRADA
    val valid = description.isNotBlank() &&
        cents != null && cents > 0 &&
        (!precisaDeParticipante || marcados.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo lançamento") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Valor (R$)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Rotulo("Tipo")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MovementType.entries.forEach { option ->
                        FilterChip(
                            selected = option == type,
                            onClick = {
                                type = option
                                // Trocar para Coletivo remarca todo mundo; sair dele limpa,
                                // senão o rateio anterior vaza para um lançamento privado.
                                if (option == MovementType.COLETIVO) {
                                    elegiveis.forEach { selecionados[it] = true }
                                } else {
                                    selecionados.clear()
                                }
                            },
                            label = { Text(rotulo(option)) }
                        )
                    }
                }

                Rotulo("Pagador")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LancamentoForm.pagadores.forEach { option ->
                        FilterChip(
                            selected = option == payer,
                            onClick = { payer = option },
                            label = { Text(option) }
                        )
                    }
                }

                if (precisaDeParticipante) {
                    Rotulo("Rateado entre (${marcados.size})")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        elegiveis.forEach { name ->
                            FilterChip(
                                selected = selecionados[name] == true,
                                onClick = {
                                    selecionados[name] = selecionados[name] != true
                                },
                                label = { Text(name) }
                            )
                        }
                    }
                    Text(
                        text = "Todos entram com peso 1. Peso diferente disso ainda é no " +
                            "formulário.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Nome que a planilha tem e o Forms não: quem não aparece na lista acima
                // precisa saber por quê, senão vira "sumiu o Fulano".
                val semCampo = LancamentoForm.semCampoNoForms(participants)
                if (semCampo.isNotEmpty()) {
                    Text(
                        text = "Sem campo no formulário: ${semCampo.joinToString(", ")}. " +
                            "Para incluir alguém daí, use o formulário direto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val draft = LancamentoDraft(
                        description = description.trim(),
                        type = type,
                        payer = payer,
                        valueCents = cents ?: 0L,
                        weights = if (precisaDeParticipante) {
                            marcados.associateWith { 1.0 }
                        } else {
                            emptyMap()
                        }
                    )
                    onDismiss()
                    onOpenForm(LancamentoForm.urlFor(draft))
                }
            ) {
                Text("Abrir formulário")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun Rotulo(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

private fun rotulo(type: MovementType): String = when (type) {
    MovementType.PRIVADO -> "Privado"
    MovementType.COLETIVO -> "Coletivo"
    MovementType.SAIDA -> "Saída"
    MovementType.ENTRADA -> "Entrada"
}
