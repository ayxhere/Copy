package ayx.copy

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ayx.copy.databinding.ActivitySettingsApplockBinding
import ayx.copy.utils.BiometricHelper
import ayx.copy.utils.PrefManager

class AppLockSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsApplockBinding
    private lateinit var prefManager: PrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsApplockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(applicationContext)

        binding.btnBack.setOnClickListener { finish() }

        binding.switchAppLock.isChecked = prefManager.appLockEnabled
        binding.switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) enableAppLock() else disableAppLock()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.switchAppLock.isChecked = prefManager.appLockEnabled
    }

    private fun enableAppLock() {
        when (BiometricHelper.checkAvailability(this)) {
            BiometricHelper.Availability.TOO_OLD,
            BiometricHelper.Availability.NO_HARDWARE -> {
                Toast.makeText(this, getString(R.string.biometric_not_available), Toast.LENGTH_LONG).show()
                binding.switchAppLock.isChecked = false
            }
            BiometricHelper.Availability.READY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    BiometricHelper.showPrompt(
                        context    = this,
                        title      = getString(R.string.biometric_prompt_title),
                        subtitle   = "Confirm fingerprint to enable App Lock",
                        cancelText = getString(R.string.biometric_prompt_cancel),
                        onSuccess  = {
                            prefManager.appLockEnabled = true
                            Toast.makeText(this, "App Lock enabled", Toast.LENGTH_SHORT).show()
                        },
                        onError    = { binding.switchAppLock.isChecked = false }
                    )
                }
            }
        }
    }

    private fun disableAppLock() {
        when (BiometricHelper.checkAvailability(this)) {
            BiometricHelper.Availability.READY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    BiometricHelper.showPrompt(
                        context    = this,
                        title      = "Disable App Lock",
                        subtitle   = "Confirm fingerprint to turn off App Lock",
                        cancelText = getString(R.string.biometric_prompt_cancel),
                        onSuccess  = {
                            prefManager.appLockEnabled = false
                            Toast.makeText(this, "App Lock disabled", Toast.LENGTH_SHORT).show()
                        },
                        onError    = { binding.switchAppLock.isChecked = true }
                    )
                }
            }
            else -> {
                prefManager.appLockEnabled = false
            }
        }
    }
}
