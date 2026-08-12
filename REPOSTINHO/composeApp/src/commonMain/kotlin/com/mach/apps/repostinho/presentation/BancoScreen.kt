package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import com.mach.apps.repostinho.data.model.CaixinhaLine
import com.mach.apps.repostinho.data.model.MemberBalance
import com.mach.apps.repostinho.data.model.Movement
import com.mach.apps.repostinho.data.repository.SyncState

/** As abas espelham as da planilha: Saldos_pessoas, Movimentações e Saldos_caixinha. */
private enum class BancoTab(val label: String) {
    SALDOS("Saldos"),
    LANCAMENTOS("Lançamentos"),
    CAIXINHA("Caixinha")
}

@Composable
fun BancoScreen(
    balances: List<MemberBalance>,
    movements: List<Movement>,
    caixinha: List<CaixinhaLine>,
    currentMemberName: String,
    syncState: SyncState,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(BancoTab.SALDOS) }

    Column(modifier = modifier.fillMaxSize()) {
        SyncBanner(syncState)

        PrimaryTabRow(selectedTabIndex = selected.ordinal) {
            BancoTab.entries.forEach { tab ->
                Tab(
                    selected = tab == selected,
                    onClick = { selected = tab },
                    text = { Text(tab.label) }
                )
            }
        }

        val content = Modifier.fillMaxSize().padding(horizontal = 16.dp)

        when (selected) {
            BancoTab.SALDOS -> SaldosScreen(
                balances = balances,
                currentMemberName = currentMemberName,
                modifier = content
            )

            BancoTab.LANCAMENTOS -> LancamentosScreen(
                movements = movements,
                currentMemberName = currentMemberName,
                modifier = content
            )

            BancoTab.CAIXINHA -> CaixinhaScreen(
                lines = caixinha,
                modifier = content
            )
        }
    }
}

/**
 * Diz de onde vieram os números.
 *
 * Sem isso, um saldo desatualizado por falta de rede fica idêntico a um atualizado — e o
 * morador toma decisão de dinheiro com o número errado sem desconfiar.
 */
@Composable
private fun SyncBanner(syncState: SyncState) {
    val (message, color) = when (syncState) {
        is SyncState.Loading ->
            "Buscando a planilha…" to MaterialTheme.colorScheme.onSurfaceVariant

        is SyncState.Live ->
            "Direto da planilha" to MaterialTheme.colorScheme.onSurfaceVariant

        // `reason` fica de fora: é mensagem de erro de rede (um NSURLError inteiro, no
        // iOS) e ocupava a tela toda. Ela segue no estado, para log.
        is SyncState.Fallback ->
            "Sem conexão com a planilha — mostrando a última cópia" to
                MaterialTheme.colorScheme.error
    }

    Text(
        text = message,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
