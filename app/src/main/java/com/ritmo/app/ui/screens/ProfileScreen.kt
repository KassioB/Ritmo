package com.ritmo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ritmo.app.domain.Achievement
import com.ritmo.app.domain.Achievements
import com.ritmo.app.domain.Evolution
import com.ritmo.app.domain.Mood
import com.ritmo.app.domain.dias
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.ChunkyProgress
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.components.Mascot
import com.ritmo.app.ui.components.SectionBanner
import com.ritmo.app.ui.theme.Ritmo

@Composable
fun ProfileScreen(state: UiState) {
    val brand = Ritmo.brand
    val unlocked = Achievements.all.count { it.id in state.unlocked }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionBanner(
                overline = "Seu progresso",
                title = "Nível ${state.level.level}",
                side = { Mascot(state.petId, state.stage.index, state.mood, sizeDp = 92.dp) },
                footer = {
                    Spacer(Modifier.height(10.dp))
                    ChunkyProgress(state.level.progress, color = brand.bannerFill, track = brand.bannerLip)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${state.level.xpIntoLevel} de ${state.level.xpForNextLevel} XP",
                        color = brand.onBanner,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )
        }
        item { EvolutionCard(state) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("${state.mainStreak.best}", "Recorde de streak", Modifier.weight(1f))
                Tile("${state.stats.perfectDays}", "Dias perfeitos", Modifier.weight(1f))
                Tile("${state.stats.completions}", "Concluídos", Modifier.weight(1f))
            }
        }
        item {
            GroupCard(title = "Conquistas", trailing = "$unlocked de ${Achievements.all.size}") {
                Achievements.all.forEach { achievement ->
                    AchievementLine(achievement, achievement.id in state.unlocked)
                }
            }
        }
    }
}

/** A trilha de fases: a mascote cresce com o streak e recomeça se ele quebrar. */
@Composable
private fun EvolutionCard(state: UiState) {
    val streak = state.mainStreak.current
    val next = Evolution.next(streak)
    GroupCard(title = "Evolução", trailing = state.stage.name) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Evolution.stages.forEach { stage ->
                val reached = stage.index <= state.stage.index
                val current = stage.index == state.stage.index
                Column(
                    Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${stage.name}, a partir de ${dias(stage.minStreak)}" +
                                if (reached) ", alcançada" else ", bloqueada"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (current) {
                                    Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Mascot(
                            state.petId,
                            stage.index,
                            if (reached) Mood.HAPPY else Mood.SLEEPY,
                            Modifier.fillMaxSize().padding(3.dp).alpha(if (reached) 1f else 0.3f),
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${stage.minStreak}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
            ChunkyProgress(Evolution.progress(streak), height = 10.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                if (next == null) {
                    "Fase final alcançada. Agora é manter o fogo aceso."
                } else {
                    val left = next.minStreak - streak
                    "${if (left == 1) "Falta" else "Faltam"} ${dias(left)} de streak para ${next.name}."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun Tile(value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun AchievementLine(achievement: Achievement, unlocked: Boolean) {
    val brand = Ritmo.brand
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(if (unlocked) brand.done else scheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (unlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                contentDescription = if (unlocked) "Desbloqueada" else "Bloqueada",
                tint = if (unlocked) brand.onDone else scheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(achievement.title, style = MaterialTheme.typography.titleMedium)
            Text(
                achievement.description,
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text("+${achievement.coins}", style = MaterialTheme.typography.titleMedium)
    }
}
