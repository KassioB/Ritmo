package com.ritmo.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ritmo.app.domain.Difficulty
import com.ritmo.app.domain.Period
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountTest {
    private val today = 20762L
    private val email = "ana@gmail.com"
    private val password = "senha-forte"
    private lateinit var database: RitmoDatabase
    private lateinit var repository: RitmoRepository
    private lateinit var accounts: AccountRepository

    @Before fun open() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            RitmoDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RitmoRepository(database)
        accounts = AccountRepository(database)
        runBlocking { repository.ensureSetup(today) }
    }

    @After fun close() = database.close()

    private suspend fun state() = repository.load()!!

    private suspend fun signUp() = accounts.signUp("Ana", email, password)

    private suspend fun progress(): Long {
        repository.save(
            Objective(
                title = "Ler",
                emoji = "📚",
                colorIndex = 0,
                daysMask = Objective.EVERY_DAY,
                difficulty = Difficulty.EASY,
                period = Period.ANYTIME,
                createdDay = today,
            ),
            today,
        )
        val id = state().objectives.last().id
        repository.toggle(id, today)
        return id
    }

    @Test fun nobodyHasAnAccountAtFirst() = runBlocking {
        assertNull(state().account)
        assertFalse(state().signedIn)
        // Sem conta ainda não é o mesmo que ter saído: os lembretes de quem atualizou continuam.
        assertFalse(state().signedOut)
    }

    @Test fun signingUpEntersTheAccount() = runBlocking {
        assertNull(signUp())

        val account = state().account!!
        assertEquals("Ana", account.name)
        assertEquals(email, account.email)
        assertTrue(state().signedIn)
    }

    @Test fun whatIsSavedIsAHashNotThePassword() = runBlocking {
        signUp()

        val stored = state().account!!.password!!
        assertNotEquals(password, stored)
        assertFalse(stored.contains(password))
    }

    @Test fun nameAndEmailAreCleanedUp() = runBlocking {
        accounts.signUp("  Ana Souza ", "  Ana@Gmail.COM ", password)

        assertEquals("Ana Souza", state().account!!.name)
        assertEquals(email, state().account!!.email)
    }

    @Test fun creatingAnAccountKeepsThePreviousProgress() = runBlocking {
        progress()
        val before = state()

        signUp()

        val after = state()
        assertEquals(before.objectives, after.objectives)
        assertEquals(before.records, after.records)
        assertEquals(before.ledger, after.ledger)
    }

    @Test fun aSecondAccountIsRefusedAndChangesNothing() = runBlocking {
        signUp()
        val account = state().account

        assertEquals(AuthFailure.ALREADY_HAS_ACCOUNT, accounts.signUp("Bia", "bia@gmail.com", password))
        assertEquals(account, state().account)
    }

    @Test fun signingOutKeepsTheDataAndTheAccount() = runBlocking {
        signUp()
        progress()
        val before = state()

        accounts.signOut()

        val after = state()
        assertFalse(after.signedIn)
        assertTrue(after.signedOut)
        assertEquals(email, after.account!!.email)
        assertEquals(before.objectives, after.objectives)
        assertEquals(before.records, after.records)
        assertEquals(before.coins, after.coins)
    }

    @Test fun theRightPasswordEntersAgain() = runBlocking {
        signUp()
        accounts.signOut()

        assertNull(accounts.signIn(email, password))
        assertTrue(state().signedIn)
    }

    @Test fun theEmailCanBeTypedDifferentlyAtSignIn() = runBlocking {
        signUp()
        accounts.signOut()

        assertNull(accounts.signIn(" ANA@gmail.com ", password))
        assertTrue(state().signedIn)
    }

    @Test fun aWrongPasswordOrEmailDoesNotEnter() = runBlocking {
        signUp()
        accounts.signOut()

        assertEquals(AuthFailure.WRONG_CREDENTIALS, accounts.signIn(email, "senha-errada"))
        assertEquals(AuthFailure.WRONG_CREDENTIALS, accounts.signIn("bia@gmail.com", password))
        assertFalse(state().signedIn)
    }

    @Test fun withoutAnAccountThereIsNothingToEnter() = runBlocking {
        assertEquals(AuthFailure.NO_ACCOUNT, accounts.signIn(email, password))
    }

    @Test fun aNewPasswordReplacesTheOldOneAndKeepsTheData() = runBlocking {
        signUp()
        progress()
        accounts.signOut()
        val coins = state().coins

        assertNull(accounts.resetPassword(email, "outra-senha"))
        assertTrue(state().signedIn)
        assertEquals(coins, state().coins)

        accounts.signOut()
        assertEquals(AuthFailure.WRONG_CREDENTIALS, accounts.signIn(email, password))
        assertNull(accounts.signIn(email, "outra-senha"))
    }

    @Test fun anotherEmailCannotChangeThePassword() = runBlocking {
        signUp()
        accounts.signOut()
        val stored = state().account!!.password

        assertEquals(AuthFailure.NOT_THE_ACCOUNT_EMAIL, accounts.resetPassword("bia@gmail.com", "outra-senha"))
        assertEquals(stored, state().account!!.password)
        assertFalse(state().signedIn)
    }
}
