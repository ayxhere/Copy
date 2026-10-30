package ayx.copy

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ayx.copy.databinding.ActivitySettingsAiBinding
import ayx.copy.utils.PrefManager

class AiSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsAiBinding
    private lateinit var prefManager: PrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsAiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(applicationContext)

        binding.btnBack.setOnClickListener { finish() }

        binding.switchAi.isChecked = prefManager.aiEnabled
        binding.etGroqKey.setText(prefManager.groqApiKey)
        binding.switchAi.setOnCheckedChangeListener { _, v -> prefManager.aiEnabled = v }
        binding.btnSaveAiKey.setOnClickListener {
            prefManager.groqApiKey = binding.etGroqKey.text?.toString()?.trim().orEmpty()
            Toast.makeText(this, getString(R.string.groq_key_saved), Toast.LENGTH_SHORT).show()
        }
    }
}
