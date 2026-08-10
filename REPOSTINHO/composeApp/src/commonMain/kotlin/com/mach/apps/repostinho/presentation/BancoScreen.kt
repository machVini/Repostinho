package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.data.model.Resident

private enum class BancoTab(val label: String) {
    SALDO("Saldo"),
    LANCAR("Lançar"),
    MORADORES("Moradores")
}

@Composable
fun BancoScreen(
    state: BankUiState,
    onSaveExpense: (String, String, Long, Map<String, Double>) -> Unit,
    onRegisterPayment: (String, Long) -> Unit,
    onSaveResident: (Resident) -> Unit,
    onRemoveResident: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(BancoTab.SALDO) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selected.ordinal) {
            BancoTab.entries.forEach { tab ->
                Tab(
                    selected = tab == selected,
                    onClick = { selected = tab },
                    text = { Text(tab.label) }
                )
            }
        }

        when (selected) {
            BancoTab.SALDO -> BalanceScreen(
                state = state,
                onRegisterPayment = onRegisterPayment,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            )

            BancoTab.LANCAR -> TransactionFormScreen(
                state = state,
                onSaveExpense = onSaveExpense,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            )

            BancoTab.MORADORES -> ResidentsScreen(
                state = state,
                onSaveResident = onSaveResident,
                onRemoveResident = onRemoveResident,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            )
        }
    }
}
