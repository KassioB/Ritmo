package com.ritmo.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ritmo.app.domain.DayStatus
import com.ritmo.app.domain.Difficulty
import com.ritmo.app.domain.Period
import com.ritmo.app.domain.Shop
import com.ritmo.app.domain.Streaks
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StreakRepairTest {
    private val today = 20762L
    private val repairItem = Shop.items.first { it.id == Shop.REPAIR_ID }
    private lateinit var database: RitmoDatabase
    private lateinit var repository: RitmoRepository

    @Before fun open() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            RitmoDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RitmoRepository(database)
        runBlocking {
            repository.ensureSetup(today - 10)
            // Sem bloqueios: a falha precisa acontecer de verdade.
            database.dao().addLedger(LedgerEntry("sem-bloqueio", freezes = -1, day = today - 10))
        }
    }

    @After fun close() = database.close()

    private suspend fun state() = repository.load()!!

    private suspend fun coins(amount: Int) = database.dao().addLedger(LedgerEntry("presente:$amount", coins = amount, day = today))

    private suspend fun objective(): Long {
        repository.save(
            Objective(
                title = "Ler",
                emoji = "📚",
                colorIndex = 0,
                daysMask = Objective.EVERY_DAY,
                difficulty = Difficulty.EASY,
                period = Period.ANYTIME,
                createdDay = today - 10,
            ),
            today - 10,
        )
        return state().objectives.last().id
    }

    /** Três dias concluídos e ontem perdido. */
    private suspend fun brokenStreak(): Long {
        val id = objective()
        listOf(today - 4, today - 3, today - 2).forEach { repository.toggle(id, it) }
        repository.closePastDays(today)
        assertEquals(0, state().mainStreak(today).current)
        return id
    }

    @Test fun buyingBringsTheStreakBackAndChargesThePrice() = runBlocking {
        val id = brokenStreak()
        coins(repairItem.price + 7)
        val before = state().coins

        val achievements = repository.buy(repairItem, today)!!

        assertEquals(3, state().mainStreak(today).current)
        // A primeira compra também rende a conquista da loja.
        assertEquals(before - repairItem.price + achievements.sumOf { it.coins }, state().coins)
        assertEquals(3, Streaks.forObjective(id, state().records).current)
        assertEquals(DayStatus.FROZEN, state().records.first { it.day == today - 1 }.status)
    }

    @Test fun theSameBreakIsOnlyPaidOnce() = runBlocking {
        brokenStreak()
        coins(repairItem.price * 3)
        repository.buy(repairItem, today)
        val after = state().coins

        assertNull(repository.buy(repairItem, today))
        assertEquals(after, state().coins)
    }

    @Test fun withoutEnoughCoinsNothingChanges() = runBlocking {
        brokenStreak()
        val before = state()
        assertTrue(before.coins < repairItem.price)

        assertNull(repository.buy(repairItem, today))
        assertEquals(0, state().mainStreak(today).current)
        assertEquals(before.coins, state().coins)
    }

    @Test fun withNothingBrokenNoCoinsAreTaken() = runBlocking {
        val id = objective()
        repository.toggle(id, today - 1)
        coins(repairItem.price)
        val before = state().coins

        assertNull(repository.buy(repairItem, today))
        assertEquals(before, state().coins)
    }

    @Test fun repairingCountsAsAPurchase() = runBlocking {
        brokenStreak()
        coins(repairItem.price)
        val achievements = repository.buy(repairItem, today)!!
        assertTrue(achievements.any { it.id == "shop_1" })
    }

    @Test fun aBrokenStreakIsAlsoSeenBeforeTheDayIsClosed() = runBlocking {
        val id = objective()
        listOf(today - 3, today - 2).forEach { repository.toggle(id, it) }
        database.dao().saveSettings(state().settings.copy(lastClosedDay = today - 2))

        assertEquals(2, state().mainStreak(today).current)
        assertEquals(0, state().closedUntil(today).mainStreak(today).current)
        assertEquals(2, state().mainStreak(today).current)
    }
}
