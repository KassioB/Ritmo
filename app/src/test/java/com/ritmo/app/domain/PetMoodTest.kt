package com.ritmo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

class PetMoodTest {
    private fun mood(
        scheduled: Int = 2,
        done: Int = 0,
        complete: Boolean = false,
        main: Streak = Streak(0, 0),
        hasHistory: Boolean = false,
        canRepair: Boolean = false,
        hour: Int = 10,
    ) = PetMood.of(scheduled, done, complete, main, hasHistory, canRepair, hour)

    @Test fun nothingScheduledMeansRest() {
        assertEquals(Mood.SLEEPY, mood(scheduled = 0).mood)
    }

    @Test fun aCompleteDayIsACelebrationEvenAtNight() {
        assertEquals(Mood.EXCITED, mood(done = 2, complete = true, main = Streak(5, 5), hour = 22).mood)
    }

    @Test fun theStreakOnlyWorriesThePetFromTheEveningOn() {
        val main = Streak(5, 5)
        assertEquals(Mood.HAPPY, mood(main = main, hour = PetMood.EVENING_HOUR - 1).mood)
        assertEquals(Mood.WORRIED, mood(main = main, hour = PetMood.EVENING_HOUR).mood)
    }

    @Test fun losingTheStreakMakesThePetSad() {
        assertEquals(Mood.SAD, mood(main = Streak(0, 5), hasHistory = true).mood)
    }

    @Test fun aStartedDayCheersThePetUp() {
        assertEquals(Mood.HAPPY, mood(done = 1, main = Streak(0, 5), hasHistory = true).mood)
    }

    @Test fun thePetPointsToTheShopOnlyWhenTheStreakCanBeRepaired() {
        val broken = Streak(0, 5)
        assertTrue("loja" in mood(main = broken, hasHistory = true, canRepair = true).speech)
        assertTrue("loja" !in mood(main = broken, hasHistory = true, canRepair = false).speech)
    }

    @Test fun moodChangesAtSixPmAndAtMidnight() {
        val day = LocalDateTime.of(2026, 11, 5, 9, 0)
        assertEquals(day.withHour(18), PetMood.nextChange(day))
        assertEquals(day.withHour(18), PetMood.nextChange(day.withHour(17).withMinute(59)))
        assertEquals(LocalDateTime.of(2026, 11, 6, 0, 0), PetMood.nextChange(day.withHour(18)))
        assertEquals(LocalDateTime.of(2026, 11, 6, 0, 0), PetMood.nextChange(day.withHour(23).withMinute(59)))
    }

    @Test fun everyPetChangesIconWithTheStage() {
        assertEquals("Faisca3Worried", PetIcons.alias(null, 3, Mood.WORRIED))
        assertEquals("Maromba0Sad", PetIcons.alias(Pets.MAROMBA, 0, Mood.SAD))
        assertNotEquals(PetIcons.alias(Pets.RAIO, 0, Mood.HAPPY), PetIcons.alias(Pets.RAIO, 5, Mood.HAPPY))
    }

    @Test fun everyPetStateHasAnIconAndOnlyOneStartsEnabled() {
        val manifest = File("src/main/pet-icons/AndroidManifest.xml").readText()
        val declared = Regex("android:name=\"com\\.ritmo\\.app\\.icon\\.(\\w+)\"")
            .findAll(manifest).map { it.groupValues[1] }.toSet()
        val expected = Pets.all.flatMap { pet ->
            Evolution.stages.flatMap { stage -> Mood.entries.map { PetIcons.alias(pet.id, stage.index, it) } }
        }.toSet()
        assertEquals(expected, declared)
        assertEquals(1, Regex("android:enabled=\"true\"").findAll(manifest).count())
    }

    /** Sem isso, desligar o ícone antigo destrói a tela aberta e tira o app dos recentes. */
    @Test fun theLauncherEntryHandsTheTaskOverToTheApp() {
        val manifest = File("src/main/pet-icons/AndroidManifest.xml").readText()
        assertTrue(Regex("LauncherActivity\"[^>]*android:relinquishTaskIdentity=\"true\"").containsMatchIn(manifest))
    }
}
