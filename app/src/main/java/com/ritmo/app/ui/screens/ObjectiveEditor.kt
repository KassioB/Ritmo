package com.ritmo.app.ui.screens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ritmo.app.data.Objective
import com.ritmo.app.domain.Difficulty
import com.ritmo.app.domain.Period
import com.ritmo.app.domain.Streak
import com.ritmo.app.domain.clock
import com.ritmo.app.domain.withReminder
import com.ritmo.app.notifications.Reminders
import com.ritmo.app.ui.components.ChoiceTile
import com.ritmo.app.ui.components.ColorPicker
import com.ritmo.app.ui.components.EditorPage
import com.ritmo.app.ui.components.IconPicker
import com.ritmo.app.ui.components.IconPreview
import com.ritmo.app.ui.components.NoticeRow
import com.ritmo.app.ui.components.SectionLabel
import com.ritmo.app.ui.theme.ObjectiveColors

private const val DEFAULT_ICON = "💧"
private const val NEW_REMINDER = -1
private const val DEFAULT_REMINDER = 8 * 60
private const val LAST_MINUTE = 23 * 60 + 59

private val dayLabels = listOf("S", "T", "Q", "Q", "S", "S", "D")
private val dayNames = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")

private data class Draft(
    val title: String,
    val emoji: String,
    val colorIndex: Int,
    val daysMask: Int,
    val difficulty: Difficulty,
    val period: Period,
    val reminders: List<Int>,
)

private fun Objective?.toDraft() = Draft(
    title = this?.title.orEmpty(),
    emoji = this?.emoji ?: DEFAULT_ICON,
    colorIndex = this?.colorIndex ?: 0,
    daysMask = this?.daysMask ?: Objective.EVERY_DAY,
    difficulty = this?.difficulty ?: Difficulty.MEDIUM,
    period = this?.period ?: Period.ANYTIME,
    reminders = this?.reminders.orEmpty(),
)

