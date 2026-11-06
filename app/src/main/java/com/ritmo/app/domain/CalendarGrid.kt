package com.ritmo.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Quantos dias de cada tipo um mês teve; é o que um leitor de tela diz no calendário do ano. */
data class MonthTally(val perfect: Int, val missed: Int, val frozen: Int)

/** Monta as semanas do calendário. Toda linha tem 7 casas, nulas onde o dia é de outro mês. */
object CalendarGrid {
    private const val DAYS_IN_WEEK = 7

    fun week(start: LocalDate): List<List<Long?>> = listOf(List(DAYS_IN_WEEK) { start.toEpochDay() + it })

    fun month(month: YearMonth, weekStart: DayOfWeek): List<List<Long?>> {
        val first = month.atDay(1)
        val leading = (first.dayOfWeek.value - weekStart.value).mod(DAYS_IN_WEEK)
        val days = List(month.lengthOfMonth()) { first.toEpochDay() + it }
        return (List(leading) { null } + days).chunked(DAYS_IN_WEEK).map { row ->
            row + List(DAYS_IN_WEEK - row.size) { null }
        }
    }

    fun tally(month: YearMonth, outcomes: Map<Long, DayStatus>): MonthTally {
        val days = (month.atDay(1).toEpochDay()..month.atEndOfMonth().toEpochDay()).mapNotNull { outcomes[it] }
        return MonthTally(
            perfect = days.count { it == DayStatus.DONE },
            missed = days.count { it == DayStatus.MISSED },
            frozen = days.count { it == DayStatus.FROZEN },
        )
    }
}
