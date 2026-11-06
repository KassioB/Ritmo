package com.ritmo.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritmo.app.domain.Bucket
import com.ritmo.app.domain.DayStatus
import com.ritmo.app.domain.DaySummary
import com.ritmo.app.domain.Insights
import com.ritmo.app.domain.Mood
import com.ritmo.app.domain.Range
import com.ritmo.app.domain.Report
import com.ritmo.app.domain.Score
import com.ritmo.app.domain.Streakline
import com.ritmo.app.domain.dias
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.ChoiceTile
import com.ritmo.app.ui.components.ChunkyProgress
import com.ritmo.app.ui.components.EmptyMessage
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.components.PtBr
import com.ritmo.app.ui.theme.Ritmo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val dayMonth = DateTimeFormatter.ofPattern("d 'de' MMM", PtBr)
private val weekdayDay = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", PtBr)
private val monthYear = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PtBr)
private val monthOnly = DateTimeFormatter.ofPattern("MMMM", PtBr)
private val monthLetters = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
private val sundayLetters = listOf("D", "S", "T", "Q", "Q", "S", "S")
private val mondayLetters = listOf("S", "T", "Q", "Q", "S", "S", "D")

private const val FAILED_DAYS_PREVIEW = 5
private const val MONTH_LABEL_EVERY = 7

/** Comparar turnos ou dificuldades só faz sentido com pelo menos dois grupos. */
private const val MIN_GROUPS = 2

private fun date(day: Long) = LocalDate.ofEpochDay(day)

private fun capitalized(text: String) = text.replaceFirstChar { it.uppercase() }

private fun periodLabel(report: Report): String = when (report.range) {
    Range.WEEK -> if (report.start.month == report.end.month) {
        "${report.start.dayOfMonth} a ${report.end.dayOfMonth} de ${report.end.format(monthOnly)}"
    } else {
        "${report.start.format(dayMonth)} a ${report.end.format(dayMonth)}"
    }
    Range.MONTH -> capitalized(report.start.format(monthYear))
    Range.YEAR -> "${report.start.year}"
}

private fun spanLabel(span: Streakline): String {
    val start = date(span.start)
    val end = date(span.end)
    return when {
        span.start == span.end -> start.format(dayMonth)
        start.month == end.month -> "${start.dayOfMonth} a ${end.format(dayMonth)}"
        else -> "${start.format(dayMonth)} a ${end.format(dayMonth)}"
    }
}

private fun tarefas(count: Int) = if (count == 1) "1 tarefa" else "$count tarefas"

private fun percent(rate: Float) = "${(rate * 100).roundToInt()}%"

@Composable
fun DashboardScreen(state: UiState, onOpenPro: (String) -> Unit) {
    val plan = state.plan
    var chosenRange by rememberSaveable { mutableStateOf(Range.WEEK) }
    // Quem sai do Pro com o mês aberto volta para a semana em vez de ficar numa visão bloqueada.
    val range = chosenRange.takeIf { it in plan.ranges } ?: Range.WEEK
    var anchor by rememberSaveable { mutableStateOf(state.today) }
    val weekStart = if (state.settings.weekStartsMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val report = remember(state.history, range, anchor, weekStart, state.today) {
        Insights.report(
            state.history.days,
            state.history.records,
            state.history.objectives,
            range,
            date(anchor),
            weekStart,
            state.today,
        )
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "filtro") {
            PeriodFilter(
                report = report,
                available = plan.ranges,
                canAdvance = report.end.toEpochDay() < state.today,
                onRange = {
                    chosenRange = it
                    anchor = state.today
                },
                onLocked = { onOpenPro(ProReason.RANGES) },
                onShift = { anchor = Insights.shift(range, date(anchor), it.toLong()).toEpochDay() },
            )
        }
        if (!report.hasData) {
            item(key = "vazio") {
                Box(Modifier.height(380.dp)) {
                    EmptyMessage(
                        "Sem dados neste período",
                        "Conclua objetivos e itens de listas para ver seu desempenho aqui.",
                        state.petId,
                        state.stage.index,
                        Mood.HAPPY,
                    )
                }
            }
        } else {
            item(key = "resumo") { Headline(report) }
            item(key = "numeros") { KpiGrid(report) }
            item(key = "grafico") { ChartCard(report, weekStart) }
            if (plan.isPro) {
                item(key = "dicas") { TipsCard(report) }
                item(key = "detalhes") { DetailsCard(report) }
            } else {
                item(key = "pro") {
                    ProPromo("Entenda seu ritmo", "Mês e ano, dicas e análises por dia, turno e dificuldade.") {
                        onOpenPro(ProReason.NONE)
                    }
                }
            }
            item(key = "pico") { PeakCard(report) }
            item(key = "recaidas") { LapsesCard(report) }
            item(key = "falhas") { FailedDaysCard(report, state.records) }
            // Na semana cada dia aparece uma vez só, e o gráfico de cima já mostra isso.
            if (plan.isPro && report.range != Range.WEEK && report.byWeekday.isNotEmpty()) {
                item(key = "dias-da-semana") { WeekdayCard(report) }
            }
            if (plan.isPro && report.byPeriod.size >= MIN_GROUPS) {
                item(key = "turnos") { ScoresCard("Por turno", report.byPeriod) }
            }
            if (plan.isPro && report.byDifficulty.size >= MIN_GROUPS) {
                item(key = "dificuldades") { ScoresCard("Por dificuldade", report.byDifficulty) }
            }
            if (report.byObjective.isNotEmpty()) {
                item(key = "objetivos") { ObjectivesCard(report.byObjective) }
            }
        }
    }
}

