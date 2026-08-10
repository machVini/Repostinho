package com.mach.apps.repostinho

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mach.apps.repostinho.presentation.BancoScreen
import com.mach.apps.repostinho.presentation.DashboardViewModel
import com.mach.apps.repostinho.presentation.HomeScreen
import com.mach.apps.repostinho.presentation.PerfilScreen
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
    PERFIL("Perfil", "Perfil", RepIcons.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    RepostinhoTheme {
        KoinContext {
            val viewModel = koinInject<DashboardViewModel>()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val tasks by viewModel.tasks.collectAsStateWithLifecycle()
            var selectedTab by remember { mutableStateOf(AppTab.HOME) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(selectedTab.title) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = barContainerColor(),
                            titleContentColor = onBarColor()
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
                                onClick = { selectedTab = tab },
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
                            tasks = tasks,
                            onGoToBanco = { selectedTab = AppTab.BANCO },
                            onGoToTarefas = { selectedTab = AppTab.TAREFAS },
                            modifier = inset
                        )

                        AppTab.BANCO -> BancoScreen(
                            state = state,
                            onSaveExpense = viewModel::saveExpense,
                            onRegisterPayment = { id, amount ->
                                viewModel.savePayment(id, amount)
                            },
                            onSaveResident = viewModel::saveResident,
                            onRemoveResident = viewModel::removeResident,
                            modifier = full
                        )

                        AppTab.TAREFAS -> TarefasScreen(
                            tasks = tasks,
                            residents = state.residents,
                            currentResidentId = state.currentResidentId,
                            onSetDone = viewModel::setTaskDone,
                            modifier = inset
                        )

                        AppTab.PERFIL -> PerfilScreen(
                            state = state,
                            tasks = tasks,
                            modifier = inset
                        )
                    }
                }
            }
        }
    }
}
