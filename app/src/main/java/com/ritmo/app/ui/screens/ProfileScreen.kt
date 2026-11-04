package com.ritmo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ritmo.app.domain.Achievement
import com.ritmo.app.domain.Achievements
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.Faisca
import com.ritmo.app.ui.theme.Gold

@Composable
fun ProfileScreen(state: UiState) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Faisca(state.mood, state.accessoryId, sizeDp = 88.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Nível ${state.level.level}", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { state.level.progress },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            drawStopIndicator = {},
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${state.level.xpIntoLevel} de ${state.level.xpForNextLevel} XP",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("${state.mainStreak.best}", "Recorde de streak", Modifier.weight(1f))
                Tile("${state.stats.perfectDays}", "Dias perfeitos", Modifier.weight(1f))
                Tile("${state.stats.completions}", "Concluídos", Modifier.weight(1f))
            }
        }
        item {
            val count = Achievements.all.count { it.id in state.unlocked }
            Text(
                "Conquistas ($count de ${Achievements.all.size})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(Achievements.all, key = { it.id }) { achievement ->
            AchievementRow(achievement, achievement.id in state.unlocked)
        }
    }
}

@Composable
private fun Tile(value: String, label: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AchievementRow(achievement: Achievement, unlocked: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(
                    if (unlocked) Gold.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (unlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                contentDescription = if (unlocked) "Desbloqueada" else "Bloqueada",
                tint = if (unlocked) Gold else MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(achievement.title, fontWeight = FontWeight.Bold)
            Text(achievement.description, style = MaterialTheme.typography.bodySmall)
        }
        Text("+${achievement.coins}", fontWeight = FontWeight.Bold)
    }
}
