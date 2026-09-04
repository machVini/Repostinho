package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.data.model.EmergencyContact
import com.mach.apps.repostinho.data.model.MedicalRecord
import com.mach.apps.repostinho.data.model.Resident

/**
 * As fichas médicas dos outros moradores.
 *
 * Uma tela só de leitura, e de propósito: a ficha é preenchida no cadastro, pelo mesmo
 * caminho de administração dos outros dados do morador. Editar a ficha alheia do celular
 * seria o jeito mais fácil de alguém escrever uma alergia errada na ficha de quem vai
 * precisar dela.
 *
 * A ordem é alfabética. Numa emergência ninguém lembra em que quarto a pessoa mora, mas
 * todo mundo sabe o nome dela.
 */
@Composable
fun FichaMedicaScreen(
    residents: List<Resident>,
    currentResidentId: String,
    modifier: Modifier = Modifier
) {
    // Só quem mora aqui hoje: ex-morador continua no cadastro por causa do histórico do
    // banco, e a ficha dele numa emergência não serve para nada.
    val outros = residents
        .filter { it.isActive && it.id != currentResidentId }
        .sortedBy { it.name.lowercase() }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Text(
                text = "Em uma emergência, o que está aqui é o que a rep sabe. " +
                    "Quem preenche é quem cuida do cadastro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )
        }

        items(outros.size) { index ->
            val resident = outros[index]
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ResidentPhoto(
                            photoUrl = resident.photoUrl,
                            name = resident.name,
                            size = 44.dp
                        )
                        Text(
                            text = resident.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    FichaMedicaLines(resident.medical)
                }
            }
        }
    }
}

/**
 * As linhas da ficha, iguais no perfil e na lista.
 *
 * Compartilhadas para que as duas telas nunca discordem sobre o que é "alergia" ou sobre
 * como um campo vazio aparece — uma ficha que muda de forma dependendo de onde é lida é
 * uma ficha em que não se confia.
 */
@Composable
fun FichaMedicaLines(record: MedicalRecord?) {
    if (record == null || record.isEmpty) {
        Text(
            text = "Ficha não preenchida.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        return
    }

    InfoLine("Tipo sanguíneo", record.bloodType.orDash())
    InfoLine("Alergia a remédios", record.drugAllergies.orDash())
    InfoLine("Outras alergias", record.otherAllergies.orDash())
    InfoLine("Condições", record.conditions.orDash())
    InfoLine("Medicamentos", record.medications.orDash())

    // Só quem toma algo tem onde guardar: a linha em branco atrapalharia a leitura de
    // quem está com pressa.
    record.medicationLocation?.takeIf { it.isNotBlank() }?.let {
        InfoLine("Onde guarda", it)
    }

    // Fora do rótulo-valor: contato é nome e telefone, e o telefone é o que a pessoa vai
    // ler em voz alta enquanto disca. Espremido numa coluna estreita, sairia cortado.
    if (record.contacts.isNotEmpty()) {
        Text(
            text = "Emergência",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )
        record.contacts.forEach { contact ->
            Text(
                text = contactLabel(contact),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    // Observação é frase, e é onde está o detalhe que os campos não pegam — "se eu
    // desmaiar, me deixa deitada" não cabe em nenhuma outra linha.
    record.notes?.takeIf { it.isNotBlank() }?.let { notes ->
        Text(
            text = notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

/** "Wilma (Mãe) — (11) 96527-4391", ou só o que existir. */
private fun contactLabel(contact: EmergencyContact): String {
    val quem = contact.relationship?.takeIf { it.isNotBlank() }
        ?.let { "${contact.name} ($it)" }
        ?: contact.name
    val phone = contact.phone?.takeIf { it.isNotBlank() } ?: return quem
    return "$quem — $phone"
}

private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "—"
