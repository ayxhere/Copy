package ayx.copy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ayx.copy.databinding.ActivitySplashBinding
import ayx.copy.utils.AnimationUtil
import ayx.copy.utils.BiometricHelper
import ayx.copy.utils.PrefManager

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var prefManager: PrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(applicationContext)

        binding.tvLogo.translationY      = 30f
        binding.tvAppName.translationY   = 20f
        binding.tvTagline.translationY   = 16f
        binding.progressBar.translationY = 10f
        binding.tvLoading.translationY   = 10f

        AnimationUtil.staggeredReveal(
            listOf(binding.tvLogo, binding.tvAppName, binding.tvTagline,
                   binding.progressBar, binding.tvLoading),
            startDelay = 100, stepDelay = 110
        )

        if (prefManager.appLockEnabled) {
            Handler(Looper.getMainLooper()).postDelayed({ showBiometric() }, 600)
        } else {
            proceedAfterDelay()
        }
    }

    private fun showBiometric() {
        when (BiometricHelper.checkAvailability(this)) {
            BiometricHelper.Availability.TOO_OLD -> {
                Toast.makeText(this, getString(R.string.biometric_not_available), Toast.LENGTH_SHORT).show()
                proceedAfterDelay()
            }
            BiometricHelper.Availability.NO_HARDWARE -> {
                Toast.makeText(this, getString(R.string.biometric_not_available), Toast.LENGTH_SHORT).show()
                proceedAfterDelay()
            }
            BiometricHelper.Availability.READY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    BiometricHelper.showPrompt(
                        context    = this,
                        title      = getString(R.string.biometric_prompt_title),
                        subtitle   = getString(R.string.biometric_prompt_subtitle),
                        cancelText = getString(R.string.biometric_prompt_cancel),
                        onSuccess  = { proceedToMain() },
                        onError    = { msg -> if (msg == "cancelled") finish() }
                    )
                } else {
                    proceedAfterDelay()
                }
            }
        }
    }

    private fun proceedAfterDelay() {
        Handler(Looper.getMainLooper()).postDelayed({ proceedToMain() }, SPLASH_MS)
    }

    private fun proceedToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    companion object { private const val SPLASH_MS = 1400L }
}
