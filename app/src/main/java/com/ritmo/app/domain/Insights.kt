package com.ritmo.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Tudo que aconteceu em um dia. [pending] só existe em hoje: o dia ainda não fechou. */
data class DaySummary(
    val day: Long,
    val done: Int = 0,
    val missed: Int = 0,
    val frozen: Int = 0,
    val pending: Int = 0,
    val items: Int = 0,
    val xp: Int = 0,
    val coins: Int = 0,
    val lists: Int = 0,
) {
    val notDone get() = missed + frozen
    val scheduled get() = done + notDone + pending
    val completions get() = done + items
    val perfect get() = done > 0 && notDone == 0 && pending == 0

    /** Dia encerrado em que alguma tarefa ficou de fora. */
    val failed get() = notDone > 0

    /** Dia encerrado com tarefas em que nada foi feito. */
    val blank get() = notDone > 0 && done == 0
}

enum class Range(val label: String) {
    WEEK("Semana"),
    MONTH("Mês"),
    YEAR("Ano"),
}

/** Trecho contínuo de dias; [days] não conta os dias sem tarefas que ficaram no meio. */
data class Streakline(val start: Long, val end: Long, val days: Int)

/** Uma coluna do gráfico: um dia na semana e no mês, um mês no ano. */
data class Bucket(
    val start: Long,
    val end: Long,
    val done: Int,
    val notDone: Int,
) {
    val total get() = done + notDone
}

/** Parte do programado que foi feita em um recorte: um objetivo, um dia da semana, um turno. */
data class Score(val title: String, val done: Int, val total: Int) {
    val rate get() = if (total == 0) 0f else done.toFloat() / total
}

/** O que o Dashboard precisa saber de cada objetivo para agrupar o histórico. */
data class ObjectiveInfo(val title: String, val period: Period, val difficulty: Difficulty)

data class Report(
    val range: Range,
    val start: LocalDate,
    val end: LocalDate,
    /** Parte do que estava programado que foi feita. Nulo quando não havia nada programado. */
    val rate: Float?,
    val previousRate: Float?,
    val done: Int,
    val notDone: Int,
    val items: Int,
    val xp: Int,
    val perfectDays: Int,
    val failedDays: List<DaySummary>,
    val bestRun: Streakline?,
    val bestDay: DaySummary?,
    val lapses: List<Streakline>,
    val buckets: List<Bucket>,
    val byObjective: List<Score>,
    /** Dias já vividos no período, sem contar os anteriores ao início do uso do app. */
    val elapsedDays: Int,
    val activeDays: Int,
    val protectedDays: Int,
    val coins: Int,
    val lists: Int,
    /** Só os dias com tarefas, na ordem da semana escolhida pelo usuário. */
    val byWeekday: List<Score>,
    val byPeriod: List<Score>,
    val byDifficulty: List<Score>,
) {
    val hasData get() = done + notDone + items > 0
    val dailyAverage get() = if (elapsedDays == 0) 0f else (done + items).toFloat() / elapsedDays

    /** Só há dia mais forte ou mais fraco quando os dias da semana diferem entre si. */
    private val weekdaysDiffer get() = byWeekday.map { it.rate }.distinct().size > 1
    val bestWeekday get() = byWeekday.maxByOrNull { it.rate }.takeIf { weekdaysDiffer }
    val worstWeekday get() = byWeekday.minByOrNull { it.rate }.takeIf { weekdaysDiffer }
}

/**
 * Métricas do Dashboard. Definições usadas em toda a tela:
 *
 * - Dia não cumprido: dia encerrado com ao menos uma tarefa de fora (perdida ou salva por bloqueio).
 * - Recaída: [MIN_LAPSE_DAYS] ou mais dias seguidos com tarefas em que nada foi feito.
 * - Pico: a maior sequência de dias perfeitos do período.
 *
 * Dias sem tarefas programadas são neutros e não quebram nenhuma sequência. Hoje ainda não
 * fechou, então o que está pendente não conta como falha.
 */
