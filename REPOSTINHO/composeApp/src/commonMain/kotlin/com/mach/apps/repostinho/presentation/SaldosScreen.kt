package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.data.model.MemberBalance
import com.mach.apps.repostinho.ui.positiveColor

@Composable
fun SaldosScreen(
    balances: List<MemberBalance>,
    currentMemberName: String,
    modifier: Modifier = Modifier
) {
    val current = balances.filter { !it.isFormer }
    val former = balances.filter { it.isFormer }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "Saldos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Negativo quer dizer que a pessoa deve ao banco.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(current) { balance ->
            BalanceCard(balance, highlighted = balance.name == currentMemberName)
        }

        // A planilha mantém ex-moradores e agregados numa tabela à parte: eles ainda devem,
        // mas não entram em rateio novo.
        if (former.isNotEmpty()) {
            item {
                Text(
                    text = "Ex-moradores e agregados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }
            items(former) { balance -> BalanceCard(balance, highlighted = false) }
        }

        item { Column(modifier = Modifier.padding(bottom = 16.dp)) {} }
    }
}

@Composable
private fun BalanceCard(balance: MemberBalance, highlighted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = if (highlighted) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(balance.name, fontWeight = FontWeight.Bold)
                    if (highlighted) {
                        Text(
                            text = "Você",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = formatBrl(balance.finalCents),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (balance.finalCents < 0) MaterialTheme.colorScheme.error
                    else positiveColor()
                )
            }

            // As três parcelas que a planilha usa para chegar no saldo final.
            DetailLine("Saldo anterior", balance.previousCents)
            DetailLine("Gastos", balance.expensesCents)
            DetailLine("Pagamentos", balance.paymentsCents)
        }
    }
}

@Composable
private fun DetailLine(label: String, cents: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = formatBrl(cents), style = MaterialTheme.typography.bodySmall)
    }
}
