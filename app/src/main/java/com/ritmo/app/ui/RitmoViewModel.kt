package com.ritmo.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ritmo.app.RitmoApp
import com.ritmo.app.data.Objective
import com.ritmo.app.data.Snapshot
import com.ritmo.app.domain.Achievement
import com.ritmo.app.domain.DayStatus
import com.ritmo.app.domain.LevelInfo
import com.ritmo.app.domain.Progression
import com.ritmo.app.domain.ShopItem
import com.ritmo.app.domain.Stats
import com.ritmo.app.domain.Streak
import com.ritmo.app.domain.Streaks
import com.ritmo.app.domain.dias
import com.ritmo.app.notifications.Reminders
import com.ritmo.app.ui.components.Celebration
import com.ritmo.app.ui.components.Mood
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class ObjectiveRow(val objective: Objective, val done: Boolean, val streak: Streak)

data class UiState(
    val today: Long,
    val todayRows: List<ObjectiveRow>,
    val otherRows: List<ObjectiveRow>,
    val mainStreak: Streak,
    val complete: Boolean,
    val level: LevelInfo,
    val xp: Int,
    val coins: Int,
    val freezes: Int,
    val stats: Stats,
    val unlocked: Set<String>,
    val owned: Set<String>,
    val themeId: String?,
    val accessoryId: String?,
    val dayOutcomes: Map<Long, DayStatus>,
    val records: Map<Long, List<Pair<String, DayStatus>>>,
    val mood: Mood,
    val speech: String,
)

sealed interface AppEvent {
    data class Reward(val text: String) : AppEvent
    data class Celebrate(val celebration: Celebration) : AppEvent
}

class RitmoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RitmoApp).repository
    private val today = MutableStateFlow(currentDay())
    private val eventChannel = Channel<AppEvent>(Channel.BUFFERED)

    val events = eventChannel.receiveAsFlow()

    val state = combine(repository.snapshot, today) { snapshot, day -> snapshot.toUi(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            repository.ensureSetup(today.value)
            refreshDay()
            while (true) {
                delay(60_000)
                refreshDay()
            }
        }
    }

    /** Chamado ao abrir o app e a cada minuto, para fechar o dia mesmo com o app aberto na virada. */
    fun refreshDay() {
        viewModelScope.launch {
            val day = currentDay()
            today.value = day
            val closing = repository.closePastDays(day)
            val frozen = closing.frozenDays
            if (frozen.isNotEmpty()) {
                val text = if (frozen.size == 1) "um bloqueio salvou" else "${frozen.size} bloqueios salvaram"
                eventChannel.send(
                    AppEvent.Celebrate(
                        Celebration("Ufa!", "Você perdeu um dia, mas $text seus streaks.", confetti = false),
                    ),
                )
            }
            closing.achievements.forEach { announce(it) }
            syncReminders()
        }
    }

    fun toggle(objectiveId: Long) {
        viewModelScope.launch {
            val outcome = repository.toggle(objectiveId, today.value)
            if (outcome.xp > 0) {
                val coins = when {
                    outcome.coins == 1 -> " · +1 moeda"
                    outcome.coins > 1 -> " · +${outcome.coins} moedas"
                    else -> ""
                }
                eventChannel.send(AppEvent.Reward("+${outcome.xp} XP$coins"))
            }
            if (outcome.perfectDay) {
                val streak = repository.load()?.mainStreak(today.value)?.current ?: 0
                eventChannel.send(
                    AppEvent.Celebrate(
                        Celebration(
                            "Dia perfeito!",
                            "${dias(streak)} de streak. " +
                                "Bônus de ${Progression.PERFECT_DAY_XP} XP e ${Progression.PERFECT_DAY_COINS} moedas.",
                        ),
                    ),
                )
            }
            outcome.newLevel?.let {
                eventChannel.send(
                    AppEvent.Celebrate(Celebration("Nível $it!", "Você subiu de nível. Continue no ritmo.")),
                )
            }
            outcome.achievements.forEach { announce(it) }
        }
    }

    fun save(objective: Objective) {
        viewModelScope.launch {
            repository.save(objective, today.value)
            syncReminders()
        }
    }

    fun archive(objective: Objective) {
        viewModelScope.launch {
            repository.archive(objective, today.value)
            syncReminders()
        }
    }

    fun buy(item: ShopItem) {
        viewModelScope.launch {
            val achievements = repository.buy(item, today.value) ?: return@launch
            eventChannel.send(AppEvent.Reward("Comprado: ${item.name}"))
            achievements.forEach { announce(it) }
        }
    }

    fun toggleEquipped(item: ShopItem) {
        viewModelScope.launch { repository.toggleEquipped(item) }
    }

    private suspend fun announce(achievement: Achievement) {
        eventChannel.send(
            AppEvent.Celebrate(
                Celebration(
                    "Conquista: ${achievement.title}",
                    "${achievement.description}. +${achievement.coins} moedas.",
                ),
            ),
        )
    }

    private suspend fun syncReminders() {
        val state = repository.load() ?: return
        Reminders.sync(getApplication(), state.objectives)
    }

    private fun currentDay() = LocalDate.now().toEpochDay()
}

