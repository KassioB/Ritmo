package com.ritmo.app.domain

import com.ritmo.app.data.Objective
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderPlanTest {
    /** Quinta-feira. */
    private val day = LocalDate.of(2026, 11, 5)
    private fun at(hour: Int, minute: Int = 0, plusDays: Long = 0): LocalDateTime =
        day.plusDays(plusDays).atTime(hour, minute)

    private fun objective(
        vararg reminders: Int,
        id: Long = 1,
        daysMask: Int = Objective.EVERY_DAY,
        archivedDay: Long? = null,
    ) = Objective(
        id = id,
        title = "agua",
        emoji = "💧",
        colorIndex = 0,
        daysMask = daysMask,
        difficulty = Difficulty.MEDIUM,
        period = Period.ANYTIME,
        reminders = reminders.toList(),
        createdDay = day.toEpochDay() - 30,
        archivedDay = archivedDay,
    )

    @Test fun everyReminderOfTheDayIsPlannedInOrder() {
        val water = objective(8 * 60, 12 * 60 + 15, 18 * 60)
        assertEquals(at(8), ReminderPlan.next(at(0), listOf(water)))
        assertEquals(at(12, 15), ReminderPlan.next(at(8), listOf(water)))
        assertEquals(at(18), ReminderPlan.next(at(12, 15), listOf(water)))
    }

    @Test fun theEveningCheckComesBeforeTomorrowsReminder() {
        val water = objective(8 * 60)
        assertEquals(at(20), ReminderPlan.next(at(9), listOf(water)))
        assertEquals(at(8, plusDays = 1), ReminderPlan.next(at(20), listOf(water)))
    }

    @Test fun skipsDaysTheObjectiveIsNotScheduled() {
        val fridayOnly = objective(9 * 60, daysMask = 1 shl 4)
        assertEquals(at(20), ReminderPlan.next(at(10), listOf(fridayOnly)))
        assertEquals(at(9, plusDays = 1), ReminderPlan.next(at(20), listOf(fridayOnly)))
    }

    @Test fun archivedObjectivesNeverRing() {
        val archived = objective(9 * 60, archivedDay = day.toEpochDay())
        assertEquals(at(20), ReminderPlan.next(at(8), listOf(archived)))
        assertEquals(emptyList<DueReminder>(), ReminderPlan.between(at(8), at(10), listOf(archived)))
    }

    @Test fun aLateCheckStillFindsEveryReminderItMissed() {
        val water = objective(8 * 60, 9 * 60, 10 * 60)
        val due = ReminderPlan.between(at(7), at(9, 30), listOf(water))
        assertEquals(listOf(at(8), at(9)), due.map { it.at })
    }

    @Test fun theIntervalExcludesTheStartAndIncludesTheEnd() {
        val water = objective(8 * 60)
        assertEquals(emptyList<DueReminder>(), ReminderPlan.between(at(8), at(9), listOf(water)))
        assertEquals(1, ReminderPlan.between(at(7), at(8), listOf(water)).size)
    }

    @Test fun aCheckCrossingMidnightCoversBothDays() {
        val late = objective(23 * 60 + 50, 10)
        val due = ReminderPlan.between(at(23), at(0, 30, plusDays = 1), listOf(late))
        assertEquals(listOf(at(23, 50), at(0, 10, plusDays = 1)), due.map { it.at })
    }

    @Test fun noObjectivesStillSchedulesTheEveningCheck() {
        assertEquals(at(20), ReminderPlan.next(at(12), emptyList()))
    }

    @Test fun eveningIsDetectedOnlyInsideTheInterval() {
        assertTrue(ReminderPlan.eveningBetween(at(19), at(20)))
        assertFalse(ReminderPlan.eveningBetween(at(20), at(21)))
        assertFalse(ReminderPlan.eveningBetween(at(10), at(19, 59)))
    }

    @Test fun hundredsOfRemindersAreHandled() {
        val many = objective(*(0 until 1440 step 2).toList().toIntArray())
        assertEquals(at(12, 2), ReminderPlan.next(at(12), listOf(many)))
        assertEquals(30, ReminderPlan.between(at(12), at(13), listOf(many)).size)
    }

    @Test fun addingAReminderKeepsOrderAndIgnoresDuplicates() {
        assertEquals(listOf(480, 600), listOf(600).withReminder(480))
        assertEquals(listOf(480), listOf(480).withReminder(480))
    }

    @Test fun editingAReminderReplacesIt() {
        assertEquals(listOf(500, 600), listOf(480, 600).withReminder(500, replacing = 480))
        assertEquals(listOf(600), listOf(480, 600).withReminder(600, replacing = 480))
    }

    @Test fun clockFormatsMinutesOfTheDay() {
        assertEquals("08:05", clock(485))
        assertEquals("23:59", clock(1439))
    }
}
