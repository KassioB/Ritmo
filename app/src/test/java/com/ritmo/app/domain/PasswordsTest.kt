package com.ritmo.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordsTest {
    /** Bem abaixo do valor real: aqui só importa a lógica, não a lentidão. */
    private val fast = 1_000

    @Test fun theRightPasswordMatches() {
        assertTrue(Passwords.matches("minha senha", Passwords.hash("minha senha", fast)))
    }

    @Test fun aWrongPasswordDoesNot() {
        val stored = Passwords.hash("minha senha", fast)
        assertFalse(Passwords.matches("Minha senha", stored))
        assertFalse(Passwords.matches("", stored))
    }

    @Test fun thePasswordIsNotStored() {
        assertFalse(Passwords.hash("minha-senha-secreta", fast).contains("minha-senha-secreta"))
    }

    @Test fun twoPeopleWithTheSamePasswordGetDifferentHashes() {
        assertNotEquals(Passwords.hash("igual", fast), Passwords.hash("igual", fast))
    }

    @Test fun anOldHashKeepsWorkingAfterTheIterationsChange() {
        val old = Passwords.hash("minha senha", fast)
        val current = Passwords.hash("minha senha")

        assertTrue(Passwords.matches("minha senha", old))
        assertTrue(Passwords.matches("minha senha", current))
    }

    @Test fun acceptsAccentsAndSymbols() {
        assertTrue(Passwords.matches("çãé!@#😀", Passwords.hash("çãé!@#😀", fast)))
    }
}