private fun Snapshot.toUi(day: Long): UiState {
    val active = objectives.filter { it.archivedDay == null }
    fun row(objective: Objective) = ObjectiveRow(
        objective,
        isDone(objective.id, day),
        Streaks.forObjective(objective.id, records),
    )
    val scheduled = active.filter { it.isScheduledOn(day) }
    val main = mainStreak(day)
    val complete = isComplete(day)
    val titles = objectives.associate { it.id to it.title }

    val outcomes = records.filter { it.day < day }
        .groupBy { it.day }
        .mapValues { (_, list) -> Streaks.dayOutcome(list.map { it.status }) }
        .let { if (complete) it + (day to DayStatus.DONE) else it }

    val (mood, speech) = moodFor(scheduled.size, complete, main, records.isNotEmpty(), scheduled.count { isDone(it.id, day) })

    return UiState(
        today = day,
        todayRows = scheduled.map(::row).sortedBy { it.objective.period.ordinal },
        otherRows = (active - scheduled.toSet()).map(::row),
        mainStreak = main,
        complete = complete,
        level = Progression.levelFor(xp),
        xp = xp,
        coins = coins,
        freezes = freezes,
        stats = stats(day),
        unlocked = unlocked.map { it.id }.toSet(),
        owned = owned,
        themeId = settings.themeId,
        accessoryId = settings.accessoryId,
        dayOutcomes = outcomes,
        records = records.groupBy { it.day }
            .mapValues { (_, list) -> list.map { (titles[it.objectiveId] ?: "Objetivo") to it.status } },
        mood = mood,
        speech = speech,
    )
}

private fun pendentes(count: Int) = if (count == 1) "Falta 1" else "Faltam $count"

private fun moodFor(
    scheduled: Int,
    complete: Boolean,
    main: Streak,
    hasHistory: Boolean,
    doneCount: Int,
): Pair<Mood, String> {
    val evening = LocalTime.now().hour >= 18
    return when {
        scheduled == 0 -> Mood.SLEEPY to "Nada para hoje. Aproveite o descanso!"
        complete -> Mood.EXCITED to "Dia perfeito! Sou a Faísca mais feliz do mundo."
        evening && main.current > 0 -> Mood.WORRIED to "Seu streak de ${dias(main.current)} está em risco!"
        doneCount > 0 -> Mood.HAPPY to "Boa! ${pendentes(scheduled - doneCount)} para fechar o dia."
        hasHistory && main.current == 0 && main.best > 0 -> Mood.SAD to "Tudo bem, recomeçar também é ritmo."
        else -> Mood.HAPPY to if (scheduled == 1) "Vamos lá? Hoje é só 1 objetivo." else "Vamos lá? Hoje são $scheduled objetivos."
    }
}

