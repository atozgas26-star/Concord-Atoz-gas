package com.example.auth

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

enum class PhoneOtpStage {
    IDLE,
    SENDING,
    CODE_SENT,
    VERIFYING,
    VERIFIED,
    ERROR
}

data class PhoneOtpUiState(
    val stage: PhoneOtpStage = PhoneOtpStage.IDLE,
    val normalizedPhone: String = "",
    val verificationId: String? = null,
    val message: String = "",
    val otpSentAtMs: Long = 0L,
    val otpExpiresAtMs: Long = 0L,
    val cooldownEndsAtMs: Long = 0L,
    val resendCount: Int = 0,
    val maxResends: Int = PhoneOtpAuthManager.MAX_RESEND_COUNT
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean =
        otpExpiresAtMs > 0L && nowMs > otpExpiresAtMs

    fun remainingCooldownSeconds(nowMs: Long = System.currentTimeMillis()): Int =
        if (cooldownEndsAtMs > nowMs) ((cooldownEndsAtMs - nowMs) / 1000L).toInt().coerceAtLeast(1) else 0

    fun remainingExpirySeconds(nowMs: Long = System.currentTimeMillis()): Int =
        if (otpExpiresAtMs > nowMs) ((otpExpiresAtMs - nowMs) / 1000L).toInt().coerceAtLeast(0) else 0

    fun canResend(nowMs: Long = System.currentTimeMillis()): Boolean =
        resendCount < maxResends && remainingCooldownSeconds(nowMs) == 0
}

object UaePhoneValidator {
    /**
     * Normalizes UAE mobile numbers into strict E.164 format (+9715XXXXXXXX).
     * Accepts inputs like "+971 50 123 4567", "971501234567", "0501234567", or "501234567".
     * Returns null if not a valid 9-digit UAE mobile number starting with 5.
     */
    fun normalizeUaePhone(raw: String): String? {
        val digits = raw.trim().replace(Regex("[^0-9+]"), "")
        if (digits.isBlank()) return null
        val onlyDigits = digits.removePrefix("+")
        val local9 = when {
            onlyDigits.startsWith("00971") -> onlyDigits.removePrefix("00971")
            onlyDigits.startsWith("971") -> onlyDigits.removePrefix("971")
            onlyDigits.startsWith("05") && onlyDigits.length == 10 -> onlyDigits.removePrefix("0")
            onlyDigits.startsWith("5") && onlyDigits.length == 9 -> onlyDigits
            else -> return null
        }
        if (local9.length != 9 || !local9.startsWith("5") || !local9.all { it.isDigit() }) {
            return null
        }
        return "+971$local9"
    }

    fun formatDisplay(e164: String): String {
        val norm = normalizeUaePhone(e164) ?: return e164
        val local = norm.removePrefix("+971")
        return "+971 ${local.substring(0, 2)} ${local.substring(2, 5)} ${local.substring(5)}"
    }
}

