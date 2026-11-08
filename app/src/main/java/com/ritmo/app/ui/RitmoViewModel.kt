package com.ritmo.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ritmo.app.RitmoApp
import com.ritmo.app.data.Account
import com.ritmo.app.data.AuthFailure
import com.ritmo.app.data.Checklist
import com.ritmo.app.data.ChecklistItem
import com.ritmo.app.data.FocusSession
import com.ritmo.app.data.MatrixTask
import com.ritmo.app.data.Objective
import com.ritmo.app.data.Settings
import com.ritmo.app.data.Snapshot
import com.ritmo.app.data.ToggleOutcome
import com.ritmo.app.domain.Achievement
import com.ritmo.app.domain.DayRecord
import com.ritmo.app.domain.DayStatus
import com.ritmo.app.domain.DaySummary
import com.ritmo.app.domain.Difficulty
import com.ritmo.app.domain.Evolution
import com.ritmo.app.domain.EvolutionStage
import com.ritmo.app.domain.Export
import com.ritmo.app.domain.Focus
import com.ritmo.app.domain.FocusKind
import com.ritmo.app.domain.FocusMode
import com.ritmo.app.domain.FocusPhase
import com.ritmo.app.domain.FocusTarget
import com.ritmo.app.domain.Insights
import com.ritmo.app.domain.LevelInfo
import com.ritmo.app.domain.ObjectiveInfo
import com.ritmo.app.domain.Mood
import com.ritmo.app.domain.Pets
import com.ritmo.app.domain.Plan
import com.ritmo.app.domain.Progression
import com.ritmo.app.domain.Quadrant
import com.ritmo.app.domain.Screen
import com.ritmo.app.domain.ShopItem
import com.ritmo.app.domain.ShopKind
import com.ritmo.app.domain.Stats
import com.ritmo.app.domain.Streak
import com.ritmo.app.domain.StreakRepair
import com.ritmo.app.domain.Streaks
import com.ritmo.app.domain.dias
import com.ritmo.app.notifications.Reminders
import com.ritmo.app.ui.components.Celebration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

data class ObjectiveRow(val objective: Objective, val done: Boolean, val streak: Streak)

data class ListRow(val list: Checklist, val pending: List<ChecklistItem>, val done: List<ChecklistItem>) {
    val total get() = pending.size + done.size
    val progress get() = if (total == 0) 0f else done.size.toFloat() / total
}

/** O histórico bruto que o Dashboard recorta por período. */
data class History(
    val days: Map<Long, DaySummary>,
    val records: List<DayRecord>,
    val objectives: Map<Long, ObjectiveInfo>,
)

data class UiState(
    val today: Long,
    val todayRows: List<ObjectiveRow>,
    val otherRows: List<ObjectiveRow>,
    val lists: List<ListRow>,
    val mainStreak: Streak,
    val complete: Boolean,
    val level: LevelInfo,
    val xp: Int,
    val coins: Int,
    val freezes: Int,
    val stats: Stats,
    val unlocked: Set<String>,
    val owned: Set<String>,
    val settings: Settings,
    val stage: EvolutionStage,
    val history: History,
    val dayOutcomes: Map<Long, DayStatus>,
    val records: Map<Long, List<Pair<String, DayStatus>>>,
    val mood: Mood,
    val speech: String,
    val streakRepair: StreakRepair?,
    val account: Account?,
    /** Pendentes e as concluídas hoje; as de outros dias saem da matriz. */
    val tasks: List<MatrixTask>,
    val focusToday: List<FocusSession>,
) {
    val signedIn get() = account?.signedIn == true
    val tabs: List<Screen> get() = settings.tabs

    /** Se o foco escolhido ainda está por fazer hoje, para oferecer marcá-lo ao fim da sessão. */
    fun isPending(target: FocusTarget): Boolean = when (target.kind) {
        FocusKind.HABIT -> todayRows.any { it.objective.id == target.id && !it.done }
        FocusKind.ITEM -> lists.any { row -> row.pending.any { it.id == target.id } }
        FocusKind.TASK -> tasks.any { it.id == target.id && it.doneDay == null }
    }
    val themeId get() = settings.themeId
    val petId get() = settings.petId
    val plan get() = Plan.of(settings.isPro)
    val activeObjectives get() = todayRows.size + otherRows.size
}

