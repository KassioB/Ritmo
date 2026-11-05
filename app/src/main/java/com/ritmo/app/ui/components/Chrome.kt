package com.ritmo.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritmo.app.domain.DayStatus
import com.ritmo.app.ui.theme.Ritmo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ChunkShape = RoundedCornerShape(16.dp)
private val LipDepth = 4.dp
private val BannerLip = 5.dp
val PtBr: Locale = Locale.forLanguageTag("pt-BR")

/** Botão em relevo: a borda inferior dá volume e a face afunda sobre ela ao pressionar. */
@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = Ritmo.brand.action,
    lip: Color = Ritmo.brand.actionLip,
    contentColor: Color = Ritmo.brand.onAction,
    minHeight: Dp = 52.dp,
    horizontalPadding: Dp = 20.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val face = if (enabled) color else scheme.surfaceContainerHigh
    val edge = if (enabled) lip else scheme.outline
    val ink = if (enabled) contentColor else scheme.onSurfaceVariant
    ChunkyBase(onClick, enabled, edge, ChunkShape, LipDepth, modifier) { pressedOffset ->
        Row(
            Modifier
                .padding(bottom = LipDepth)
                .offset(y = pressedOffset)
                .background(face, ChunkShape)
                .defaultMinSize(minHeight = minHeight)
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text.uppercase(PtBr), color = ink, style = MaterialTheme.typography.titleMedium, letterSpacing = 0.6.sp)
        }
    }
}

/** Botão redondo de ação, como o "+" flutuante. */
@Composable
fun ChunkyFab(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Add,
    size: Dp = 60.dp,
    enabled: Boolean = true,
) {
    val brand = Ritmo.brand
    val scheme = MaterialTheme.colorScheme
    val face = if (enabled) brand.action else scheme.surfaceContainerHigh
    val edge = if (enabled) brand.actionLip else scheme.outline
    val ink = if (enabled) brand.onAction else scheme.onSurfaceVariant
    ChunkyBase(onClick, enabled, edge, CircleShape, LipDepth, modifier) { pressedOffset ->
        Box(
            Modifier
                .padding(bottom = LipDepth)
                .offset(y = pressedOffset)
                .size(size)
                .background(face, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = description, tint = ink, modifier = Modifier.size(size * 0.55f))
        }
    }
}

@Composable
private fun ChunkyBase(
    onClick: () -> Unit,
    enabled: Boolean,
    edge: Color,
    shape: Shape,
    depth: Dp,
    modifier: Modifier,
    face: @Composable (Dp) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier.clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        ),
        propagateMinConstraints = true,
    ) {
        Box(Modifier.matchParentSize().padding(top = depth).background(edge, shape))
        face(if (pressed) depth else 0.dp)
    }
}

/** Barra de progresso grossa, com o brilho claro no topo. */
@Composable
fun ChunkyProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Ritmo.brand.done,
    track: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    height: Dp = 14.dp,
) {
    val value by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progresso")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(track, CircleShape)
            .progressSemantics(value),
    ) {
        if (value > 0f) {
            Box(Modifier.fillMaxWidth(value).fillMaxHeight().background(color, CircleShape)) {
                Box(
                    Modifier
                        .padding(start = height / 2, end = height / 2, top = height / 4)
                        .fillMaxWidth()
                        .height(height / 5)
                        .background(Color.White.copy(alpha = 0.5f), CircleShape),
                )
            }
        }
    }
}

/** Faixa que abre cada tela: uma linha pequena em caixa alta e um título. */
@Composable
fun SectionBanner(
    overline: String,
    title: String,
    modifier: Modifier = Modifier,
    side: (@Composable () -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val brand = Ritmo.brand
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().padding(top = BannerLip).background(brand.bannerLip, shape))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = BannerLip)
                .height(IntrinsicSize.Min)
                .clip(shape)
                .background(brand.banner),
        ) {
            Column(Modifier.weight(1f).padding(16.dp)) {
                Text(
                    overline.uppercase(PtBr),
                    color = brand.onBanner,
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(title, color = brand.onBanner, style = MaterialTheme.typography.titleLarge)
                footer?.invoke(this)
            }
            if (side != null) {
                Box(
                    Modifier.width(104.dp).fillMaxHeight().background(brand.bannerSide),
                    contentAlignment = Alignment.Center,
                ) { side() }
            }
        }
    }
}

/** Cartão que agrupa linhas sem separá-las, como as seções do TickTick. */
@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        if (title != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (trailing != null) {
                    Text(
                        trailing,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        content()
        Spacer(Modifier.height(6.dp))
    }
}

/** Opção marcável com a borda colorida quando escolhida. */
@Composable
fun ChoiceTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainer)
            .border(2.dp, if (selected) accent else MaterialTheme.colorScheme.outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** Ícone de um objetivo ou lista, sobre um círculo na cor escolhida. */
@Composable
fun IconBadge(emoji: String, color: Color, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    Box(
        modifier.size(size).background(color.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size.value * 0.5f).sp)
    }
}

