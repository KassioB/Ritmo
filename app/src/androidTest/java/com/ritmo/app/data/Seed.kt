package com.ritmo.app.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ritmo.app.domain.Difficulty
import com.ritmo.app.domain.Period

const val TODAY = 20762L

fun memoryDatabase(): RitmoDatabase = Room.inMemoryDatabaseBuilder(
    InstrumentationRegistry.getInstrumentation().targetContext,
    RitmoDatabase::class.java,
).allowMainThreadQueries().build()

/** Um pouco de tudo o que a pessoa acumula: objetivo cumprido, recompensas, compra, preferência e lista. */
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
    updateSettings { it.copy(petId = "pet_luz", themeId = "theme_gold", isPro = true) }
}

/** O progresso sem a conta, para comparar dois bancos. */
fun Snapshot.progress() = listOf(objectives, records, ledger, unlocked, settings, lists, items)
