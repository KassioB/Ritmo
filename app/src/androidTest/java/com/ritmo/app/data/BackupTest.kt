package com.ritmo.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupTest {
    private lateinit var database: RitmoDatabase
    private lateinit var repository: RitmoRepository
    private lateinit var backup: Backup

    @Before fun open() {
        database = memoryDatabase()
        repository = RitmoRepository(database)
        backup = Backup(database)
        runBlocking { repository.ensureSetup(TODAY) }
    }

    @After fun close() = database.close()

    private suspend fun state() = repository.load()!!

    @Test fun theProgressComesBackExactlyAsItWas() = runBlocking {
        repository.seedProgress()
        val before = state().progress()

        val other = memoryDatabase()
        try {
            val otherRepository = RitmoRepository(other)
            otherRepository.ensureSetup(TODAY + 30)
            Backup(other).restore(backup.export())
            assertEquals(before, otherRepository.load()!!.progress())
        } finally {
            other.close()
        }
    }

    @Test fun restoringReplacesWhatTheDeviceHad() = runBlocking {
        val empty = backup.export()
        repository.seedProgress()

        backup.restore(empty)

        val after = state()
        assertTrue(after.objectives.isEmpty())
        assertTrue(after.lists.isEmpty())
        assertEquals(listOf("starter-freeze"), after.ledger.map { it.key })
        assertEquals(false, after.settings.isPro)
    }

    @Test fun clearingRemovesTheProgressAndTheAccount() = runBlocking {
        repository.seedProgress()
        database.dao().saveAccount(Account(name = "Ana", email = "ana@gmail.com", password = null, signedIn = true))

        backup.clear()

        assertNull(repository.load())
        assertNull(database.dao().account())
    }

    @Test fun thePackageIsSmallEvenWithYearsOfHistory() = runBlocking {
        repository.seedProgress()
        val id = state().objectives.single().id
        database.dao().saveRecords((1..1500).map { DayRecordRow(id, TODAY - it, com.ritmo.app.domain.DayStatus.DONE) })

        // O limite de um documento do Firestore é 1 MiB.
        assertTrue(backup.export().size < 100_000)
    }
}
