package com.ritmo.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ritmo.app.data.Objective
import com.ritmo.app.domain.clock
import com.ritmo.app.ui.ObjectiveRow
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.CheckToggle
import com.ritmo.app.ui.components.ChunkyProgress
import com.ritmo.app.ui.components.EmptyMessage
import com.ritmo.app.ui.components.Faisca
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.components.IconBadge
import com.ritmo.app.ui.components.Mood
import com.ritmo.app.ui.components.PtBr
import com.ritmo.app.ui.components.SectionBanner
import com.ritmo.app.ui.components.WeekStrip
import com.ritmo.app.ui.theme.ObjectiveColors
import com.ritmo.app.ui.theme.Ritmo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PtBr)

@Composable
fun TodayScreen(
    state: UiState,
    onToggle: (Long) -> Unit,
    onEdit: (Objective) -> Unit,
    onCreate: () -> Unit,
) {
    if (state.todayRows.isEmpty() && state.otherRows.isEmpty()) {
        EmptyMessage(
            "Oi! Eu sou a Faísca.",
            "Crie seu primeiro objetivo e vamos construir seu ritmo.",
            state.accessoryId,
            Mood.HAPPY,
            action = "Criar objetivo",
            onAction = onCreate,
        )
        return
    }

    val grouped = state.todayRows.groupBy { it.objective.period }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "faixa") { TodayBanner(state) }
        item(key = "semana") { WeekStrip(state.today, state.dayOutcomes, Modifier.padding(vertical = 2.dp)) }
        grouped.forEach { (period, rows) ->
            item(key = "grupo-$period") {
                GroupCard(title = period.label, trailing = "${rows.count { it.done }}/${rows.size}") {
                    rows.forEach { row ->
                        ObjectiveLine(row, onToggle = { onToggle(row.objective.id) }, onEdit = { onEdit(row.objective) })
                    }
                }
            }
        }
        if (state.otherRows.isNotEmpty()) {
            item(key = "fora-de-hoje") {
                GroupCard(title = "Fora de hoje") {
                    state.otherRows.forEach { row ->
                        ObjectiveLine(row, onToggle = null, onEdit = { onEdit(row.objective) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayBanner(state: UiState) {
    val total = state.todayRows.size
    val done = state.todayRows.count { it.done }
    val brand = Ritmo.brand
    SectionBanner(
        overline = LocalDate.ofEpochDay(state.today).format(dateFormat).replace("-feira", ""),
        title = state.speech,
        side = { Faisca(state.mood, state.accessoryId, sizeDp = 84.dp) },
        footer = {
            if (total > 0) {
                Spacer(Modifier.height(10.dp))
                ChunkyProgress(done.toFloat() / total, color = brand.bannerFill, track = brand.bannerLip)
                Spacer(Modifier.height(4.dp))
                Text(
                    "$done de $total concluídos",
                    color = brand.onBanner,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ObjectiveLine(row: ObjectiveRow, onToggle: (() -> Unit)?, onEdit: () -> Unit) {
    val objective = row.objective
    val accent = ObjectiveColors[objective.colorIndex % ObjectiveColors.size]
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Editar objetivo", onClick = onEdit)
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(objective.emoji, accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                objective.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (row.done) TextDecoration.LineThrough else null,
            )
            // Cada grupo desce inteiro para a linha de baixo quando falta espaço.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CaptionGroup {
                    Icon(
                        Icons.Rounded.LocalFireDepartment,
                        contentDescription = "Sequência",
                        tint = if (row.streak.current > 0) accent else scheme.outline,
                        modifier = Modifier.size(16.dp),
                    )
                    Caption("${row.streak.current}")
                }
                Caption("+${objective.difficulty.xp} XP")
                reminderLabel(objective)?.let { label ->
                    CaptionGroup {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = "Lembretes",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Caption(label)
                    }
                }
            }
        }
        if (onToggle != null) {
            Spacer(Modifier.width(4.dp))
            CheckToggle(row.done, objective.title, accent, CircleShape, onToggle)
        }
    }
}

@Composable
private fun CaptionGroup(content: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) { content() }
}

@Composable
private fun Caption(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
    )
}

private fun reminderLabel(objective: Objective): String? = when (objective.reminders.size) {
    0 -> null
    1 -> clock(objective.reminders.first())
    else -> "${objective.reminders.size} horários"
}
