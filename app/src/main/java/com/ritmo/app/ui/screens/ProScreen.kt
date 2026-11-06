package com.ritmo.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ritmo.app.domain.Mood
import com.ritmo.app.domain.Plan
import com.ritmo.app.domain.dias
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.ChunkyButton
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.components.Mascot
import com.ritmo.app.ui.components.PageTopBar
import com.ritmo.app.ui.components.SectionBanner

/** O que levou o usuário ao Pro, mostrado no topo da tela. Vazio quando ele veio por conta própria. */
object ProReason {
    const val NONE = ""
    const val RANGES = "O calendário e as análises de mês e ano fazem parte do Pro."
    const val THEME = "Este tema é exclusivo do Pro."
    const val EXPORT = "Exportar o histórico faz parte do Pro."

    fun objectives(plan: Plan) = "O plano grátis tem até ${plan.maxObjectives} objetivos ativos."

    fun reminders(plan: Plan) = "O plano grátis tem até ${plan.maxRemindersPerObjective} lembretes por objetivo."

    fun lists(plan: Plan) = "O plano grátis tem até ${plan.maxLists} listas."

    fun items(plan: Plan) = "O plano grátis tem até ${plan.maxItemsPerList} itens por lista."
}

private data class Comparison(val label: String, val free: String, val pro: String)

private fun limit(value: Int?) = value?.toString() ?: "Sem limite"

private fun comparisons(): List<Comparison> {
    val free = Plan.FREE
    val pro = Plan.PRO
    return listOf(
        Comparison("Objetivos ativos", limit(free.maxObjectives), limit(pro.maxObjectives)),
        Comparison(
            "Lembretes por objetivo",
            limit(free.maxRemindersPerObjective),
            limit(pro.maxRemindersPerObjective),
        ),
        Comparison("Listas", limit(free.maxLists), limit(pro.maxLists)),
        Comparison("Itens por lista", limit(free.maxItemsPerList), limit(pro.maxItemsPerList)),
        Comparison("Dashboard", "Semana", "Semana, mês e ano"),
        Comparison("Calendário do streak", "Semana", "Mês e ano"),
        Comparison("Dicas e análises", "Não", "Sim"),
        Comparison("Bloqueios guardados", "${free.maxFreezes}", "${pro.maxFreezes}"),
        Comparison("Bloqueios de presente", "Não", "${pro.monthlyFreezes} por mês"),
        Comparison("Prazo para retomar streak", dias(free.repairWindowDays), dias(pro.repairWindowDays)),
        Comparison("Preço para retomar", "${free.repairPrice} moedas", "${pro.repairPrice} moedas"),
        Comparison("Moedas por dia de streak", "${free.dailyCoins}", "${pro.dailyCoins}"),
        Comparison("Temas exclusivos", "Não", "Ouro e Esmeralda"),
        Comparison("Exportar histórico", "Não", "Planilha CSV"),
    )
}

/** Sem cobrança por enquanto: ativar é só ligar o plano, até a compra pela loja de apps existir. */
@Composable
fun ProScreen(
    state: UiState,
    reason: String,
    onActivate: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val pro = state.plan.isPro
    val scheme = MaterialTheme.colorScheme

    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        Column(Modifier.fillMaxSize()) {
            PageTopBar("Ritmo Pro", Icons.Rounded.Close, "Fechar", onClose)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionBanner(
                    overline = if (pro) "Seu plano" else "Plano Pro",
                    title = if (pro) "Você é Pro! Aproveite tudo sem limites." else "Mais proteção para o streak e mais clareza sobre o seu ritmo",
                    side = { Mascot(state.petId, state.stage.index, Mood.EXCITED, sizeDp = 84.dp) },
                )
                if (reason.isNotEmpty() && !pro) LimitNotice(reason)
                ComparisonCard()
                if (!pro) {
                    ChunkyButton("Ativar o Pro", onActivate, Modifier.fillMaxWidth())
                    Note("Versão de testes: ativar é grátis e não há cobrança.")
                }
            }
        }
    }
}

@Composable
private fun LimitNotice(reason: String) {
    val scheme = MaterialTheme.colorScheme
    GroupCard {
        Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(reason, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun ComparisonCard() {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    GroupCard {
        Column(
            Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.clearAndSetSemantics {}) {
                Spacer(Modifier.weight(LABEL_WEIGHT))
                Text("Grátis", Modifier.weight(1f), style = typography.labelLarge, textAlign = TextAlign.Center)
                Text(
                    "Pro",
                    Modifier.weight(1f),
                    color = scheme.primary,
                    style = typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
            comparisons().forEach { line ->
                Row(
                    Modifier.fillMaxWidth().clearAndSetSemantics {
                        contentDescription = "${line.label}: grátis, ${line.free}; Pro, ${line.pro}"
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(line.label, Modifier.weight(LABEL_WEIGHT), style = typography.bodySmall)
                    Text(
                        line.free,
                        Modifier.weight(1f),
                        color = scheme.onSurfaceVariant,
                        style = typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        line.pro,
                        Modifier.weight(1f),
                        style = typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private const val LABEL_WEIGHT = 1.35f

@Composable
private fun Note(text: String) {
    Text(
        text,
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
}

/** Convite discreto para o Pro, sempre com um motivo ligado ao que está na tela. */
@Composable
fun ProPromo(title: String, detail: String, onClick: () -> Unit) =
    ActionCard(Icons.Rounded.WorkspacePremium, title, detail, "Ver", onClick)

/** Cartão de uma linha com ícone, texto e um botão. */
@Composable
fun ActionCard(icon: ImageVector, title: String, detail: String, action: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    GroupCard {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(detail, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(10.dp))
            ChunkyButton(action, onClick, minHeight = 40.dp, horizontalPadding = 14.dp)
        }
    }
}
