package com.ritmo.app.domain

/** As quatro caixas da matriz de Eisenhower, na ordem em que aparecem na tela. */
enum class Quadrant(val title: String, val detail: String) {
    DO("Fazer agora", "Urgente e importante"),
    PLAN("Agendar", "Importante, não urgente"),
    DELEGATE("Delegar", "Urgente, não importante"),
    DROP("Eliminar", "Nem urgente nem importante"),
}

/** As telas que podem ir para o rodapé. */
enum class Screen(val label: String, val detail: String) {
    TODAY("Hoje", "Seus hábitos do dia"),
    LISTS("Listas", "Compras, estudos, treinos"),
    FOCUS("Foco", "Pomodoro e cronômetro"),
    MATRIX("Matriz", "Matriz de Eisenhower"),
    DASHBOARD("Dashboard", "Seu histórico e análises"),
    PROFILE("Conquistas", "Nível e medalhas"),
    SHOP("Loja", "Temas, pets e bloqueios"),
}

/**
 * Quais telas ficam no rodapé e em que ordem; a primeira é a que abre com o app. Guardado como texto
 * ("TODAY,LISTS,...") nas preferências. Vazio ou inválido vale o padrão.
 */
object HomeTabs {
    const val MIN = 2
    const val MAX = 5
    val DEFAULT = listOf(Screen.TODAY, Screen.LISTS, Screen.FOCUS, Screen.MATRIX, Screen.DASHBOARD)

    fun parse(value: String): List<Screen> {
        val screens = value.split(',')
            .mapNotNull { name -> Screen.entries.firstOrNull { it.name == name.trim() } }
            .distinct()
        return if (screens.size in MIN..MAX) screens else DEFAULT
    }

    fun format(screens: List<Screen>): String = screens.joinToString(",") { it.name }

    /** Liga ou desliga uma tela; nulo se passaria do limite, para a tela explicar o porquê. */
    fun toggle(screens: List<Screen>, screen: Screen): List<Screen>? = when {
        screen in screens -> (screens - screen).takeIf { it.size >= MIN }
        screens.size >= MAX -> null
        else -> screens + screen
    }

    /** Sobe ([delta] = -1) ou desce (+1) uma tela na ordem do rodapé. */
    fun move(screens: List<Screen>, screen: Screen, delta: Int): List<Screen> {
        val from = screens.indexOf(screen)
        val to = from + delta
        if (from < 0 || to !in screens.indices) return screens
        return screens.toMutableList().apply { add(to, removeAt(from)) }
    }
}
