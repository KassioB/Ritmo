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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Storefront
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ritmo.app.domain.ThemeMode
import com.ritmo.app.ui.components.Celebration
import com.ritmo.app.ui.components.CelebrationDialog
import com.ritmo.app.ui.components.ChunkyFab
import com.ritmo.app.ui.components.LocalVibration
import com.ritmo.app.ui.components.NavItem
import com.ritmo.app.ui.components.RewardPill
import com.ritmo.app.ui.components.RitmoBottomBar
import com.ritmo.app.ui.components.RitmoHeader
import com.ritmo.app.ui.screens.AuthScreen
import com.ritmo.app.ui.screens.DashboardScreen
import com.ritmo.app.ui.screens.ListDetailScreen
import com.ritmo.app.ui.screens.ListEditor
import com.ritmo.app.ui.screens.ListsScreen
import com.ritmo.app.ui.screens.ObjectiveEditor
import com.ritmo.app.ui.screens.ProReason
import com.ritmo.app.ui.screens.ProScreen
import com.ritmo.app.ui.screens.ProfileScreen
import com.ritmo.app.ui.screens.SettingsScreen
import com.ritmo.app.ui.screens.ShopScreen
import com.ritmo.app.ui.screens.TodayScreen
import com.ritmo.app.ui.theme.ObjectiveColors
import com.ritmo.app.ui.theme.Ritmo
import com.ritmo.app.ui.theme.RitmoTheme
import kotlinx.coroutines.delay

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Hoje", Icons.Rounded.Home),
    LISTS("Listas", Icons.Rounded.Checklist),
    DASHBOARD("Dashboard", Icons.Rounded.BarChart),
    PROFILE("Conquistas", Icons.Rounded.EmojiEvents),
    SHOP("Loja", Icons.Rounded.Storefront),
}

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
                    else -> AuthScreen(current, viewModel::signUp, viewModel::signIn, viewModel::resetPassword)
                }
            }
        }
    }
}

@Composable
private fun Home(current: UiState, viewModel: RitmoViewModel) {
    val scheme = MaterialTheme.colorScheme
    val brand = Ritmo.brand

    // Telas abertas ficam em rememberSaveable para sobreviver a girar o aparelho.
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
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
                    onStreak = { tab = Tab.DASHBOARD },
                    onShop = { tab = Tab.SHOP },
                    onLevel = { tab = Tab.PROFILE },
                    onSettings = { settingsOpen = true },
                )
            },
            bottomBar = {
                val tints = listOf(
                    scheme.primary,
                    brand.doneText,
                    ObjectiveColors[1],
                    ObjectiveColors[4],
                    ObjectiveColors[5],
                )
                RitmoBottomBar(
                    items = Tab.entries.map { NavItem(it.label, it.icon, tints[it.ordinal]) },
                    selected = tab.ordinal,
                    onSelect = { tab = Tab.entries[it] },
                )
            },
            floatingActionButton = {
                val plan = current.plan
                when (tab) {
                    Tab.TODAY -> ChunkyFab("Novo objetivo", {
                        if (plan.canAddObjective(current.activeObjectives)) {
                            objectiveEditor = NEW
                        } else {
                            proReason = ProReason.objectives(plan)
                        }
                    })
                    Tab.LISTS -> ChunkyFab("Nova lista", {
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
                    Tab.TODAY -> TodayScreen(
                        current,
                        onToggle = viewModel::toggle,
                        onEdit = { objectiveEditor = it.id },
                        onCreate = { objectiveEditor = NEW },
                    )
                    Tab.LISTS -> ListsScreen(
                        current,
                        onOpen = { openList = it },
                        onCreate = { listEditor = NEW },
                    )
                    Tab.DASHBOARD -> DashboardScreen(current, { proReason = it }, viewModel::exportHistory)
                    Tab.PROFILE -> ProfileScreen(current)
                    Tab.SHOP -> ShopScreen(current, viewModel::buy, viewModel::toggleEquipped) {
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
                    tab = Tab.SHOP
                },
                onOpenPro = { proReason = it },
                onExport = viewModel::exportHistory,
                onSignOut = viewModel::signOut,
                onBack = { settingsOpen = false },
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
