package com.ritmo.app.domain

import com.ritmo.app.data.Objective
import java.time.LocalDate
import java.time.LocalDateTime

/** Um lembrete vencido: qual objetivo e em que momento ele devia tocar. */
data class DueReminder(val objective: Objective, val at: LocalDateTime)

/** "08:05". */
fun clock(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

/** Inclui [minutes] em ordem, trocando [replacing] e ignorando horários repetidos. */
fun List<Int>.withReminder(minutes: Int, replacing: Int? = null): List<Int> =
    (filterNot { it == replacing } + minutes).distinct().sorted()

/**
 * Decide quando os lembretes tocam. O app mantém um único alarme, sempre no próximo horário
 * pendente, e ao disparar trata tudo que venceu desde a última verificação. Por isso não há
 * limite de lembretes (o Android recusa acima de 500 alarmes por app), remover um horário não
 * deixa alarme órfão e um disparo atrasado pelo sistema não perde nenhum horário.
 */
object ReminderPlan {
    /** Horário do aviso de streak em risco. */
    const val EVENING_MINUTES = 20 * 60
    private const val DAYS_AHEAD = 7L

    private fun LocalDate.at(minutes: Int) = atTime(minutes / 60, minutes % 60)

    /** Lembretes programados no intervalo (from, to], em ordem. */
    fun between(from: LocalDateTime, to: LocalDateTime, objectives: List<Objective>): List<DueReminder> {
        val days = generateSequence(from.toLocalDate()) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to.toLocalDate()) }
            .toList()
        return days.flatMap { date ->
            objectives.filter { it.isScheduledOn(date.toEpochDay()) }.flatMap { objective ->
                objective.reminders.map { DueReminder(objective, date.at(it)) }
            }
        }.filter { it.at.isAfter(from) && !it.at.isAfter(to) }.sortedBy { it.at }
    }

    fun eveningBetween(from: LocalDateTime, to: LocalDateTime): Boolean {
        val next = nextEvening(from)
        return !next.isAfter(to)
    }

    /** O primeiro momento depois de [after] em que algo precisa ser verificado. */
    fun next(after: LocalDateTime, objectives: List<Objective>): LocalDateTime {
        val reminder = (0..DAYS_AHEAD)
            .map { after.toLocalDate().plusDays(it) }
            .flatMap { date ->
                objectives.filter { it.isScheduledOn(date.toEpochDay()) }
                    .flatMap { objective -> objective.reminders.map { date.at(it) } }
            }
            .filter { it.isAfter(after) }
            .minOrNull()
        val evening = nextEvening(after)
        return if (reminder != null && reminder.isBefore(evening)) reminder else evening
    }

    private fun nextEvening(after: LocalDateTime): LocalDateTime {
        val today = after.toLocalDate().at(EVENING_MINUTES)
        return if (today.isAfter(after)) today else today.plusDays(1)
    }
}
