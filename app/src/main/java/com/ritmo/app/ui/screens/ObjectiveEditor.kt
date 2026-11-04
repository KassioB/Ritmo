package com.ritmo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ritmo.app.data.Objective
import com.ritmo.app.domain.Difficulty
import com.ritmo.app.domain.Period
import com.ritmo.app.domain.Streak
import com.ritmo.app.ui.theme.ObjectiveColors

private val emojis = listOf("💧", "📚", "🏃", "🧘", "🥗", "💊", "🛏️", "✍️", "🎯", "🧹", "🎸", "💻", "🌱", "☀️", "🐶", "🧠")
private val dayLabels = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

/** Criação ([objective] nulo) ou edição. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ObjectiveEditor(
    objective: Objective?,
    streak: Streak?,
    today: Long,
    onSave: (Objective) -> Unit,
    onArchive: (Objective) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(objective?.title.orEmpty()) }
    var emoji by remember { mutableStateOf(objective?.emoji ?: emojis.first()) }
    var colorIndex by remember { mutableIntStateOf(objective?.colorIndex ?: 0) }
    var daysMask by remember { mutableIntStateOf(objective?.daysMask ?: Objective.EVERY_DAY) }
    var difficulty by remember { mutableStateOf(objective?.difficulty ?: Difficulty.MEDIUM) }
    var period by remember { mutableStateOf(objective?.period ?: Period.ANYTIME) }
    var reminder by remember { mutableStateOf(objective?.reminderMinutes) }
    var pickingTime by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }

    val valid = title.isNotBlank() && daysMask != 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (objective == null) "Novo objetivo" else "Editar objetivo",
                style = MaterialTheme.typography.titleLarge,
            )
            if (streak != null) {
                Text("Streak atual: ${streak.current} · Recorde: ${streak.best}")
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Section("Ícone")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                emojis.forEach { option ->
                    val chosen = option == emoji
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(
                                if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                CircleShape,
                            )
                            .clickable { emoji = option }
                            .semantics { selected = chosen },
                        contentAlignment = Alignment.Center,
                    ) { Text(option) }
                }
            }

            Section("Cor")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ObjectiveColors.forEachIndexed { index, color ->
                    val chosen = index == colorIndex
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(color, CircleShape)
                            .border(
                                if (chosen) 3.dp else 0.dp,
                                MaterialTheme.colorScheme.onSurface,
                                CircleShape,
                            )
                            .clickable { colorIndex = index }
                            .semantics {
                                selected = chosen
                                contentDescription = "Cor ${index + 1}"
                            },
                    )
                }
            }

            Section("Dias")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                dayLabels.forEachIndexed { index, label ->
                    val bit = 1 shl index
                    FilterChip(
                        selected = daysMask and bit != 0,
                        onClick = { daysMask = daysMask xor bit },
                        label = { Text(label) },
                    )
                }
            }
            if (daysMask == 0) {
                Text("Escolha ao menos um dia.", color = MaterialTheme.colorScheme.error)
            }

            Section("Período")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Period.entries.forEach { option ->
                    FilterChip(period == option, { period = option }, label = { Text(option.label) })
                }
            }

            Section("Dificuldade")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Difficulty.entries.forEach { option ->
                    FilterChip(
                        difficulty == option,
                        { difficulty = option },
                        label = { Text("${option.label} · ${option.xp} XP") },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lembrete", style = MaterialTheme.typography.titleMedium)
                    reminder?.let { minutes ->
                        TextButton(onClick = { pickingTime = true }) {
                            Text("%02d:%02d".format(minutes / 60, minutes % 60))
                        }
                    }
                }
                Switch(
                    checked = reminder != null,
                    onCheckedChange = { on ->
                        if (on) pickingTime = true else reminder = null
                    },
                )
            }

            Button(
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                onClick = {
                    val base = objective ?: Objective(
                        title = "",
                        emoji = emoji,
                        colorIndex = colorIndex,
                        daysMask = daysMask,
                        difficulty = difficulty,
                        period = period,
                        reminderMinutes = reminder,
                        createdDay = today,
                    )
                    onSave(
                        base.copy(
                            title = title.trim(),
                            emoji = emoji,
                            colorIndex = colorIndex,
                            daysMask = daysMask,
                            difficulty = difficulty,
                            period = period,
                            reminderMinutes = reminder,
                        ),
                    )
                },
            ) { Text("Salvar", fontWeight = FontWeight.Bold) }

            if (objective != null) {
                TextButton(onClick = { confirmArchive = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Arquivar objetivo", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (pickingTime) {
        val initial = reminder ?: (8 * 60)
        val pickerState = rememberTimePickerState(initial / 60, initial % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            confirmButton = {
                TextButton(onClick = {
                    reminder = pickerState.hour * 60 + pickerState.minute
                    pickingTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancelar") } },
            text = { TimePicker(pickerState) },
        )
    }

    if (confirmArchive && objective != null) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("Arquivar objetivo?") },
            text = { Text("Ele sai da sua rotina e o streak dele para de contar. O histórico continua no calendário.") },
            confirmButton = {
                TextButton(onClick = { onArchive(objective) }) { Text("Arquivar") }
            },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}
