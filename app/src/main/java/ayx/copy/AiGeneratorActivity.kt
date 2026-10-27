package ayx.copy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ayx.copy.databinding.ActivityAiGeneratorBinding
import ayx.copy.utils.GroqClient
import ayx.copy.utils.PrefManager
import kotlinx.coroutines.launch

class AiGeneratorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiGeneratorBinding
    private lateinit var prefManager: PrefManager
    private var mode = GroqClient.Mode.IMPROVE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefManager = PrefManager(applicationContext)

        binding.btnBack.setOnClickListener { finish() }
        selectMode(GroqClient.Mode.IMPROVE)
        binding.chipAiImprove.setOnClickListener { selectMode(GroqClient.Mode.IMPROVE) }
        binding.chipAiEnglish.setOnClickListener { selectMode(GroqClient.Mode.ENGLISH) }
        binding.chipAiRoman.setOnClickListener { selectMode(GroqClient.Mode.ROMAN) }
        binding.btnGenerate.setOnClickListener { generate() }
        binding.btnCopy.setOnClickListener {
            val out = binding.tvOutput.text?.toString().orEmpty()
            if (out.isBlank()) { Toast.makeText(this, "Pehle Generate karo", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("AyX AI", out))
            Toast.makeText(this, getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
        }
        binding.btnSaveAsCommand.setOnClickListener {
            val out = binding.tvOutput.text?.toString().orEmpty()
            if (out.isBlank()) { Toast.makeText(this, "Pehle Generate karo", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val intent = Intent(this, AddCommandActivity::class.java)
            intent.putExtra(AddCommandActivity.EXTRA_PREFILL_TEXT, out)
            startActivity(intent)
        }
    }

    private fun selectMode(newMode: GroqClient.Mode) {
        mode = newMode
        binding.chipAiImprove.isChecked = newMode == GroqClient.Mode.IMPROVE
        binding.chipAiEnglish.isChecked = newMode == GroqClient.Mode.ENGLISH
        binding.chipAiRoman.isChecked = newMode == GroqClient.Mode.ROMAN
    }

    private fun generate() {
        val input = binding.etInput.text?.toString()?.trim().orEmpty()
        if (input.isEmpty()) { Toast.makeText(this, "Text likho pehle", Toast.LENGTH_SHORT).show(); return }
        val key = prefManager.groqApiKey
        if (key.isBlank()) { Toast.makeText(this, getString(R.string.groq_key_missing), Toast.LENGTH_LONG).show(); return }
        binding.progressAi.visibility = View.VISIBLE
        binding.btnGenerate.isEnabled = false
        binding.tvOutput.text = getString(R.string.ai_generating)
        lifecycleScope.launch {
            try {
                val result = GroqClient.generate(key, mode, input)
                binding.tvOutput.text = result
            } catch (e: Exception) {
                binding.tvOutput.text = ""
                Toast.makeText(this@AiGeneratorActivity, "AI failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressAi.visibility = View.GONE
                binding.btnGenerate.isEnabled = true
            }
        }
    }
}
