package com.ritmo.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ritmo.app.domain.Screen
import com.ritmo.app.domain.ThemeMode
import com.ritmo.app.ui.components.Celebration
import com.ritmo.app.ui.components.CelebrationDialog
import com.ritmo.app.ui.components.ChunkyFab
import com.ritmo.app.ui.components.LocalVibration
import com.ritmo.app.ui.components.NavItem
import com.ritmo.app.ui.components.RewardPill
import com.ritmo.app.ui.components.RitmoBottomBar
import com.ritmo.app.ui.components.RitmoHeader
import com.ritmo.app.ui.components.icon
import com.ritmo.app.ui.components.tint
import com.ritmo.app.ui.screens.AccountActions
import com.ritmo.app.ui.screens.AuthScreen
import com.ritmo.app.ui.screens.DashboardScreen
import com.ritmo.app.ui.screens.FocusActions
import com.ritmo.app.ui.screens.FocusPicker
import com.ritmo.app.ui.screens.FocusScreen
import com.ritmo.app.ui.screens.ListDetailScreen
import com.ritmo.app.ui.screens.ListEditor
import com.ritmo.app.ui.screens.ListsScreen
import com.ritmo.app.ui.screens.MatrixScreen
import com.ritmo.app.ui.screens.ObjectiveEditor
import com.ritmo.app.ui.screens.ProReason
import com.ritmo.app.ui.screens.ProScreen
import com.ritmo.app.ui.screens.ProfileScreen
import com.ritmo.app.ui.screens.SettingsScreen
import com.ritmo.app.ui.screens.ShopScreen
import com.ritmo.app.ui.screens.TodayScreen
import com.ritmo.app.ui.theme.RitmoTheme
import kotlinx.coroutines.delay

/** Identificador de "novo" nos editores: o Room nunca usa 0 em um registro salvo. */
private const val NEW = 0L

@Composable
fun RitmoRoot(viewModel: RitmoViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state

    RitmoTheme(current?.themeId, current?.settings?.themeMode ?: ThemeMode.SYSTEM) {
        CompositionLocalProvider(LocalVibration provides (current?.settings?.hapticsEnabled ?: true)) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when {
                    current == null -> Unit
                    // Sair da conta tira Home da tela e descarta o que estava aberto nela.
                    current.signedIn -> Home(current, viewModel)
                    else -> AuthScreen(current, viewModel::signUp, viewModel::signIn, viewModel::sendPasswordReset)
                }
            }
        }
    }
}

