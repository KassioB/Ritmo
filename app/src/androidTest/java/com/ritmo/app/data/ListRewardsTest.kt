package com.ritmo.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ritmo.app.domain.Achievements
import com.ritmo.app.domain.Difficulty
import com.ritmo.app.domain.Progression
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListRewardsTest {
    private val today = 20762L

    /** O primeiro item concluído também desbloqueia esta conquista, que fica mesmo ao desmarcar. */
    private val firstStepCoins = Achievements.all.first { it.id == "first_step" }.coins
    private lateinit var database: RitmoDatabase
    private lateinit var repository: RitmoRepository

    @Before fun open() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            RitmoDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RitmoRepository(database)
        runBlocking { repository.ensureSetup(today) }
    }

    @After fun close() = database.close()

    private suspend fun state() = repository.load()!!

    private suspend fun list(vararg difficulties: Difficulty): List<Long> {
        val id = repository.saveList(Checklist(title = "Compras", emoji = "🛒", colorIndex = 0, createdDay = today))
        difficulties.forEach { repository.addItem(id, "Item", it) }
        return state().items.filter { it.listId == id }.map { it.id }
    }

    @Test fun itemsPayTheSameXpAsTheRoutineForEachDifficulty() = runBlocking {
        val items = list(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.MEDIUM)

        assertEquals(10, repository.toggleItem(items[0], today).xp)
        assertEquals(20, repository.toggleItem(items[1], today).xp)
        assertEquals(30, repository.toggleItem(items[2], today).xp)
        assertEquals(60, state().xp)
    }

    @Test fun listsPayNoCoinsEvenWhenFinished() = runBlocking {
        list(Difficulty.HARD, Difficulty.HARD).forEach { repository.toggleItem(it, today) }

        assertEquals(firstStepCoins, state().coins)
    }

    @Test fun uncheckingOnTheSameDayGivesTheRewardBack() = runBlocking {
        val items = list(Difficulty.MEDIUM, Difficulty.MEDIUM)
        repository.toggleItem(items[0], today)
        repository.toggleItem(items[0], today)

        assertEquals(0, state().xp)
        assertEquals(firstStepCoins, state().coins)
        assertNull(state().items.first { it.id == items[0] }.doneDay)
    }

    @Test fun checkingAndUncheckingRepeatedlyNeverFarmsXp() = runBlocking {
        val items = list(Difficulty.HARD, Difficulty.HARD)
        repeat(5) {
            repository.toggleItem(items[0], today)
            repository.toggleItem(items[0], today)
        }
        repository.toggleItem(items[0], today)

        assertEquals(Difficulty.HARD.xp, state().xp)
    }

    @Test fun finishingAListPaysTheBonusOnce() = runBlocking {
        val items = list(Difficulty.EASY, Difficulty.EASY)
        assertNull(repository.toggleItem(items[0], today).finishedList)

        val outcome = repository.toggleItem(items[1], today)

        assertEquals("Compras", outcome.finishedList)
        assertEquals(10 + Progression.LIST_COMPLETE_XP, outcome.xp)
        assertEquals(20 + Progression.LIST_COMPLETE_XP, state().xp)
    }

    @Test fun reopeningAFinishedListRemovesTheBonus() = runBlocking {
        val items = list(Difficulty.EASY, Difficulty.EASY)
        items.forEach { repository.toggleItem(it, today) }

        repository.toggleItem(items[0], today)
        assertEquals(10, state().xp)

        val again = repository.toggleItem(items[0], today)
        assertEquals("Compras", again.finishedList)
        assertEquals(20 + Progression.LIST_COMPLETE_XP, state().xp)
    }

    @Test fun aSingleItemListHasNoBonus() = runBlocking {
        val items = list(Difficulty.EASY)
        val outcome = repository.toggleItem(items[0], today)

        assertNull(outcome.finishedList)
        assertEquals(10, state().xp)
    }

    @Test fun aListCanBeRedoneOnAnotherDayButNotTwiceInOneDay() = runBlocking {
        val items = list(Difficulty.MEDIUM, Difficulty.MEDIUM)
        repository.toggleItem(items[0], today)
        assertEquals(20, state().xp)

        // Desmarcar amanhã não devolve o XP de hoje, e marcar de novo rende o XP de amanhã.
        repository.toggleItem(items[0], today + 1)
        assertEquals(20, state().xp)
        repository.toggleItem(items[0], today + 1)
        assertEquals(40, state().xp)
    }

    @Test fun uncheckAllSameDayGivesEverythingBack() = runBlocking {
        val items = list(Difficulty.EASY, Difficulty.EASY)
        val listId = state().items.first().listId
        items.forEach { repository.toggleItem(it, today) }

        repository.uncheckAll(listId, today)

        assertEquals(0, state().xp)
        assertTrue(state().items.all { it.doneDay == null })
    }

    @Test fun uncheckAllOnALaterDayKeepsWhatWasEarned() = runBlocking {
        val items = list(Difficulty.EASY, Difficulty.EASY)
        val listId = state().items.first().listId
        items.forEach { repository.toggleItem(it, today) }

        repository.uncheckAll(listId, today + 1)

        assertEquals(20 + Progression.LIST_COMPLETE_XP, state().xp)
        assertTrue(state().items.all { it.doneDay == null })
    }

    @Test fun deletingAListKeepsTheXpAndRemovesItsItems() = runBlocking {
        val items = list(Difficulty.HARD, Difficulty.HARD)
        val listId = state().items.first().listId
        repository.toggleItem(items[0], today)

        repository.deleteList(listId)

        assertEquals(30, state().xp)
        assertEquals(emptyList<ChecklistItem>(), state().items)
        assertEquals(emptyList<Checklist>(), state().lists)
    }

    @Test fun renamingAListKeepsItsItems() = runBlocking {
        val items = list(Difficulty.EASY, Difficulty.EASY)
        val saved = state().lists.single()

        assertEquals(saved.id, repository.saveList(saved.copy(title = "Mercado")))

        assertEquals("Mercado", state().lists.single().title)
        assertEquals(items, state().items.map { it.id })
    }

    @Test fun listCompletionsCountForAchievementsAndLevel() = runBlocking {
        val items = list(Difficulty.EASY, Difficulty.EASY)
        val outcome = repository.toggleItem(items[0], today)

        assertNotNull(outcome.achievements.firstOrNull { it.id == "first_step" })
        assertEquals(1, state().stats(today).completions)
    }

    @Test fun manyItemsInOneList() = runBlocking {
        val items = list(*Array(300) { Difficulty.EASY })
        assertEquals(300, items.size)
        items.forEach { repository.toggleItem(it, today) }

        assertEquals(300 * 10 + Progression.LIST_COMPLETE_XP, state().xp)
    }
}
