package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.mach.apps.repostinho.data.model.MovementType
import com.mach.apps.repostinho.data.remote.LancamentoDraft
import com.mach.apps.repostinho.data.remote.LancamentoForm
import com.mach.apps.repostinho.ui.MenuMaxHeight
import com.mach.apps.repostinho.ui.RepIcons
import com.mach.apps.repostinho.ui.accentColor
import com.mach.apps.repostinho.ui.dismissKeyboardOnTap
import com.mach.apps.repostinho.ui.rememberMenuToggle

/**
 * Monta um lançamento e abre o formulário do banco já preenchido.
 *
 * O app não envia nada: quem envia é o morador, no Forms, depois de conferir. A planilha
 * continua sendo a única fonte da verdade — este dialog só evita percorrer os 26 campos de
 * peso no celular, que é o que dói no formulário de hoje.
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
    // Sem tipo pré-selecionado: um padrão vira o tipo mais lançado por descuido, e o tipo
    // decide se o gasto é rateado ou não. Escolher é obrigatório.
    var type by remember { mutableStateOf<MovementType?>(null) }
    var payer by remember { mutableStateOf(currentMemberName) }

    // Só oferece quem tem campo no Forms: um peso para alguém de fora não teria onde ser
    // preenchido, e o lançamento sairia errado sem ninguém perceber.
    val elegiveis = remember(participants) {
        participants.filter { it in LancamentoForm.nomesConhecidos }.sortedByNome()
    }

    // Caixas nas pontas e pessoas em ordem alfabética no meio: misturar "Caix. Crédito"
    // entre os nomes esconderia justamente as três opções que não são gente.
    val pagadores = remember {
        LancamentoForm.caixasDaRep +
            LancamentoForm.nomesConhecidos.toList().sortedByNome() +
            LancamentoForm.caixasExternos
    }

    // Nome -> peso como o morador digitou. Texto, e não Double, porque "0," no meio da
    // digitação não é número mas também não pode apagar o que a pessoa escreveu.
    val pesos = remember { mutableStateMapOf<String, String>() }

    val cents = parseBrlToCents(value)
    // Entrada não tem rateio: é dinheiro chegando, e a planilha espera peso nenhum.
    val precisaDeRateio = type != MovementType.ENTRADA
    val pesosValidos = pesos.mapNotNull { (name, raw) ->
        parseWeight(raw)?.let { name to it }
    }.toMap()
    val pesoInvalido = pesos.any { parseWeight(it.value) == null }

    val valid = description.isNotBlank() &&
        cents != null && cents > 0 &&
        type != null &&
        !pesoInvalido &&
        (!precisaDeRateio || pesosValidos.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo lançamento") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .dismissKeyboardOnTap()
            ) {
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
                    isError = value.isNotBlank() && cents == null,
                    // O que vai para o formulário aparece aqui: ele só aceita ponto, e
                    // quem digita com vírgula precisa ver que a conversão aconteceu.
                    supportingText = {
                        Text(
                            if (cents != null) "Vai como ${LancamentoForm.formatValor(cents)}"
                            else "Use vírgula ou ponto: 22,50"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Rotulo("Tipo")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MovementType.entries.forEach { option ->
                        FilterChip(
                            selected = option == type,
                            onClick = { type = option },
                            label = { Text(rotulo(option)) }
                        )
                    }
                }
                // Sem isto, o botão desligado não explica o que falta.
                if (type == null) {
                    Text(
                        text = "Escolha o tipo do lançamento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Rotulo("Pagador")
                Seletor(
                    selecionado = payer,
                    opcoes = pagadores,
                    onSelect = { payer = it }
                )

                if (precisaDeRateio) {
                    Rotulo("Rateado entre (${pesosValidos.size})")

                    val disponiveis = elegiveis.filterNot { it in pesos }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Seletor(
                                selecionado = "Adicionar pessoa…",
                                opcoes = disponiveis,
                                enabled = disponiveis.isNotEmpty(),
                                onSelect = { pesos[it] = "1" }
                            )
                        }
                        OutlinedButton(
                            onClick = { elegiveis.forEach { pesos[it] = "1" } },
                            enabled = disponiveis.isNotEmpty()
                        ) {
                            Text("Todos")
                        }
                    }

                    if (pesos.isEmpty()) {
                        Text(
                            text = "Ninguém no rateio ainda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Uma linha por pessoa escolhida, em vez da lista inteira sempre à
                    // vista: com 26 nomes, o rateio tomaria o dialog todo.
                    elegiveis.filter { it in pesos }.forEach { name ->
                        LinhaDePeso(
                            name = name,
                            peso = pesos[name].orEmpty(),
                            onPeso = { pesos[name] = it },
                            onRemover = { pesos.remove(name) }
                        )
                    }
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
                        modifier = Modifier.padding(top = 12.dp)
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
                        // `valid` já garante que há tipo; sem ele o botão está desligado.
                        type = type ?: return@TextButton,
                        payer = payer,
                        valueCents = cents ?: 0L,
                        weights = if (precisaDeRateio) pesosValidos else emptyMap()
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

/** Nome à esquerda, peso num campo estreito à direita, e o X para tirar do rateio. */
@Composable
private fun LinhaDePeso(
    name: String,
    peso: String,
    onPeso: (String) -> Unit,
    onRemover: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = peso,
            onValueChange = onPeso,
            label = { Text("Peso") },
            singleLine = true,
            isError = parseWeight(peso) == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(104.dp)
        )
        IconButton(onClick = onRemover) {
            Icon(
                imageVector = RepIcons.Close,
                contentDescription = "Tirar $name do rateio",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Campo que abre a lista em menu.
 *
 * Os pagadores são 31 e os participantes 25 — como chips, qualquer um dos dois empurraria
 * o resto do formulário para fora da tela.
 */
@Composable
private fun Seletor(
    selecionado: String,
    opcoes: List<String>,
    onSelect: (String) -> Unit,
    enabled: Boolean = true
) {
    val menu = rememberMenuToggle()

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { menu.onAnchorClick() },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selecionado,
                modifier = Modifier.weight(1f),
                color = if (enabled) accentColor() else MaterialTheme.colorScheme.onSurfaceVariant
            )
            // A seta vira para cima quando aberto: é ela que o morador toca para fechar.
            Icon(
                imageVector = if (menu.expanded) RepIcons.ExpandLess else RepIcons.ExpandMore,
                contentDescription = if (menu.expanded) "Fechar a lista" else "Abrir a lista"
            )
        }
        DropdownMenu(
            expanded = menu.expanded,
            onDismissRequest = { menu.dismiss() },
            // Com 31 pagadores, o menu sem limite cobre a tela inteira e esconde o
            // formulário atrás dele.
            modifier = Modifier.heightIn(max = MenuMaxHeight)
        ) {
            opcoes.forEach { opcao ->
                DropdownMenuItem(
                    text = { Text(opcao) },
                    onClick = {
                        onSelect(opcao)
                        menu.select()
                    }
                )
            }
        }
    }
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
