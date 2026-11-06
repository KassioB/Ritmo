package com.ritmo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class InsightsTest {
    /** Quinta-feira, 5 de novembro de 2026. */
    private val date = LocalDate.of(2026, 11, 5)
    private val today = date.toEpochDay()

    private fun day(offset: Long, done: Int = 0, missed: Int = 0, frozen: Int = 0, pending: Int = 0, items: Int = 0, xp: Int = 0) =
        DaySummary(today + offset, done, missed, frozen, pending, items, xp)

    private fun days(vararg summaries: DaySummary) = summaries.associateBy { it.day }

    private fun info(title: String, period: Period = Period.ANYTIME, difficulty: Difficulty = Difficulty.EASY) =
        ObjectiveInfo(title, period, difficulty)

    private fun report(days: Map<Long, DaySummary>, range: Range = Range.MONTH, anchor: LocalDate = date) =
        Insights.report(days, emptyList(), emptyMap(), range, anchor, DayOfWeek.SUNDAY, today)

    @Test fun weekRespectsTheChosenFirstDay() {
        assertEquals(LocalDate.of(2026, 11, 1), Insights.startOf(Range.WEEK, date, DayOfWeek.SUNDAY))
        assertEquals(LocalDate.of(2026, 11, 2), Insights.startOf(Range.WEEK, date, DayOfWeek.MONDAY))
    }

    @Test fun monthAndYearHaveTheirNaturalLimits() {
        assertEquals(LocalDate.of(2026, 11, 30), Insights.endOf(Range.MONTH, LocalDate.of(2026, 11, 1)))
        assertEquals(LocalDate.of(2026, 2, 28), Insights.endOf(Range.MONTH, LocalDate.of(2026, 2, 1)))
        assertEquals(LocalDate.of(2026, 12, 31), Insights.endOf(Range.YEAR, LocalDate.of(2026, 1, 1)))
    }

    @Test fun rateCountsOnlyWhatWasDecided() {
        val data = days(day(-2, done = 3, missed = 1), day(0, done = 1, pending = 2))
        assertEquals(0.8f, report(data).rate!!, 0.001f)
    }

    @Test fun noScheduledTasksMeansNoRateInsteadOfZero() {
        val r = report(days(day(0, items = 2)))
        assertNull(r.rate)
        assertEquals(2, r.items)
    }

    @Test fun todayPendingIsNotAFailure() {
        val r = report(days(day(0, done = 1, pending = 1)))
        assertEquals(emptyList<DaySummary>(), r.failedDays)
        assertEquals(0, r.perfectDays)
    }

    @Test fun failedDaysAreListedFromNewestToOldest() {
        val r = report(days(day(-4, missed = 1), day(-3, done = 2), day(-2, done = 1, frozen = 1)))
        assertEquals(listOf(today - 2, today - 4), r.failedDays.map { it.day })
    }

    @Test fun twoBlankDaysInARowAreALapse() {
        val r = report(days(day(-4, done = 1), day(-3, missed = 2), day(-2, missed = 1), day(-1, done = 1)))
        assertEquals(listOf(Streakline(today - 3, today - 2, 2)), r.lapses)
    }

    @Test fun oneBlankDayAloneIsNotALapse() {
        val r = report(days(day(-3, missed = 1), day(-2, done = 1), day(-1, missed = 1)))
        assertEquals(emptyList<Streakline>(), r.lapses)
    }

    @Test fun aPartialDayBreaksALapse() {
        val r = report(days(day(-3, missed = 1), day(-2, done = 1, missed = 1), day(-1, missed = 1)))
        assertEquals(emptyList<Streakline>(), r.lapses)
    }

    @Test fun daysWithoutTasksDoNotInterruptALapse() {
        val r = report(days(day(-4, missed = 1), day(-1, missed = 1)))
        assertEquals(listOf(Streakline(today - 4, today - 1, 2)), r.lapses)
    }

    @Test fun aProtectedDayStillCountsAsStopping() {
        val r = report(days(day(-3, frozen = 1), day(-2, missed = 1)))
        assertEquals(listOf(Streakline(today - 3, today - 2, 2)), r.lapses)
    }

    @Test fun todayPendingDoesNotExtendALapse() {
        val r = report(days(day(-2, missed = 1), day(-1, missed = 1), day(0, pending = 2)))
        assertEquals(listOf(Streakline(today - 2, today - 1, 2)), r.lapses)
    }

    @Test fun peakIsTheLongestPerfectRun() {
        val r = report(
            days(
                day(-4, done = 1),
                day(-3, done = 1, missed = 1),
                day(-2, done = 2),
                day(-1, done = 1),
                day(0, done = 3),
            ),
        )
        assertEquals(Streakline(today - 2, today, 3), r.bestRun)
    }

    @Test fun daysWithoutTasksDoNotBreakThePeak() {
        val r = report(days(day(-4, done = 1), day(-1, done = 1)))
        assertEquals(Streakline(today - 4, today - 1, 2), r.bestRun)
    }

    @Test fun onATieTheMostRecentPeakWins() {
        val r = report(days(day(-4, done = 1), day(-3, missed = 1), day(-2, done = 1)))
        assertEquals(Streakline(today - 2, today - 2, 1), r.bestRun)
    }

    @Test fun bestDayCountsListItemsToo() {
        val r = report(days(day(-2, done = 3), day(-1, done = 1, items = 4)))
        assertEquals(today - 1, r.bestDay?.day)
    }

    @Test fun previousPeriodIsTheOneRightBefore() {
        val october = today - 10
        val data = days(DaySummary(october, done = 1, missed = 1), day(-1, done = 1))
        val r = report(data, Range.MONTH)
        assertEquals(1f, r.rate!!, 0.001f)
        assertEquals(0.5f, r.previousRate!!, 0.001f)
    }

    @Test fun weekHasSevenColumnsMonthOnePerDayYearTwelve() {
        val data = days(day(0, done = 1))
        assertEquals(7, report(data, Range.WEEK).buckets.size)
        assertEquals(30, report(data, Range.MONTH).buckets.size)
        assertEquals(12, report(data, Range.YEAR).buckets.size)
    }

    @Test fun yearColumnsAddUpTheMonth() {
        val data = days(day(-1, done = 2, missed = 1), day(0, done = 1))
        val november = report(data, Range.YEAR).buckets[10]
        assertEquals(3, november.done)
        assertEquals(1, november.notDone)
    }

    @Test fun anOlderPeriodIgnoresWhatCameAfter() {
        val r = report(days(day(0, done = 3)), Range.MONTH, LocalDate.of(2026, 9, 10))
        assertEquals(0, r.done)
        assertNull(r.rate)
    }

    @Test fun objectivesAreRankedFromBestToWorst() {
        val records = listOf(
            DayRecord(1, today - 2, DayStatus.MISSED),
            DayRecord(1, today - 1, DayStatus.DONE),
            DayRecord(2, today - 2, DayStatus.DONE),
            DayRecord(2, today - 1, DayStatus.DONE),
        )
        val r = Insights.report(
            emptyMap(), records, mapOf(1L to info("ler"), 2L to info("agua")),
            Range.MONTH, date, DayOfWeek.SUNDAY, today,
        )
        assertEquals(listOf("agua", "ler"), r.byObjective.map { it.title })
        assertEquals(0.5f, r.byObjective.last().rate, 0.001f)
    }

    @Test fun daysJoinRecordsItemsAndXp() {
        val summary = Insights.days(
            records = listOf(DayRecord(1, today, DayStatus.DONE), DayRecord(2, today, DayStatus.MISSED)),
            itemDays = listOf(today, today),
            xpByDay = mapOf(today to 40),
            today = today,
            pendingToday = 1,
        ).getValue(today)
        assertEquals(DaySummary(today, done = 1, missed = 1, pending = 1, items = 2, xp = 40), summary)
    }

    @Test fun daysAlsoJoinCoinsAndFinishedLists() {
        val summary = Insights.days(
            records = emptyList(),
            itemDays = emptyList(),
            xpByDay = emptyMap(),
            today = today,
            pendingToday = 0,
            coinsByDay = mapOf(today to 7),
            listDays = listOf(today, today),
        ).getValue(today)
        assertEquals(7, summary.coins)
        assertEquals(2, summary.lists)
    }

    @Test fun averageOnlyCountsDaysAlreadyLived() {
        val r = report(days(day(-4, done = 2), day(-2, done = 3, items = 1), day(0, done = 1, pending = 1)))
        assertEquals(5, r.elapsedDays)
        assertEquals(7f / 5f, r.dailyAverage, 0.001f)
    }

    @Test fun daysBeforeTheAppWasInstalledDoNotLowerTheAverage() {
        val r = report(days(day(-1, done = 2), day(0, done = 2)), Range.YEAR)
        assertEquals(2, r.elapsedDays)
        assertEquals(2f, r.dailyAverage, 0.001f)
    }

    @Test fun anEmptyPeriodHasNoAverage() {
        val r = report(days(day(0)), Range.MONTH, date.minusMonths(3))
        assertEquals(0, r.elapsedDays)
        assertEquals(0f, r.dailyAverage, 0.001f)
    }

    @Test fun activeAndProtectedDaysAreCounted() {
        val r = report(days(day(-3, done = 1), day(-2, items = 2), day(-1, frozen = 1), day(0, pending = 2)))
        assertEquals(2, r.activeDays)
        assertEquals(1, r.protectedDays)
    }

    @Test fun coinsAndListsAreAddedUpForThePeriodOnly() {
        val data = days(
            DaySummary(today - 1, coins = 5, lists = 1),
            DaySummary(today, coins = 2, lists = 1),
            DaySummary(today - 40, coins = 90, lists = 3),
        )
        val r = report(data)
        assertEquals(7, r.coins)
        assertEquals(2, r.lists)
    }

    @Test fun weekdaysFollowTheChosenFirstDayAndSkipDaysWithoutTasks() {
        // 1 de novembro de 2026 é domingo, 2 é segunda.
        val records = listOf(
            DayRecord(1, today - 4, DayStatus.DONE),
            DayRecord(1, today - 3, DayStatus.MISSED),
        )
        fun titles(start: DayOfWeek) =
            Insights.report(emptyMap(), records, emptyMap(), Range.MONTH, date, start, today).byWeekday.map { it.title }
        assertEquals(listOf("Domingo", "Segunda"), titles(DayOfWeek.SUNDAY))
        assertEquals(listOf("Segunda", "Domingo"), titles(DayOfWeek.MONDAY))
    }

    @Test fun strongestAndWeakestWeekdaysComeFromTheRate() {
        val records = listOf(
            DayRecord(1, today - 4, DayStatus.DONE),
            DayRecord(2, today - 4, DayStatus.DONE),
            DayRecord(1, today - 3, DayStatus.DONE),
            DayRecord(2, today - 3, DayStatus.MISSED),
        )
        val r = Insights.report(emptyMap(), records, emptyMap(), Range.MONTH, date, DayOfWeek.SUNDAY, today)
        assertEquals("Domingo", r.bestWeekday?.title)
        assertEquals("Segunda", r.worstWeekday?.title)
    }

    @Test fun thereIsNoStrongestWeekdayWhenTheyAreAllEqual() {
        val records = listOf(DayRecord(1, today - 4, DayStatus.DONE), DayRecord(1, today - 3, DayStatus.DONE))
        val r = Insights.report(emptyMap(), records, emptyMap(), Range.MONTH, date, DayOfWeek.SUNDAY, today)
        assertNull(r.bestWeekday)
        assertNull(r.worstWeekday)
    }

    @Test fun periodsAndDifficultiesGroupTheObjectivesInTheirNaturalOrder() {
        val records = listOf(
            DayRecord(1, today - 1, DayStatus.DONE),
            DayRecord(2, today - 1, DayStatus.MISSED),
            DayRecord(3, today - 1, DayStatus.DONE),
        )
        val objectives = mapOf(
            1L to info("a", Period.EVENING, Difficulty.HARD),
            2L to info("b", Period.MORNING, Difficulty.EASY),
            3L to info("c", Period.MORNING, Difficulty.EASY),
        )
        val r = Insights.report(emptyMap(), records, objectives, Range.MONTH, date, DayOfWeek.SUNDAY, today)
        assertEquals(listOf(Score("Manhã", 1, 2), Score("Noite", 1, 1)), r.byPeriod)
        assertEquals(listOf(Score("Fácil", 1, 2), Score("Difícil", 1, 1)), r.byDifficulty)
    }
}
