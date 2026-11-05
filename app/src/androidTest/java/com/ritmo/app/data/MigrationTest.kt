package com.ritmo.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val name = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RitmoDatabase::class.java,
    )

    private fun objective(id: Long, title: String, reminder: Int?, archivedDay: Long? = null) =
        ContentValues().apply {
            put("id", id)
            put("title", title)
            put("emoji", "💧")
            put("colorIndex", 2)
            put("daysMask", 0b0011111)
            put("difficulty", "HARD")
            put("period", "MORNING")
            put("reminderMinutes", reminder)
            put("createdDay", 20000L)
            put("archivedDay", archivedDay)
        }

    @Test
    fun keepsEverythingFromVersionOne() {
        helper.createDatabase(name, 1).apply {
            insert("objectives", SQLiteDatabase.CONFLICT_ABORT, objective(1, "agua", 735))
            insert("objectives", SQLiteDatabase.CONFLICT_ABORT, objective(2, "ler", null))
            insert("objectives", SQLiteDatabase.CONFLICT_ABORT, objective(5, "antigo", 480, archivedDay = 20500L))
            execSQL("INSERT INTO day_records (objectiveId, day, status) VALUES (1, 20761, 'DONE')")
            execSQL("INSERT INTO ledger (`key`, xp, coins, freezes, day) VALUES ('starter-freeze', 0, 0, 1, 20761)")
            execSQL("INSERT INTO ledger (`key`, xp, coins, freezes, day) VALUES ('done:1:20761', 30, 3, 0, 20761)")
            execSQL("INSERT INTO unlocked_achievements (id, day) VALUES ('first_step', 20761)")
            execSQL("INSERT INTO settings (id, lastClosedDay, themeId, accessoryId) VALUES (0, 20761, NULL, 'acc_crown')")
            close()
        }

        // Valida o esquema final contra o que o Room espera e executa a migração real.
        helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, RitmoDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val state = runBlocking { RitmoRepository(database).load() }!!
            val byId = state.objectives.associateBy { it.id }

            assertEquals(listOf(1L, 2L, 5L), state.objectives.map { it.id })
            assertEquals(listOf(735), byId.getValue(1).reminders)
            assertEquals(emptyList<Int>(), byId.getValue(2).reminders)
            assertEquals(listOf(480), byId.getValue(5).reminders)
            assertEquals(20500L, byId.getValue(5).archivedDay)
            assertNull(byId.getValue(1).archivedDay)
            assertEquals("agua", byId.getValue(1).title)
            assertEquals(2, byId.getValue(1).colorIndex)
            assertEquals(0b0011111, byId.getValue(1).daysMask)
            assertEquals("HARD", byId.getValue(1).difficulty.name)
            assertEquals("MORNING", byId.getValue(1).period.name)

            assertEquals(1, state.records.size)
            assertEquals(30, state.xp)
            assertEquals(3, state.coins)
            assertEquals(1, state.freezes)
            assertEquals(listOf("first_step"), state.unlocked.map { it.id })
            assertEquals("acc_crown", state.settings.accessoryId)
            assertEquals(emptyList<Checklist>(), state.lists)

            // Um objetivo novo não pode reaproveitar o id de um antigo.
            val created = runBlocking {
                database.dao().saveObjective(state.objectives.first().copy(id = 0, title = "novo"))
            }
            assertEquals(6L, created)
        } finally {
            database.close()
        }
    }
}
