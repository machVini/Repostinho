package com.mach.apps.repostinho

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mach.apps.repostinho.presentation.BancoScreen
import com.mach.apps.repostinho.presentation.CalendarioScreen
import com.mach.apps.repostinho.presentation.DashboardViewModel
import com.mach.apps.repostinho.presentation.HomeScreen
import com.mach.apps.repostinho.presentation.PerfilScreen
import com.mach.apps.repostinho.presentation.SheetUiState
import com.mach.apps.repostinho.presentation.TarefasScreen
import com.mach.apps.repostinho.ui.RepIcons
import com.mach.apps.repostinho.ui.RepostinhoTheme
import com.mach.apps.repostinho.ui.barContainerColor
import com.mach.apps.repostinho.ui.barIndicatorColor
import com.mach.apps.repostinho.ui.onBarColor
import com.mach.apps.repostinho.ui.onBarIndicatorColor
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

private enum class AppTab(val label: String, val title: String, val icon: ImageVector) {
    HOME("Home", "República Postinho", RepIcons.Home),
    BANCO("Banco", "Banco da Rep", RepIcons.Bank),
    TAREFAS("Tarefas", "Escala de Tarefas", RepIcons.Tasks),
    CALENDARIO("Calendário", "Calendário da Rep", RepIcons.Calendar),
    PERFIL("Perfil", "Perfil", RepIcons.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    // A escolha do morador na toolbar vence o sistema; enquanto ninguém tocar no botão,
    // `null` mantém o app seguindo o tema do aparelho.
    val systemDark = isSystemInDarkTheme()
    var darkOverride by remember { mutableStateOf<Boolean?>(null) }
    val darkTheme = darkOverride ?: systemDark

    RepostinhoTheme(darkTheme = darkTheme) {
        KoinContext {
            val viewModel = koinInject<DashboardViewModel>()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val sheet by viewModel.sheet.collectAsStateWithLifecycle()
            val tasks by viewModel.tasks.collectAsStateWithLifecycle()
            val events by viewModel.events.collectAsStateWithLifecycle()

            var selectedTab by remember { mutableStateOf(AppTab.HOME) }

            // O app não tem pilha de navegação: as abas são um `when`. Para o botão de
            // voltar ter o que desfazer, as trocas de aba ficam registradas aqui.
            val backStack = remember { mutableStateListOf<AppTab>() }

            fun navigateTo(tab: AppTab) {
                if (tab == selectedTab) return
                backStack.add(selectedTab)
                selectedTab = tab
            }

            fun navigateBack() {
                if (backStack.isEmpty()) return
                selectedTab = backStack.removeAt(backStack.lastIndex)
            }

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = selectedTab.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        navigationIcon = {
                            // Só aparece quando existe aba anterior — um botão de voltar
                            // que não volta para lugar nenhum só confunde.
                            if (backStack.isNotEmpty()) {
                                IconButton(onClick = { navigateBack() }) {
                                    Icon(RepIcons.Back, contentDescription = "Voltar")
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { darkOverride = !darkTheme }) {
                                Icon(
                                    imageVector = if (darkTheme) RepIcons.LightMode
                                    else RepIcons.DarkMode,
                                    contentDescription = if (darkTheme) "Usar modo claro"
                                    else "Usar modo escuro"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = barContainerColor(),
                            titleContentColor = onBarColor(),
                            navigationIconContentColor = onBarColor(),
                            actionIconContentColor = onBarColor()
                        )
                    )
                },
                bottomBar = {
                    // Mesma cor da toolbar: as duas leem `barContainerColor`.
                    NavigationBar(
                        containerColor = barContainerColor(),
                        contentColor = onBarColor()
                    ) {
                        AppTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = tab == selectedTab,
                                onClick = { navigateTo(tab) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    // O ícone selecionado fica sobre a pílula; o rótulo
                                    // fica sobre a barra.
                                    selectedIconColor = onBarIndicatorColor(),
                                    indicatorColor = barIndicatorColor(),
                                    selectedTextColor = onBarColor(),
                                    unselectedIconColor = onBarColor().copy(alpha = 0.7f),
                                    unselectedTextColor = onBarColor().copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                // O Banco fica sem margem lateral aqui porque as abas internas ocupam a
                // largura inteira; o recuo é aplicado no conteúdo de cada uma.
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    val full = Modifier.fillMaxSize()
                    val inset = Modifier.fillMaxSize().padding(horizontal = 16.dp)

                    when (selectedTab) {
                        AppTab.HOME -> HomeScreen(
                            state = state,
                            sheet = sheet,
                            tasks = tasks,
                            onGoToBanco = { navigateTo(AppTab.BANCO) },
                            onGoToTarefas = { navigateTo(AppTab.TAREFAS) },
                            onRefresh = viewModel::refreshSheet,
                            modifier = inset
                        )

                        AppTab.BANCO -> BancoScreen(
                            balances = sheet.balances,
                            movements = sheet.movements,
                            caixinha = sheet.caixinha,
                            currentMemberName = SheetUiState.CURRENT_MEMBER_NAME,
                            syncState = sheet.syncState,
                            isRefreshing = sheet.isRefreshing,
                            onRefresh = viewModel::refreshSheet,
                            modifier = full
                        )

                        AppTab.TAREFAS -> TarefasScreen(
                            tasks = tasks,
                            residents = state.residents,
                            currentResidentId = state.currentResidentId,
                            onSetDone = viewModel::setTaskDone,
                            modifier = inset
                        )

                        AppTab.CALENDARIO -> CalendarioScreen(
                            events = events,
                            modifier = inset
                        )

                        AppTab.PERFIL -> PerfilScreen(
                            state = state,
                            myBalanceCents = sheet.myBalanceCents,
                            tasks = tasks,
                            modifier = inset
                        )
                    }
                }
            }
        }
    }
}
