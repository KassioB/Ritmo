package com.ritmo.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ritmo.app.domain.CalendarGrid
import com.ritmo.app.domain.DayStatus
import com.ritmo.app.domain.Range
import com.ritmo.app.domain.Report
import com.ritmo.app.domain.Streak
import com.ritmo.app.domain.dias
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.components.PtBr
import com.ritmo.app.ui.theme.BrandColors
import com.ritmo.app.ui.theme.Ritmo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", PtBr)
private val monthName = DateTimeFormatter.ofPattern("MMMM", PtBr)
private val mondayLetters = listOf("S", "T", "Q", "Q", "S", "S", "D")
private val sundayLetters = listOf("D", "S", "T", "Q", "Q", "S", "S")

private val DarkInk = Color(0xFF111114)

/** Acima disto, o número sobre a cor de concluído fica mais legível em tinta escura. */
private const val DARK_INK_LUMINANCE = 0.2f
private const val FROZEN_TINT = 0.3f
private const val MONTHS_PER_ROW = 3
private const val DAYS_IN_WEEK = 7
private const val ROWS_IN_MONTH = 6
private const val EMPTY_DAY_ALPHA = 0.35f
private const val FUTURE_DAY_ALPHA = 0.15f

internal fun weekLetters(weekStart: DayOfWeek) =
    if (weekStart == DayOfWeek.MONDAY) mondayLetters else sundayLetters

private fun statusLabel(status: DayStatus?) = when (status) {
    DayStatus.DONE -> "dia perfeito"
    DayStatus.MISSED -> "dia incompleto"
    DayStatus.FROZEN -> "protegido por bloqueio"
    null -> "sem registro"
}

private fun perfectDays(count: Int) = if (count == 1) "1 dia perfeito" else "$count dias perfeitos"

private fun incompleteDays(count: Int) = if (count == 1) "1 incompleto" else "$count incompletos"

/**
 * Cada estado tem uma forma, não só uma cor: nos temas Branco, Ouro e Esmeralda o azul do bloqueio
 * fica perto demais da cor de concluído. Cheio é dia perfeito, só o anel é dia incompleto, e tinta
 * clara com anel é dia salvo por um bloqueio.
 */
private fun Modifier.dayMark(status: DayStatus?, brand: BrandColors) = when (status) {
    DayStatus.DONE -> background(brand.chartDone, CircleShape)
    DayStatus.MISSED -> border(2.dp, brand.chartMissed, CircleShape)
    DayStatus.FROZEN -> background(brand.freeze.copy(alpha = FROZEN_TINT), CircleShape)
        .border(2.dp, brand.freeze, CircleShape)
    null -> this
}

/** Segue o filtro do Dashboard, então é ele que deixa o plano grátis só na semana. */
@Composable
fun StreakCalendar(state: UiState, report: Report, weekStart: DayOfWeek, onOpenMonth: (YearMonth) -> Unit) {
    if (report.range == Range.YEAR) {
        YearCalendar(state, report, weekStart, onOpenMonth)
    } else {
        DaysCalendar(state, report, weekStart)
    }
}

@Composable
private fun DaysCalendar(state: UiState, report: Report, weekStart: DayOfWeek) {
    val isWeek = report.range == Range.WEEK
    val start = report.start.toEpochDay()
    val end = report.end.toEpochDay()
    var selected by rememberSaveable(report.range, start) {
        mutableStateOf(state.today.takeIf { it in start..end })
    }
    val rows = remember(report.range, start, weekStart) {
        if (isWeek) CalendarGrid.week(report.start) else CalendarGrid.month(YearMonth.from(report.start), weekStart)
    }

    GroupCard(
        title = if (isWeek) "Streak da semana" else "Calendário do mês",
        trailing = perfectDays(report.perfectDays),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StreakLine(state.mainStreak)
            DayGrid(rows, weekStart, state, selected) { selected = it }
            CalendarLegend()
            DayDetails(selected, state.today, state.records)
        }
    }
}

@Composable
private fun StreakLine(streak: Streak) {
    InsightLine(
        Icons.Rounded.LocalFireDepartment,
        Ritmo.brand.chartDone,
        "Streak atual: ${dias(streak.current)}",
        "Recorde: ${dias(streak.best)}",
    )
}

