package com.ritmo.app.data

import com.ritmo.app.domain.Credentials
import com.ritmo.app.domain.Passwords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AuthFailure(val message: String) {
    NO_ACCOUNT("Ainda não há conta neste aparelho. Crie uma para começar."),
    WRONG_CREDENTIALS("E-mail ou senha incorretos."),
    ALREADY_HAS_ACCOUNT("Este aparelho já tem uma conta. Entre com ela."),
    NOT_THE_ACCOUNT_EMAIL("Esse não é o e-mail da conta deste aparelho."),
}

/**
 * Login por e-mail e senha, verificado só no aparelho: ainda não existe servidor. Por isso a conta
 * não vai para outro celular e a senha não se recupera por e-mail. Quando o login com Google
 * entrar, é esta classe que passa a falar com o serviço de contas; as telas não mudam.
 *
 * Cada operação devolve o motivo da falha, ou nulo se deu certo.
 */
class AccountRepository(db: RitmoDatabase) {
    private val dao = db.dao()

    /** O que já existe no aparelho continua igual e passa a pertencer à conta criada. */
    suspend fun signUp(name: String, email: String, password: String): AuthFailure? {
        if (dao.account() != null) return AuthFailure.ALREADY_HAS_ACCOUNT
        dao.saveAccount(
            Account(
                name = name.trim(),
                email = Credentials.normalize(email),
                password = hash(password),
                signedIn = true,
            ),
        )
        return null
    }

    suspend fun signIn(email: String, password: String): AuthFailure? {
        val account = dao.account() ?: return AuthFailure.NO_ACCOUNT
        val stored = account.password
        if (stored == null || account.email != Credentials.normalize(email) || !matches(password, stored)) {
            return AuthFailure.WRONG_CREDENTIALS
        }
        dao.saveAccount(account.copy(signedIn = true))
        return null
    }

    /** Os dados ficam onde estão, à espera do próximo login. */
    suspend fun signOut() {
        dao.account()?.let { dao.saveAccount(it.copy(signedIn = false)) }
    }

    /**
     * Sem servidor não há como confirmar a identidade por e-mail, então basta saber qual é o e-mail
     * da conta. Sem esta saída, esquecer a senha deixaria o progresso preso no aparelho para sempre.
     */
    suspend fun resetPassword(email: String, password: String): AuthFailure? {
        val account = dao.account()
        if (account == null || account.email != Credentials.normalize(email)) {
            return AuthFailure.NOT_THE_ACCOUNT_EMAIL
        }
        dao.saveAccount(account.copy(password = hash(password), signedIn = true))
        return null
    }

    private suspend fun hash(password: String) = withContext(Dispatchers.Default) { Passwords.hash(password) }

    private suspend fun matches(password: String, stored: String) =
        withContext(Dispatchers.Default) { Passwords.matches(password, stored) }
}
