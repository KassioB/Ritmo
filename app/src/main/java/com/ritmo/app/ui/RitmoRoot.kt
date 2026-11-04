package com.ritmo.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.ritmo.app.data.Objective
import com.ritmo.app.ui.components.Celebration
import com.ritmo.app.ui.components.CelebrationDialog
import com.ritmo.app.ui.components.RewardPill
import com.ritmo.app.ui.screens.CalendarScreen
import com.ritmo.app.ui.screens.ObjectiveEditor
import com.ritmo.app.ui.screens.ProfileScreen
import com.ritmo.app.ui.screens.ShopScreen
import com.ritmo.app.ui.screens.TodayScreen
import com.ritmo.app.ui.theme.RitmoTheme
import kotlinx.coroutines.delay

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Hoje", Icons.Filled.Today),
    CALENDAR("Calendário", Icons.Filled.CalendarMonth),
    PROFILE("Conquistas", Icons.Filled.EmojiEvents),
    SHOP("Loja", Icons.Filled.Storefront),
}

private sealed interface EditorTarget {
    data object New : EditorTarget
    data class Existing(val objective: Objective) : EditorTarget
}

@Composable
fun RitmoRoot(viewModel: RitmoViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state

    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    var reward by remember { mutableStateOf<String?>(null) }
    var rewardSerial by remember { mutableStateOf(0) }
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

    RitmoTheme(current?.themeId) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            },
            floatingActionButton = {
                if (tab == Tab.TODAY && current != null) {
                    FloatingActionButton(onClick = { editor = EditorTarget.New }) {
                        Icon(Icons.Filled.Add, contentDescription = "Novo objetivo")
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (current != null) {
                    when (tab) {
                        Tab.TODAY -> TodayScreen(
                            current,
                            onToggle = viewModel::toggle,
                            onEdit = { editor = EditorTarget.Existing(it) },
                        )
                        Tab.CALENDAR -> CalendarScreen(current)
                        Tab.PROFILE -> ProfileScreen(current)
                        Tab.SHOP -> ShopScreen(current, viewModel::buy, viewModel::toggleEquipped)
                    }
                }
                AnimatedVisibility(
                    visible = reward != null,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                ) {
                    RewardPill(reward.orEmpty())
                }
            }
        }

        if (current != null) {
            editor?.let { target ->
                val existing = (target as? EditorTarget.Existing)?.objective
                val streak = existing?.let { objective ->
                    (current.todayRows + current.otherRows)
                        .firstOrNull { it.objective.id == objective.id }?.streak
                }
                ObjectiveEditor(
                    objective = existing,
                    streak = streak,
                    today = current.today,
                    onSave = {
                        viewModel.save(it)
                        editor = null
                    },
                    onArchive = {
                        viewModel.archive(it)
                        editor = null
                    },
                    onDismiss = { editor = null },
                )
            }
            celebrations.firstOrNull()?.let { celebration ->
                CelebrationDialog(celebration, current.accessoryId) {
                    celebrations.removeAt(0)
                }
            }
        }
    }
}
