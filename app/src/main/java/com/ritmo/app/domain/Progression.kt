package com.ritmo.app.domain

enum class Difficulty(val label: String, val xp: Int, val coins: Int) {
    EASY("Fácil", 10, 1),
    MEDIUM("Médio", 20, 2),
    HARD("Difícil", 30, 3),
}

enum class Period(val label: String) {
    MORNING("Manhã"),
    AFTERNOON("Tarde"),
    EVENING("Noite"),
    ANYTIME("Qualquer hora"),
}

/** "1 dia", "2 dias". */
fun dias(count: Int) = if (count == 1) "1 dia" else "$count dias"

data class LevelInfo(val level: Int, val xpIntoLevel: Int, val xpForNextLevel: Int) {
    val progress: Float get() = xpIntoLevel.toFloat() / xpForNextLevel
}

object Progression {
    const val PERFECT_DAY_XP = 25
    const val PERFECT_DAY_COINS = 5
    /** Fechar uma lista vale o mesmo que um dia perfeito. */
    const val LIST_COMPLETE_XP = PERFECT_DAY_XP
    const val LIST_COMPLETE_COINS = PERFECT_DAY_COINS
    const val MIN_ITEMS_FOR_LIST_BONUS = 2
    const val STARTING_FREEZES = 1
    const val MAX_FREEZES = 2

    /** XP total para alcançar o nível: 100, 300, 600, 1000... */
    fun xpToReach(level: Int): Int = 50 * level * (level - 1)

    fun levelFor(totalXp: Int): LevelInfo {
        val xp = totalXp.coerceAtLeast(0)
        var level = 1
        while (xp >= xpToReach(level + 1)) level++
        val base = xpToReach(level)
        return LevelInfo(level, xp - base, xpToReach(level + 1) - base)
    }
}
