package com.ritmo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialsTest {
    @Test fun theSameEmailTypedDifferentlyIsTheSameEmail() {
        assertEquals("ana@gmail.com", Credentials.normalize("  Ana@Gmail.COM "))
    }

    @Test fun acceptsCommonEmails() {
        listOf("ana@gmail.com", "ana.souza+ritmo@empresa.com.br", " Ana@Gmail.com ").forEach {
            assertNull(it, Credentials.emailProblem(it))
        }
    }

    @Test fun refusesWhatCannotBeAnEmail() {
        listOf("", "ana", "ana@", "@gmail.com", "ana@gmail", "ana souza@gmail.com", "ana@@gmail.com").forEach {
            assertNotNull(it, Credentials.emailProblem(it))
        }
    }

    @Test fun aNameCannotBeBlank() {
        assertNotNull(Credentials.nameProblem("   "))
        assertNull(Credentials.nameProblem("Ana"))
    }

    @Test fun aNewPasswordNeedsTheMinimumLength() {
        assertNotNull(Credentials.newPasswordProblem("a".repeat(Credentials.MIN_PASSWORD - 1)))
        assertNull(Credentials.newPasswordProblem("a".repeat(Credentials.MIN_PASSWORD)))
    }

    @Test fun maskingKeepsOnlyWhatIsNeededToRecognizeTheEmail() {
        assertEquals("a••@gmail.com", Credentials.masked("ana@gmail.com"))
        assertEquals("a@gmail.com", Credentials.masked("a@gmail.com"))
    }
}
