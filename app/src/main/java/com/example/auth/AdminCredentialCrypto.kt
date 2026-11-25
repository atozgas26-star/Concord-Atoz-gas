package com.example.auth

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cryptographic utilities for Admin username/password authentication and
 * 6-digit Gmail verification code challenges.
 */
object AdminCredentialCrypto {
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_PREFIX = "pbkdf2"
    private const val DEFAULT_ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val KEY_LENGTH_BITS = 256

    private val secureRandom = SecureRandom()
    private val usernameRegex = Regex("^[a-zA-Z0-9._-]{3,40}$")
    private val emailRegex = Regex("^[a-zA-Z0-9._%+\\-]{1,64}@[a-zA-Z0-9.\\-]{1,120}\\.[a-zA-Z]{2,12}$")

    fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        val derived = derivePbkdf2(password.toCharArray(), salt, DEFAULT_ITERATIONS)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(derived, Base64.NO_WRAP)
        return "$PBKDF2_PREFIX$$DEFAULT_ITERATIONS$$saltB64$$hashB64"
    }

    fun verifyPassword(password: String, encodedHash: String): Boolean {
        if (password.isEmpty() || encodedHash.isBlank()) return false
        val parts = encodedHash.split('$')
        if (parts.size != 4 || parts[0] != PBKDF2_PREFIX) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        if (iterations !in 10_000..500_000) return false
        return try {
            val salt = Base64.decode(parts[2], Base64.NO_WRAP)
            val expected = Base64.decode(parts[3], Base64.NO_WRAP)
            val actual = derivePbkdf2(password.toCharArray(), salt, iterations)
            MessageDigest.isEqual(actual, expected)
        } catch (_: Exception) {
            false
        }
    }

    fun generateSixDigitCode(): String {
        val value = secureRandom.nextInt(900_000) + 100_000
        return value.toString()
    }

    fun generateRandomSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hashVerificationCode(
        code: String,
        salt: String,
        purpose: String,
        email: String
    ): String {
        val normalized = "${purpose.trim().uppercase()}|${email.trim().lowercase()}|$salt|${code.trim()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    fun verifyVerificationCode(
        inputCode: String,
        expectedHash: String,
        salt: String,
        purpose: String,
        email: String
    ): Boolean {
        if (inputCode.trim().length != 6 || !inputCode.trim().all { it.isDigit() }) return false
        val actual = hashVerificationCode(inputCode, salt, purpose, email)
        return try {
            MessageDigest.isEqual(
                Base64.decode(actual, Base64.NO_WRAP),
                Base64.decode(expectedHash, Base64.NO_WRAP)
            )
        } catch (_: Exception) {
            false
        }
    }

    fun isValidUsername(username: String): Boolean =
        usernameRegex.matches(username.trim())

    fun isValidPassword(password: String): Boolean =
        password.length in 6..64

    fun isValidEmail(email: String): Boolean {
        val clean = email.trim()
        if (clean.contains('\r') || clean.contains('\n')) return false
        return emailRegex.matches(clean)
    }

    fun sanitizeEmailHeader(value: String): String =
        value.replace("\r", "").replace("\n", "").replace("\u0000", "").trim()

    fun maskEmail(email: String): String {
        val clean = email.trim()
        val atIdx = clean.indexOf('@')
        if (atIdx <= 1) return clean
        val local = clean.substring(0, atIdx)
        val domain = clean.substring(atIdx)
        val visibleStart = local.take(2)
        val visibleEnd = if (local.length > 4) local.takeLast(1) else ""
        return "$visibleStart***$visibleEnd$domain"
    }

    private fun derivePbkdf2(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
