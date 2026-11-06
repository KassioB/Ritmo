package com.ritmo.app.domain

import com.ritmo.app.data.Objective
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderPlanTest {
    /** Quinta-feira. */
    private val day = LocalDate.of(2026, 11, 5)
    private val alert = ReminderPlan.DEFAULT_ALERT_MINUTES

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
        assertEquals(at(8), ReminderPlan.next(at(0), listOf(water), alert))
        assertEquals(at(12, 15), ReminderPlan.next(at(8), listOf(water), alert))
        assertEquals(at(18), ReminderPlan.next(at(12, 15), listOf(water), alert))
    }

    @Test fun theEveningCheckComesBeforeTomorrowsReminder() {
        val water = objective(8 * 60)
        assertEquals(at(20), ReminderPlan.next(at(9), listOf(water), alert))
        assertEquals(at(8, plusDays = 1), ReminderPlan.next(at(20), listOf(water), alert))
    }

    @Test fun skipsDaysTheObjectiveIsNotScheduled() {
        val fridayOnly = objective(9 * 60, daysMask = 1 shl 4)
        assertEquals(at(20), ReminderPlan.next(at(10), listOf(fridayOnly), alert))
        assertEquals(at(9, plusDays = 1), ReminderPlan.next(at(20), listOf(fridayOnly), alert))
    }

    @Test fun archivedObjectivesNeverRing() {
        val archived = objective(9 * 60, archivedDay = day.toEpochDay())
        assertEquals(at(20), ReminderPlan.next(at(8), listOf(archived), alert))
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
        assertEquals(at(20), ReminderPlan.next(at(12), emptyList(), alert))
    }

    @Test fun eveningIsDetectedOnlyInsideTheInterval() {
        assertTrue(ReminderPlan.alertBetween(at(19), at(20), alert))
        assertFalse(ReminderPlan.alertBetween(at(20), at(21), alert))
        assertFalse(ReminderPlan.alertBetween(at(10), at(19, 59), alert))
    }

    @Test fun theStreakAlertFollowsTheChosenTime() {
        assertEquals(at(21, 30), ReminderPlan.next(at(12), emptyList(), 21 * 60 + 30))
        assertTrue(ReminderPlan.alertBetween(at(21), at(22), 21 * 60 + 30))
    }

    @Test fun withTheAlertOffOnlyRemindersCount() {
        val water = objective(8 * 60)
        assertEquals(at(8, plusDays = 1), ReminderPlan.next(at(9), listOf(water), null))
    }

    @Test fun withEverythingOffNothingIsScheduled() {
        assertNull(ReminderPlan.next(at(9), emptyList(), null))
    }

    @Test fun hundredsOfRemindersAreHandled() {
        val many = objective(*(0 until 1440 step 2).toList().toIntArray())
        assertEquals(at(12, 2), ReminderPlan.next(at(12), listOf(many), alert))
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
