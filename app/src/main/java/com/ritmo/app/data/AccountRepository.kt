package com.ritmo.app.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.ritmo.app.BuildConfig
import com.ritmo.app.domain.Credentials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

enum class AuthFailure(val message: String) {
    WRONG_CREDENTIALS("E-mail ou senha incorretos."),
    WRONG_PASSWORD("Senha incorreta."),
    EMAIL_IN_USE("Já existe uma conta com este e-mail. Entre com ela."),
    INVALID_EMAIL("Digite um e-mail válido."),
    WEAK_PASSWORD("Essa senha é fácil demais de adivinhar. Escolha outra."),
    OFFLINE("Sem conexão com a internet. Confira e tente de novo."),
    TOO_MANY_ATTEMPTS("Muitas tentativas seguidas. Espere alguns minutos e tente de novo."),
    NOT_SAVED("Seu progresso mais recente ainda não chegou à conta. Conecte-se à internet e tente de novo."),
    UNAVAILABLE("O login não está disponível nesta versão do app."),
    UNKNOWN("Algo deu errado. Tente de novo em instantes."),
}

/**
 * Liga o app ao Firebase. Com o google-services.json, o próprio Firebase se configura ao abrir o app;
 * sem ele, só os testes com os emuladores locais (FIREBASE_EMULATOR_HOST) têm login.
 */
object Cloud {
    private const val AUTH_PORT = 9099
    private const val FIRESTORE_PORT = 8080

    @Volatile
    var available = false
        private set

    fun init(context: Context) {
        if (available) return
        val host = BuildConfig.FIREBASE_EMULATOR_HOST
        if (FirebaseApp.getApps(context).isEmpty()) {
            if (host.isEmpty()) return
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId("demo-ritmo")
                    .setApplicationId("1:000000000000:android:0000000000000000")
                    .setApiKey("demo-api-key")
                    .build(),
            )
        }
        if (host.isNotEmpty()) {
            FirebaseAuth.getInstance().useEmulator(host, AUTH_PORT)
            FirebaseFirestore.getInstance().useEmulator(host, FIRESTORE_PORT)
        }
        available = true
    }
}

/**
 * A conta vive no Firebase Authentication e o progresso em um documento por pessoa no Firestore
 * (`users/{uid}`), então os dois sobrevivem a limpar o armazenamento, reinstalar ou trocar de celular.
 * O banco local é uma cópia de trabalho: entrar baixa o progresso da conta, e cada mudança sobe sozinha.
 *
 * Cada operação devolve o motivo da falha, ou nulo se deu certo.
 */
class AccountRepository(db: RitmoDatabase, private val repository: RitmoRepository) {
    private val dao = db.dao()
    private val backup = Backup(db)
    private val pushing = Mutex()
    private var lastPushed: Int? = null

    private val auth get() = FirebaseAuth.getInstance()
    private fun document(uid: String) = FirebaseFirestore.getInstance().collection(USERS).document(uid)
    private fun user(): FirebaseUser? = if (Cloud.available) auth.currentUser else null

    /**
     * Ao abrir o app: uma conta marcada como conectada sem sessão no Firebase é da versão em que a
     * conta ficava só no aparelho. Ela precisa entrar de novo, e o progresso local continua ali.
     */
    suspend fun reconcile() {
        val account = dao.account() ?: return
        if (account.signedIn && user() == null) dao.saveAccount(account.copy(signedIn = false))
    }

    /** O que já existe no aparelho passa a ser o progresso da conta nova. */
    suspend fun signUp(name: String, email: String, password: String): AuthFailure? = attempt {
        val user = auth.createUserWithEmailAndPassword(Credentials.normalize(email), password).await().user
            ?: return@attempt AuthFailure.UNKNOWN
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        saveLocal(user, name.trim())
        // Sem esperar o servidor: se a conexão cair agora, o Firestore envia assim que ela voltar.
        push(user.uid, force = true)
        null
    }

    suspend fun signIn(email: String, password: String): AuthFailure? = attempt {
        val user = auth.signInWithEmailAndPassword(Credentials.normalize(email), password).await().user
            ?: return@attempt AuthFailure.UNKNOWN
        // Direto do servidor: sem saber o que a conta tem, subir o banco local poderia apagar o progresso dela.
        val remote = try {
            document(user.uid).get(Source.SERVER).await()
        } catch (e: Exception) {
            auth.signOut()
            throw e
        }
        val data = remote.getBlob(DATA)
        if (data != null) backup.restore(data.toBytes())
        saveLocal(user, remote.getString(NAME))
        if (data == null) push(user.uid, force = true) else lastPushed = null
        null
    }

    /**
     * A resposta é a mesma exista a conta ou não, para a tela não servir para descobrir quem usa o app.
     * O Firebase já faz isso com a proteção contra enumeração de e-mails ligada; aqui vale mesmo sem ela.
     */
    suspend fun sendPasswordReset(email: String): AuthFailure? = attempt {
        auth.setLanguageCode("pt-BR")
        try {
            auth.sendPasswordResetEmail(Credentials.normalize(email)).await()
        } catch (_: FirebaseAuthInvalidUserException) {
            // Conta inexistente: responde como se tivesse enviado.
        }
        null
    }

