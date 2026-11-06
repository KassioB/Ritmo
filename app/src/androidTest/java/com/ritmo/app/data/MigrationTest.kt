package com.ritmo.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ritmo.app.domain.ReminderPlan
import com.ritmo.app.domain.ThemeMode
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
    fun boughtAccessoriesAreRefundedWhenTheyLeaveTheShop() {
        helper.createDatabase(name, 2).apply {
            execSQL("INSERT INTO settings (id, lastClosedDay, themeId, accessoryId) VALUES (0, 20761, 'theme_ocean', 'acc_crown')")
            execSQL("INSERT INTO ledger (`key`, xp, coins, freezes, day) VALUES ('earned', 0, 500, 0, 20700)")
            execSQL("INSERT INTO ledger (`key`, xp, coins, freezes, day) VALUES ('purchase:acc_crown', 0, -300, 0, 20750)")
            execSQL("INSERT INTO ledger (`key`, xp, coins, freezes, day) VALUES ('purchase:theme_ocean', 0, -100, 0, 20751)")
            close()
        }
        helper.runMigrationsAndValidate(name, 3, true, MIGRATION_2_3).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, RitmoDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()
        try {
            val state = runBlocking { RitmoRepository(database).load() }!!
            assertEquals(400, state.coins)
            assertEquals("theme_ocean", state.settings.themeId)
            assertNull(state.settings.petId)
        } finally {
            database.close()
        }
    }

    @Test
    fun everyoneStartsOnTheFreePlanAndKeepsTheirData() {
        helper.createDatabase(name, 3).apply {
            execSQL(
                "INSERT INTO settings (id, lastClosedDay, themeId, petId, themeMode, remindersEnabled, " +
                    "streakAlertEnabled, streakAlertMinutes, hapticsEnabled, weekStartsMonday) " +
                    "VALUES (0, 20761, 'theme_ocean', 'pet_raio', 'DARK', 1, 0, 1140, 0, 1)",
            )
            execSQL("INSERT INTO ledger (`key`, xp, coins, freezes, day) VALUES ('earned', 120, 77, 2, 20700)")
            close()
        }
        helper.runMigrationsAndValidate(name, 4, true, MIGRATION_3_4).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, RitmoDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()
        try {
            val state = runBlocking { RitmoRepository(database).load() }!!
            assertEquals(false, state.settings.isPro)
            assertEquals("theme_ocean", state.settings.themeId)
            assertEquals("pet_raio", state.settings.petId)
            assertEquals(ThemeMode.DARK, state.settings.themeMode)
            assertEquals(1140, state.settings.streakAlertMinutes)
            assertEquals(true, state.settings.weekStartsMonday)
            assertEquals(77, state.coins)
            assertEquals(120, state.xp)
            assertEquals(2, state.freezes)
        } finally {
            database.close()
        }
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
        helper.runMigrationsAndValidate(name, 3, true, MIGRATION_2_3).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, RitmoDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
            assertEquals(20761L, state.settings.lastClosedDay)
            assertNull(state.settings.petId)
            assertEquals(ThemeMode.SYSTEM, state.settings.themeMode)
            assertEquals(true, state.settings.remindersEnabled)
            assertEquals(ReminderPlan.DEFAULT_ALERT_MINUTES, state.settings.streakAlertMinutes)
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