/** Criação ([objective] nulo) ou edição. Todo o rascunho sobrevive a girar a tela. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ObjectiveEditor(
    objective: Objective?,
    streak: Streak?,
    today: Long,
    onSave: (Objective) -> Unit,
    onArchive: (Objective) -> Unit,
    onClose: () -> Unit,
) {
    val initial = remember { objective.toDraft() }
    var title by rememberSaveable { mutableStateOf(initial.title) }
    var emoji by rememberSaveable { mutableStateOf(initial.emoji) }
    var colorIndex by rememberSaveable { mutableIntStateOf(initial.colorIndex) }
    var daysMask by rememberSaveable { mutableIntStateOf(initial.daysMask) }
    var difficulty by rememberSaveable { mutableStateOf(initial.difficulty) }
    var period by rememberSaveable { mutableStateOf(initial.period) }
    var reminders by rememberSaveable { mutableStateOf(initial.reminders) }
    var editingReminder by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmArchive by rememberSaveable { mutableStateOf(false) }

    val accent = ObjectiveColors[colorIndex % ObjectiveColors.size]
    val draft = Draft(title, emoji, colorIndex, daysMask, difficulty, period, reminders)

    EditorPage(
        title = if (objective == null) "Novo objetivo" else "Editar objetivo",
        canSave = title.isNotBlank() && daysMask != 0,
        dirty = draft != initial,
        onClose = onClose,
        onSave = {
            val base = objective ?: Objective(
                title = "",
                emoji = emoji,
                colorIndex = colorIndex,
                daysMask = daysMask,
                difficulty = difficulty,
                period = period,
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
                    reminders = reminders,
                ),
            )
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconPreview(emoji, accent)
            Spacer(Modifier.width(14.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        if (streak != null) {
            Text(
                "Streak atual: ${streak.current} · Recorde: ${streak.best}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionLabel("Ícone")
        IconPicker(emoji, accent) { emoji = it }

        SectionLabel("Cor")
        ColorPicker(colorIndex) { colorIndex = it }

        SectionLabel("Dias")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dayLabels.forEachIndexed { index, label ->
                val bit = 1 shl index
                DayToggle(label, dayNames[index], daysMask and bit != 0, accent, Modifier.weight(1f)) {
                    daysMask = daysMask xor bit
                }
            }
        }
        if (daysMask == 0) {
            Text("Escolha ao menos um dia.", color = MaterialTheme.colorScheme.error)
        }

        SectionLabel("Período")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Period.entries.forEach { option ->
                ChoiceTile(period == option, { period = option }, accent = accent) {
                    Text(option.label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        SectionLabel("Dificuldade")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { option ->
                ChoiceTile(difficulty == option, { difficulty = option }, Modifier.weight(1f), accent) {
                    Text(option.label, style = MaterialTheme.typography.labelLarge)
                    Text("${option.xp} XP", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        SectionLabel("Lembretes", "Adicione quantos horários quiser.")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            reminders.forEach { minutes ->
                ReminderChip(
                    minutes,
                    accent,
                    onEdit = { editingReminder = minutes },
                    onRemove = { reminders = reminders - minutes },
                )
            }
            AddReminderChip { editingReminder = NEW_REMINDER }
        }
        if (reminders.isNotEmpty()) ReminderNotices()

        if (objective != null) {
            TextButton(onClick = { confirmArchive = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Arquivar objetivo", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    editingReminder?.let { target ->
        val start = when {
            target != NEW_REMINDER -> target
            reminders.isEmpty() -> DEFAULT_REMINDER
            else -> (reminders.last() + 60).coerceAtMost(LAST_MINUTE)
        }
        ReminderDialog(
            start = start,
            onConfirm = { minutes ->
                reminders = reminders.withReminder(minutes, replacing = target.takeIf { it != NEW_REMINDER })
                editingReminder = null
            },
            onDismiss = { editingReminder = null },
        )
    }

    if (confirmArchive && objective != null) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            properties = DialogProperties(dismissOnClickOutside = false),
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
private fun DayToggle(
    label: String,
    name: String,
    on: Boolean,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (on) accent else scheme.surfaceContainerHigh)
            .selectable(selected = on, role = Role.Checkbox, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (on) Color.White else scheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ReminderChip(minutes: Int, accent: Color, onEdit: () -> Unit, onRemove: () -> Unit) {
    val time = clock(minutes)
    Row(
        Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.16f))
            .border(2.dp, accent, CircleShape)
            .clickable(onClickLabel = "Mudar horário", onClick = onEdit)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(time, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onRemove, modifier = Modifier.size(38.dp)) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remover lembrete das $time",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AddReminderChip(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .clip(CircleShape)
            .background(scheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text("Adicionar lembrete", color = scheme.primary, style = MaterialTheme.typography.titleSmall)
    }
}

/** Sem estas permissões o lembrete atrasa ou nem aparece, então o aviso fica onde ele é criado. */
@Composable
private fun ReminderNotices() {
    val context = LocalContext.current
    var exact by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        exact = Reminders.canBeExact(context)
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose {}
    }
    if (!notifications) {
        NoticeRow("As notificações estão desligadas, então os lembretes não vão aparecer.", "Ativar") {
            openNotificationSettings(context)
        }
    } else if (!exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        NoticeRow("Sem a permissão de alarmes, um lembrete pode atrasar até 1 hora.", "Permitir") {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
            )
        }
    }
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )
}

/**
 * Só fecha pelos botões ou pelo voltar: um toque fora não perde o horário em escolha. Usa um
 * diálogo próprio porque o relógio deitado é mais largo que o limite do AlertDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDialog(start: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val picker = rememberTimePickerState(start / 60, start % 60, is24Hour = true)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Deitado, a altura mal comporta o relógio e os botões.
                if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    Text(
                        "Horário do lembrete",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                TimePicker(picker)
                Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    TextButton(onClick = { onConfirm(picker.hour * 60 + picker.minute) }) { Text("OK") }
                }
            }
        }
    }
}
