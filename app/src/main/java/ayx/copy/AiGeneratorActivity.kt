package ayx.copy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ayx.copy.database.AppDatabase
import ayx.copy.database.Command
import ayx.copy.databinding.ActivityAiGeneratorBinding
import ayx.copy.utils.GroqClient
import ayx.copy.utils.PrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            showSaveDialog(out)
        }
    }

    private fun showSaveDialog(reply: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_save_command, null)
        val tilName = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilCommandName)
        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCommandName)
        val etReply = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etReplyText)
        etReply.setText(reply)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.btnDialogCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.btnDialogSave).setOnClickListener {
            var name = etName.text?.toString()?.trim().orEmpty()
            val text = etReply.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                tilName.error = getString(R.string.command_required)
                return@setOnClickListener
            }
            if (!name.startsWith(".") && !name.startsWith(",")) name = ".$name"
            if (text.isEmpty()) {
                Toast.makeText(this, getString(R.string.text_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            tilName.error = null
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                val exists = withContext(Dispatchers.IO) {
                    db.commandDao().getCommandByExactName(name)
                }
                if (exists != null) {
                    tilName.error = getString(R.string.ai_command_exists)
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    db.commandDao().insert(Command(command = name, replacementText = text))
                }
                dialog.dismiss()
                Toast.makeText(this@AiGeneratorActivity, getString(R.string.command_saved), Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
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
