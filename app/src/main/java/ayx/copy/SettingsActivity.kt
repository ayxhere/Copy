package ayx.copy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ayx.copy.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.rowAccessibility.setOnClickListener {
            startActivity(Intent(this, AccessibilitySettingsActivity::class.java))
        }
        binding.rowCommands.setOnClickListener {
            startActivity(Intent(this, CommandsSettingsActivity::class.java))
        }
        binding.rowAppLock.setOnClickListener {
            startActivity(Intent(this, AppLockSettingsActivity::class.java))
        }
        binding.rowAi.setOnClickListener {
            startActivity(Intent(this, AiSettingsActivity::class.java))
        }
        binding.rowBackup.setOnClickListener {
            startActivity(Intent(this, BackupSettingsActivity::class.java))
        }
        binding.rowPrivacyPolicy.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }
        binding.rowAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
}
