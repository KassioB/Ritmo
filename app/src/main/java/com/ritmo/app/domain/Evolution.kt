package com.ritmo.app.domain

/** Fase da mascote. Sobe com o streak principal e recomeça se ele quebrar. */
data class EvolutionStage(val index: Int, val name: String, val minStreak: Int)

object Evolution {
    val stages = listOf(
        EvolutionStage(0, "Brasa", 0),
        EvolutionStage(1, "Fagulha", 1),
        EvolutionStage(2, "Chama", 3),
        EvolutionStage(3, "Labareda", 7),
        EvolutionStage(4, "Fogo azul", 30),
        EvolutionStage(5, "Supernova", 100),
    )

    fun stageFor(streak: Int): EvolutionStage = stages.last { streak >= it.minStreak }

    fun next(streak: Int): EvolutionStage? = stages.firstOrNull { it.minStreak > streak }

    /** Quanto já foi percorrido entre a fase atual e a próxima; 1 na fase final. */
    fun progress(streak: Int): Float {
        val next = next(streak) ?: return 1f
        val current = stageFor(streak)
        return (streak - current.minStreak).toFloat() / (next.minStreak - current.minStreak)
    }
}