object Insights {
    const val MIN_LAPSE_DAYS = 2

    private val WeekdayNames = mapOf(
        DayOfWeek.MONDAY to "Segunda",
        DayOfWeek.TUESDAY to "Terça",
        DayOfWeek.WEDNESDAY to "Quarta",
        DayOfWeek.THURSDAY to "Quinta",
        DayOfWeek.FRIDAY to "Sexta",
        DayOfWeek.SATURDAY to "Sábado",
        DayOfWeek.SUNDAY to "Domingo",
    )

    fun startOf(range: Range, date: LocalDate, weekStart: DayOfWeek): LocalDate = when (range) {
        Range.WEEK -> date.with(TemporalAdjusters.previousOrSame(weekStart))
        Range.MONTH -> date.withDayOfMonth(1)
        Range.YEAR -> date.withDayOfYear(1)
    }

    fun endOf(range: Range, start: LocalDate): LocalDate = shift(range, start, 1).minusDays(1)

    fun shift(range: Range, date: LocalDate, by: Long): LocalDate = when (range) {
        Range.WEEK -> date.plusWeeks(by)
        Range.MONTH -> date.plusMonths(by)
        Range.YEAR -> date.plusYears(by)
    }

    /** Junta os registros de cada dia. [itemDays] traz um dia por item de lista concluído. */
    fun days(
        records: List<DayRecord>,
        itemDays: List<Long>,
        xpByDay: Map<Long, Int>,
        today: Long,
        pendingToday: Int,
        coinsByDay: Map<Long, Int> = emptyMap(),
        listDays: List<Long> = emptyList(),
    ): Map<Long, DaySummary> {
        val byDay = records.groupBy { it.day }
        val items = itemDays.groupingBy { it }.eachCount()
        val lists = listDays.groupingBy { it }.eachCount()
        val all = byDay.keys + items.keys + xpByDay.keys + coinsByDay.keys + lists.keys + today
        return all.associateWith { day ->
            val list = byDay[day].orEmpty()
            DaySummary(
                day = day,
                done = list.count { it.status == DayStatus.DONE },
                missed = list.count { it.status == DayStatus.MISSED },
                frozen = list.count { it.status == DayStatus.FROZEN },
                pending = if (day == today) pendingToday else 0,
                items = items[day] ?: 0,
                xp = xpByDay[day] ?: 0,
                coins = coinsByDay[day] ?: 0,
                lists = lists[day] ?: 0,
            )
        }
    }

    fun report(
        days: Map<Long, DaySummary>,
        records: List<DayRecord>,
        objectives: Map<Long, ObjectiveInfo>,
        range: Range,
        anchor: LocalDate,
        weekStart: DayOfWeek,
        today: Long,
    ): Report {
        val start = startOf(range, anchor, weekStart)
        val end = endOf(range, start)
        val window = between(days, start.toEpochDay(), minOf(end.toEpochDay(), today))
        val previousStart = shift(range, start, -1)
        val previous = between(days, previousStart.toEpochDay(), start.toEpochDay() - 1)
        val lastDay = minOf(end.toEpochDay(), today)
        val inRange = records.filter { it.day in start.toEpochDay()..lastDay }
        val firstDay = maxOf(start.toEpochDay(), days.keys.minOrNull() ?: today)
        val week = (0L until 7L).map { weekStart.plus(it) }

        return Report(
            range = range,
            start = start,
            end = end,
            rate = rate(window),
            previousRate = rate(previous),
            done = window.sumOf { it.done },
            notDone = window.sumOf { it.notDone },
            items = window.sumOf { it.items },
            xp = window.sumOf { it.xp },
            perfectDays = window.count { it.perfect },
            failedDays = window.filter { it.failed }.sortedByDescending { it.day },
            bestRun = longest(window, { it.perfect }, { it.failed }),
            bestDay = window.filter { it.completions > 0 }.maxByOrNull { it.completions },
            lapses = lapses(window),
            buckets = buckets(days, range, start, end, today),
            byObjective = inRange.groupBy { it.objectiveId }
                .map { (id, list) -> score(objectives[id]?.title ?: "Objetivo", list) }
                .sortedWith(compareByDescending<Score> { it.rate }.thenByDescending { it.total }),
            elapsedDays = (lastDay - firstDay + 1).coerceAtLeast(0).toInt(),
            activeDays = window.count { it.completions > 0 },
            protectedDays = window.count { it.frozen > 0 },
            coins = window.sumOf { it.coins },
            lists = window.sumOf { it.lists },
            byWeekday = grouped(inRange, week, { WeekdayNames.getValue(it) }) { LocalDate.ofEpochDay(it.day).dayOfWeek },
            byPeriod = grouped(inRange, Period.entries, { it.label }) { objectives[it.objectiveId]?.period },
            byDifficulty = grouped(inRange, Difficulty.entries, { it.label }) { objectives[it.objectiveId]?.difficulty },
        )
    }

