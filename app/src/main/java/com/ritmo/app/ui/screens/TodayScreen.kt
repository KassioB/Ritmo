package com.ritmo.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ritmo.app.data.Objective
import com.ritmo.app.ui.ObjectiveRow
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.EmptyMessage
import com.ritmo.app.ui.components.Faisca
import com.ritmo.app.ui.components.Mood
import com.ritmo.app.ui.components.WalletRow
import com.ritmo.app.ui.theme.Flame
import com.ritmo.app.ui.theme.ObjectiveColors
import com.ritmo.app.ui.theme.Success
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormat = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

@Composable
fun TodayScreen(
    state: UiState,
    onToggle: (Long) -> Unit,
    onEdit: (Objective) -> Unit,
) {
    if (state.todayRows.isEmpty() && state.otherRows.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            WalletRow(state.mainStreak.current, state.coins, state.freezes)
            EmptyMessage(
                "Oi! Eu sou a Faísca.",
                "Crie seu primeiro objetivo no botão + e vamos construir seu ritmo.",
                state.accessoryId,
                Mood.HAPPY,
            )
        }
        return
    }

    val grouped = state.todayRows.groupBy { it.objective.period }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                LocalDate.ofEpochDay(state.today).format(dateFormat).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(10.dp))
            WalletRow(state.mainStreak.current, state.coins, state.freezes)
            Spacer(Modifier.height(6.dp))
        }
        item { MascotCard(state) }
        grouped.forEach { (period, rows) ->
            item(key = "header-$period") {
                Text(
                    period.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(rows, key = { it.objective.id }) { row ->
                ObjectiveCard(row, onToggle = { onToggle(row.objective.id) }, onEdit = { onEdit(row.objective) })
            }
        }
        if (state.otherRows.isNotEmpty()) {
            item {
                Text(
                    "Fora de hoje",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(state.otherRows, key = { it.objective.id }) { row ->
                ObjectiveCard(row, onToggle = null, onEdit = { onEdit(row.objective) })
            }
        }
    }
}

@Composable
private fun MascotCard(state: UiState) {
    val total = state.todayRows.size
    val done = state.todayRows.count { it.done }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Faisca(state.mood, state.accessoryId, sizeDp = 84.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(state.speech, style = MaterialTheme.typography.titleMedium)
                if (total > 0) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { done.toFloat() / total },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        drawStopIndicator = {},
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("$done de $total concluídos", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ObjectiveCard(row: ObjectiveRow, onToggle: (() -> Unit)?, onEdit: () -> Unit) {
    val objective = row.objective
    val accent = ObjectiveColors[objective.colorIndex % ObjectiveColors.size]
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onEdit),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).background(accent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(objective.emoji, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    objective.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (row.done) TextDecoration.LineThrough else null,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (row.streak.current > 0) Flame else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        " ${row.streak.current} · +${objective.difficulty.xp} XP" + reminderLabel(objective),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (onToggle != null) {
                Spacer(Modifier.width(8.dp))
                CheckButton(row.done, objective.title, accent, onToggle)
            }
        }
    }
}

private fun reminderLabel(objective: Objective): String {
    val minutes = objective.reminderMinutes ?: return ""
    return " · %02d:%02d".format(minutes / 60, minutes % 60)
}

@Composable
private fun CheckButton(done: Boolean, title: String, accent: Color, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val pop by animateFloatAsState(
        if (done) 1.12f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pop",
    )
    val fill by animateColorAsState(if (done) Success else Color.Transparent, label = "fill")
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics {
                role = Role.Checkbox
                stateDescription = if (done) "$title concluído" else "$title pendente"
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .scale(pop)
                .background(fill, CircleShape)
                .border(2.5.dp, if (done) Success else accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
        }
    }
}
