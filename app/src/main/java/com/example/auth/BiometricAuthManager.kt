package com.example.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    AVAILABLE,
    NOT_ENROLLED,
    UNAVAILABLE
}

/**
 * Hardware-backed Android Biometric (Fingerprint & Face Lock) manager.
 * Supports both strong (Class 3 fingerprint/3D face) and weak (Class 2 face)
 * biometrics via Android's official BiometricPrompt API.
 */
object BiometricAuthManager {

    private const val ALLOWED_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun checkAvailability(context: Context): BiometricAvailability {
        return try {
            when (BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
                else -> BiometricAvailability.UNAVAILABLE
            }
        } catch (_: Throwable) {
            BiometricAvailability.UNAVAILABLE
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String = "Use Password / PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        val availability = checkAvailability(activity)
        if (availability == BiometricAvailability.NOT_ENROLLED) {
            onError("No fingerprint or face lock is set up on this phone. Sign in with password or enroll biometrics in device Settings.")
            return
        }
        if (availability == BiometricAvailability.UNAVAILABLE) {
            onError("Fingerprint or face hardware is unavailable on this device. Sign in with password.")
            return
        }

        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> onCancelled()
                        else -> onError(errString.toString().ifBlank { "Biometric unlock failed." })
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // System prompt stays open and lets the user retry fingerprint/face
                }
            }

            val prompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build()

            prompt.authenticate(promptInfo)
        } catch (e: Throwable) {
            onError(e.localizedMessage ?: "Unable to open Fingerprint / Face prompt.")
        }
    }
}
