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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ritmo.app.domain.DayStatus
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.components.PtBr
import com.ritmo.app.ui.components.SectionBanner
import com.ritmo.app.ui.theme.BrandColors
import com.ritmo.app.ui.theme.Ritmo
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val monthFormat = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PtBr)
private val dayFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", PtBr)
private val sundayFirst = listOf("D", "S", "T", "Q", "Q", "S", "S")
private val mondayFirst = listOf("S", "T", "Q", "Q", "S", "S", "D")

private fun statusColor(brand: BrandColors, status: DayStatus?) = when (status) {
    DayStatus.DONE -> brand.done
    DayStatus.MISSED -> brand.missed
    DayStatus.FROZEN -> brand.freeze
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
    val brand = Ritmo.brand
    val perfectDays = state.dayOutcomes.values.count { it == DayStatus.DONE }
    val weekStartsMonday = state.settings.weekStartsMonday
    val weekLabels = if (weekStartsMonday) mondayFirst else sundayFirst

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionBanner(
            overline = "Calendário",
            title = when (perfectDays) {
                0 -> "Seu primeiro dia perfeito está perto"
                1 -> "1 dia perfeito até agora"
                else -> "$perfectDays dias perfeitos até agora"
            },
        )

        GroupCard {
            Row(
                Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { month = month.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Mês anterior")
                }
                Text(
                    month.format(monthFormat).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { month = month.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Próximo mês")
                }
            }

            Column(Modifier.padding(horizontal = 10.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    weekLabels.forEach {
                        Text(
                            it,
                            Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                val weekday = month.atDay(1).dayOfWeek.value
                val leading = if (weekStartsMonday) weekday - 1 else weekday % 7
                val cells: List<Int?> = List(leading) { null } + (1..month.lengthOfMonth()).toList()
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { number ->
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).padding(3.dp),
                                contentAlignment = Alignment.Center,
                            ) {
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

                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Legend(brand.done, "Perfeito")
                    Legend(brand.missed, "Incompleto")
                    Legend(brand.freeze, "Bloqueio")
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        val entries = state.records[selected].orEmpty()
        GroupCard(title = LocalDate.ofEpochDay(selected).format(dayFormat)) {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (entries.isEmpty()) {
                    Text("Nenhum registro neste dia.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    entries.forEach { (title, status) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(statusColor(brand, status), CircleShape))
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
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DayCell(number: Int, status: DayStatus?, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val brand = Ritmo.brand
    val ring = MaterialTheme.colorScheme.primary
    val ink = when (status) {
        DayStatus.DONE, DayStatus.FROZEN -> brand.onDone
        DayStatus.MISSED -> Color.White
        null -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(statusColor(brand, status), CircleShape)
            .then(
                if (isToday || isSelected) {
                    Modifier.border(2.5.dp, if (isSelected) ring else ring.copy(alpha = 0.5f), CircleShape)
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Dia $number, ${statusLabel(status)}" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$number",
            color = ink,
            fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold,
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
