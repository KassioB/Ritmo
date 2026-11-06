package com.ritmo.app.domain

/** Regras do formulário de conta. Cada verificação devolve o problema, ou nulo se estiver certo. */
object Credentials {
    const val MIN_PASSWORD = 8

    private val Email = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

    /** O mesmo e-mail digitado de outro jeito continua sendo a mesma conta. */
    fun normalize(email: String) = email.trim().lowercase()

    fun nameProblem(name: String) = if (name.isBlank()) "Digite seu nome." else null

    fun emailProblem(email: String) = if (Email.matches(normalize(email))) null else "Digite um e-mail válido."

    fun newPasswordProblem(password: String) =
        if (password.length < MIN_PASSWORD) "Use pelo menos $MIN_PASSWORD caracteres." else null

    /** O bastante para a pessoa reconhecer o próprio e-mail: `k•••••@gmail.com`. */
    fun masked(email: String): String {
        val at = email.indexOf('@')
        if (at <= 0) return email
        return email.take(1) + "•".repeat(at - 1) + email.substring(at)
    }
}
