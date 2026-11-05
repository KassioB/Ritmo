package com.ritmo.app.data

import androidx.room.withTransaction
import com.ritmo.app.domain.Achievement
import com.ritmo.app.domain.Achievements
import com.ritmo.app.domain.DayCloser
import com.ritmo.app.domain.DayRecord
import com.ritmo.app.domain.DayStatus
import com.ritmo.app.domain.Difficulty
import com.ritmo.app.domain.Progression
import com.ritmo.app.domain.ShopItem
import com.ritmo.app.domain.ShopKind
import com.ritmo.app.domain.Stats
import com.ritmo.app.domain.Streak
import com.ritmo.app.domain.Streaks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

/** Foto do banco; todo o estado derivado (streaks, nível, saldo) é calculado a partir dela. */
data class Snapshot(
    val objectives: List<Objective>,
    val records: List<DayRecord>,
    val ledger: List<LedgerEntry>,
    val unlocked: List<UnlockedAchievement>,
    val settings: Settings,
    val lists: List<Checklist> = emptyList(),
    val items: List<ChecklistItem> = emptyList(),
) {
    val xp get() = ledger.sumOf { it.xp }
    val coins get() = ledger.sumOf { it.coins }
    val freezes get() = ledger.sumOf { it.freezes }
    val owned get() = ledger.filter { it.key.startsWith(PURCHASE) }.map { it.key.removePrefix(PURCHASE) }.toSet()

    fun scheduledOn(day: Long) = objectives.filter { it.isScheduledOn(day) }

    fun isDone(objectiveId: Long, day: Long) =
        records.any { it.objectiveId == objectiveId && it.day == day && it.status == DayStatus.DONE }

    fun isComplete(day: Long): Boolean {
        val scheduled = scheduledOn(day)
        return scheduled.isNotEmpty() && scheduled.all { isDone(it.id, day) }
    }

    fun mainStreak(today: Long): Streak = Streaks.main(records, today, isComplete(today))

    fun stats(today: Long) = Stats(
        completions = records.count { it.status == DayStatus.DONE } + ledger.count { it.key.startsWith(ITEM) },
        perfectDays = ledger.count { it.key.startsWith(PERFECT) },
        bestMainStreak = mainStreak(today).best,
        bestObjectiveStreak = objectives.maxOfOrNull { Streaks.forObjective(it.id, records).best } ?: 0,
        level = Progression.levelFor(xp).level,
        freezesUsed = ledger.count { it.key.startsWith(FREEZE_USED) },
        purchases = ledger.count { it.key.startsWith(PURCHASE) || it.key.startsWith(FREEZE_BOUGHT) },
    )

    companion object {
        const val PURCHASE = "purchase:"
        const val FREEZE_BOUGHT = "freeze-bought:"
        const val PERFECT = "perfect:"
        const val FREEZE_USED = "freeze-used:"
        const val ITEM = "item:"
        const val LIST_DONE = "list-done:"
    }
}

data class ToggleOutcome(
    val xp: Int = 0,
    val coins: Int = 0,
    val perfectDay: Boolean = false,
    /** Título da lista que acabou de ser concluída por este toque. */
    val finishedList: String? = null,
    val newLevel: Int? = null,
    val achievements: List<Achievement> = emptyList(),
)

data class ClosingOutcome(
    val frozenDays: List<Long> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
)

class RitmoRepository(private val db: RitmoDatabase) {
    private val dao = db.dao()

    private val routine: Flow<Snapshot> = combine(
        dao.observeObjectives(),
        dao.observeRecords(),
        dao.observeLedger(),
        dao.observeUnlocked(),
        dao.observeSettings().filterNotNull(),
    ) { objectives, records, ledger, unlocked, settings ->
        Snapshot(objectives, records.map { it.toDomain() }, ledger, unlocked, settings)
    }

    val snapshot: Flow<Snapshot> = combine(routine, dao.observeLists(), dao.observeItems()) { state, lists, items ->
        state.copy(lists = lists, items = items)
    }

    suspend fun load(): Snapshot? {
        val settings = dao.settings() ?: return null
        return Snapshot(
            dao.objectives(),
            dao.records().map { it.toDomain() },
            dao.ledger(),
            dao.unlocked(),
            settings,
            dao.lists(),
            dao.items(),
        )
    }