    /**
     * Só sai depois de o servidor confirmar o progresso: com a sessão encerrada, o que ficasse na fila
     * de envio seria recusado. Depois limpa o aparelho para a próxima pessoa que entrar nele.
     */
    suspend fun signOut(today: Long): AuthFailure? = attempt {
        user()?.let { user ->
            withTimeoutOrNull(SAVE_TIMEOUT_MS) { push(user.uid, force = true)?.await() ?: Unit }
                ?: return@attempt AuthFailure.NOT_SAVED
        }
        auth.signOut()
        forgetLocal(today)
        null
    }

    suspend fun updateName(name: String): AuthFailure? = attempt {
        val user = user() ?: return@attempt AuthFailure.UNAVAILABLE
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        dao.account()?.let { dao.saveAccount(it.copy(name = name.trim())) }
        push(user.uid, force = true)
        null
    }

    suspend fun changePassword(current: String, new: String): AuthFailure? = attempt(AuthFailure.WRONG_PASSWORD) {
        val user = reauthenticate(current) ?: return@attempt AuthFailure.UNAVAILABLE
        user.updatePassword(new).await()
        null
    }

    /** Apaga o progresso guardado e a conta. Não tem volta, por isso pede a senha de novo. */
    suspend fun deleteAccount(password: String, today: Long): AuthFailure? = attempt(AuthFailure.WRONG_PASSWORD) {
        val user = reauthenticate(password) ?: return@attempt AuthFailure.UNAVAILABLE
        document(user.uid).delete().await()
        user.delete().await()
        forgetLocal(today)
        null
    }

    /** Chamado a cada mudança no banco: sobe o progresso de quem está conectado. */
    suspend fun sync() {
        val user = user() ?: return
        if (dao.account()?.signedIn != true) return
        try {
            push(user.uid, force = false)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Não deu para enviar o progresso", e)
        }
    }

    /**
     * Grava o documento da conta. O Firestore guarda a escrita no aparelho e a envia quando houver
     * conexão; a tarefa devolvida termina quando o servidor confirma. Nulo se não havia nada novo.
     */
    private suspend fun push(uid: String, force: Boolean) = pushing.withLock {
        val data = backup.export()
        val hash = data.contentHashCode()
        if (!force && hash == lastPushed) return@withLock null
        val account = dao.account()
        lastPushed = hash
        document(uid).set(
            mapOf(
                NAME to account?.name,
                EMAIL to account?.email,
                SCHEMA to Backup.SCHEMA,
                DATA to Blob.fromBytes(data),
                UPDATED to FieldValue.serverTimestamp(),
            ),
        )
    }

    private suspend fun reauthenticate(password: String): FirebaseUser? {
        val user = user() ?: return null
        val email = user.email ?: return null
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
        return user
    }

    private suspend fun saveLocal(user: FirebaseUser, name: String?) {
        val email = user.email.orEmpty()
        dao.saveAccount(
            Account(
                name = name?.takeIf { it.isNotBlank() } ?: user.displayName ?: email.substringBefore('@'),
                email = email,
                password = null,
                signedIn = true,
            ),
        )
    }

    private suspend fun forgetLocal(today: Long) {
        backup.clear()
        lastPushed = null
        repository.ensureSetup(today)
    }

    private suspend fun attempt(
        wrongPassword: AuthFailure = AuthFailure.WRONG_CREDENTIALS,
        block: suspend () -> AuthFailure?,
    ): AuthFailure? {
        if (!Cloud.available) return AuthFailure.UNAVAILABLE
        return try {
            block()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            failureOf(e, wrongPassword).also { if (it == AuthFailure.UNKNOWN) Log.w(TAG, "Falha na conta", e) }
        }
    }

    private fun failureOf(e: Exception, wrongPassword: AuthFailure) = when (e) {
        is FirebaseAuthUserCollisionException -> AuthFailure.EMAIL_IN_USE
        is FirebaseAuthWeakPasswordException -> AuthFailure.WEAK_PASSWORD
        is FirebaseAuthInvalidCredentialsException ->
            if (e.errorCode == "ERROR_INVALID_EMAIL") AuthFailure.INVALID_EMAIL else wrongPassword
        is FirebaseAuthInvalidUserException -> wrongPassword
        is FirebaseNetworkException -> AuthFailure.OFFLINE
        is FirebaseTooManyRequestsException -> AuthFailure.TOO_MANY_ATTEMPTS
        is FirebaseFirestoreException ->
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) AuthFailure.OFFLINE else AuthFailure.UNKNOWN
        else -> AuthFailure.UNKNOWN
    }

    private companion object {
        const val TAG = "Ritmo"
        const val USERS = "users"
        const val NAME = "name"
        const val EMAIL = "email"
        const val SCHEMA = "schema"
        const val DATA = "data"
        const val UPDATED = "updatedAt"
        const val SAVE_TIMEOUT_MS = 10_000L
    }
}
