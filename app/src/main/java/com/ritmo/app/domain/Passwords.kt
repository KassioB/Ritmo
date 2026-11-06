package com.ritmo.app.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * A senha nunca é gravada: só este hash. O valor guardado leva as iterações usadas, então dá para
 * aumentá-las no futuro sem invalidar as senhas que já existem. É lento de propósito, então precisa
 * rodar fora da thread principal.
 */
object Passwords {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    /** No Android este cálculo roda em Java: 600 mil levavam 4 s no emulador, tempo demais para entrar. */
    private const val ITERATIONS = 150_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private const val SEPARATOR = ":"

    fun hash(password: String, iterations: Int = ITERATIONS): String {
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val key = derive(password, salt, iterations)
        return listOf("$iterations", base64(salt), base64(key)).joinToString(SEPARATOR)
    }

    fun matches(password: String, stored: String): Boolean {
        val (iterations, salt, key) = stored.split(SEPARATOR)
        val decoder = Base64.getDecoder()
        return MessageDigest.isEqual(derive(password, decoder.decode(salt), iterations.toInt()), decoder.decode(key))
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun base64(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
}
