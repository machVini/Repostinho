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
import com.mach.apps.repostinho.data.model.CaixinhaLine
import com.mach.apps.repostinho.data.model.MemberBalance
import com.mach.apps.repostinho.data.model.Movement

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
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(BancoTab.SALDOS) }

    Column(modifier = modifier.fillMaxSize()) {
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
