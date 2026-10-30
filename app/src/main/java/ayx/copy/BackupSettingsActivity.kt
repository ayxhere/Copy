package ayx.copy

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ayx.copy.database.AppDatabase
import ayx.copy.database.Command
import ayx.copy.databinding.ActivitySettingsBackupBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class BackupSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBackupBinding
    private lateinit var db: AppDatabase

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { exportToUri(it) } }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { importFromUri(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(applicationContext)

        binding.btnBack.setOnClickListener { finish() }
        binding.rowExport.setOnClickListener { exportLauncher.launch("ayx.json") }
        binding.rowImport.setOnClickListener { importLauncher.launch("*/*") }
    }

    private fun exportToUri(uri: Uri) {
        lifecycleScope.launch {
            val commands = withContext(Dispatchers.IO) { db.commandDao().getAllCommandsSync() }
            val root = JSONObject()
            root.put("app", "AyX Paste")
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
            Toast.makeText(this@BackupSettingsActivity, getString(R.string.backup_exported), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@BackupSettingsActivity, "$inserted commands imported", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@BackupSettingsActivity, "Invalid backup file", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
