package com.ritmo.app.domain

/** Como um objetivo terminou em um dia já registrado. */
enum class DayStatus { DONE, MISSED, FROZEN }

/** Fato imutável do histórico: o que aconteceu com um objetivo em um dia. */
data class DayRecord(val objectiveId: Long, val day: Long, val status: DayStatus)

data class Streak(val current: Int, val best: Int)

/** O que retomar a sequência devolve: [days] viram dias protegidos e o streak volta a [streak]. */
data class StreakRepair(val days: List<Long>, val streak: Int)

/**
 * Regras de streak. Dias são `LocalDate.toEpochDay()`.
 *
 * - DONE soma, MISSED zera, FROZEN mantém sem somar.
 * - Dias sem registro (objetivo não programado) são neutros.
 * - Hoje só conta quando concluído: enquanto pendente, não quebra nada.
 */
object Streaks {
    /** Até quantos dias atrás uma quebra ainda pode ser desfeita na loja. */
    const val REPAIR_WINDOW_DAYS = 3

    fun forObjective(objectiveId: Long, records: List<DayRecord>): Streak =
        walk(records.filter { it.objectiveId == objectiveId }.sortedBy { it.day }.map { it.status })

    /**
     * Streak principal: um dia soma quando todos os objetivos programados foram concluídos.
     * [todayComplete] indica que hoje tem objetivos e todos estão feitos; os registros de hoje
     * são ignorados aqui para que um dia parcial não seja tratado como perfeito.
     */
    fun main(records: List<DayRecord>, today: Long, todayComplete: Boolean): Streak {
        val closedDays = records.filter { it.day < today }
            .groupBy { it.day }
            .toSortedMap()
            .values
            .map { day -> dayOutcome(day.map { it.status }) }
        return walk(if (todayComplete) closedDays + DayStatus.DONE else closedDays)
    }

    /**
     * A quebra mais recente do streak principal, se ainda der para desfazê-la. Retomar vale como
     * um bloqueio usado depois: a falha vira dia protegido, que mantém o streak sem somar. Só
     * serve para um tropeço recente; uma ausência maior que a janela fica como está.
     */
    fun repairable(records: List<DayRecord>, today: Long, todayComplete: Boolean): StreakRepair? {
        val closed = records.filter { it.day < today }
            .groupBy { it.day }
            .toSortedMap()
            .map { (day, list) -> day to dayOutcome(list.map { it.status }) }
        val lastBreak = closed.indexOfLast { (_, outcome) -> outcome == DayStatus.MISSED }
        if (lastBreak < 0) return null
        val gap = closed.take(lastBreak + 1)
            .takeLastWhile { (_, outcome) -> outcome != DayStatus.DONE }
            .filter { (_, outcome) -> outcome == DayStatus.MISSED }
            .map { (day, _) -> day }
        if (gap.first() < today - REPAIR_WINDOW_DAYS) return null

        val repaired = records.map {
            if (it.day in gap && it.status == DayStatus.MISSED) it.copy(status = DayStatus.FROZEN) else it
        }
        val streak = main(repaired, today, todayComplete).current
        return StreakRepair(gap, streak).takeIf { streak > main(records, today, todayComplete).current }
    }

    /** Resultado do dia inteiro: uma falha pesa mais que um bloqueio, que pesa mais que concluir. */
    fun dayOutcome(statuses: List<DayStatus>): DayStatus = when {
        DayStatus.MISSED in statuses -> DayStatus.MISSED
        DayStatus.FROZEN in statuses -> DayStatus.FROZEN
        else -> DayStatus.DONE
    }

    private fun walk(statuses: List<DayStatus>): Streak {
        var current = 0
        var best = 0
        for (status in statuses) {
            when (status) {
                DayStatus.DONE -> {
                    current++
                    best = maxOf(best, current)
                }
                DayStatus.MISSED -> current = 0
                DayStatus.FROZEN -> Unit
            }
        }
        return Streak(current, best)
    }
}
