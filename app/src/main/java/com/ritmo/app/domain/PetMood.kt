package com.ritmo.app.domain

import java.time.LocalDateTime
import java.time.LocalTime

enum class Mood { HAPPY, EXCITED, WORRIED, SAD, SLEEPY }

/** Como o pet está agora e o que ele diz sobre isso. */
data class PetFeeling(val mood: Mood, val speech: String)

object PetMood {
    /** A partir desta hora, um dia incompleto já põe o streak em risco. */
    const val EVENING_HOUR = 18

    fun of(
        scheduled: Int,
        done: Int,
        complete: Boolean,
        main: Streak,
        hasHistory: Boolean,
        canRepair: Boolean,
        hour: Int,
    ): PetFeeling = when {
        scheduled == 0 -> PetFeeling(Mood.SLEEPY, "Nada para hoje. Aproveite o descanso!")
        complete -> PetFeeling(Mood.EXCITED, "Dia perfeito! Você fechou tudo hoje.")
        hour >= EVENING_HOUR && main.current > 0 ->
            PetFeeling(Mood.WORRIED, "Seu streak de ${dias(main.current)} está em risco!")
        done > 0 -> PetFeeling(Mood.HAPPY, "Boa! ${pendentes(scheduled - done)} para fechar o dia.")
        hasHistory && main.current == 0 && main.best > 0 -> PetFeeling(
            Mood.SAD,
            if (canRepair) "Seu streak quebrou, mas ainda dá para retomar na loja." else "Tudo bem, recomeçar também é ritmo.",
        )
        else -> PetFeeling(
            Mood.HAPPY,
            if (scheduled == 1) "Vamos lá? Hoje é só 1 objetivo." else "Vamos lá? Hoje são $scheduled objetivos.",
        )
    }

    /** O humor muda sozinho em dois momentos do dia: à noite e na virada para o dia seguinte. */
    fun nextChange(now: LocalDateTime): LocalDateTime {
        val evening = now.toLocalDate().atTime(LocalTime.of(EVENING_HOUR, 0))
        return if (now.isBefore(evening)) evening else now.toLocalDate().plusDays(1).atStartOfDay()
    }

    private fun pendentes(count: Int) = if (count == 1) "Falta 1" else "Faltam $count"
}

/** Um ícone de app para cada estado do pet; os atalhos vêm de tools/generate_pet_icons.py. */
object PetIcons {
    /** A Faísca muda de corpo a cada fase; os outros pets só crescem, então um ícone basta. */
    fun alias(pet: String?, stage: Int, mood: Mood): String {
        val name = when (pet) {
            Pets.RAIO -> "Raio"
            Pets.LUZ -> "Luz"
            Pets.MAROMBA -> "Maromba"
            else -> "Faisca$stage"
        }
        return name + mood.name.lowercase().replaceFirstChar { it.uppercase() }
    }
}
