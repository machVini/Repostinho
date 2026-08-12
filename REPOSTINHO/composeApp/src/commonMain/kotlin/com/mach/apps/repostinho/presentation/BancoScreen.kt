package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.mach.apps.repostinho.ui.AutoSizeLabel
import com.mach.apps.repostinho.ui.accentColor

/** As abas espelham as da planilha: Saldos_pessoas, Movimentações e Saldos_caixinha. */
private enum class BancoTab(val label: String) {
    SALDOS("Saldos"),
    LANCAMENTOS("Lançamentos"),
    CAIXINHA("Caixinha")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BancoScreen(
    balances: List<MemberBalance>,
    movements: List<Movement>,
    caixinha: List<CaixinhaLine>,
    currentMemberName: String,
    syncState: SyncState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(BancoTab.SALDOS) }

    Column(modifier = modifier.fillMaxSize()) {
        SyncBanner(syncState)

        // Cores por aba, e não via `contentColor`: este último pintaria as três de ouro,
        // apagando justamente a diferença entre selecionada e não selecionada. O
        // indicador também precisa ser explícito — o padrão usa `primary`, que no escuro
        // sai azul e briga com o rótulo dourado.
        PrimaryTabRow(
            selectedTabIndex = selected.ordinal,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(
                        selectedTabIndex = selected.ordinal,
                        matchContentSize = true
                    ),
                    color = accentColor()
                )
            }
        ) {
            BancoTab.entries.forEach { tab ->
                Tab(
                    selected = tab == selected,
                    onClick = { selected = tab },
                    text = { AutoSizeLabel(tab.label) },
                    selectedContentColor = accentColor(),
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // O gesto envolve só o conteúdo: as abas ficam paradas enquanto a lista desce.
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
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

        // A data pode vir vazia se o banco-api publicado for anterior ao campo, ou se o
        // cache tiver sido gravado nessa época. A frase precisa continuar de pé sem ela.
        is SyncState.Live -> {
            val text = syncState.generatedAtLabel.takeIf { it.isNotBlank() }
                ?.let { "Última atualização: $it" }
                ?: "Atualizado agora"
            text to MaterialTheme.colorScheme.onSurfaceVariant
        }

        // O "sem conexão" fica junto da data: só o horário não diz se o app tentou
        // rebuscar e falhou, ou se aquele é mesmo o estado atual da planilha.
        is SyncState.Cached -> {
            val text = syncState.generatedAtLabel.takeIf { it.isNotBlank() }
                ?.let { "Sem conexão · Última atualização: $it" }
                ?: "Sem conexão — mostrando a última cópia"
            text to MaterialTheme.colorScheme.error
        }

        // `reason` fica de fora: é mensagem de erro de rede (um NSURLError inteiro, no
        // iOS) e ocupava a tela toda. Ela segue no estado, para log.
        is SyncState.Failed ->
            "Não foi possível carregar o banco" to MaterialTheme.colorScheme.error
    }

    Text(
        text = message,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
