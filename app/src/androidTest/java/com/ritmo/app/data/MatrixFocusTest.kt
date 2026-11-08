package com.ritmo.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritmo.app.domain.FinishedPhase
import com.ritmo.app.domain.Focus
import com.ritmo.app.domain.FocusMode
import com.ritmo.app.domain.FocusPhase
import com.ritmo.app.domain.Progression
import com.ritmo.app.domain.Quadrant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatrixFocusTest {
    private lateinit var database: RitmoDatabase
    private lateinit var repository: RitmoRepository

    @Before fun open() {
        database = memoryDatabase()
        repository = RitmoRepository(database)
        runBlocking { repository.ensureSetup(TODAY) }
    }

    @After fun close() = database.close()

    private suspend fun state() = repository.load()!!

    private suspend fun task() = state().tasks.single()

    @Test fun aTaskPaysOnceAndUncheckingTakesItBack() = runBlocking {
        repository.addTask("Pagar a luz", Quadrant.DO, TODAY)
        val xp = state().xp

        val done = repository.toggleTask(task().id, TODAY)
        assertEquals(Progression.TASK_XP, done.xp)
        assertEquals(0, done.coins)
        assertEquals(TODAY, task().doneDay)

        repository.toggleTask(task().id, TODAY)
        assertNull(task().doneDay)
        assertEquals(xp, state().xp)
    }

    @Test fun deletingADoneTaskKeepsTheXp() = runBlocking {
        repository.addTask("Pagar a luz", Quadrant.DO, TODAY)
        repository.toggleTask(task().id, TODAY)
        val xp = state().xp

        repository.deleteTask(task().id)

        assertTrue(state().tasks.isEmpty())
        assertEquals(xp, state().xp)
    }

    @Test fun aTaskCanMoveToAnotherQuadrant() = runBlocking {
        repository.addTask("Responder e-mails", Quadrant.DO, TODAY)

        repository.updateTask(task().copy(quadrant = Quadrant.DELEGATE, title = "Pedir ajuda com e-mails"))

        assertEquals(Quadrant.DELEGATE, task().quadrant)
        assertEquals("Pedir ajuda com e-mails", task().title)
    }

    @Test fun aFinishedPomodoroIsSavedWithItsXp() = runBlocking {
        val pomodoro = FinishedPhase(FocusMode.POMODORO, FocusPhase.FOCUS, FOCUS_START, 25 * 60_000L)

        val outcome = repository.recordFocus(pomodoro, "📚 Ler", TODAY)

        val session = state().sessions.single()
        assertEquals(TODAY, session.day)
        assertEquals(25 * 60, session.seconds)
        assertEquals("📚 Ler", session.title)
        assertEquals(FocusMode.POMODORO, session.mode)
        assertEquals(Focus.xpFor(25 * 60), outcome.xp)
    }

    @Test fun aShortSessionIsSavedButPaysNothing() = runBlocking {
        val short = FinishedPhase(FocusMode.STOPWATCH, FocusPhase.FOCUS, FOCUS_START, 3 * 60_000L)

        val outcome = repository.recordFocus(short, null, TODAY)

        assertEquals(1, state().sessions.size)
        assertEquals(0, outcome.xp)
        assertTrue(state().ledger.none { it.key.startsWith(Snapshot.FOCUS) })
    }

    @Test fun theMatrixAndTheFocusHistoryTravelWithTheAccount() = runBlocking {
        repository.seedProgress()
        val before = state().progress()
        val other = memoryDatabase()
        try {
            val otherRepository = RitmoRepository(other)
            otherRepository.ensureSetup(TODAY)
            Backup(other).restore(Backup(database).export())
            val after = otherRepository.load()!!
            assertEquals(before, after.progress())
            assertEquals(2, after.tasks.size)
            assertEquals(1, after.sessions.size)
            assertEquals(50, after.settings.focusMinutes)
        } finally {
            other.close()
        }
    }
}
