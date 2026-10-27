package ayx.copy.utils

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi

/**
 * Fingerprint/biometric auth using android.hardware.biometrics.BiometricPrompt
 * which is built into Android API 28+ — no external library needed.
 *
 * MinSdk is 26, so we guard the call site with Build.VERSION.SDK_INT >= 28.
 * On API 26-27 the toggle is shown but tapping it displays a "not supported" message.
 */
object BiometricHelper {

    enum class Availability { READY, NO_HARDWARE, TOO_OLD }

    fun checkAvailability(context: Context): Availability {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return Availability.TOO_OLD
        val hasSensor = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        return if (hasSensor) Availability.READY else Availability.NO_HARDWARE
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun showPrompt(
        context    : Context,
        title      : String,
        subtitle   : String,
        cancelText : String,
        onSuccess  : () -> Unit,
        onError    : (String) -> Unit
    ) {
        val cancel = CancellationSignal()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED) {
                    onError(errString.toString())
                } else {
                    onError("cancelled")
                }
            }
            override fun onAuthenticationFailed() {
                // Finger not recognised — prompt stays open, user retries
            }
        }

        BiometricPrompt.Builder(context)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton(cancelText, context.mainExecutor) { _, _ ->
                onError("cancelled")
            }
            .build()
            .authenticate(cancel, context.mainExecutor, callback)
    }
}
