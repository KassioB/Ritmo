package com.ritmo.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Histórico em CSV. Ponto e vírgula e BOM são o que o Excel em português abre sem configurar nada. */
object Export {
    private const val SEPARATOR = ";"
    private const val BOM = "\uFEFF"
    private const val LINE = "\r\n"
    private val date = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun csv(records: List<DayRecord>, titles: Map<Long, String>): String {
        val rows = records.sortedWith(compareBy({ it.day }, { it.objectiveId })).map { record ->
            listOf(
                LocalDate.ofEpochDay(record.day).format(date),
                titles[record.objectiveId] ?: "Objetivo",
                label(record.status),
            )
        }
        val table = listOf(listOf("Data", "Objetivo", "Situação")) + rows
        return BOM + table.joinToString(LINE, postfix = LINE) { row -> row.joinToString(SEPARATOR, transform = ::field) }
    }

    private fun label(status: DayStatus) = when (status) {
        DayStatus.DONE -> "Concluído"
        DayStatus.MISSED -> "Não concluído"
        DayStatus.FROZEN -> "Protegido"
    }

    private fun field(value: String): String {
        val special = SEPARATOR in value || '"' in value || '\n' in value || '\r' in value
        return if (special) "\"" + value.replace("\"", "\"\"") + "\"" else value
    }
}
