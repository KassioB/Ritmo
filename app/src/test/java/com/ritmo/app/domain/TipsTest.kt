package com.ritmo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TipsTest {
    private val date = LocalDate.of(2026, 11, 5)
    private val allGood = listOf("Nenhum ponto fraco claro")

    private fun titles(report: Report) = Insights.tips(report).map { it.title }

    private fun report(
        range: Range = Range.MONTH,
        byObjective: List<Score> = emptyList(),
        byWeekday: List<Score> = emptyList(),
        byPeriod: List<Score> = emptyList(),
        byDifficulty: List<Score> = emptyList(),
    ) = Report(
        range = range,
        start = date,
        end = date,
        rate = null,
        previousRate = null,
        done = 1,
        notDone = 0,
        items = 0,
        xp = 0,
        perfectDays = 0,
        failedDays = emptyList(),
        bestRun = null,
        bestDay = null,
        lapses = emptyList(),
        buckets = emptyList(),
        byObjective = byObjective,
        elapsedDays = 1,
        activeDays = 1,
        protectedDays = 0,
        coins = 0,
        lists = 0,
        byWeekday = byWeekday,
        byPeriod = byPeriod,
        byDifficulty = byDifficulty,
    )

    @Test fun pointsAtTheObjectiveThatSlipsTheMost() {
        val title = titles(report(byObjective = listOf(Score("Ler", 9, 10), Score("Treinar", 4, 10)))).single()
        assertTrue(title, "\"Treinar\"" in title && "40%" in title)
    }

    @Test fun aFewTasksAreNotAPattern() {
        assertEquals(allGood, titles(report(byObjective = listOf(Score("Ler", 0, 2)))))
    }

    @Test fun inAWeekEachDayHappensOnceSoThereIsNoWeakWeekday() {
        val week = listOf(Score("Segunda", 3, 3), Score("Quarta", 0, 3))
        assertEquals(allGood, titles(report(range = Range.WEEK, byWeekday = week)))
        assertTrue("Quarta" in titles(report(byWeekday = week)).single())
    }

    @Test fun anytimeIsNotATurnOfTheDay() {
        val periods = listOf(Score("Manhã", 5, 10), Score(Period.ANYTIME.label, 10, 10))
        assertEquals(allGood, titles(report(byPeriod = periods)))
    }

    @Test fun recommendsTheBestTurnWhenTheGapIsClear() {
        val periods = listOf(Score("Manhã", 9, 10), Score("Noite", 5, 10))
        assertTrue("Manhã" in titles(report(byPeriod = periods)).single())
    }

    @Test fun nearlyEqualTurnsAreNotWorthATip() {
        val periods = listOf(Score("Manhã", 9, 10), Score("Noite", 8, 10))
        assertEquals(allGood, titles(report(byPeriod = periods)))
    }

    @Test fun suggestsSmallerStepsWhenHardObjectivesStruggle() {
        val levels = listOf(Score(Difficulty.EASY.label, 9, 10), Score(Difficulty.HARD.label, 4, 10))
        assertTrue("difíceis" in titles(report(byDifficulty = levels)).single())
    }

    @Test fun neverGivesMoreThanThreeTips() {
        val tips = titles(
            report(
                byObjective = listOf(Score("Ler", 1, 10)),
                byWeekday = listOf(Score("Segunda", 9, 10), Score("Quarta", 1, 10)),
                byPeriod = listOf(Score("Manhã", 9, 10), Score("Noite", 1, 10)),
                byDifficulty = listOf(Score(Difficulty.HARD.label, 1, 10)),
            ),
        )
        assertEquals(3, tips.size)
    }
}
