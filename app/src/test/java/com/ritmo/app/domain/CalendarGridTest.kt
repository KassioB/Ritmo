package com.ritmo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarGridTest {
    /** Novembro de 2026 começa em um domingo e tem 30 dias. */
    private val november = YearMonth.of(2026, 11)

    private fun day(number: Int) = november.atDay(number).toEpochDay()

    @Test fun aWeekIsOneLineOfSevenDaysInARow() {
        val start = LocalDate.of(2026, 11, 1)
        val rows = CalendarGrid.week(start)

        assertEquals(1, rows.size)
        assertEquals((0L..6L).map { start.toEpochDay() + it }, rows.single())
    }

    @Test fun aMonthStartsOnItsWeekdayColumn() {
        assertEquals(day(1), CalendarGrid.month(november, DayOfWeek.SUNDAY)[0][0])

        val mondayFirst = CalendarGrid.month(november, DayOfWeek.MONDAY)[0]
        assertEquals(List(6) { null }, mondayFirst.take(6))
        assertEquals(day(1), mondayFirst[6])
    }

    @Test fun everyLineHasSevenCellsAndEveryDayAppearsOnce() {
        listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY).forEach { weekStart ->
            (1..12).map { YearMonth.of(2026, it) }.forEach { month ->
                val rows = CalendarGrid.month(month, weekStart)
                assertTrue(rows.all { it.size == 7 })
                assertEquals(
                    (month.atDay(1).toEpochDay()..month.atEndOfMonth().toEpochDay()).toList(),
                    rows.flatten().filterNotNull(),
                )
            }
        }
    }

    @Test fun theLastLineIsFilledWithEmptyCells() {
        val last = CalendarGrid.month(november, DayOfWeek.SUNDAY).last()
        assertEquals(day(29), last[0])
        assertEquals(day(30), last[1])
        assertNull(last[2])
    }

    @Test fun noMonthNeedsMoreThanSixLines() {
        // Agosto de 2026 começa em um sábado: o pior caso com a semana começando no domingo.
        assertEquals(6, CalendarGrid.month(YearMonth.of(2026, 8), DayOfWeek.SUNDAY).size)
        (2026..2036).forEach { year ->
            (1..12).forEach { month ->
                assertTrue(CalendarGrid.month(YearMonth.of(year, month), DayOfWeek.MONDAY).size <= 6)
            }
        }
    }

    @Test fun theTallyCountsOnlyDaysOfThatMonth() {
        val outcomes = mapOf(
            day(1) to DayStatus.DONE,
            day(2) to DayStatus.DONE,
            day(3) to DayStatus.MISSED,
            day(30) to DayStatus.FROZEN,
            day(1) - 1 to DayStatus.DONE,
            day(30) + 1 to DayStatus.MISSED,
        )

        assertEquals(MonthTally(perfect = 2, missed = 1, frozen = 1), CalendarGrid.tally(november, outcomes))
    }
}
