package ayx.copy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ayx.copy.database.AppDatabase
import ayx.copy.database.Command
import ayx.copy.databinding.ActivitySettingsBinding
import ayx.copy.utils.BiometricHelper
import ayx.copy.utils.PrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefManager: PrefManager
    private lateinit var db: AppDatabase

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { exportToUri(it) } }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { importFromUri(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(applicationContext)
        db = AppDatabase.getInstance(applicationContext)

        binding.btnBack.setOnClickListener { finish() }

        binding.switchAi.isChecked = prefManager.aiEnabled
        binding.etGroqKey.setText(prefManager.groqApiKey)
        binding.switchAi.setOnCheckedChangeListener { _, v -> prefManager.aiEnabled = v }
        binding.btnSaveAiKey.setOnClickListener {
            prefManager.groqApiKey = binding.etGroqKey.text?.toString()?.trim().orEmpty()
            Toast.makeText(this, getString(R.string.groq_key_saved), Toast.LENGTH_SHORT).show()
        }

        binding.switchAutoReplace.isChecked = prefManager.autoReplaceEnabled
        binding.switchAutoPaste.isChecked   = prefManager.autoPasteEnabled
        binding.switchAppLock.isChecked     = prefManager.appLockEnabled

        binding.switchAutoReplace.setOnCheckedChangeListener { _, v -> prefManager.autoReplaceEnabled = v }
        binding.switchAutoPaste.setOnCheckedChangeListener   { _, v -> prefManager.autoPasteEnabled   = v }

        binding.switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) enableAppLock() else disableAppLock()
        }

        binding.btnEnableService.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.rowExport.setOnClickListener { exportLauncher.launch("ayx.json") }
        binding.rowImport.setOnClickListener { importLauncher.launch("*/*") }
        binding.rowPrivacyPolicy.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        binding.switchAppLock.isChecked = prefManager.appLockEnabled
    }

    // ---------- App Lock ----------

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

    // ---------- Accessibility ----------

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

    // ---------- Backup (.ayx) ----------

    private fun exportToUri(uri: Uri) {
        lifecycleScope.launch {
            val commands = withContext(Dispatchers.IO) { db.commandDao().getAllCommandsSync() }
            val root = JSONObject()
            root.put("app", "AyX Copy")
            root.put("version", 2)
            val array = JSONArray()
            for (c in commands) {
                val obj = JSONObject()
                obj.put("command", c.command)
                obj.put("replacementText", c.replacementText)
                obj.put("isFavorite", c.isFavorite)
                array.put(obj)
            }
            root.put("commands", array)
            withContext(Dispatchers.IO) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    OutputStreamWriter(out).use { it.write(root.toString(2)) }
                }
            }
            Toast.makeText(this@SettingsActivity, getString(R.string.backup_exported), Toast.LENGTH_SHORT).show()
        }
    }

    private fun importFromUri(uri: Uri) {
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { BufferedReader(InputStreamReader(it)).readText() }
            } ?: return@launch
            try {
                // Accepts 3 shapes:
                //  ayx.json:      {"commands": [{"command","replacementText","isFavorite"}]}
                //  pasteme:       {"templates": [{"shortcut","content","isFavourite"}]}
                //  plain array:   [{"command","replacementText"}] or pasteme objects
                val raw: List<JSONObject> = if (text.trimStart().startsWith("{")) {
                    val root = JSONObject(text)
                    val arr = when {
                        root.has("commands") -> root.getJSONArray("commands")
                        root.has("templates") -> root.getJSONArray("templates")
                        else -> throw IllegalArgumentException("No commands/templates array")
                    }
                    (0 until arr.length()).map { arr.getJSONObject(it) }
                } else {
                    val arr = JSONArray(text)
                    (0 until arr.length()).map { arr.getJSONObject(it) }
                }
                val toInsert = raw.mapNotNull { obj ->
                    val cmd = obj.optString("command").ifBlank { obj.optString("shortcut") }
                    val txt = obj.optString("replacementText").ifBlank { obj.optString("content") }
                    if (cmd.isBlank() || txt.isBlank()) null
                    else Command(
                        command = cmd,
                        replacementText = txt,
                        isFavorite = obj.optBoolean("isFavorite", obj.optBoolean("isFavourite", obj.optBoolean("isPinned", false)))
                    )
                }
                var inserted = 0
                withContext(Dispatchers.IO) {
                    for (item in toInsert) {
                        // Exact-name dedupe: ".8" and ",8" stay separate rows,
                        // re-importing the same file does not duplicate rows.
                        if (db.commandDao().getCommandByExactName(item.command) == null) {
                            db.commandDao().insert(item)
                            inserted++
                        }
                    }
                }
                Toast.makeText(this@SettingsActivity, "$inserted commands imported", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Invalid backup file", Toast.LENGTH_SHORT).show()
            }
        }
    }
