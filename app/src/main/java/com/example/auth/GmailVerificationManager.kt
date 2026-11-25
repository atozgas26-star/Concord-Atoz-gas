package com.example.auth

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Base64
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class RecoveryPurpose(
    val title: String,
    val emailSubject: String,
    val actionLabel: String
) {
    FORGOT_PASSWORD(
        title = "Forgot Admin Password",
        emailSubject = "C&A Gas - Admin Password Reset Verification Code",
        actionLabel = "reset your Admin password"
    ),
    FORGOT_USERNAME(
        title = "Forgot Admin Username",
        emailSubject = "C&A Gas - Admin Username Recovery Verification Code",
        actionLabel = "recover your Admin username"
    )
}

enum class RecoveryStep {
    REQUEST_CODE,
    VERIFY_CODE,
    USERNAME_REVEALED
}

data class AdminRecoveryUiState(
    val activePurpose: RecoveryPurpose? = null,
    val step: RecoveryStep = RecoveryStep.REQUEST_CODE,
    val targetEmail: String = "",
    val maskedEmail: String = "",
    val isSending: Boolean = false,
    val isVerifying: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val codeExpiresAtMs: Long = 0L,
    val resendAvailableAtMs: Long = 0L,
    val revealedUsername: String? = null
)

sealed interface GmailAuthResult {
    data class Authorized(val accessToken: String) : GmailAuthResult
    data class NeedsConsent(val pendingIntent: PendingIntent) : GmailAuthResult
    data class Error(val message: String) : GmailAuthResult
}

data class GmailSendReceipt(
    val messageId: String,
    val recipientEmail: String,
    val sentAtMs: Long
)

/**
 * Sends real 6-digit verification codes via the Gmail API (`gmail.send` OAuth scope)
 * for Admin Forgot Password and Forgot Username recovery flows.
 */
class GmailVerificationManager(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    suspend fun authorizeGmailSend(allowConsentPrompt: Boolean): GmailAuthResult {
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(SCOPE_GMAIL_SEND)))
                .build()
            val result = Identity.getAuthorizationClient(context.applicationContext)
                .authorize(request)
                .await()

            if (result.hasResolution()) {
                val pending = result.pendingIntent
                if (allowConsentPrompt && pending != null) {
                    GmailAuthResult.NeedsConsent(pending)
                } else {
                    GmailAuthResult.Error("Gmail send authorization needed. Tap Send Code to approve Gmail access.")
                }
            } else {
                val token = result.accessToken
                if (!token.isNullOrBlank()) {
                    GmailAuthResult.Authorized(token)
                } else {
                    GmailAuthResult.Error("Gmail authorization token was empty.")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GmailAuthResult.Error(e.localizedMessage ?: "Unable to connect Gmail authorization.")
        }
    }

    fun extractTokenFromConsentResult(data: Intent?): String? {
        if (data == null) return null
        return try {
            Identity.getAuthorizationClient(context.applicationContext)
                .getAuthorizationResultFromIntent(data)
                .accessToken
        } catch (_: Exception) {
            null
        }
    }

    suspend fun sendVerificationCodeEmail(
        recipientEmail: String,
        verificationCode: String,
        purpose: RecoveryPurpose,
        accessTokenOverride: String? = null
    ): Result<GmailSendReceipt> = withContext(Dispatchers.IO) {
        val safeRecipient = AdminCredentialCrypto.sanitizeEmailHeader(recipientEmail)
        if (!AdminCredentialCrypto.isValidEmail(safeRecipient)) {
            return@withContext Result.failure(IllegalArgumentException("Enter a valid Gmail address."))
        }
        val cleanCode = verificationCode.trim()
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            return@withContext Result.failure(IllegalArgumentException("Verification code must be 6 digits."))
        }

        try {
            val token = accessTokenOverride ?: when (val auth = authorizeGmailSend(allowConsentPrompt = false)) {
                is GmailAuthResult.Authorized -> auth.accessToken
                is GmailAuthResult.NeedsConsent ->
                    return@withContext Result.failure(IllegalStateException("Approve Gmail access to send verification code."))
                is GmailAuthResult.Error ->
                    return@withContext Result.failure(IllegalStateException(auth.message))
            }

            val rawMime = buildRfc2822Message(
                recipientEmail = safeRecipient,
                subject = AdminCredentialCrypto.sanitizeEmailHeader(purpose.emailSubject),
                bodyText = buildString {
                    appendLine("C&A GAS - Admin Security Verification")
                    appendLine()
                    appendLine("Use this 6-digit verification code to ${purpose.actionLabel}:")
                    appendLine()
                    appendLine("    VERIFICATION CODE: $cleanCode")
                    appendLine()
                    appendLine("This code expires in 10 minutes and can only be used once.")
                    appendLine("If you did not request this code, you can safely ignore this email.")
                }
            )
            val encodedRaw = Base64.encodeToString(
                rawMime.toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            val jsonPayload = JSONObject().put("raw", encodedRaw).toString()

            val request = Request.Builder()
                .url(GMAIL_SEND_ENDPOINT)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json; charset=utf-8")
                .post(jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                val rawBody = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Gmail send failed (${resp.code}): ${extractGmailError(rawBody)}")
                    )
                }
                val parsedId = try {
                    JSONObject(rawBody).optString("id")
                } catch (_: Exception) {
                    ""
                }
                Result.success(
                    GmailSendReceipt(
                        messageId = parsedId.ifBlank { "gmail_${System.currentTimeMillis()}" },
                        recipientEmail = safeRecipient,
                        sentAtMs = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun buildRfc2822Message(
        recipientEmail: String,
        subject: String,
        bodyText: String
    ): String {
        val cleanTo = AdminCredentialCrypto.sanitizeEmailHeader(recipientEmail)
        val cleanSubject = AdminCredentialCrypto.sanitizeEmailHeader(subject)
        return buildString {
            append("To: ").append(cleanTo).append("\r\n")
            append("Subject: ").append(cleanSubject).append("\r\n")
            append("MIME-Version: 1.0\r\n")
            append("Content-Type: text/plain; charset=\"UTF-8\"\r\n")
            append("\r\n")
            append(bodyText)
        }
    }

    private fun extractGmailError(raw: String): String {
        return try {
            val errObj = JSONObject(raw).optJSONObject("error")
            errObj?.optString("message")?.takeIf { it.isNotBlank() } ?: raw.take(180)
        } catch (_: Exception) {
            raw.take(180).ifBlank { "Network or Gmail API authorization error" }
        }
    }

    companion object {
        const val SCOPE_GMAIL_SEND = "https://www.googleapis.com/auth/gmail.send"
        const val GMAIL_SEND_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
    }
}
