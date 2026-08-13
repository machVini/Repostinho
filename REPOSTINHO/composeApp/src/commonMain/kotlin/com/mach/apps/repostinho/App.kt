package com.mach.apps.repostinho

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mach.apps.repostinho.data.local.ThemeMode
import com.mach.apps.repostinho.data.local.ThemePreferenceStore
import com.mach.apps.repostinho.presentation.BancoScreen
import com.mach.apps.repostinho.presentation.CalendarioScreen
import com.mach.apps.repostinho.presentation.DashboardViewModel
import com.mach.apps.repostinho.presentation.HomeScreen
import com.mach.apps.repostinho.presentation.PerfilScreen
import com.mach.apps.repostinho.presentation.SheetUiState
import com.mach.apps.repostinho.presentation.TarefasScreen
import com.mach.apps.repostinho.ui.AutoSizeLabel
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
    // KoinContext por fora do tema: a preferência salva precisa ser lida antes de decidir
    // a cor, e quem a guarda vem da injeção.
    KoinContext {
        val themeStore = koinInject<ThemePreferenceStore>()
        val systemDark = isSystemInDarkTheme()

        // `remember` para não reler o arquivo a cada recomposição. Enquanto o morador não
        // tocar no botão, o modo é SISTEMA e o aparelho manda.
        var themeMode by remember { mutableStateOf(themeStore.read()) }
        val darkTheme = themeMode.isDark(systemDark)

        RepostinhoTheme(darkTheme = darkTheme) {
            val viewModel = koinInject<DashboardViewModel>()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val sheet by viewModel.sheet.collectAsStateWithLifecycle()
            val tasks by viewModel.tasks.collectAsStateWithLifecycle()
            val events by viewModel.events.collectAsStateWithLifecycle()
            val notes by viewModel.meetingNotes.collectAsStateWithLifecycle()
            val rotation by viewModel.rotation.collectAsStateWithLifecycle()
            val eventsShared by viewModel.eventsShared.collectAsStateWithLifecycle()

            var selectedTab by remember { mutableStateOf(AppTab.HOME) }

            // Entrar na aba de Tarefas rebusca o que a rep marcou. É o gesto mais próximo
            // de "quero ver a escala agora" que existe nesta tela, que não tem
            // puxar-para-atualizar.
            LaunchedEffect(selectedTab) {
                if (selectedTab == AppTab.TAREFAS) viewModel.refreshChores()
            }

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
                            IconButton(onClick = {
                                // Grava na hora: se o app for morto logo depois, a
                                // escolha já está em disco. O arquivo tem uma palavra.
                                // Lê `themeMode` aqui, e não o `darkTheme` capturado na
                                // composição: capturado, ele pode estar velho e o toque
                                // regrava o mesmo valor, sem efeito visível.
                                themeMode = if (themeMode.isDark(systemDark)) {
                                    ThemeMode.CLARO
                                } else {
                                    ThemeMode.ESCURO
                                }
                                themeStore.write(themeMode)
                            }) {
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
                                label = {
                                    AutoSizeLabel(
                                        text = tab.label,
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
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
                            notes = notes,
                            tasks = tasks,
                            onGoToBanco = { navigateTo(AppTab.BANCO) },
                            onGoToTarefas = { navigateTo(AppTab.TAREFAS) },
                            onRefresh = { viewModel.refreshSheet(fresh = true) },
                            modifier = inset
                        )

                        AppTab.BANCO -> BancoScreen(
                            balances = sheet.balances,
                            movements = sheet.movements,
                            caixinha = sheet.caixinha,
                            currentMemberName = SheetUiState.CURRENT_MEMBER_NAME,
                            syncState = sheet.syncState,
                            isRefreshing = sheet.isRefreshing,
                            onRefresh = { viewModel.refreshSheet(fresh = true) },
                            modifier = full
                        )

                        AppTab.TAREFAS -> TarefasScreen(
                            tasks = tasks,
                            residents = state.residents,
                            currentResidentId = state.currentResidentId,
                            rotation = rotation,
                            onSetDone = viewModel::setTaskDone,
                            modifier = inset
                        )

                        AppTab.CALENDARIO -> CalendarioScreen(
                            occurrences = events,
                            isShared = eventsShared,
                            year = viewModel.currentYear,
                            onAddEvent = viewModel::addEvent,
                            onRemoveEvent = viewModel::removeEvent,
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