/** Marcação animada de concluído; o formato muda entre rotina (círculo) e lista (quadrado). */
@Composable
fun CheckToggle(
    done: Boolean,
    label: String,
    color: Color,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val brand = Ritmo.brand
    val haptics = LocalHapticFeedback.current
    val pop by animateFloatAsState(
        if (done) 1.12f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pop",
    )
    val fill by animateColorAsState(if (done) brand.done else Color.Transparent, label = "preenchimento")
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics {
                role = Role.Checkbox
                stateDescription = if (done) "$label concluído" else "$label pendente"
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .scale(pop)
                .background(fill, shape)
                .border(2.5.dp, if (done) brand.done else color, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = brand.onDone)
        }
    }
}

/** Cabeçalho das abas: os números do jogador em linha, cada um na sua cor. */
@Composable
fun RitmoHeader(
    streak: Int,
    freezes: Int,
    coins: Int,
    level: Int,
    onStreak: () -> Unit,
    onShop: () -> Unit,
    onLevel: () -> Unit,
) {
    val brand = Ritmo.brand
    Row(
        Modifier
            .fillMaxWidth()
            .background(brand.header)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderStat(Icons.Rounded.LocalFireDepartment, brand.streak, "$streak", "Streak: $streak", onStreak)
        HeaderStat(Icons.Rounded.AcUnit, brand.freeze, "$freezes", "Bloqueios: $freezes", onShop)
        HeaderStat(Icons.Rounded.MonetizationOn, brand.coin, "$coins", "Moedas: $coins", onShop)
        HeaderStat(Icons.Rounded.Bolt, brand.level, "$level", "Nível $level", onLevel)
    }
}

@Composable
private fun RowScope.HeaderStat(
    icon: ImageVector,
    tint: Color,
    value: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(5.dp))
        Text(value, color = tint, style = MaterialTheme.typography.titleLarge, maxLines = 1)
    }
}

data class NavItem(val label: String, val icon: ImageVector, val tint: Color)

/** Rodapé só com ícones; a aba atual ganha uma moldura arredondada. */
@Composable
fun RitmoBottomBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().background(scheme.surface)) {
        HorizontalDivider(thickness = 2.dp, color = scheme.outline)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEachIndexed { index, item ->
                val chosen = index == selected
                val shape = RoundedCornerShape(14.dp)
                Box(
                    Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(shape)
                        .then(
                            if (chosen) {
                                Modifier.background(item.tint.copy(alpha = 0.16f)).border(2.dp, item.tint, shape)
                            } else {
                                Modifier
                            },
                        )
                        .selectable(selected = chosen, role = Role.Tab, onClick = { onSelect(index) })
                        .semantics { contentDescription = item.label },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = item.tint,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}

/** Barra superior das telas internas, com a mesma cor do cabeçalho. */
@Composable
fun PageTopBar(
    title: String,
    navIcon: ImageVector,
    navDescription: String,
    onNav: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val brand = Ritmo.brand
    Row(
        Modifier
            .fillMaxWidth()
            .background(brand.header)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNav) { Icon(navIcon, navDescription, tint = brand.onHeader) }
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            title,
            color = brand.onHeader,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

private val weekdayFormat = DateTimeFormatter.ofPattern("EEE", PtBr)
private val weekdayName = DateTimeFormatter.ofPattern("EEEE", PtBr)

/** Os últimos sete dias, terminando em hoje. */
@Composable
fun WeekStrip(today: Long, outcomes: Map<Long, DayStatus>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        (6 downTo 0).forEach { daysAgo ->
            val day = today - daysAgo
            WeekDay(LocalDate.ofEpochDay(day), outcomes[day], day == today, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WeekDay(date: LocalDate, status: DayStatus?, isToday: Boolean, modifier: Modifier) {
    val brand = Ritmo.brand
    val scheme = MaterialTheme.colorScheme
    val (fill, ink) = when {
        status == DayStatus.DONE -> brand.done to brand.onDone
        isToday -> scheme.primary to scheme.onPrimary
        else -> Color.Transparent to scheme.onSurface
    }
    val ring = when (status) {
        DayStatus.MISSED -> brand.missed
        DayStatus.FROZEN -> brand.freeze
        else -> null
    }
    val meaning = when (status) {
        DayStatus.DONE -> "dia perfeito"
        DayStatus.MISSED -> "dia incompleto"
        DayStatus.FROZEN -> "protegido por bloqueio"
        null -> if (isToday) "hoje" else "sem registro"
    }
    Column(
        modifier.semantics(mergeDescendants = true) {
            contentDescription = "${date.format(weekdayName)}, dia ${date.dayOfMonth}, $meaning"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            date.format(weekdayFormat),
            color = scheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .size(38.dp)
                .background(fill, CircleShape)
                .then(if (ring != null) Modifier.border(2.dp, ring, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text("${date.dayOfMonth}", color = ink, style = MaterialTheme.typography.titleMedium)
        }
    }
}