    suspend fun ensureSetup(today: Long) = db.withTransaction {
        if (dao.settings() == null) {
            dao.saveSettings(Settings(lastClosedDay = today - 1))
            dao.addLedger(LedgerEntry("starter-freeze", freezes = Progression.STARTING_FREEZES, day = today))
        }
    }

    /** Registra os dias já encerrados e informa os que foram salvos por um bloqueio. */
    suspend fun closePastDays(today: Long): ClosingOutcome = db.withTransaction {
        val state = load() ?: return@withTransaction ClosingOutcome()
        val from = state.settings.lastClosedDay + 1
        val to = today - 1
        if (from > to) return@withTransaction ClosingOutcome()

        val result = DayCloser.close(
            fromDay = from,
            toDay = to,
            schedule = { day -> state.scheduledOn(day).map { it.id } },
            existing = state.records,
            freezesAvailable = state.freezes,
        )
        dao.saveRecords(result.newRecords.map { DayRecordRow(it.objectiveId, it.day, it.status) })
        result.frozenDays.forEach { day ->
            dao.addLedger(LedgerEntry("${Snapshot.FREEZE_USED}$day", freezes = -1, day = day))
        }
        dao.saveSettings(state.settings.copy(lastClosedDay = to))
        ClosingOutcome(result.frozenDays, unlockAchievements(today))
    }

    suspend fun toggle(objectiveId: Long, today: Long): ToggleOutcome = db.withTransaction {
        val objective = dao.objective(objectiveId) ?: return@withTransaction ToggleOutcome()
        val before = load() ?: return@withTransaction ToggleOutcome()
        val key = "done:$objectiveId:$today"
        val wasDone = dao.record(objectiveId, today)?.status == DayStatus.DONE

        if (wasDone) {
            dao.deleteRecord(objectiveId, today)
            dao.deleteLedger(key)
        } else {
            dao.saveRecords(listOf(DayRecordRow(objectiveId, today, DayStatus.DONE)))
            dao.addLedger(
                LedgerEntry(key, objective.difficulty.xp, objective.difficulty.coins, day = today),
            )
        }
        syncPerfectDay(today)
        outcome(before, today)
    }

    /**
     * Mesmo esquema da rotina: cada marcação rende o XP da dificuldade e desmarcar no mesmo dia
     * devolve. A chave inclui o dia, então uma lista reaproveitada (treino, estudo) volta a
     * render em outro dia, mas nunca duas vezes no mesmo.
     */
    suspend fun toggleItem(itemId: Long, today: Long): ToggleOutcome = db.withTransaction {
        val item = dao.item(itemId) ?: return@withTransaction ToggleOutcome()
        val before = load() ?: return@withTransaction ToggleOutcome()
        val key = itemKey(itemId, today)
        val wasDone = item.doneDay != null

        if (wasDone) {
            dao.saveItem(item.copy(doneDay = null))
            dao.deleteLedger(key)
        } else {
            dao.saveItem(item.copy(doneDay = today))
            dao.addLedger(LedgerEntry(key, item.difficulty.xp, item.difficulty.coins, day = today))
        }
        val complete = syncListBonus(item.listId, today)
        val finished = if (complete && !wasDone) dao.list(item.listId)?.title else null
        outcome(before, today, finished)
    }

    suspend fun save(objective: Objective, today: Long) = db.withTransaction {
        dao.saveObjective(objective)
        syncPerfectDay(today)
    }

    suspend fun archive(objective: Objective, today: Long) = db.withTransaction {
        dao.saveObjective(objective.copy(archivedDay = today))
        syncPerfectDay(today)
    }

    suspend fun saveList(list: Checklist): Long = dao.saveList(list)

    /** Excluir a lista não tira o XP já ganho: ele fica no livro-razão. */
    suspend fun deleteList(listId: Long) = db.withTransaction {
        dao.deleteItemsOf(listId)
        dao.deleteList(listId)
    }

    suspend fun addItem(listId: Long, title: String, difficulty: Difficulty) {
        dao.saveItem(ChecklistItem(listId = listId, title = title, difficulty = difficulty))
    }

    suspend fun updateItem(item: ChecklistItem) {
        dao.saveItem(item)
    }

    suspend fun deleteItem(itemId: Long) = dao.deleteItem(itemId)

    /** Prepara a lista para ser refeita. O que foi ganho em outros dias continua seu. */
    suspend fun uncheckAll(listId: Long, today: Long) = db.withTransaction {
        dao.itemsOf(listId).filter { it.doneDay != null }.forEach { item ->
            dao.saveItem(item.copy(doneDay = null))
            dao.deleteLedger(itemKey(item.id, today))
        }
        dao.deleteLedger(listDoneKey(listId, today))
    }

