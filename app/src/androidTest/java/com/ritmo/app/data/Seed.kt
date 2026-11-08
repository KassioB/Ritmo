package com.ritmo.app.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ritmo.app.domain.Difficulty
import com.ritmo.app.domain.FinishedPhase
import com.ritmo.app.domain.FocusMode
import com.ritmo.app.domain.FocusPhase
import com.ritmo.app.domain.HomeTabs
import com.ritmo.app.domain.Period
import com.ritmo.app.domain.Quadrant
import com.ritmo.app.domain.Screen

const val TODAY = 20762L

fun memoryDatabase(): RitmoDatabase = Room.inMemoryDatabaseBuilder(
    InstrumentationRegistry.getInstrumentation().targetContext,
    RitmoDatabase::class.java,
).allowMainThreadQueries().build()

/** Um pouco de tudo o que a pessoa acumula: objetivo cumprido, recompensas, compra, preferências, lista, matriz e foco. */
suspend fun RitmoRepository.seedProgress() {
    save(
        Objective(
            title = "Ler",
            emoji = "📚",
            colorIndex = 2,
            daysMask = Objective.EVERY_DAY,
            difficulty = Difficulty.HARD,
            period = Period.MORNING,
            reminders = listOf(420, 1260),
            createdDay = TODAY,
        ),
        TODAY,
    )
    toggle(load()!!.objectives.last().id, TODAY)
    val list = saveList(Checklist(title = "Compras", emoji = "🛒", colorIndex = 1, createdDay = TODAY))
    addItem(list, "Café", Difficulty.EASY)
    addItem(list, "Pão", Difficulty.MEDIUM)
    updateSettings {
        it.copy(
            petId = "pet_luz",
            themeId = "theme_gold",
            isPro = true,
            homeTabs = HomeTabs.format(listOf(Screen.FOCUS, Screen.MATRIX, Screen.TODAY)),
            focusMinutes = 50,
        )
    }
    addTask("Pagar a luz", Quadrant.DO, TODAY)
    addTask("Planejar a semana", Quadrant.PLAN, TODAY)
    toggleTask(load()!!.tasks.first().id, TODAY)
    recordFocus(FinishedPhase(FocusMode.POMODORO, FocusPhase.FOCUS, FOCUS_START, 25 * 60_000L), "📚 Ler", TODAY)
}

/** 14h de [TODAY] no fuso do aparelho, para a sessão cair no dia certo. */
val FOCUS_START: Long = java.time.LocalDate.ofEpochDay(TODAY).atTime(14, 0)
    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

/** O progresso sem a conta, para comparar dois bancos. */
fun Snapshot.progress() = listOf(objectives, records, ledger, unlocked, settings, lists, items, tasks, sessions)