@Composable
private fun DayGrid(
    rows: List<List<Long?>>,
    weekStart: DayOfWeek,
    state: UiState,
    selected: Long?,
    onSelect: (Long) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            weekLetters(weekStart).forEach {
                Text(
                    it,
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        rows.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                        if (day != null) {
                            DayCell(day, state.dayOutcomes[day], day == state.today, day == selected, day <= state.today) {
                                onSelect(day)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Long,
    status: DayStatus?,
    isToday: Boolean,
    isSelected: Boolean,
    hasHappened: Boolean,
    onClick: () -> Unit,
) {
    val brand = Ritmo.brand
    val scheme = MaterialTheme.colorScheme
    val date = LocalDate.ofEpochDay(day)
    val shape = RoundedCornerShape(12.dp)
    val ink = when {
        status == DayStatus.DONE -> if (brand.chartDone.luminance() > DARK_INK_LUMINANCE) DarkInk else Color.White
        hasHappened -> scheme.onSurface
        else -> scheme.onSurfaceVariant
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(if (isSelected) scheme.surfaceContainerHigh else Color.Transparent)
            .clickable(enabled = hasHappened, onClickLabel = "Ver este dia", onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = date.format(dayFormat) + ", " + (if (isToday) "hoje, " else "") + statusLabel(status)
                selected = isSelected
            }
            .padding(3.dp),
    ) {
        Box(Modifier.fillMaxSize().dayMark(status, brand), contentAlignment = Alignment.Center) {
            Text("${date.dayOfMonth}", color = ink, fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold)
            if (isToday) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(width = 12.dp, height = 3.dp)
                        .background(ink, CircleShape),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarLegend() {
    val brand = Ritmo.brand
    FlowRow(
        Modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(DayStatus.DONE to "Perfeito", DayStatus.MISSED to "Incompleto", DayStatus.FROZEN to "Bloqueio")
            .forEach { (status, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).dayMark(status, brand))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
    }
}

@Composable
private fun DayDetails(day: Long?, today: Long, records: Map<Long, List<Pair<String, DayStatus>>>) {
    val brand = Ritmo.brand
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (day == null) {
            Text("Toque em um dia para ver o que foi feito.", color = muted, style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        Text(LocalDate.ofEpochDay(day).format(dayFormat), style = MaterialTheme.typography.titleSmall)
        val entries = records[day].orEmpty()
        if (entries.isEmpty()) {
            Text(
                if (day == today) "O dia ainda está em andamento." else "Nenhum registro neste dia.",
                color = muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        entries.forEach { (title, status) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).dayMark(status, brand))
                Spacer(Modifier.width(8.dp))
                Text(
                    title + when (status) {
                        DayStatus.DONE -> " · concluído"
                        DayStatus.MISSED -> " · perdido"
                        DayStatus.FROZEN -> " · protegido"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun YearCalendar(state: UiState, report: Report, weekStart: DayOfWeek, onOpenMonth: (YearMonth) -> Unit) {
    val year = report.start.year
    GroupCard(title = "Calendário do ano", trailing = perfectDays(report.perfectDays)) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StreakLine(state.mainStreak)
            (1..12).map { YearMonth.of(year, it) }.chunked(MONTHS_PER_ROW).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { month ->
                        MonthTile(month, weekStart, state.dayOutcomes, state.today, Modifier.weight(1f)) {
                            onOpenMonth(month)
                        }
                    }
                }
            }
            CalendarLegend()
            Text(
                "Toque em um mês para abrir.",
                Modifier.padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Sempre com a altura de 6 semanas, para os meses da mesma linha ficarem alinhados. */
@Composable
private fun MonthTile(
    month: YearMonth,
    weekStart: DayOfWeek,
    outcomes: Map<Long, DayStatus>,
    today: Long,
    modifier: Modifier,
    onOpen: () -> Unit,
) {
    val brand = Ritmo.brand
    val scheme = MaterialTheme.colorScheme
    val name = month.format(monthName).replaceFirstChar { it.uppercase() }
    val tally = CalendarGrid.tally(month, outcomes)
    val rows = remember(month, weekStart) { CalendarGrid.month(month, weekStart) }
    val hasStarted = month.atDay(1).toEpochDay() <= today

    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = hasStarted, onClickLabel = "Abrir mês", onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = "$name: ${perfectDays(tally.perfect)}, ${incompleteDays(tally.missed)}, " +
                    "${tally.frozen} com bloqueio"
            }
            .padding(4.dp),
    ) {
        Text(
            name,
            color = if (hasStarted) scheme.onSurface else scheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Canvas(Modifier.fillMaxWidth().aspectRatio(DAYS_IN_WEEK.toFloat() / ROWS_IN_MONTH)) {
            val slot = size.width / DAYS_IN_WEEK
            val gap = 2.dp.toPx()
            val side = slot - gap
            val line = 1.5.dp.toPx()
            val corner = CornerRadius(3.dp.toPx())
            rows.forEachIndexed { row, week ->
                week.forEachIndexed { column, day ->
                    if (day == null) return@forEachIndexed
                    val origin = Offset(column * slot + gap / 2, row * slot + gap / 2)
                    val inset = Offset(line / 2, line / 2)
                    val outlined = Size(side - line, side - line)
                    when (outcomes[day]) {
                        DayStatus.DONE -> drawRoundRect(brand.chartDone, origin, Size(side, side), corner)
                        DayStatus.MISSED -> drawRoundRect(brand.chartMissed, origin + inset, outlined, corner, Stroke(line))
                        DayStatus.FROZEN -> {
                            drawRoundRect(brand.freeze.copy(alpha = FROZEN_TINT), origin, Size(side, side), corner)
                            drawRoundRect(brand.freeze, origin + inset, outlined, corner, Stroke(line))
                        }
                        null -> {
                            val alpha = if (day <= today) EMPTY_DAY_ALPHA else FUTURE_DAY_ALPHA
                            drawRoundRect(scheme.outline.copy(alpha = alpha), origin, Size(side, side), corner)
                        }
                    }
                }
            }
        }
    }
}
