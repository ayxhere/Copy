package ayx.copy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import ayx.copy.adapter.CommandAdapter
import ayx.copy.database.AppDatabase
import ayx.copy.database.Command
import ayx.copy.databinding.ActivityMainBinding
import ayx.copy.databinding.BottomSheetCommandOptionsBinding
import ayx.copy.utils.AnimationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CommandAdapter
    private lateinit var db: AppDatabase

    private var currentCategory = Category.ALL
    private var currentQuery = ""
    private var collectJob: kotlinx.coroutines.Job? = null

    enum class Category { ALL, FAVORITES, RECENT }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(applicationContext)

        setupRecyclerView()
        setupSearch()
        setupChips()
        setupFab()
        setupSettingsButton()
        setupAiButton()

        loadCommands()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    // ---------- Setup ----------

    private fun setupRecyclerView() {
        adapter = CommandAdapter(
            onItemLongClick = { command -> showOptionsSheet(command) },
            onFavoriteClick = { command -> toggleFavorite(command) },
            onCopyClick     = { command -> copyToClipboard(command) }
        )
        binding.rvCommands.layoutManager = LinearLayoutManager(this)
        binding.rvCommands.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString().orEmpty()
                binding.btnClearSearch.visibility =
                    if (currentQuery.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                loadCommands()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.setText("")
        }
    }

    private fun setupChips() {
        binding.chipAll.setOnClickListener {
            selectCategory(Category.ALL)
        }
        binding.chipFavorites.setOnClickListener {
            selectCategory(Category.FAVORITES)
        }
        binding.chipRecent.setOnClickListener {
            selectCategory(Category.RECENT)
        }
    }

    private fun selectCategory(category: Category) {
        currentCategory = category
        binding.chipAll.isChecked = category == Category.ALL
        binding.chipFavorites.isChecked = category == Category.FAVORITES
        binding.chipRecent.isChecked = category == Category.RECENT
        loadCommands()
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            AnimationUtil.scaleBounce(binding.fab)
            startActivity(Intent(this, AddCommandActivity::class.java))
            overridePendingTransition(R.anim.slide_up, android.R.anim.fade_out)
        }
    }

    private fun setupAiButton() {
        binding.btnAi.setOnClickListener {
            AnimationUtil.scaleBounce(binding.btnAi)
            startActivity(Intent(this, AiGeneratorActivity::class.java))
        }
    }

    private fun setupSettingsButton() {
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.ivServiceStatus.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    // ---------- Data loading ----------

    private fun loadCommands() {
        collectJob?.cancel()
        val flow: Flow<List<Command>> = when {
            currentQuery.isNotBlank() -> db.commandDao().searchCommands(currentQuery)
            currentCategory == Category.FAVORITES -> db.commandDao().getFavoriteCommands()
            currentCategory == Category.RECENT -> db.commandDao().getRecentCommands()
            else -> db.commandDao().getAllCommands()
        }

        collectJob = lifecycleScope.launch {
            flow.collectLatest { list ->
                adapter.submitList(list)
                binding.layoutEmpty.visibility =
                    if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.rvCommands.visibility =
                    if (list.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            }
        }
    }

    // ---------- Actions ----------

    private fun copyToClipboard(command: Command) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(command.command, command.replacementText))

        // Visible snackbar so user always sees the confirmation
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            "✓  ${command.command}  copied",
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).setBackgroundTint(getColor(R.color.card))
         .setTextColor(getColor(R.color.accent))
         .show()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.commandDao().incrementUseCount(command.id, System.currentTimeMillis())
            }
        }
    }

    private fun toggleFavorite(command: Command) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.commandDao().setFavorite(command.id, !command.isFavorite)
            }
        }
    }

    private fun showOptionsSheet(command: Command) {
        val sheetBinding = BottomSheetCommandOptionsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvSheetCommand.text = command.command

        sheetBinding.optionEdit.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, EditCommandActivity::class.java)
            intent.putExtra(EditCommandActivity.EXTRA_COMMAND_ID, command.id)
            startActivity(intent)
        }

        sheetBinding.optionCopy.setOnClickListener {
            dialog.dismiss()
            copyToClipboard(command)
        }

        sheetBinding.optionDuplicate.setOnClickListener {
            dialog.dismiss()
            duplicateCommand(command)
        }

        sheetBinding.optionShare.setOnClickListener {
            dialog.dismiss()
            shareCommand(command)
        }

        sheetBinding.optionDelete.setOnClickListener {
            dialog.dismiss()
            confirmDelete(command)
        }

        dialog.show()
    }

    private fun duplicateCommand(command: Command) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.commandDao().insert(
                    command.copy(
                        id = 0,
                        command = command.command + "_copy",
                        createdAt = System.currentTimeMillis(),
                        useCount = 0,
                        lastUsedAt = 0L
                    )
                )
            }
            Toast.makeText(this@MainActivity, getString(R.string.command_duplicated), Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCommand(command: Command) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, command.replacementText)
        }
        startActivity(Intent.createChooser(shareIntent, command.command))
    }

    private fun confirmDelete(command: Command) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.delete_confirm))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { db.commandDao().delete(command) }
                    Toast.makeText(this@MainActivity, getString(R.string.command_deleted), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ---------- Service status ----------

    private fun updateServiceStatus() {
        val isEnabled = isAccessibilityServiceEnabled()
        binding.ivServiceStatus.setImageResource(
            if (isEnabled) R.drawable.ic_dot_active else R.drawable.ic_dot_inactive
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any { it.id.contains(packageName) }
    }
}
