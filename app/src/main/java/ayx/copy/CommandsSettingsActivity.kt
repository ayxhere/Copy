package ayx.copy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ayx.copy.databinding.ActivitySettingsCommandsBinding
import ayx.copy.utils.PrefManager

class CommandsSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsCommandsBinding
    private lateinit var prefManager: PrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsCommandsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(applicationContext)

        binding.btnBack.setOnClickListener { finish() }

        binding.switchAutoReplace.isChecked = prefManager.autoReplaceEnabled
        binding.switchAutoPaste.isChecked = prefManager.autoPasteEnabled

        binding.switchAutoReplace.setOnCheckedChangeListener { _, v -> prefManager.autoReplaceEnabled = v }
        binding.switchAutoPaste.setOnCheckedChangeListener { _, v -> prefManager.autoPasteEnabled = v }
    }
}