@Composable
private fun Home(current: UiState, viewModel: RitmoViewModel) {
    val scheme = MaterialTheme.colorScheme
    val focus by viewModel.focus.collectAsStateWithLifecycle()

    // Telas abertas ficam em rememberSaveable para sobreviver a girar o aparelho.
    // O app abre na primeira tela do rodapé, que a pessoa escolhe nas configurações.
    var tab by rememberSaveable { mutableStateOf(current.tabs.first()) }
    var pickingFocus by rememberSaveable { mutableStateOf(false) }
    var objectiveEditor by rememberSaveable { mutableStateOf<Long?>(null) }
    var listEditor by rememberSaveable { mutableStateOf<Long?>(null) }
    var openList by rememberSaveable { mutableStateOf<Long?>(null) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var proReason by rememberSaveable { mutableStateOf<String?>(null) }

    var reward by remember { mutableStateOf<String?>(null) }
    var rewardSerial by remember { mutableIntStateOf(0) }
    val celebrations = remember { mutableStateListOf<Celebration>() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppEvent.Reward -> {
                    reward = event.text
                    rewardSerial++
                }
                is AppEvent.Celebrate -> celebrations += event.celebration
            }
        }
    }
    LaunchedEffect(rewardSerial) {
        if (reward != null) {
            delay(1800)
            reward = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = scheme.background,
            topBar = {
                RitmoHeader(
                    streak = current.mainStreak.current,
                    freezes = current.freezes,
                    coins = current.coins,
                    level = current.level.level,
                    onStreak = { tab = Screen.DASHBOARD },
                    onShop = { tab = Screen.SHOP },
                    onLevel = { tab = Screen.PROFILE },
                    onSettings = { settingsOpen = true },
                )
            },
            bottomBar = {
                // Uma tela aberta pelo cabeçalho que não está no rodapé deixa nenhum ícone marcado.
                val tabs = current.tabs
                RitmoBottomBar(
                    items = tabs.map { NavItem(it.label, it.icon, it.tint()) },
                    selected = tabs.indexOf(tab),
                    onSelect = { tab = tabs[it] },
                )
            },
            floatingActionButton = {
                val plan = current.plan
                when (tab) {
                    Screen.TODAY -> ChunkyFab("Novo objetivo", {
                        if (plan.canAddObjective(current.activeObjectives)) {
                            objectiveEditor = NEW
                        } else {
                            proReason = ProReason.objectives(plan)
                        }
                    })
                    Screen.LISTS -> ChunkyFab("Nova lista", {
                        if (plan.canAddList(current.lists.size)) {
                            listEditor = NEW
                        } else {
                            proReason = ProReason.lists(plan)
                        }
                    })
                    else -> Unit
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    Screen.TODAY -> TodayScreen(
                        current,
                        onToggle = viewModel::toggle,
                        onEdit = { objectiveEditor = it.id },
                        onCreate = { objectiveEditor = NEW },
                    )
                    Screen.LISTS -> ListsScreen(
                        current,
                        onOpen = { openList = it },
                        onCreate = { listEditor = NEW },
                    )
                    Screen.FOCUS -> FocusScreen(
                        current,
                        focus,
                        remember(viewModel) {
                            FocusActions(
                                setMode = viewModel::setFocusMode,
                                pick = { pickingFocus = true },
                                clearTarget = { viewModel.setFocusTarget(null) },
                                start = viewModel::startFocus,
                                pause = viewModel::pauseFocus,
                                stop = viewModel::stopFocus,
                                finish = viewModel::finishStopwatch,
                                complete = viewModel::completeTarget,
                                changeSettings = viewModel::updateSettings,
                            )
                        },
                    )
                    Screen.MATRIX -> MatrixScreen(
                        current,
                        onAdd = viewModel::addTask,
                        onToggle = viewModel::toggleTask,
                        onUpdate = viewModel::updateTask,
                        onDelete = viewModel::deleteTask,
                    )
                    Screen.DASHBOARD -> DashboardScreen(current, { proReason = it }, viewModel::exportHistory)
                    Screen.PROFILE -> ProfileScreen(current)
                    Screen.SHOP -> ShopScreen(current, viewModel::buy, viewModel::toggleEquipped) {
                        proReason = it
                    }
                }
            }
        }

        Page(openList?.let { id -> current.lists.firstOrNull { it.list.id == id } }) { row ->
            ListDetailScreen(
                row = row,
                onBack = { openList = null },
                onToggle = viewModel::toggleItem,
                canAdd = current.plan.canAddItem(row.total),
                onLimit = { proReason = ProReason.items(current.plan) },
                onAdd = { title, difficulty -> viewModel.addItem(row.list.id, title, difficulty) },
                onUpdateItem = viewModel::updateItem,
                onDeleteItem = viewModel::deleteItem,
                onEditList = { listEditor = row.list.id },
                onUncheckAll = { viewModel.uncheckAll(row.list.id) },
                onDeleteList = {
                    openList = null
                    viewModel.deleteList(row.list.id)
                },
            )
        }
        Page(listEditor) { id ->
            ListEditor(
                list = current.lists.firstOrNull { it.list.id == id }?.list,
                today = current.today,
                onSave = { list ->
                    listEditor = null
                    viewModel.saveList(list) { saved -> openList = saved }
                },
                onClose = { listEditor = null },
            )
        }
        Page(if (settingsOpen) Unit else null) {
            SettingsScreen(
                state = current,
                onChange = viewModel::updateSettings,
                onOpenShop = {
                    settingsOpen = false
                    tab = Screen.SHOP
                },
                onOpenPro = { proReason = it },
                onExport = viewModel::exportHistory,
                account = remember(viewModel) {
                    AccountActions(
                        viewModel::updateName,
                        viewModel::changePassword,
                        viewModel::signOut,
                        viewModel::deleteAccount,
                    )
                },
                onBack = { settingsOpen = false },
            )
        }
        Page(if (pickingFocus) Unit else null) {
            FocusPicker(
                state = current,
                selected = focus.target,
                onPick = { target ->
                    viewModel.setFocusTarget(target)
                    pickingFocus = false
                },
                onClose = { pickingFocus = false },
            )
        }
        Page(objectiveEditor) { id ->
            val row = (current.todayRows + current.otherRows).firstOrNull { it.objective.id == id }
            ObjectiveEditor(
                objective = row?.objective,
                streak = row?.streak,
                today = current.today,
                plan = current.plan,
                onReminderLimit = { proReason = ProReason.reminders(current.plan) },
                onSave = {
                    viewModel.save(it)
                    objectiveEditor = null
                },
                onArchive = {
                    viewModel.archive(it)
                    objectiveEditor = null
                },
                onClose = { objectiveEditor = null },
            )
        }
        // Por último para abrir por cima de qualquer tela, sem perder o que estava nela.
        Page(proReason) { reason ->
            ProScreen(
                state = current,
                reason = reason,
                onActivate = {
                    viewModel.activatePro()
                    proReason = null
                },
                onClose = { proReason = null },
            )
        }

        AnimatedVisibility(
            visible = reward != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 58.dp),
        ) {
            RewardPill(reward.orEmpty())
        }
    }

    celebrations.firstOrNull()?.let { celebration ->
        // Um toque fora pode dispensar duas vezes; remover pelo item torna isso inofensivo.
        CelebrationDialog(celebration, current.petId, current.stage.index) {
            celebrations.remove(celebration)
        }
    }
}

private class Held<T : Any>(var value: T? = null)

/** Mostra uma tela interna por cima das abas e mantém o último conteúdo durante a saída. */
@Composable
private fun <T : Any> Page(target: T?, content: @Composable (T) -> Unit) {
    val held = remember { Held<T>() }
    if (target != null) held.value = target
    val shown = target ?: held.value
    AnimatedVisibility(
        visible = target != null,
        enter = slideInVertically { it / 12 } + fadeIn(),
        exit = slideOutVertically { it / 12 } + fadeOut(),
    ) {
        if (shown != null) content(shown)
    }
}
