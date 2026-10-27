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
import ayx.copy.databinding.ActivityEditCommandBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditCommandActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditCommandBinding
    private lateinit var db: AppDatabase
    private var existingCommand: Command? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditCommandBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(applicationContext)

        val commandId = intent.getLongExtra(EXTRA_COMMAND_ID, -1L)
        if (commandId == -1L) { finish(); return }

        loadCommand(commandId)

        // Char count watcher
        binding.etText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tvCharCount.text = "${s?.length ?: 0} characters"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // "..." paste trigger for own-app EditTexts
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
        binding.btnSave.setOnClickListener { saveChanges() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun loadCommand(id: Long) {
        lifecycleScope.launch {
            val command = withContext(Dispatchers.IO) { db.commandDao().getCommandById(id) }
            if (command == null) { finish(); return@launch }
            existingCommand = command
            binding.etCommand.setText(command.command)
            binding.etText.setText(command.replacementText)
            binding.tvCharCount.text = "${command.replacementText.length} characters"
        }
    }

    private fun saveChanges() {
        val original = existingCommand ?: return
        var command = binding.etCommand.text?.toString()?.trim().orEmpty()
        val text = binding.etText.text?.toString()?.trim().orEmpty()

        if (command.isEmpty()) { binding.tilCommand.error = getString(R.string.command_required); return }
        if (!command.startsWith(".") && !command.startsWith(",")) command = ".$command"
        if (text.isEmpty()) { binding.tilText.error = getString(R.string.text_required); return }

        binding.tilCommand.error = null
        binding.tilText.error = null

        lifecycleScope.launch {
            if (command != original.command) {
                val existing = withContext(Dispatchers.IO) { db.commandDao().getCommandByExactName(command) }
                if (existing != null && existing.id != original.id) {
                    binding.tilCommand.error = "This command already exists"
                    return@launch
                }
            }
            withContext(Dispatchers.IO) { db.commandDao().update(original.copy(command = command, replacementText = text)) }
            Toast.makeText(this@EditCommandActivity, getString(R.string.command_saved), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun confirmDelete() {
        val command = existingCommand ?: return
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.delete_confirm))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { db.commandDao().delete(command) }
                    Toast.makeText(this@EditCommandActivity, getString(R.string.command_deleted), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    companion object {
        const val EXTRA_COMMAND_ID = "extra_command_id"
    }
}
