package com.ritmo.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ritmo.app.domain.DayStatus
import com.ritmo.app.domain.Difficulty
import com.ritmo.app.domain.Period
import com.ritmo.app.domain.Plan
import com.ritmo.app.domain.Shop
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProPlanTest {
    /** 5 de novembro de 2026. */
    private val today = 20762L
    private val nextMonth = today + 30
    private val freezeItem = Shop.items.first { it.id == Shop.FREEZE_ID }
    private val repairItem = Shop.items.first { it.id == Shop.REPAIR_ID }
    private val goldTheme = Shop.items.first { it.id == "theme_gold" }
    private lateinit var database: RitmoDatabase
    private lateinit var repository: RitmoRepository

    @Before fun open() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            RitmoDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RitmoRepository(database)
        runBlocking { repository.ensureSetup(today - 20) }
    }

    @After fun close() = database.close()

    private suspend fun state() = repository.load()!!

    private suspend fun coins(amount: Int) = database.dao().addLedger(LedgerEntry("presente", coins = amount, day = today))

    private suspend fun objective(difficulty: Difficulty = Difficulty.EASY): Long {
        repository.save(
            Objective(
                title = "Ler",
                emoji = "📚",
                colorIndex = 0,
                daysMask = Objective.EVERY_DAY,
                difficulty = difficulty,
                period = Period.ANYTIME,
                createdDay = today - 20,
            ),
            today - 20,
        )
        return state().objectives.last().id
    }

    /** Três dias concluídos e uma falha há cinco dias: fora do prazo grátis, dentro do Pro. */
    private suspend fun brokenFiveDaysAgo(id: Long) {
        val done = (8L downTo 6L).map { DayRecordRow(id, today - it, DayStatus.DONE) }
        database.dao().saveRecords(done + DayRecordRow(id, today - 5, DayStatus.MISSED))
    }

    @Test fun everyoneStartsOnTheFreePlan() = runBlocking {
        assertEquals(Plan.FREE, state().plan)
    }

    private suspend fun earned(key: String) = state().ledger.firstOrNull { it.key == key }?.coins

    @Test fun proDoublesCoinsAndTheBonusAndUndoRemovesThem() = runBlocking {
        val id = objective()
        repository.setPro(true, today)

        repository.toggle(id, today)
        assertEquals(2, earned("done:$id:$today"))
        assertEquals(10, earned("${Snapshot.PERFECT}$today"))

        repository.toggle(id, today)
        assertNull(earned("done:$id:$today"))
        assertNull(earned("${Snapshot.PERFECT}$today"))
    }

    @Test fun theFreePlanKeepsNormalCoins() = runBlocking {
        val id = objective()
        repository.toggle(id, today)
        assertEquals(1, earned("done:$id:$today"))
        assertEquals(5, earned("${Snapshot.PERFECT}$today"))
    }

    @Test fun xpIsTheSameInBothPlans() = runBlocking {
        val id = objective()
        val free = repository.toggle(id, today).xp
        repository.toggle(id, today)
        repository.setPro(true, today)
        assertEquals(free, repository.toggle(id, today).xp)
    }

    @Test fun proDoublesListCoins() = runBlocking {
        repository.setPro(true, today)
        val list = repository.saveList(Checklist(title = "Compras", emoji = "🛒", colorIndex = 0, createdDay = today))
        repository.addItem(list, "Arroz", Difficulty.MEDIUM)
        repository.addItem(list, "Feijão", Difficulty.MEDIUM)
        val items = database.dao().itemsOf(list)

        repository.toggleItem(items[0].id, today)
        repository.toggleItem(items[1].id, today)
        assertEquals(4, earned("${Snapshot.ITEM}${items[0].id}:$today"))
        assertEquals(4, earned("${Snapshot.ITEM}${items[1].id}:$today"))
        assertEquals(10, earned("${Snapshot.LIST_DONE}$list:$today"))
    }

    @Test fun proRepairsAnOlderBreakForHalfThePrice() = runBlocking {
        val id = objective()
        brokenFiveDaysAgo(id)
        coins(50)
        assertNull(state().streakRepair(today))

        repository.setPro(true, today)
        assertNotNull(state().streakRepair(today))
        assertNotNull(repository.buy(repairItem, today))
        assertEquals(3, state().mainStreak(today).current)
        assertEquals(-50, earned("${Snapshot.STREAK_REPAIR}${today - 5}"))
    }

    @Test fun repairNeedsOnlyTheProPriceInCoins() = runBlocking {
        val id = objective()
        brokenFiveDaysAgo(id)
        repository.setPro(true, today)
        coins(49)
        assertNull(repository.buy(repairItem, today))
        assertEquals(0, state().mainStreak(today).current)
    }

    @Test fun proKeepsMoreFreezes() = runBlocking {
        coins(1000)
        repeat(4) { repository.buy(freezeItem, today) }
        assertEquals(Plan.FREE.maxFreezes, state().freezes)

        repository.setPro(true, today) // Traz os 2 de presente.
        assertEquals(4, state().freezes)
        repeat(4) { repository.buy(freezeItem, today) }
        assertEquals(Plan.PRO.maxFreezes, state().freezes)
    }

    @Test fun monthlyFreezesArriveOncePerMonthEvenTogglingThePlan() = runBlocking {
        assertEquals(2, repository.setPro(true, today))
        assertEquals(0, repository.grantMonthlyFreezes(today))
        repository.setPro(false, today)
        assertEquals(0, repository.setPro(true, today))
        assertEquals(3, state().freezes)

        assertEquals(2, repository.grantMonthlyFreezes(nextMonth))
        assertEquals(5, state().freezes)
    }

    @Test fun monthlyFreezesNeverPassTheCapacity() = runBlocking {
        repository.setPro(true, today)
        assertEquals(2, repository.grantMonthlyFreezes(nextMonth))
        assertEquals(5, state().freezes)
        assertEquals(0, repository.grantMonthlyFreezes(nextMonth + 31))
        assertEquals(5, state().freezes)
    }

    @Test fun theFreePlanGetsNoMonthlyFreezes() = runBlocking {
        assertEquals(0, repository.grantMonthlyFreezes(today))
        assertEquals(1, state().freezes)
    }

    @Test fun leavingProKeepsEverythingButTheExclusiveTheme() = runBlocking {
        repeat(7) { objective() }
        repository.setPro(true, today)
        repository.toggleEquipped(goldTheme)
        assertEquals("theme_gold", state().settings.themeId)

        repository.setPro(false, today)
        assertFalse(state().settings.isPro)
        assertNull(state().settings.themeId)
        assertEquals(7, state().objectives.size)
        assertEquals(3, state().freezes)
        assertFalse(state().plan.canAddObjective(state().objectives.size))
    }

    @Test fun leavingProKeepsABoughtTheme() = runBlocking {
        coins(500)
        val ocean = Shop.items.first { it.id == "theme_ocean" }
        repository.setPro(true, today)
        repository.buy(ocean, today)
        repository.setPro(false, today)
        assertEquals("theme_ocean", state().settings.themeId)
    }

    @Test fun exclusiveThemesCannotBeBoughtWithCoins() = runBlocking {
        coins(500)
        assertNull(repository.buy(goldTheme, today))
        assertFalse("theme_gold" in state().owned)
    }
}
