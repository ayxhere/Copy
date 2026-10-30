package ayx.copy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import ayx.copy.databinding.ActivitySettingsAccessibilityBinding

class AccessibilitySettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsAccessibilityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsAccessibilityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnEnableService.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        val isEnabled = isAccessibilityServiceEnabled()
        binding.ivStatusDot.setImageResource(
            if (isEnabled) R.drawable.ic_dot_active else R.drawable.ic_dot_inactive)
        binding.tvStatus.text = getString(
            if (isEnabled) R.string.service_active else R.string.service_inactive)
        binding.btnEnableService.text = getString(
            if (isEnabled) R.string.service_active else R.string.enable_service)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any { it.id.contains(packageName) }
    }
}