sealed interface AppEvent {
    data class Reward(val text: String) : AppEvent
    data class Celebrate(val celebration: Celebration) : AppEvent
}

class RitmoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RitmoApp).repository
    private val accounts = (application as RitmoApp).accounts
    private val timer = (application as RitmoApp).focus
    private val today = MutableStateFlow(currentDay())
    private val eventChannel = Channel<AppEvent>(Channel.BUFFERED)

    val events = eventChannel.receiveAsFlow()

    val focus = timer.clock

    val state = combine(repository.snapshot, today) { snapshot, day -> snapshot.toUi(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            repository.ensureSetup(today.value)
            refreshDay()
            launch {
                timer.finished.collect { outcome ->
                    if (outcome.finished.phase == FocusPhase.BREAK) {
                        eventChannel.send(AppEvent.Reward("A pausa acabou"))
                    } else {
                        outcome.reward?.let { report(it, "${Focus.minutes((outcome.finished.millis / 1000).toInt())} de foco") }
                    }
                }
            }
            while (true) {
                delay(60_000)
                closeDays()
            }
        }
    }

    /** Chamado ao abrir o app e ao voltar para ele. */
    fun refreshDay() {
        viewModelScope.launch {
            closeDays()
            syncReminders()
            timer.settle()
        }
    }

    /** Roda também a cada minuto, para fechar o dia mesmo com o app aberto na virada. */
    private suspend fun closeDays() {
        val day = currentDay()
        today.value = day
        // Os avisos de bloqueio usado e de conquista esperam a pessoa entrar para aparecer.
        if (repository.load()?.signedIn != true) return
        // Antes de fechar os dias, para o presente do mês já poder salvar um deles.
        val granted = repository.grantMonthlyFreezes(day)
        if (granted > 0) eventChannel.send(AppEvent.Reward("Pro: ${bloqueios(granted)} de presente"))
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
    }

    fun toggle(objectiveId: Long) {
        viewModelScope.launch { report(repository.toggle(objectiveId, today.value)) }
    }

    fun toggleItem(itemId: Long) {
        viewModelScope.launch { report(repository.toggleItem(itemId, today.value)) }
    }

    private suspend fun report(outcome: ToggleOutcome, label: String? = null) {
        if (outcome.xp > 0) {
            val coins = when {
                outcome.coins == 1 -> " · +1 moeda"
                outcome.coins > 1 -> " · +${outcome.coins} moedas"
                else -> ""
            }
            eventChannel.send(AppEvent.Reward("+${outcome.xp} XP$coins${label?.let { " · $it" }.orEmpty()}"))
        } else if (label != null) {
            eventChannel.send(AppEvent.Reward(label))
        }
        val snapshot = repository.load()
        if (outcome.perfectDay) {
            val streak = snapshot?.mainStreak(today.value)?.current ?: 0
            val coins = snapshot?.plan?.dailyCoins ?: Progression.PERFECT_DAY_COINS
            eventChannel.send(
                AppEvent.Celebrate(
                    Celebration(
                        "Dia perfeito!",
                        "${dias(streak)} de streak. Bônus de ${Progression.PERFECT_DAY_XP} XP e $coins moedas.",
                    ),
                ),
            )
        }
        outcome.finishedList?.let { title ->
            eventChannel.send(
                AppEvent.Celebrate(
                    Celebration("Lista completa!", "Você fechou \"$title\". Bônus de ${Progression.LIST_COMPLETE_XP} XP."),
                ),
            )
        }
        outcome.evolvedTo?.let { stage ->
            val pet = Pets.byId(repository.load()?.settings?.petId)
            val article = pet.article.replaceFirstChar { it.uppercase() }
            eventChannel.send(
                AppEvent.Celebrate(
                    Celebration(
                        "Evoluiu: ${stage.name}!",
                        "${dias(stage.minStreak)} seguidos no ritmo. $article ${pet.name} está mais forte.",
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

    fun save(objective: Objective) {
        viewModelScope.launch {
            repository.save(objective, today.value)
            syncReminders(edited = true)
        }
    }

    fun archive(objective: Objective) {
        viewModelScope.launch {
            repository.archive(objective, today.value)
            syncReminders(edited = true)
        }
    }

    fun saveList(list: Checklist, onSaved: (Long) -> Unit) {
        viewModelScope.launch { onSaved(repository.saveList(list)) }
    }

    fun deleteList(listId: Long) {
        viewModelScope.launch { repository.deleteList(listId) }
    }

    fun addItem(listId: Long, title: String, difficulty: Difficulty) {
        viewModelScope.launch { repository.addItem(listId, title, difficulty) }
    }

    fun updateItem(item: ChecklistItem) {
        viewModelScope.launch { repository.updateItem(item) }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch { repository.deleteItem(itemId) }
    }

    fun uncheckAll(listId: Long) {
        viewModelScope.launch { repository.uncheckAll(listId, today.value) }
    }

    fun addTask(title: String, quadrant: Quadrant) {
        viewModelScope.launch { repository.addTask(title, quadrant, today.value) }
    }

    fun updateTask(task: MatrixTask) {
        viewModelScope.launch { repository.updateTask(task) }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch { repository.deleteTask(taskId) }
    }

    fun toggleTask(taskId: Long) {
        viewModelScope.launch { report(repository.toggleTask(taskId, today.value)) }
    }

    fun startFocus() {
        viewModelScope.launch { timer.start() }
    }

    fun pauseFocus() {
        viewModelScope.launch { timer.pause() }
    }

    fun stopFocus() {
        viewModelScope.launch { timer.stop() }
    }

    fun finishStopwatch() {
        viewModelScope.launch { timer.finishStopwatch() }
    }

    fun setFocusMode(mode: FocusMode) {
        viewModelScope.launch { timer.setMode(mode) }
    }

    fun setFocusTarget(target: FocusTarget?) {
        viewModelScope.launch { timer.setTarget(target) }
    }

    /** Marca como feito aquilo em que a pessoa focou, com a recompensa de sempre. */
    fun completeTarget(target: FocusTarget) {
        when (target.kind) {
            FocusKind.HABIT -> toggle(target.id)
            FocusKind.ITEM -> toggleItem(target.id)
            FocusKind.TASK -> toggleTask(target.id)
        }
    }

    fun buy(item: ShopItem) {
        viewModelScope.launch {
            val achievements = repository.buy(item, today.value) ?: return@launch
            if (item.kind == ShopKind.REPAIR) {
                val streak = repository.load()?.mainStreak(today.value)?.current ?: 0
                eventChannel.send(
                    AppEvent.Celebrate(Celebration("Sequência retomada!", "Seu streak voltou para ${dias(streak)}.")),
                )
            } else {
                eventChannel.send(AppEvent.Reward("Comprado: ${item.name}"))
            }
            achievements.forEach { announce(it) }
        }
    }

    fun toggleEquipped(item: ShopItem) {
        viewModelScope.launch { repository.toggleEquipped(item) }
    }

    fun activatePro() {
        viewModelScope.launch {
            val granted = repository.setPro(true, today.value)
            val gift = if (granted > 0) " e ${bloqueios(granted)} de presente" else ""
            eventChannel.send(
                AppEvent.Celebrate(Celebration("Bem-vindo ao Pro!", "Limites liberados, moedas em dobro$gift.")),
            )
        }
    }

    fun signUp(name: String, email: String, password: String, onFailure: (AuthFailure) -> Unit) {
        viewModelScope.launch { finishAuth(accounts.signUp(name, email, password), onFailure) }
    }

    fun signIn(email: String, password: String, onFailure: (AuthFailure) -> Unit) {
        viewModelScope.launch { finishAuth(accounts.signIn(email, password), onFailure) }
    }

    /** [onResult] recebe nulo quando o e-mail de recuperação foi pedido. */
    fun sendPasswordReset(email: String, onResult: (AuthFailure?) -> Unit) {
        viewModelScope.launch { onResult(accounts.sendPasswordReset(email)) }
    }

    /** Entrar recomeça os lembretes de agora: os horários que passaram com a conta fora não contam. */
    private suspend fun finishAuth(failure: AuthFailure?, onFailure: (AuthFailure) -> Unit) {
        if (failure != null) {
            onFailure(failure)
            return
        }
        closeDays()
        syncReminders(edited = true)
    }

    /** Só sai com o progresso salvo na conta; se faltar internet, [onFailure] explica e nada muda. */
    fun signOut(onFailure: (AuthFailure) -> Unit) {
        viewModelScope.launch {
            val failure = accounts.signOut(currentDay())
            if (failure != null) {
                onFailure(failure)
            } else {
                timer.clear()
                syncReminders(edited = true)
            }
        }
    }

    fun updateName(name: String, onResult: (AuthFailure?) -> Unit) {
        viewModelScope.launch { onResult(accounts.updateName(name)) }
    }

    fun changePassword(current: String, new: String, onResult: (AuthFailure?) -> Unit) {
        viewModelScope.launch { onResult(accounts.changePassword(current, new)) }
    }

    fun deleteAccount(password: String, onResult: (AuthFailure?) -> Unit) {
        viewModelScope.launch {
            val failure = accounts.deleteAccount(password, currentDay())
            if (failure == null) {
                timer.clear()
                syncReminders(edited = true)
            }
            onResult(failure)
        }
    }

    fun exportHistory(target: Uri) {
        viewModelScope.launch {
            val state = repository.load() ?: return@launch
            val csv = Export.csv(state.records, state.objectives.associate { it.id to it.title })
            val saved = withContext(Dispatchers.IO) { write(target, csv) }
            eventChannel.send(AppEvent.Reward(if (saved) "Planilha salva" else "Não deu para salvar a planilha"))
        }
    }

    private fun write(target: Uri, text: String): Boolean = try {
        val stream = getApplication<RitmoApp>().contentResolver.openOutputStream(target)
        stream?.use { it.write(text.toByteArray()) } != null
    } catch (_: IOException) {
        false
    }

    fun updateSettings(change: (Settings) -> Settings) {
        viewModelScope.launch {
            repository.updateSettings(change)
            syncReminders(edited = true)
        }
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

    private suspend fun syncReminders(edited: Boolean = false) {
        val state = repository.load() ?: return
        Reminders.sync(getApplication(), state, edited)
    }

    private fun currentDay() = LocalDate.now().toEpochDay()

    private fun bloqueios(count: Int) = if (count == 1) "1 bloqueio" else "$count bloqueios"
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

    val feeling = feeling(day, LocalTime.now().hour)

    return UiState(
        today = day,
        todayRows = scheduled.map(::row).sortedBy { it.objective.period.ordinal },
        otherRows = (active - scheduled.toSet()).map(::row),
        lists = lists.map { list ->
            val (done, pending) = items.filter { it.listId == list.id }.partition { it.doneDay != null }
            ListRow(list, pending, done)
        },
        mainStreak = main,
        complete = complete,
        level = Progression.levelFor(xp),
        xp = xp,
        coins = coins,
        freezes = freezes,
        stats = stats(day),
        unlocked = unlocked.map { it.id }.toSet(),
        owned = owned,
        settings = settings,
        stage = Evolution.stageFor(main.current),
        history = History(
            days = Insights.days(
                records,
                ledger.filter { it.key.startsWith(Snapshot.ITEM) }.map { it.day },
                ledger.groupBy { it.day }.mapValues { (_, entries) -> entries.sumOf { it.xp } },
                day,
                scheduled.count { !isDone(it.id, day) },
                coinsByDay = ledger.filter { it.coins > 0 }
                    .groupBy { it.day }
                    .mapValues { (_, entries) -> entries.sumOf { it.coins } },
                listDays = ledger.filter { it.key.startsWith(Snapshot.LIST_DONE) }.map { it.day },
            ),
            records = records,
            objectives = objectives.associate { it.id to ObjectiveInfo(it.title, it.period, it.difficulty) },
        ),
        dayOutcomes = outcomes,
        records = records.groupBy { it.day }
            .mapValues { (_, list) -> list.map { (titles[it.objectiveId] ?: "Objetivo") to it.status } },
        mood = feeling.mood,
        speech = feeling.speech,
        streakRepair = streakRepair(day),
        account = account,
        tasks = tasks.filter { it.doneDay == null || it.doneDay == day },
        focusToday = sessions.filter { it.day == day },
    )
}

