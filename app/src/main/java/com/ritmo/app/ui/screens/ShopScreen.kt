package com.ritmo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ritmo.app.domain.Mood
import com.ritmo.app.domain.Progression
import com.ritmo.app.domain.Shop
import com.ritmo.app.domain.ShopItem
import com.ritmo.app.domain.ShopKind
import com.ritmo.app.domain.dias
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.ChunkyButton
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.components.Mascot
import com.ritmo.app.ui.components.SectionBanner
import com.ritmo.app.ui.theme.Ritmo
import com.ritmo.app.ui.theme.ThemeSwatches

/** Fase usada nas vitrines da loja, para mostrar o pet já crescido. */
private const val SHOWCASE_STAGE = 3

/** O que protege o streak vem primeiro: é o que o usuário procura quando está com pressa. */
private val ShopGroups = listOf(
    "Streak" to setOf(ShopKind.FREEZE, ShopKind.REPAIR),
    "Pets" to setOf(ShopKind.PET),
    "Temas" to setOf(ShopKind.THEME),
)

@Composable
fun ShopScreen(state: UiState, onBuy: (ShopItem) -> Unit, onToggleEquip: (ShopItem) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionBanner(
                overline = "Loja",
                title = if (state.coins == 1) "Você tem 1 moeda" else "Você tem ${state.coins} moedas",
            )
        }
        ShopGroups.forEach { (title, kinds) ->
            item(key = title) {
                GroupCard(title = title) {
                    Shop.items.filter { it.kind in kinds }.forEach { item ->
                        ShopLine(item, state, onBuy, onToggleEquip)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopLine(
    item: ShopItem,
    state: UiState,
    onBuy: (ShopItem) -> Unit,
    onToggleEquip: (ShopItem) -> Unit,
) {
    val brand = Ritmo.brand
    val scheme = MaterialTheme.colorScheme
    val owned = item.id in state.owned
    val equipped = item.id == state.themeId || item.id == state.petId
    val repair = state.streakRepair
    val unavailable = when (item.kind) {
        ShopKind.FREEZE -> state.freezes >= Progression.MAX_FREEZES
        ShopKind.REPAIR -> repair == null
        ShopKind.THEME, ShopKind.PET -> false
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            when (item.kind) {
                ShopKind.FREEZE -> Icon(Icons.Rounded.AcUnit, null, tint = brand.freeze, modifier = Modifier.size(34.dp))
                ShopKind.REPAIR -> Icon(Icons.Rounded.Restore, null, tint = brand.streak, modifier = Modifier.size(34.dp))
                ShopKind.THEME -> Swatches(item.id)
                ShopKind.PET -> Mascot(item.id, SHOWCASE_STAGE, Mood.HAPPY, sizeDp = 64.dp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text(item.description, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            val status = when (item.kind) {
                ShopKind.FREEZE -> "Você tem ${state.freezes} de ${Progression.MAX_FREEZES}"
                ShopKind.REPAIR -> repair?.let { "Seu streak volta para ${dias(it.streak)}" } ?: "Nenhum streak para retomar agora"
                ShopKind.THEME, ShopKind.PET -> null
            }
            if (status != null) {
                Text(status, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            owned -> ChunkyButton(
                if (equipped) "Remover" else "Usar",
                { onToggleEquip(item) },
                color = scheme.surfaceContainerHigh,
                lip = scheme.outline,
                contentColor = scheme.onSurface,
                minHeight = 40.dp,
                horizontalPadding = 14.dp,
            )
            else -> ChunkyButton(
                "${item.price}",
                { onBuy(item) },
                enabled = state.coins >= item.price && !unavailable,
                icon = Icons.Rounded.MonetizationOn,
                minHeight = 40.dp,
                horizontalPadding = 12.dp,
            )
        }
    }
}

@Composable
fun Swatches(themeId: String) {
    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
        ThemeSwatches[themeId].orEmpty().forEach { color ->
            Box(
                Modifier
                    .size(24.dp)
                    .background(color, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
    }
}