    private fun score(title: String, records: List<DayRecord>) =
        Score(title, records.count { it.status == DayStatus.DONE }, records.size)

    /** Uma linha por chave, na ordem de [keys]; as que não têm registros ficam de fora. */
    private fun <K> grouped(
        records: List<DayRecord>,
        keys: List<K>,
        title: (K) -> String,
        keyOf: (DayRecord) -> K?,
    ): List<Score> {
        val groups = records.groupBy(keyOf)
        return keys.mapNotNull { key -> groups[key]?.let { score(title(key), it) } }
    }

    private fun between(days: Map<Long, DaySummary>, from: Long, to: Long) =
        days.values.filter { it.day in from..to }.sortedBy { it.day }

    private fun rate(days: List<DaySummary>): Float? {
        val done = days.sumOf { it.done }
        val total = done + days.sumOf { it.notDone }
        return if (total == 0) null else done.toFloat() / total
    }

    /** Todas as recaídas do período, da mais recente para a mais antiga. */
    fun lapses(days: List<DaySummary>): List<Streakline> =
        runs(days, { it.blank }, { it.done > 0 })
            .filter { it.days >= MIN_LAPSE_DAYS }
            .sortedByDescending { it.start }

    /** A sequência mais longa; no empate, a mais recente. */
    fun longest(
        days: List<DaySummary>,
        counts: (DaySummary) -> Boolean,
        breaks: (DaySummary) -> Boolean,
    ): Streakline? = runs(days, counts, breaks).maxWithOrNull(compareBy({ it.days }, { it.start }))

    /** Dias que não [counts] nem [breaks] (sem tarefas, ou hoje pendente) não interrompem a sequência. */
    private fun runs(
        days: List<DaySummary>,
        counts: (DaySummary) -> Boolean,
        breaks: (DaySummary) -> Boolean,
    ): List<Streakline> {
        val found = mutableListOf<Streakline>()
        var start: Long? = null
        var last = 0L
        var length = 0
        for (day in days) {
            when {
                counts(day) -> {
                    if (start == null) start = day.day
                    last = day.day
                    length++
                }
                breaks(day) && start != null -> {
                    found += Streakline(start, last, length)
                    start = null
                    length = 0
                }
            }
        }
        start?.let { found += Streakline(it, last, length) }
        return found
    }

    private fun buckets(
        days: Map<Long, DaySummary>,
        range: Range,
        start: LocalDate,
        end: LocalDate,
        today: Long,
    ): List<Bucket> {
        val spans = when (range) {
            Range.WEEK, Range.MONTH -> generateSequence(start) { it.plusDays(1) }
                .takeWhile { !it.isAfter(end) }
                .map { it to it }
                .toList()
            Range.YEAR -> (0L until 12L).map { month ->
                val first = start.plusMonths(month)
                first to first.plusMonths(1).minusDays(1)
            }
        }
        return spans.map { (from, to) ->
            val slice = between(days, from.toEpochDay(), minOf(to.toEpochDay(), today))
            Bucket(from.toEpochDay(), to.toEpochDay(), slice.sumOf { it.done }, slice.sumOf { it.notDone })
        }
    }
}
