package com.ritmo.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Conversa com os emuladores do Firebase; só roda num build feito com -Pritmo.firebaseEmulator.
 * "Limpar o armazenamento" é simulado trocando o banco por um vazio e encerrando a sessão.
 */
@RunWith(AndroidJUnit4::class)
class CloudAccountTest {
    private val password = "senha-forte"
    private val email = "ana.${System.nanoTime()}@gmail.com"
    private lateinit var database: RitmoDatabase
    private lateinit var repository: RitmoRepository
    private lateinit var accounts: AccountRepository

    @Before fun open() {
        assumeTrue("Build sem os emuladores do Firebase", Cloud.available)
        FirebaseAuth.getInstance().signOut()
        device()
    }

    /** Apaga o progresso e a conta de teste, para o emulador do Firebase não acumular lixo. */
    @After fun close() {
        if (!Cloud.available) return
        runBlocking {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) runCatching { auth.signInWithEmailAndPassword(email, password).await() }
            auth.currentUser?.let { user ->
                runCatching { FirebaseFirestore.getInstance().collection("users").document(user.uid).delete().await() }
                user.delete().await()
            }
        }
        database.close()
    }

    /** Um aparelho recém-instalado (ou com o armazenamento limpo). */
    private fun device() {
        database = memoryDatabase()
        repository = RitmoRepository(database)
        accounts = AccountRepository(database, repository)
        runBlocking { repository.ensureSetup(TODAY) }
    }

    private fun wipeDevice() {
        FirebaseAuth.getInstance().signOut()
        database.close()
        device()
    }

    private suspend fun state() = repository.load()!!

    private suspend fun remote() = FirebaseFirestore.getInstance().collection("users")
        .document(FirebaseAuth.getInstance().currentUser!!.uid).get(Source.SERVER).await()

    private suspend fun signUp() = accounts.signUp("Ana", email, password)

    @Test fun signingUpEntersAndKeepsWhatTheDeviceHad() = runBlocking {
        repository.seedProgress()
        val before = state().progress()

        assertNull(signUp())

        assertTrue(state().signedIn)
        assertEquals("Ana", state().account!!.name)
        assertEquals(email, state().account!!.email)
        assertEquals(before, state().progress())
    }

    @Test fun theAccountAndTheProgressSurviveWipingTheDevice() = runBlocking {
        signUp()
        repository.seedProgress()
        accounts.sync()
        val before = state().progress()
        // Sair espera o servidor confirmar, então a nuvem já tem tudo quando o aparelho é limpo.
        assertNull(accounts.signOut(TODAY))
        wipeDevice()
        assertTrue(state().objectives.isEmpty())

        assertNull(accounts.signIn(email, password))

        assertTrue(state().signedIn)
        assertEquals("Ana", state().account!!.name)
        assertEquals(before, state().progress())
    }

    @Test fun everyChangeIsSavedInTheAccount() = runBlocking {
        signUp()
        repository.seedProgress()

        accounts.sync()

        val saved = remote()
        assertEquals("Ana", saved.getString("name"))
        assertNotNull(saved.getBlob("data"))
        val other = memoryDatabase()
        try {
            Backup(other).restore(saved.getBlob("data")!!.toBytes())
            assertEquals(state().progress(), RitmoRepository(other).load()!!.progress())
        } finally {
            other.close()
        }
    }

    @Test fun signingOutLeavesNothingOnTheDevice() = runBlocking {
        signUp()
        repository.seedProgress()

        assertNull(accounts.signOut(TODAY))

        val after = state()
        assertNull(after.account)
        assertFalse(after.signedIn)
        assertTrue(after.objectives.isEmpty())
        assertTrue(after.lists.isEmpty())
    }

    @Test fun aWrongPasswordOrUnknownEmailDoesNotEnter() = runBlocking {
        signUp()
        accounts.signOut(TODAY)

        assertEquals(AuthFailure.WRONG_CREDENTIALS, accounts.signIn(email, "senha-errada"))
        assertEquals(AuthFailure.WRONG_CREDENTIALS, accounts.signIn("ninguem.${System.nanoTime()}@gmail.com", password))
        assertFalse(state().signedIn)
    }

    @Test fun theSameEmailCannotHaveTwoAccounts() = runBlocking {
        signUp()
        wipeDevice()

        assertEquals(AuthFailure.EMAIL_IN_USE, accounts.signUp("Bia", email, password))
        assertFalse(state().signedIn)
    }

    @Test fun theEmailCanBeTypedDifferentlyAtSignIn() = runBlocking {
        signUp()
        accounts.signOut(TODAY)

        assertNull(accounts.signIn("  ${email.uppercase()} ", password))
        assertTrue(state().signedIn)
    }

    @Test fun theNameCanBeChangedAndFollowsTheAccount() = runBlocking {
        signUp()

        assertNull(accounts.updateName("Ana Souza"))
        assertEquals("Ana Souza", state().account!!.name)
        accounts.signOut(TODAY)
        wipeDevice()

        accounts.signIn(email, password)
        assertEquals("Ana Souza", state().account!!.name)
    }

    @Test fun changingThePasswordAsksForTheCurrentOne() = runBlocking {
        signUp()

        assertEquals(AuthFailure.WRONG_PASSWORD, accounts.changePassword("senha-errada", "outra-senha"))
        assertNull(accounts.changePassword(password, "outra-senha"))
        accounts.signOut(TODAY)

        assertEquals(AuthFailure.WRONG_CREDENTIALS, accounts.signIn(email, password))
        assertNull(accounts.signIn(email, "outra-senha"))
    }

    @Test fun deletingTheAccountErasesItEverywhere() = runBlocking {
        signUp()
        repository.seedProgress()
        accounts.sync()
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        assertEquals(AuthFailure.WRONG_PASSWORD, accounts.deleteAccount("senha-errada", TODAY))
        assertNull(accounts.deleteAccount(password, TODAY))

        assertNull(state().account)
        assertTrue(state().objectives.isEmpty())
        assertNull(FirebaseAuth.getInstance().currentUser)
        assertEquals(AuthFailure.WRONG_CREDENTIALS, accounts.signIn(email, password))
        // O documento apagado é conferido de fora, no emulador, porque as regras não deixam outra conta lê-lo.
        println("RITMO_DELETED_UID=$uid")
    }

    @Test fun nobodyElseCanReadMyProgress() = runBlocking {
        signUp()
        repository.seedProgress()
        accounts.sync()
        val mine = FirebaseAuth.getInstance().currentUser!!.uid
        accounts.signOut(TODAY)
        FirebaseAuth.getInstance().signOut()
        assertNull(accounts.signUp("Bia", "bia.${System.nanoTime()}@gmail.com", password))

        val denied = runCatching {
            FirebaseFirestore.getInstance().collection("users").document(mine).get(Source.SERVER).await()
        }.exceptionOrNull()

        assertTrue(denied is FirebaseFirestoreException && denied.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    @Test fun askingForANewPasswordWorksForAnyEmail() = runBlocking {
        signUp()
        accounts.signOut(TODAY)

        assertNull(accounts.sendPasswordReset(email))
        assertNull(accounts.sendPasswordReset("ninguem.${System.nanoTime()}@gmail.com"))
    }
}
