package com.ritmo.app.domain

/** Informa quais objetivos estavam programados em um dia. */
fun interface Schedule {
    fun objectivesOn(day: Long): List<Long>
}

data class ClosingResult(
    val newRecords: List<DayRecord>,
    val frozenDays: List<Long>,
)

/**
 * Fecha os dias passados ainda não registrados, em ordem. Um dia com pendências consome
 * um bloqueio se houver algum streak a proteger; caso contrário, vira falha.
 * Um bloqueio protege o dia inteiro: streak principal e todos os objetivos.
 */
object DayCloser {
    fun close(
        fromDay: Long,
        toDay: Long,
        schedule: Schedule,
        existing: List<DayRecord>,
        freezesAvailable: Int,
    ): ClosingResult {
        val all = existing.toMutableList()
        val created = mutableListOf<DayRecord>()
        val frozen = mutableListOf<Long>()
        var freezes = freezesAvailable

        for (day in fromDay..toDay) {
            val done = all.filter { it.day == day && it.status == DayStatus.DONE }
                .map { it.objectiveId }.toSet()
            val pending = schedule.objectivesOn(day).filterNot { it in done }
            if (pending.isEmpty()) continue

            val atRisk = Streaks.main(all, today = day, todayComplete = false).current > 0 ||
                pending.any { Streaks.forObjective(it, all).current > 0 }
            val status = if (atRisk && freezes > 0) {
                freezes--
                frozen += day
                DayStatus.FROZEN
            } else {
                DayStatus.MISSED
            }
            val records = pending.map { DayRecord(it, day, status) }
            all += records
            created += records
        }
        return ClosingResult(created, frozen)
    }
}
