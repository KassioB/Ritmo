package com.ritmo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ritmo.app.domain.DayStatus
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.theme.Ice
import com.ritmo.app.ui.theme.Missed
import com.ritmo.app.ui.theme.Success
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormat = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))
private val dayFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("pt-BR"))
private val weekLabels = listOf("D", "S", "T", "Q", "Q", "S", "S")

private fun statusColor(status: DayStatus?) = when (status) {
    DayStatus.DONE -> Success
    DayStatus.MISSED -> Missed
    DayStatus.FROZEN -> Ice
    null -> Color.Transparent
}

private fun statusLabel(status: DayStatus?) = when (status) {
    DayStatus.DONE -> "dia perfeito"
    DayStatus.MISSED -> "dia incompleto"
    DayStatus.FROZEN -> "protegido por bloqueio"
    null -> "sem registro"
}

@Composable
fun CalendarScreen(state: UiState) {
    var month by remember { mutableStateOf(YearMonth.from(LocalDate.ofEpochDay(state.today))) }
    var selected by remember { mutableStateOf(state.today) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Calendário", style = MaterialTheme.typography.headlineMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mês anterior")
            }
            Text(
                month.format(monthFormat).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Próximo mês")
            }
        }

        Row(Modifier.fillMaxWidth()) {
            weekLabels.forEach {
                Text(
                    it,
                    Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        val leading = month.atDay(1).dayOfWeek.value % 7
        val cells: List<Int?> = List(leading) { null } + (1..month.lengthOfMonth()).toList()
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { number ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        if (number != null) {
                            val day = month.atDay(number).toEpochDay()
                            DayCell(number, state.dayOutcomes[day], day == state.today, day == selected) {
                                selected = day
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Legend(Success, "Perfeito")
            Legend(Missed, "Incompleto")
            Legend(Ice, "Bloqueio")
        }

        val entries = state.records[selected].orEmpty()
        Text(
            LocalDate.ofEpochDay(selected).format(dayFormat),
            style = MaterialTheme.typography.titleMedium,
        )
        if (entries.isEmpty()) {
            Text("Nenhum registro neste dia.")
        } else {
            entries.forEach { (title, status) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(statusColor(status), CircleShape))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        title + when (status) {
                            DayStatus.DONE -> " · concluído"
                            DayStatus.MISSED -> " · perdido"
                            DayStatus.FROZEN -> " · protegido"
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DayCell(number: Int, status: DayStatus?, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val color = statusColor(status)
    val ring = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .fillMaxSize()
            .background(color.copy(alpha = if (status == null) 0f else 0.85f), CircleShape)
            .border(if (isToday || isSelected) 2.dp else 0.dp, if (isSelected) ring else ring.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Dia $number, ${statusLabel(status)}" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$number",
            color = if (status == null) MaterialTheme.colorScheme.onSurface else Color.White,
            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Text(" $label", style = MaterialTheme.typography.bodySmall)
    }
}