    /** Devolve as conquistas liberadas pela compra, ou `null` se ela não aconteceu. */
    suspend fun buy(item: ShopItem, today: Long): List<Achievement>? = db.withTransaction {
        val state = load() ?: return@withTransaction null
        if (state.coins < item.price) return@withTransaction null
        if (item.kind == ShopKind.FREEZE) {
            if (state.freezes >= Progression.MAX_FREEZES) return@withTransaction null
            val serial = state.ledger.count { it.key.startsWith(Snapshot.FREEZE_BOUGHT) }
            dao.addLedger(
                LedgerEntry("${Snapshot.FREEZE_BOUGHT}$serial", coins = -item.price, freezes = 1, day = today),
            )
        } else {
            if (item.id in state.owned) return@withTransaction null
            dao.addLedger(LedgerEntry("${Snapshot.PURCHASE}${item.id}", coins = -item.price, day = today))
            equip(item, state.settings)
        }
        unlockAchievements(today)
    }

    suspend fun toggleEquipped(item: ShopItem) {
        val settings = dao.settings() ?: return
        val equipped = item.id == settings.themeId || item.id == settings.accessoryId
        if (equipped) {
            dao.saveSettings(
                when (item.kind) {
                    ShopKind.THEME -> settings.copy(themeId = null)
                    else -> settings.copy(accessoryId = null)
                },
            )
        } else {
            equip(item, settings)
        }
    }

    private suspend fun equip(item: ShopItem, settings: Settings) {
        when (item.kind) {
            ShopKind.THEME -> dao.saveSettings(settings.copy(themeId = item.id))
            ShopKind.ACCESSORY -> dao.saveSettings(settings.copy(accessoryId = item.id))
            ShopKind.FREEZE -> Unit
        }
    }

    private fun itemKey(itemId: Long, day: Long) = "${Snapshot.ITEM}$itemId:$day"

    private fun listDoneKey(listId: Long, day: Long) = "${Snapshot.LIST_DONE}$listId:$day"

    private suspend fun outcome(before: Snapshot, today: Long, finishedList: String? = null): ToggleOutcome {
        val achievements = unlockAchievements(today)
        val after = load() ?: before
        val oldLevel = Progression.levelFor(before.xp).level
        val newLevel = Progression.levelFor(after.xp).level
        return ToggleOutcome(
            xp = after.xp - before.xp,
            coins = after.coins - before.coins,
            perfectDay = !before.isComplete(today) && after.isComplete(today),
            finishedList = finishedList,
            newLevel = newLevel.takeIf { it > oldLevel },
            achievements = achievements,
        )
    }

    private suspend fun syncPerfectDay(today: Long) {
        val state = load() ?: return
        val key = "${Snapshot.PERFECT}$today"
        if (state.isComplete(today)) {
            dao.addLedger(
                LedgerEntry(key, Progression.PERFECT_DAY_XP, Progression.PERFECT_DAY_COINS, day = today),
            )
        } else {
            dao.deleteLedger(key)
        }
    }

    /** O equivalente ao dia perfeito: bônus ao fechar uma lista, uma vez por dia. */
    private suspend fun syncListBonus(listId: Long, today: Long): Boolean {
        val items = dao.itemsOf(listId)
        val complete = items.size >= Progression.MIN_ITEMS_FOR_LIST_BONUS && items.all { it.doneDay != null }
        val key = listDoneKey(listId, today)
        if (complete) {
            dao.addLedger(
                LedgerEntry(key, Progression.LIST_COMPLETE_XP, Progression.LIST_COMPLETE_COINS, day = today),
            )
        } else {
            dao.deleteLedger(key)
        }
        return complete
    }

    private suspend fun unlockAchievements(today: Long): List<Achievement> {
        val state = load() ?: return emptyList()
        val stats = state.stats(today)
        val known = state.unlocked.map { it.id }.toSet()
        val fresh = Achievements.all.filter { it.id !in known && it.isUnlocked(stats) }
        fresh.forEach {
            dao.addUnlocked(UnlockedAchievement(it.id, today))
            dao.addLedger(LedgerEntry("achievement:${it.id}", coins = it.coins, day = today))
        }
        return fresh
    }
}