class PhoneOtpAuthManager(
    private val authProvider: () -> FirebaseAuth? = {
        runCatching { FirebaseAuth.getInstance() }.getOrNull()
    },
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    private val _state = MutableStateFlow(PhoneOtpUiState())
    val state: StateFlow<PhoneOtpUiState> = _state.asStateFlow()

    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun reset() {
        resendToken = null
        _state.value = PhoneOtpUiState()
    }

    fun setError(phone: String, errorMessage: String) {
        _state.value = _state.value.copy(
            stage = PhoneOtpStage.ERROR,
            normalizedPhone = phone,
            message = errorMessage
        )
    }

    fun sendOtp(
        activity: Activity,
        normalizedPhoneE164: String,
        isResend: Boolean = false,
        onAutoVerified: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val nowMs = clockMs()
        val current = _state.value
        val samePhone = current.normalizedPhone == normalizedPhoneE164
        val nextResendCount = if (isResend && samePhone) current.resendCount + 1 else 0

        if (isResend && samePhone) {
            if (current.resendCount >= MAX_RESEND_COUNT) {
                val msg = "OTP resend limit reached ($MAX_RESEND_COUNT max). Please wait and try again later."
                _state.value = current.copy(stage = PhoneOtpStage.ERROR, message = msg)
                onFailure(msg)
                return
            }
            val cooldownSec = current.remainingCooldownSeconds(nowMs)
            if (cooldownSec > 0) {
                val msg = "Please wait ${cooldownSec}s before requesting another SMS OTP."
                _state.value = current.copy(message = msg)
                onFailure(msg)
                return
            }
        }

        val auth = authProvider()
        if (auth == null) {
            val msg = "Firebase Authentication is unavailable on this device."
            _state.value = PhoneOtpUiState(
                stage = PhoneOtpStage.ERROR,
                normalizedPhone = normalizedPhoneE164,
                message = msg,
                resendCount = nextResendCount
            )
            onFailure(msg)
            return
        }

        _state.value = PhoneOtpUiState(
            stage = PhoneOtpStage.SENDING,
            normalizedPhone = normalizedPhoneE164,
            message = "Sending SMS verification code to ${UaePhoneValidator.formatDisplay(normalizedPhoneE164)}...",
            resendCount = nextResendCount
        )

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                completeCredentialVerification(
                    auth = auth,
                    credential = credential,
                    normalizedPhoneE164 = normalizedPhoneE164,
                    onVerified = onAutoVerified,
                    onFailure = onFailure
                )
            }

            override fun onVerificationFailed(e: FirebaseException) {
                val msg = e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "SMS OTP verification failed. Ensure Phone Sign-In is enabled in Firebase Console."
                _state.value = _state.value.copy(
                    stage = PhoneOtpStage.ERROR,
                    normalizedPhone = normalizedPhoneE164,
                    message = msg
                )
                onFailure(msg)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                val sentAt = clockMs()
                resendToken = token
                _state.value = PhoneOtpUiState(
                    stage = PhoneOtpStage.CODE_SENT,
                    normalizedPhone = normalizedPhoneE164,
                    verificationId = verificationId,
                    message = "6-digit SMS OTP sent to ${UaePhoneValidator.formatDisplay(normalizedPhoneE164)} (valid for 5 minutes).",
                    otpSentAtMs = sentAt,
                    otpExpiresAtMs = sentAt + OTP_EXPIRY_MS,
                    cooldownEndsAtMs = sentAt + RESEND_COOLDOWN_MS,
                    resendCount = nextResendCount,
                    maxResends = MAX_RESEND_COUNT
                )
            }
        }

        try {
            val builder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(normalizedPhoneE164)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
            if (isResend && samePhone) {
                resendToken?.let { builder.setForceResendingToken(it) }
            }
            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        } catch (e: Exception) {
            val msg = e.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Unable to start SMS OTP verification."
            _state.value = _state.value.copy(
                stage = PhoneOtpStage.ERROR,
                normalizedPhone = normalizedPhoneE164,
                message = msg
            )
            onFailure(msg)
        }
    }

    fun verifyOtpCode(
        code: String,
        onVerified: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val nowMs = clockMs()
        val current = _state.value
        val verificationId = current.verificationId
        val cleanCode = code.trim()
        if (verificationId.isNullOrBlank()) {
            val msg = "Request an SMS OTP code first."
            _state.value = current.copy(stage = PhoneOtpStage.ERROR, message = msg)
            onFailure(msg)
            return
        }
        if (current.isExpired(nowMs)) {
            val msg = "SMS OTP has expired (5-minute limit). Request a new verification code."
            _state.value = current.copy(stage = PhoneOtpStage.ERROR, message = msg)
            onFailure(msg)
            return
        }
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            val msg = "Enter the 6-digit SMS verification code."
            _state.value = current.copy(stage = PhoneOtpStage.CODE_SENT, message = msg)
            onFailure(msg)
            return
        }

        val auth = authProvider()
        if (auth == null) {
            val msg = "Firebase Authentication is unavailable."
            _state.value = current.copy(stage = PhoneOtpStage.ERROR, message = msg)
            onFailure(msg)
            return
        }

        _state.value = current.copy(
            stage = PhoneOtpStage.VERIFYING,
            message = "Verifying SMS OTP..."
        )
        val credential = PhoneAuthProvider.getCredential(verificationId, cleanCode)
        completeCredentialVerification(
            auth = auth,
            credential = credential,
            normalizedPhoneE164 = current.normalizedPhone,
            onVerified = onVerified,
            onFailure = onFailure
        )
    }

    companion object {
        const val OTP_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes
        const val RESEND_COOLDOWN_MS = 60 * 1000L // 60 seconds
        const val MAX_RESEND_COUNT = 3
    }

    private fun completeCredentialVerification(
        auth: FirebaseAuth,
        credential: PhoneAuthCredential,
        normalizedPhoneE164: String,
        onVerified: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val existingUser = auth.currentUser
        // Preserve existing Google Sign-In workspace session if signed in by updating/linking phone credential,
        // otherwise sign in directly with the verified Firebase PhoneAuthCredential.
        val task = if (existingUser != null) {
            existingUser.updatePhoneNumber(credential)
        } else {
            auth.signInWithCredential(credential)
        }

        task.addOnCompleteListener { result ->
            if (result.isSuccessful) {
                _state.value = PhoneOtpUiState(
                    stage = PhoneOtpStage.VERIFIED,
                    normalizedPhone = normalizedPhoneE164,
                    message = "UAE mobile verified via Firebase SMS OTP."
                )
                onVerified(normalizedPhoneE164)
            } else {
                // If link/update fails due to credential already linked to another user, fall back to direct phone credential sign-in
                if (existingUser != null) {
                    auth.signInWithCredential(credential).addOnCompleteListener { fallback ->
                        if (fallback.isSuccessful) {
                            _state.value = PhoneOtpUiState(
                                stage = PhoneOtpStage.VERIFIED,
                                normalizedPhone = normalizedPhoneE164,
                                message = "UAE mobile verified via Firebase SMS OTP."
                            )
                            onVerified(normalizedPhoneE164)
                        } else {
                            val msg = fallback.exception?.localizedMessage ?: "Invalid or expired OTP code."
                            _state.value = _state.value.copy(stage = PhoneOtpStage.CODE_SENT, message = msg)
                            onFailure(msg)
                        }
                    }
                } else {
                    val msg = result.exception?.localizedMessage ?: "Invalid or expired OTP code."
                    _state.value = _state.value.copy(stage = PhoneOtpStage.CODE_SENT, message = msg)
                    onFailure(msg)
                }
            }
        }
    }
}
