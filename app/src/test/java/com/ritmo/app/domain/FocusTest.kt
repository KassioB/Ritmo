package com.ritmo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTest {
    private val minute = FocusClock.MINUTE
    private val durations = FocusDurations(focusMinutes = 25, breakMinutes = 5)
    private val t0 = 1_700_000_000_000L

    @Test fun aPomodoroCountsDownFromTheChosenLength() {
        val clock = FocusClock().start(t0)

        assertTrue(clock.running)
        assertEquals(25 * minute, clock.remaining(t0, durations))
        assertEquals(15 * minute, clock.remaining(t0 + 10 * minute, durations))
        assertEquals(t0 + 25 * minute, clock.endsAt(durations))
    }

    @Test fun pausingFreezesTheTimeAndResumingMovesTheEnd() {
        val paused = FocusClock().start(t0).pause(t0 + 10 * minute)

        assertFalse(paused.running)
        assertTrue(paused.active)
        assertEquals(15 * minute, paused.remaining(t0 + 60 * minute, durations))
        assertNull(paused.endsAt(durations))

        val resumed = paused.start(t0 + 60 * minute)
        assertEquals(t0 + 75 * minute, resumed.endsAt(durations))
    }

    @Test fun aFinishedFocusBecomesASessionAndTheBreakWaitsForATap() {
        val clock = FocusClock().start(t0)

        val (early, none) = clock.settle(t0 + 24 * minute, durations)
        assertSame(clock, early)
        assertNull(none)

        // O app ficou fechado bem depois do fim: a sessão conta só os 25 minutos, a partir do início.
        val (next, finished) = clock.settle(t0 + 90 * minute, durations)
        assertEquals(FinishedPhase(FocusMode.POMODORO, FocusPhase.FOCUS, t0, 25 * minute), finished)
        assertEquals(FocusPhase.BREAK, next.phase)
        assertFalse(next.active)
        assertEquals(5 * minute, next.remaining(t0 + 90 * minute, durations))
    }

    @Test fun afterTheBreakComesFocusAgain() {
        val breaking = FocusClock(phase = FocusPhase.BREAK).start(t0)

        val (next, finished) = breaking.settle(t0 + 5 * minute, durations)

        assertEquals(FocusPhase.BREAK, finished!!.phase)
        assertEquals(FocusPhase.FOCUS, next.phase)
        assertFalse(next.active)
    }

    @Test fun stoppingThrowsAwayTheCurrentFocusOrSkipsTheBreak() {
        assertEquals(FocusClock(), FocusClock().start(t0).stop())
        assertEquals(FocusPhase.FOCUS, FocusClock(phase = FocusPhase.BREAK).stop().phase)
    }

    @Test fun theStopwatchHasNoEndAndRecordsWhenFinished() {
        val clock = FocusClock(mode = FocusMode.STOPWATCH).start(t0)

        assertNull(clock.endsAt(durations))
        assertNull(clock.settle(t0 + 600 * minute, durations).second)

        val (next, session) = clock.pause(t0 + 30 * minute).finish(t0 + 99 * minute)
        assertEquals(30 * minute, session!!.millis)
        assertFalse(next.active)
        assertEquals(FocusMode.STOPWATCH, next.mode)
    }

    @Test fun lessThanAMinuteIsNotASession() {
        val clock = FocusClock(mode = FocusMode.STOPWATCH).start(t0)

        assertNull(clock.finish(t0 + 59_000).second)
    }

    @Test fun theModeOnlyChangesWithTheClockStopped() {
        val running = FocusClock().start(t0)

        assertEquals(FocusMode.POMODORO, running.withMode(FocusMode.STOPWATCH).mode)
        assertEquals(FocusMode.STOPWATCH, FocusClock().withMode(FocusMode.STOPWATCH).mode)
    }

    @Test fun focusPaysThreeXpEveryFiveMinutesUpToACap() {
        assertEquals(0, Focus.xpFor(4 * 60 + 59))
        assertEquals(3, Focus.xpFor(5 * 60))
        assertEquals(15, Focus.xpFor(25 * 60))
        assertEquals(Focus.MAX_XP, Focus.xpFor(10 * 3600))
    }

    @Test fun timesAreWrittenTheWayAClockShowsThem() {
        assertEquals("25:00", Focus.clock(25 * minute))
        assertEquals("00:01", Focus.clock(400))
        assertEquals("1:05:09", Focus.clock((3600 + 5 * 60 + 9) * 1000L))
        assertEquals("25 min", Focus.minutes(25 * 60 + 40))
        assertEquals("1 h 10 min", Focus.minutes(70 * 60))
        assertEquals("2 h", Focus.minutes(120 * 60))
    }
}
