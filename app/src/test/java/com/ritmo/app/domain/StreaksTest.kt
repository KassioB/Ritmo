package com.ritmo.app.domain

import com.ritmo.app.domain.DayStatus.DONE
import com.ritmo.app.domain.DayStatus.FROZEN
import com.ritmo.app.domain.DayStatus.MISSED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreaksTest {
    private fun r(obj: Long, day: Long, status: DayStatus) = DayRecord(obj, day, status)

    @Test fun objectiveStreakCountsAndResets() {
        val records = listOf(r(1, 1, DONE), r(1, 2, DONE), r(1, 3, MISSED), r(1, 4, DONE))
        assertEquals(Streak(1, 2), Streaks.forObjective(1, records))
    }

    @Test fun unscheduledDaysAreNeutral() {
        val records = listOf(r(1, 1, DONE), r(1, 8, DONE))
        assertEquals(Streak(2, 2), Streaks.forObjective(1, records))
    }

    @Test fun frozenDayKeepsStreakWithoutAdding() {
        val records = listOf(r(1, 1, DONE), r(1, 2, FROZEN), r(1, 3, DONE))
        assertEquals(Streak(2, 2), Streaks.forObjective(1, records))
    }

    @Test fun mainStreakNeedsEveryObjective() {
        val records = listOf(
            r(1, 1, DONE), r(2, 1, DONE),
            r(1, 2, DONE), r(2, 2, MISSED),
            r(1, 3, DONE), r(2, 3, DONE),
        )
        assertEquals(Streak(1, 1), Streaks.main(records, today = 4, todayComplete = false))
    }

    @Test fun pendingTodayDoesNotBreakAndPartialTodayDoesNotCount() {
        val records = listOf(r(1, 1, DONE), r(1, 2, DONE), r(1, 3, DONE))
        assertEquals(Streak(2, 2), Streaks.main(records, today = 3, todayComplete = false))
        assertEquals(Streak(3, 3), Streaks.main(records, today = 3, todayComplete = true))
    }

    private fun repair(vararg records: DayRecord, today: Long = 10, todayComplete: Boolean = false) =
        Streaks.repairable(records.toList(), today, todayComplete)

    @Test fun aRecentBreakCanBeRepaired() {
        val result = repair(r(1, 7, DONE), r(1, 8, DONE), r(1, 9, MISSED))
        assertEquals(StreakRepair(listOf(9L), 2), result)
    }

    @Test fun repairingKeepsWhatWasDoneAfterTheBreak() {
        val result = repair(r(1, 6, DONE), r(1, 7, DONE), r(1, 8, MISSED), r(1, 9, DONE), today = 10, todayComplete = true)
        assertEquals(StreakRepair(listOf(8L), 4), result)
    }

    @Test fun aBreakOlderThanTheWindowCannotBeRepaired() {
        val edge = 10L - Streaks.REPAIR_WINDOW_DAYS
        assertEquals(listOf(edge), repair(r(1, edge - 1, DONE), r(1, edge, MISSED))?.days)
        assertNull(repair(r(1, edge - 2, DONE), r(1, edge - 1, MISSED)))
    }

    @Test fun aLongAbsenceIsNotARecentBreak() {
        assertNull(repair(r(1, 4, DONE), r(1, 5, MISSED), r(1, 6, MISSED), r(1, 7, MISSED), r(1, 8, MISSED), r(1, 9, MISSED)))
    }

    @Test fun nothingToRepairWithoutAStreakBeforeTheBreak() {
        assertNull(repair(r(1, 8, MISSED), r(1, 9, MISSED)))
        assertNull(repair(r(1, 9, DONE)))
        assertNull(repair())
    }

    @Test fun repairCoversEveryMissedDayOfTheGapAndSkipsFrozenOnes() {
        val result = repair(r(1, 6, DONE), r(1, 7, FROZEN), r(1, 8, MISSED), r(1, 9, MISSED))
        assertEquals(StreakRepair(listOf(8L, 9L), 1), result)
    }

    @Test fun aPartialDayCountsAsTheBreak() {
        val result = repair(r(1, 8, DONE), r(2, 8, DONE), r(1, 9, DONE), r(2, 9, MISSED))
        assertEquals(StreakRepair(listOf(9L), 1), result)
    }

    @Test fun anAlreadyRepairedGapIsNotOfferedAgain() {
        assertNull(repair(r(1, 8, DONE), r(1, 9, FROZEN)))
    }

    @Test fun closerUsesFreezeWhenStreakAtRisk() {
        val existing = listOf(r(1, 1, DONE))
        val result = DayCloser.close(2, 2, { listOf(1L) }, existing, freezesAvailable = 1)
        assertEquals(listOf(r(1, 2, FROZEN)), result.newRecords)
        assertEquals(listOf(2L), result.frozenDays)
    }

    @Test fun closerMissesWhenOutOfFreezes() {
        val existing = listOf(r(1, 1, DONE))
        val result = DayCloser.close(2, 3, { listOf(1L) }, existing, freezesAvailable = 1)
        assertEquals(listOf(r(1, 2, FROZEN), r(1, 3, MISSED)), result.newRecords)
    }

    @Test fun closerSavesFreezeWhenNothingToProtect() {
        val result = DayCloser.close(2, 2, { listOf(1L) }, emptyList(), freezesAvailable = 1)
        assertEquals(listOf(r(1, 2, MISSED)), result.newRecords)
        assertEquals(emptyList<Long>(), result.frozenDays)
    }

    @Test fun closerOnlyMarksPendingObjectives() {
        val existing = listOf(r(1, 1, DONE), r(2, 1, DONE), r(1, 2, DONE))
        val result = DayCloser.close(2, 2, { listOf(1L, 2L) }, existing, freezesAvailable = 0)
        assertEquals(listOf(r(2, 2, MISSED)), result.newRecords)
    }

    @Test fun levelCurve() {
        assertEquals(1, Progression.levelFor(0).level)
        assertEquals(1, Progression.levelFor(99).level)
        assertEquals(2, Progression.levelFor(100).level)
        assertEquals(LevelInfo(3, 50, 300), Progression.levelFor(350))
    }
}
