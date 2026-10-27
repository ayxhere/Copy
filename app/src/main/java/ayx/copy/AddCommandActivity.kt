package ayx.copy

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ayx.copy.database.AppDatabase
import ayx.copy.database.Command
import ayx.copy.databinding.ActivityAddCommandBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddCommandActivity : AppCompatActivity() {

    companion object { const val EXTRA_PREFILL_TEXT = "extra_prefill_text" }

    private lateinit var binding: ActivityAddCommandBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCommandBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(applicationContext)

        intent.getStringExtra(EXTRA_PREFILL_TEXT)?.takeIf { it.isNotBlank() }?.let {
            binding.etText.setText(it)
        }

        // Char count watcher for the replacement text field
        binding.etText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tvCharCount.text = "${s?.length ?: 0} characters"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // "..." paste trigger — works inside our own app where
        // the accessibility service cannot fire on its own views.
        // Type "... " (three dots + space) to paste clipboard content.
        val pasteWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: return
                if (text.endsWith("... ") || text.endsWith("...\n")) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip
                        ?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)?.coerceToText(applicationContext)?.toString()
                        ?: return
                    val newText = text.dropLast(4) + clip
                    // Temporarily remove this watcher to avoid recursive triggering
                    binding.etText.removeTextChangedListener(this)
                    binding.etText.setText(newText)
                    binding.etText.setSelection(newText.length)
                    binding.etText.addTextChangedListener(this)
                }
            }
        }
        binding.etText.addTextChangedListener(pasteWatcher)
        binding.etCommand.addTextChangedListener(pasteWatcher)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveCommand() }
    }

    private fun saveCommand() {
        var command = binding.etCommand.text?.toString()?.trim().orEmpty()
        val text = binding.etText.text?.toString()?.trim().orEmpty()

        if (command.isEmpty()) {
            binding.tilCommand.error = getString(R.string.command_required)
            return
        }
        if (!command.startsWith(".") && !command.startsWith(",")) {
            command = ".$command"
        }
        if (text.isEmpty()) {
            binding.tilText.error = getString(R.string.text_required)
            return
        }

        binding.tilCommand.error = null
        binding.tilText.error = null

        lifecycleScope.launch {
            val existing = withContext(Dispatchers.IO) {
                db.commandDao().getCommandByExactName(command)
            }
            if (existing != null) {
                binding.tilCommand.error = "This command already exists"
                return@launch
            }

            withContext(Dispatchers.IO) {
                db.commandDao().insert(Command(command = command, replacementText = text))
            }
            Toast.makeText(this@AddCommandActivity, getString(R.string.command_saved), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