/** Único filtro da tela: tudo abaixo dele mostra o mesmo recorte. */
@Composable
private fun PeriodFilter(
    report: Report,
    available: List<Range>,
    canAdvance: Boolean,
    onRange: (Range) -> Unit,
    onLocked: () -> Unit,
    onShift: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Range.entries.forEach { option ->
                val locked = option !in available
                ChoiceTile(report.range == option, { if (locked) onLocked() else onRange(option) }, Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (locked) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = "Disponível no Pro",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(option.label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onShift(-1) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Período anterior")
            }
            Text(
                periodLabel(report),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onShift(1) }, enabled = canAdvance) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Próximo período")
            }
        }
    }
}

@Composable
private fun Headline(report: Report) {
    val brand = Ritmo.brand
    val scheme = MaterialTheme.colorScheme
    val previous = when (report.range) {
        Range.WEEK -> "a semana passada"
        Range.MONTH -> "o mês passado"
        Range.YEAR -> "o ano passado"
    }
    GroupCard {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                report.rate?.let(::percent) ?: "—",
                fontSize = 56.sp,
                lineHeight = 60.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                if (report.rate != null) "das tarefas programadas foram concluídas" else "sem tarefas programadas",
                color = scheme.onSurfaceVariant,
            )
            val rate = report.rate
            val before = report.previousRate
            if (rate != null && before != null) {
                val points = ((rate - before) * 100).roundToInt()
                val (icon, tint) = when {
                    points > 0 -> Icons.AutoMirrored.Rounded.TrendingUp to brand.doneText
                    points < 0 -> Icons.AutoMirrored.Rounded.TrendingDown to brand.missed
                    else -> Icons.AutoMirrored.Rounded.TrendingFlat to scheme.onSurfaceVariant
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    val pontos = if (kotlin.math.abs(points) == 1) "1 ponto" else "${kotlin.math.abs(points)} pontos"
                    val change = when {
                        points == 0 -> "Igual ${if (report.range == Range.WEEK) "à semana passada" else "ao " + previous.removePrefix("o ")}"
                        points > 0 -> "$pontos acima d$previous"
                        else -> "$pontos abaixo d$previous"
                    }
                    Text(change, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun KpiGrid(report: Report) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kpi("${report.done + report.items}", "Concluídos", Modifier.weight(1f))
            Kpi("${report.perfectDays}", "Dias perfeitos", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kpi("${report.failedDays.size}", "Dias não cumpridos", Modifier.weight(1f))
            Kpi("${report.xp}", "XP ganho", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Kpi(value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** O que não cabe nos destaques, em linhas: mais números sem virar uma parede de cartões. */
@Composable
private fun DetailsCard(report: Report) {
    GroupCard(title = "Mais números") {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailLine("Média de concluídos por dia", String.format(PtBr, "%.1f", report.dailyAverage))
            DetailLine("Dias com algo concluído", "${report.activeDays} de ${report.elapsedDays}")
            DetailLine("Dias protegidos", "${report.protectedDays}")
            DetailLine("Moedas ganhas", "${report.coins}")
            DetailLine("Itens de listas concluídos", "${report.items}")
            DetailLine("Listas fechadas", "${report.lists}")
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(
            label,
            Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

private fun bucketLabel(report: Report, bucket: Bucket): String = when (report.range) {
    Range.YEAR -> capitalized(date(bucket.start).format(monthOnly))
    else -> capitalized(date(bucket.start).format(weekdayDay))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartCard(report: Report, weekStart: DayOfWeek) {
    val brand = Ritmo.brand
    val scheme = MaterialTheme.colorScheme
    var selected by remember(report) { mutableStateOf<Int?>(null) }
    var showTable by rememberSaveable { mutableStateOf(false) }
    val title = if (report.range == Range.YEAR) "Tarefas por mês" else "Tarefas por dia"
    val hasBands = report.range != Range.YEAR
    val lapses = if (hasBands) report.lapses else emptyList()
    val peak = report.bestRun?.takeIf { hasBands && it.days >= 2 }

    GroupCard(title = title) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LegendKey(brand.chartDone, "Concluídas")
                LegendKey(brand.chartMissed, "Não concluídas")
                if (lapses.isNotEmpty()) LegendKey(brand.chartMissed.copy(alpha = 0.22f), "Recaída")
                if (peak != null) LegendKey(brand.chartDone.copy(alpha = 0.22f), "Pico")
            }
            Spacer(Modifier.height(10.dp))
            ActivityChart(report, weekStart, lapses, peak, selected) { selected = it }
            Spacer(Modifier.height(6.dp))
            val bucket = selected?.let { report.buckets.getOrNull(it) }
            Text(
                if (bucket == null) {
                    "Toque em uma coluna para ver os números."
                } else {
                    "${bucketLabel(report, bucket)}: ${bucket.done} concluídas, ${bucket.notDone} não concluídas"
                },
                color = if (bucket == null) scheme.onSurfaceVariant else scheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { showTable = !showTable }) {
                Text(if (showTable) "Esconder tabela" else "Ver tabela")
            }
            if (showTable) ChartTable(report)
        }
    }
}

@Composable
private fun LegendKey(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(5.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

/** Colunas empilhadas a partir de uma só base; as faixas ao fundo marcam recaídas e o pico. */
@Composable
private fun ActivityChart(
    report: Report,
    weekStart: DayOfWeek,
    lapses: List<Streakline>,
    peak: Streakline?,
    selected: Int?,
    onSelect: (Int?) -> Unit,
) {
    val brand = Ritmo.brand
    val scheme = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = scheme.onSurfaceVariant)
    val buckets = report.buckets
    val axisTop = niceTop(buckets.maxOf { it.total })
    val letters = if (weekStart == DayOfWeek.MONDAY) mondayLetters else sundayLetters
    val summary = "Gráfico de colunas: ${report.done} concluídas e ${report.notDone} não concluídas no período. " +
        "Os valores de cada coluna estão em Ver tabela."

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = summary }
            .pointerInput(buckets) {
                detectTapGestures { tap ->
                    val axis = 28.dp.toPx()
                    val index = ((tap.x - axis) / ((size.width - axis) / buckets.size)).toInt()
                    onSelect(index.takeIf { it in buckets.indices && it != selected })
                }
            },
    ) {
        val axis = 28.dp.toPx()
        val xBand = 22.dp.toPx()
        val plotTop = 8.dp.toPx()
        val plotBottom = size.height - xBand
        val slot = (size.width - axis) / buckets.size
        fun y(value: Int) = plotBottom - (plotBottom - plotTop) * value / axisTop

        fun band(span: Streakline, color: Color) {
            val first = (span.start - report.start.toEpochDay()).toInt()
            val last = (span.end - report.start.toEpochDay()).toInt()
            drawRect(
                color,
                Offset(axis + first * slot, plotTop),
                Size((last - first + 1) * slot, plotBottom - plotTop),
            )
        }
        if (selected != null) {
            drawRect(scheme.surfaceContainerHigh, Offset(axis + selected * slot, plotTop), Size(slot, plotBottom - plotTop))
        }
        lapses.forEach { band(it, brand.chartMissed.copy(alpha = 0.14f)) }
        peak?.let { band(it, brand.chartDone.copy(alpha = 0.14f)) }

        listOf(0, axisTop / 2, axisTop).distinct().forEach { tick ->
            val lineY = y(tick)
            drawLine(scheme.outline, Offset(axis, lineY), Offset(size.width, lineY), 1.dp.toPx())
            val text = measurer.measure("$tick", axisStyle)
            drawText(text, topLeft = Offset(axis - text.size.width - 6.dp.toPx(), lineY - text.size.height / 2f))
        }

        val barWidth = (slot * 0.68f).coerceAtMost(24.dp.toPx())
        val gap = 2.dp.toPx()
        val radius = 4.dp.toPx().coerceAtMost(barWidth / 2f)
        buckets.forEachIndexed { index, bucket ->
            val left = axis + index * slot + (slot - barWidth) / 2f
            val doneTop = y(bucket.done)
            if (bucket.done > 0) {
                segment(left, doneTop, barWidth, plotBottom, brand.chartDone, if (bucket.notDone > 0) 0f else radius)
            }
            if (bucket.notDone > 0) {
                val bottom = if (bucket.done > 0) doneTop - gap else plotBottom
                segment(left, y(bucket.total), barWidth, bottom, brand.chartMissed, radius)
            }
            val label = when (report.range) {
                Range.WEEK -> letters[index]
                Range.YEAR -> monthLetters[index]
                Range.MONTH -> "${index + 1}".takeIf { index % MONTH_LABEL_EVERY == 0 }
            }
            if (label != null) {
                val text = measurer.measure(label, axisStyle)
                drawText(
                    text,
                    topLeft = Offset(
                        axis + index * slot + (slot - text.size.width) / 2f,
                        plotBottom + 5.dp.toPx(),
                    ),
                )
            }
        }
    }
}

private fun DrawScope.segment(
    left: Float,
    top: Float,
    width: Float,
    bottom: Float,
    color: Color,
    radius: Float,
) {
    if (bottom <= top) return
    val corner = CornerRadius(radius)
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                Rect(left, top, left + width, bottom),
                topLeft = corner,
                topRight = corner,
                bottomLeft = CornerRadius.Zero,
                bottomRight = CornerRadius.Zero,
            ),
        )
    }
    drawPath(path, color)
}

/** Topo do eixo em número redondo e par, para a marca do meio também ser inteira. */
private fun niceTop(max: Int): Int {
    val step = when {
        max <= 10 -> 2
        max <= 50 -> 10
        max <= 200 -> 20
        max <= 1000 -> 100
        else -> 500
    }
    return maxOf(2, (max + step - 1) / step * step)
}

/** A mesma informação do gráfico em texto, sem depender de toque nem de cor. */
@Composable
private fun ChartTable(report: Report) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text("", Modifier.weight(1f))
            TableHead("Feitas")
            TableHead("Não feitas")
        }
        report.buckets.filter { it.total > 0 }.forEach { bucket ->
            Row {
                Text(bucketLabel(report, bucket), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                TableValue("${bucket.done}")
                TableValue("${bucket.notDone}")
            }
        }
        if (report.buckets.none { it.total > 0 }) {
            Text("Nenhuma tarefa programada.", color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TableHead(text: String) {
    Text(
        text,
        Modifier.width(72.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun TableValue(text: String) {
    Text(text, Modifier.width(72.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
}

@Composable
private fun PeakCard(report: Report) {
    val brand = Ritmo.brand
    GroupCard(title = "Pico") {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val run = report.bestRun
            InsightLine(
                Icons.Rounded.LocalFireDepartment,
                brand.chartDone,
                if (run == null) "Nenhum dia perfeito ainda" else "Maior sequência: ${dias(run.days)}",
                run?.let(::spanLabel),
            )
            val day = report.bestDay
            if (day != null) {
                InsightLine(
                    Icons.Rounded.Star,
                    brand.chartDone,
                    "Dia mais produtivo: ${tarefas(day.completions)}",
                    capitalized(date(day.day).format(weekdayDay)),
                )
            }
        }
    }
}

@Composable
private fun InsightLine(icon: ImageVector, tint: Color, title: String, detail: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (detail != null) {
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun LapsesCard(report: Report) {
    val brand = Ritmo.brand
    GroupCard(title = "Recaídas", trailing = "${report.lapses.size}".takeIf { report.lapses.isNotEmpty() }) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (report.lapses.isEmpty()) {
                InsightLine(
                    Icons.Rounded.CheckCircle,
                    brand.chartDone,
                    "Nenhuma recaída neste período",
                    "Você não ficou ${Insights.MIN_LAPSE_DAYS} dias ou mais sem concluir nada.",
                )
            } else {
                report.lapses.forEach { lapse ->
                    DotLine(brand.chartMissed, "${dias(lapse.days)} sem concluir nada", spanLabel(lapse))
                }
            }
        }
    }
}

@Composable
private fun DotLine(color: Color, title: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun FailedDaysCard(report: Report, records: Map<Long, List<Pair<String, DayStatus>>>) {
    val brand = Ritmo.brand
    var showAll by remember(report) { mutableStateOf(false) }
    val days = report.failedDays
    val shown = if (showAll) days else days.take(FAILED_DAYS_PREVIEW)
    GroupCard(title = "Dias não cumpridos", trailing = "${days.size}".takeIf { days.isNotEmpty() }) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (days.isEmpty()) {
                InsightLine(Icons.Rounded.CheckCircle, brand.chartDone, "Tudo cumprido neste período", null)
            }
            shown.forEach { day -> DotLine(brand.chartMissed, dayTitle(day), missedTitles(day, records)) }
            if (days.size > FAILED_DAYS_PREVIEW) {
                Text(
                    if (showAll) "Mostrar menos" else "Ver todos (${days.size})",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showAll = !showAll }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                )
            }
        }
    }
}

private fun dayTitle(day: DaySummary) = capitalized(date(day.day).format(weekdayDay))

private fun missedTitles(day: DaySummary, records: Map<Long, List<Pair<String, DayStatus>>>): String {
    val missed = records[day.day].orEmpty().filter { it.second != DayStatus.DONE }
    return missed.joinToString { (title, status) ->
        if (status == DayStatus.FROZEN) "$title (protegido)" else title
    }
}

@Composable
private fun TipsCard(report: Report) {
    val brand = Ritmo.brand
    GroupCard(title = "Dicas para você") {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Insights.tips(report).forEach { tip ->
                InsightLine(Icons.Rounded.Lightbulb, brand.chartDone, tip.title, tip.advice)
            }
        }
    }
}

@Composable
private fun WeekdayCard(report: Report) {
    val brand = Ritmo.brand
    GroupCard(title = "Por dia da semana") {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            report.bestWeekday?.let {
                InsightLine(
                    Icons.AutoMirrored.Rounded.TrendingUp,
                    brand.chartDone,
                    "Dia mais forte: ${it.title}",
                    "${percent(it.rate)} concluído",
                )
            }
            report.worstWeekday?.let {
                InsightLine(
                    Icons.AutoMirrored.Rounded.TrendingDown,
                    brand.chartMissed,
                    "Dia mais fraco: ${it.title}",
                    "${percent(it.rate)} concluído",
                )
            }
            ScoreBars(report.byWeekday)
        }
    }
}

@Composable
private fun ScoresCard(title: String, scores: List<Score>) {
    GroupCard(title = title) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { ScoreBars(scores) }
    }
}

/** Uma série só, com o valor ao lado de cada barra: nada aqui depende de toque nem de cor. */
@Composable
private fun ScoreBars(scores: List<Score>) {
    val brand = Ritmo.brand
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        scores.forEach { score ->
            Row(
                Modifier.fillMaxWidth().clearAndSetSemantics {
                    contentDescription = "${score.title}: ${percent(score.rate)}, ${score.done} de ${score.total}"
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(score.title, Modifier.width(100.dp), style = typography.bodySmall)
                ChunkyProgress(score.rate, Modifier.weight(1f), color = brand.chartDone, height = 8.dp)
                Text(percent(score.rate), Modifier.width(44.dp), style = typography.titleSmall, textAlign = TextAlign.End)
                Text(
                    "${score.done}/${score.total}",
                    Modifier.width(52.dp),
                    color = scheme.onSurfaceVariant,
                    style = typography.bodySmall,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

/** Do melhor para o pior: mostra rápido quais objetivos estão puxando a média para baixo. */
@Composable
private fun ObjectivesCard(scores: List<Score>) {
    val brand = Ritmo.brand
    GroupCard(title = "Por objetivo") {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            scores.forEach { score ->
                Column {
                    Row {
                        Text(score.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Text(percent(score.rate), style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(4.dp))
                    ChunkyProgress(score.rate, color = brand.chartDone, height = 8.dp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${score.done} de ${score.total}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
