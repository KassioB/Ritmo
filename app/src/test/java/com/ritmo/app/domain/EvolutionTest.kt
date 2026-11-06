package com.ritmo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvolutionTest {
    @Test fun everyStreakHasExactlyOneStage() {
        assertEquals("Brasa", Evolution.stageFor(0).name)
        assertEquals("Fagulha", Evolution.stageFor(1).name)
        assertEquals("Fagulha", Evolution.stageFor(2).name)
        assertEquals("Chama", Evolution.stageFor(3).name)
        assertEquals("Labareda", Evolution.stageFor(7).name)
        assertEquals("Labareda", Evolution.stageFor(29).name)
        assertEquals("Fogo azul", Evolution.stageFor(30).name)
        assertEquals("Supernova", Evolution.stageFor(100).name)
        assertEquals("Supernova", Evolution.stageFor(5000).name)
    }

    @Test fun stagesAreOrderedAndIndexedInSequence() {
        Evolution.stages.forEachIndexed { index, stage -> assertEquals(index, stage.index) }
        assertEquals(Evolution.stages, Evolution.stages.sortedBy { it.minStreak })
    }

    @Test fun nextStageIsTheFollowingThreshold() {
        assertEquals("Chama", Evolution.next(1)?.name)
        assertEquals("Fogo azul", Evolution.next(7)?.name)
        assertNull(Evolution.next(100))
    }

    @Test fun progressGoesFromZeroToOneInsideAStage() {
        assertEquals(0f, Evolution.progress(7), 0.001f)
        assertEquals(0.5f, Evolution.progress(5), 0.001f)
        assertEquals(1f, Evolution.progress(250), 0.001f)
        assertTrue(Evolution.progress(29) < 1f)
    }
}
