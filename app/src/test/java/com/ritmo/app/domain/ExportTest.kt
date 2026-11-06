package com.ritmo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExportTest {
    private val first = LocalDate.of(2026, 11, 4).toEpochDay()
    private val second = first + 1

    private fun lines(records: List<DayRecord>, titles: Map<Long, String>) =
        Export.csv(records, titles).removePrefix("\uFEFF").trimEnd().split("\r\n")

    @Test fun startsWithTheMarkThatKeepsAccentsInExcel() {
        assertTrue(Export.csv(emptyList(), emptyMap()).startsWith("\uFEFFData;Objetivo;Situação\r\n"))
    }

    @Test fun listsRecordsByDayWithReadableStatus() {
        val records = listOf(
            DayRecord(1, second, DayStatus.MISSED),
            DayRecord(2, first, DayStatus.FROZEN),
            DayRecord(1, first, DayStatus.DONE),
        )
        assertEquals(
            listOf(
                "Data;Objetivo;Situação",
                "04/11/2026;Ler;Concluído",
                "04/11/2026;Treinar;Protegido",
                "05/11/2026;Ler;Não concluído",
            ),
            lines(records, mapOf(1L to "Ler", 2L to "Treinar")),
        )
    }

    @Test fun protectsTitlesThatWouldBreakTheColumns() {
        val titles = mapOf(1L to "Ler; escrever", 2L to "Treino \"pesado\"")
        val records = listOf(DayRecord(1, first, DayStatus.DONE), DayRecord(2, first, DayStatus.DONE))
        assertEquals(
            listOf("04/11/2026;\"Ler; escrever\";Concluído", "04/11/2026;\"Treino \"\"pesado\"\"\";Concluído"),
            lines(records, titles).drop(1),
        )
    }

    @Test fun keepsARecordWhoseObjectiveIsGone() {
        val line = lines(listOf(DayRecord(9, first, DayStatus.DONE)), emptyMap()).last()
        assertEquals("04/11/2026;Objetivo;Concluído", line)
    }
}
