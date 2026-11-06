package com.ritmo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanTest {
    private val repair = Shop.items.first { it.id == Shop.REPAIR_ID }
    private val freeze = Shop.items.first { it.id == Shop.FREEZE_ID }
    private val exclusive = Shop.items.first { it.proOnly }

    @Test fun theFreePlanStopsAtEachLimit() {
        val plan = Plan.FREE
        assertTrue(plan.canAddObjective(4))
        assertFalse(plan.canAddObjective(5))
        assertTrue(plan.canAddReminder(1))
        assertFalse(plan.canAddReminder(2))
        assertTrue(plan.canAddList(2))
        assertFalse(plan.canAddList(3))
        assertTrue(plan.canAddItem(29))
        assertFalse(plan.canAddItem(30))
    }

    @Test fun someoneAboveTheLimitCannotAddMore() {
        assertFalse(Plan.FREE.canAddObjective(9))
        assertFalse(Plan.FREE.canAddReminder(6))
    }

    @Test fun proHasNoLimits() {
        val plan = Plan.PRO
        assertTrue(plan.canAddObjective(1000))
        assertTrue(plan.canAddReminder(1000))
        assertTrue(plan.canAddList(1000))
        assertTrue(plan.canAddItem(1000))
    }

    @Test fun onlyProSeesMonthAndYear() {
        assertEquals(listOf(Range.WEEK), Plan.FREE.ranges)
        assertEquals(Range.entries, Plan.PRO.ranges)
    }

    @Test fun retomarCostsHalfForPro() {
        assertEquals(100, Plan.FREE.priceOf(repair))
        assertEquals(50, Plan.PRO.priceOf(repair))
    }

    @Test fun otherPricesAreTheSameInBothPlans() {
        assertEquals(Plan.FREE.priceOf(freeze), Plan.PRO.priceOf(freeze))
    }

    @Test fun exclusiveItemsNeedPro() {
        assertFalse(Plan.FREE.allows(exclusive))
        assertTrue(Plan.PRO.allows(exclusive))
        assertTrue(Plan.FREE.allows(freeze))
    }

    @Test fun proHasALongerWindowToRepairAStreak() {
        val records = listOf(
            DayRecord(1, 90, DayStatus.DONE),
            DayRecord(1, 91, DayStatus.DONE),
            DayRecord(1, 95, DayStatus.MISSED),
        )
        assertNull(Streaks.repairable(records, 100, false, Plan.FREE.repairWindowDays))
        assertNotNull(Streaks.repairable(records, 100, false, Plan.PRO.repairWindowDays))
    }

    @Test fun onlyExclusiveThemesAreMarkedAsProOnly() {
        assertTrue(Shop.isProOnly("theme_gold"))
        assertTrue(Shop.isProOnly("theme_emerald"))
        assertFalse(Shop.isProOnly("theme_black"))
        assertFalse(Shop.isProOnly("não existe"))
    }
}
