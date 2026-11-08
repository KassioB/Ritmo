package com.ritmo.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ritmo.app.data.FocusSession
import com.ritmo.app.data.Settings
import com.ritmo.app.domain.Focus
import com.ritmo.app.domain.FocusClock
import com.ritmo.app.domain.FocusDurations
import com.ritmo.app.domain.FocusMode
import com.ritmo.app.domain.FocusPhase
import com.ritmo.app.domain.FocusTarget
import com.ritmo.app.domain.clock
import com.ritmo.app.notifications.Reminders
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.ChoiceTile
import com.ritmo.app.ui.components.ChunkyButton
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.theme.Ritmo
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

/** Tudo o que a tela de foco pode pedir; o relógio em si vive no app, não aqui. */
class FocusActions(
    val setMode: (FocusMode) -> Unit,
    val pick: () -> Unit,
    val clearTarget: () -> Unit,
    val start: () -> Unit,
    val pause: () -> Unit,
    val stop: () -> Unit,
    val finish: () -> Unit,
    val complete: (FocusTarget) -> Unit,
    val changeSettings: ((Settings) -> Settings) -> Unit,
)

@Composable
fun FocusScreen(state: UiState, clock: FocusClock, actions: FocusActions) {
    val settings = state.settings
    val durations = FocusDurations(settings.focusMinutes, settings.breakMinutes)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmStop by rememberSaveable { mutableStateOf(false) }
    var editingDurations by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var exact by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        exact = Reminders.canBeExact(context)
        onPauseOrDispose {}
    }

    LaunchedEffect(clock.runningSince) {
        now = System.currentTimeMillis()
        while (clock.running) {
            delay(TICK_MS)
            now = System.currentTimeMillis()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FocusMode.entries.forEach { mode ->
                ModeTab(mode.label, clock.mode == mode, enabled = !clock.active || clock.mode == mode) {
                    actions.setMode(mode)
                }
                Spacer(Modifier.width(18.dp))
            }
            Spacer(Modifier.weight(1f))
            if (clock.mode == FocusMode.POMODORO) {
                IconButton(onClick = { editingDurations = true }, enabled = !clock.active) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Duração do foco e da pausa")
                }
            }
        }
        if (clock.active) {
            Text(
                "Para trocar de modo, encerre o tempo atual.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))
        TargetPicker(clock.target, canClear = !clock.active, onPick = actions.pick, onClear = actions.clearTarget)
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                clock.mode == FocusMode.STOPWATCH -> "Cronômetro livre"
                clock.phase == FocusPhase.FOCUS -> "Foco de ${settings.focusMinutes} min"
                else -> "Pausa de ${settings.breakMinutes} min"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )

        Spacer(Modifier.height(20.dp))
        TimerRing(clock, now, durations)
        Spacer(Modifier.height(28.dp))
        Controls(clock, onStop = {
            if (clock.mode == FocusMode.POMODORO && clock.phase == FocusPhase.FOCUS) confirmStop = true else actions.stop()
        }, actions = actions)

        if (clock.mode == FocusMode.POMODORO && clock.active && !exact) {
            Spacer(Modifier.height(20.dp))
            GroupCard {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Para o aviso do fim tocar na hora certa com o app fechado, permita alarmes exatos.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { openExactAlarmSettings(context) }) { Text("Permitir") }
                }
            }
        }

        val target = clock.target
        if (target != null && !clock.running && state.isPending(target) && state.focusToday.any { it.title == target.label }) {
            Spacer(Modifier.height(20.dp))
            GroupCard {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Terminou \"${target.title}\"?",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { actions.complete(target) }) { Text("Marcar como feito") }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        TodayCard(state.focusToday)
        Spacer(Modifier.height(80.dp))
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Encerrar o foco?") },
            text = { Text("Um Pomodoro encerrado antes da hora não conta como sessão nem rende XP.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    actions.stop()
                }) { Text("Encerrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Continuar focando") } },
        )
    }
    if (editingDurations) {
        DurationsDialog(settings, actions.changeSettings) { editingDurations = false }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val accent = Ritmo.brand.streak
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .selectable(selected, enabled = enabled, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleLarge,
            color = when {
                selected -> scheme.onSurface
                enabled -> scheme.onSurfaceVariant
                else -> scheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(
            Modifier.width(36.dp).clip(RoundedCornerShape(2.dp)),
            thickness = 3.dp,
            color = if (selected) accent else androidx.compose.ui.graphics.Color.Transparent,
        )
    }
}

@Composable
private fun TargetPicker(target: FocusTarget?, canClear: Boolean, onPick: () -> Unit, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = "Escolher no que focar", onClick = onPick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                target?.label ?: "Selecione um foco",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
        if (target != null && canClear) {
            IconButton(onClick = onClear) {
                Icon(Icons.Rounded.Close, contentDescription = "Tirar o foco escolhido", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun TimerRing(clock: FocusClock, now: Long, durations: FocusDurations) {
    val scheme = MaterialTheme.colorScheme
    val brand = Ritmo.brand
    val elapsed = clock.elapsed(now)
    val length = clock.length(durations)
    val shown = if (length != null) (length - elapsed).coerceAtLeast(0) else elapsed
    // O Pomodoro esvazia o anel; o cronômetro dá uma volta por minuto.
    val sweep = if (length != null) {
        if (clock.active) shown.toFloat() / length else 1f
    } else {
        (elapsed % FocusClock.MINUTE).toFloat() / FocusClock.MINUTE
    }
    val accent = if (clock.phase == FocusPhase.BREAK) brand.doneText else brand.streak
    val track = scheme.surfaceContainerHigh
    val status = when {
        clock.running && clock.phase == FocusPhase.BREAK -> "Pausa"
        clock.running -> "Focando"
        clock.active -> "Pausado"
        clock.phase == FocusPhase.BREAK -> "Hora da pausa"
        else -> "Pronto"
    }
    Box(
        Modifier
            .size(260.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$status, ${Focus.clock(shown)}"
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawCircle(track, radius = size.minDimension / 2 - inset, style = Stroke(stroke))
            if (sweep > 0f) {
                drawArc(
                    accent,
                    startAngle = -90f,
                    sweepAngle = 360f * sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Focus.clock(shown), fontSize = 60.sp, style = MaterialTheme.typography.displayMedium)
            Text(status, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Controls(clock: FocusClock, onStop: () -> Unit, actions: FocusActions) {
    val stopwatch = clock.mode == FocusMode.STOPWATCH
    val main = when {
        clock.running -> "Pausar"
        clock.active -> "Continuar"
        clock.phase == FocusPhase.BREAK -> "Começar pausa"
        else -> "Começar"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChunkyButton(
            main,
            if (clock.running) actions.pause else actions.start,
            Modifier.width(240.dp),
        )
        when {
            stopwatch && clock.active -> OutlinedButton(onClick = actions.finish, modifier = Modifier.width(240.dp)) {
                Text("Concluir")
            }
            clock.active -> TextButton(onClick = onStop) { Text("Encerrar") }
            clock.phase == FocusPhase.BREAK -> TextButton(onClick = actions.stop) { Text("Pular pausa") }
        }
    }
}

@Composable
private fun TodayCard(sessions: List<FocusSession>) {
    val total = sessions.sumOf { it.seconds }
    val summary = if (sessions.isEmpty()) null else {
        val count = if (sessions.size == 1) "1 sessão" else "${sessions.size} sessões"
        "$count · ${Focus.minutes(total)}"
    }
    GroupCard(title = "Foco de hoje", trailing = summary) {
        if (sessions.isEmpty()) {
            Text(
                "Nenhuma sessão ainda. Cada 5 minutos de foco rendem 3 XP.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            sessions.asReversed().forEach { session ->
                val time = Instant.ofEpochMilli(session.startedAt).atZone(ZoneId.systemDefault()).toLocalTime()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        clock(time.hour * 60 + time.minute),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        session.title ?: session.mode.label,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(Focus.minutes(session.seconds), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun DurationsDialog(settings: Settings, onChange: ((Settings) -> Settings) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pomodoro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Foco", style = MaterialTheme.typography.titleSmall)
                Focus.FOCUS_OPTIONS.chunked(4).forEach { row ->
                    MinuteRow(row, settings.focusMinutes) { minutes -> onChange { it.copy(focusMinutes = minutes) } }
                }
                Spacer(Modifier.height(4.dp))
                Text("Pausa", style = MaterialTheme.typography.titleSmall)
                MinuteRow(Focus.BREAK_OPTIONS, settings.breakMinutes) { minutes -> onChange { it.copy(breakMinutes = minutes) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Pronto") } },
    )
}

@Composable
private fun MinuteRow(options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { minutes ->
            ChoiceTile(selected == minutes, { onSelect(minutes) }, Modifier.weight(1f)) {
                Text("$minutes", style = MaterialTheme.typography.titleSmall)
                Text("min", style = MaterialTheme.typography.labelSmall)
            }
        }
        // Mantém a largura das opções igual nas linhas incompletas.
        repeat(4 - options.size) { Spacer(Modifier.weight(1f)) }
    }
}

private const val TICK_MS = 250L
