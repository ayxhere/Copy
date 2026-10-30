package ayx.copy.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import ayx.copy.database.AppDatabase
import ayx.copy.database.Command
import ayx.copy.utils.PrefManager
import ayx.copy.utils.GroqClient
import ayx.copy.utils.ReplaceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CopyAccessibilityService : AccessibilityService() {

    private lateinit var prefManager: PrefManager
    private lateinit var db: AppDatabase
    private lateinit var clipboardManager: ClipboardManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    // In-memory cache of commands, refreshed periodically to avoid a DB hit
    // on every single keystroke.
    private var commandCache: List<Command> = emptyList()
    private var lastCacheRefresh = 0L
    private val cacheTtlMs = 5000L

    // Avoid re-processing the same node/text combo repeatedly while the
    // replacement itself triggers a new TYPE_VIEW_TEXT_CHANGED event.
    private var isReplacing = false
    private var isAiProcessing = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefManager = PrefManager(applicationContext)
        db = AppDatabase.getInstance(applicationContext)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        refreshCache()
        Log.d(TAG, "AyX Paste accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (isReplacing) return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return

        val source = event.source ?: return

        maybeRefreshCache()

        val currentText = source.text?.toString() ?: run {
            source.recycle()
            return
        }

        // 0. AI triggers FIRST: "..ai / ..ea / ..ra" at the end.
        //    They must win over ".." quick-copy and over any saved command.
        if (!isAiProcessing && prefManager.aiEnabled) {
            val ai = GroqClient.detectTrigger(currentText)
            if (ai != null) {
                performAi(source, currentText, ai)
                return
            }
        }

        // 1. Saved commands take priority (exact, user-defined list).
        if (prefManager.autoReplaceEnabled) {
            val match = ReplaceHelper.findMatch(currentText, commandCache)
            if (match != null) {
                performReplacement(source, currentText, match)
                return
            }
        }

        // 2. Quick Copy / Quick Paste (generic ".." triggers, release v2).
        if (prefManager.autoPasteEnabled) {
            val pasteMatch = ReplaceHelper.findQuickPasteMatch(currentText)
            if (pasteMatch != null) {
                performQuickPaste(source, currentText, pasteMatch)
                return
            }
            val copyMatch = ReplaceHelper.findQuickCopyMatch(currentText)
            if (copyMatch != null) {
                performQuickCopy(source, currentText, copyMatch)
                return
            }
        }

        source.recycle()
    }

    private fun performAi(
        node: AccessibilityNodeInfo,
        currentText: String,
        ai: GroqClient.AiRequest
    ) {
        // Do NOT recycle node here: the async Groq call still needs it.
        val key = prefManager.groqApiKey
        if (key.isBlank()) {
            Toast.makeText(applicationContext, "Pehle AyX Paste > Settings me Groq key dalo", Toast.LENGTH_LONG).show()
            setNodeText(node, ai.input, onSuccess = null)
            return
        }
        isAiProcessing = true
        serviceScope.launch {
            try {
                val result = GroqClient.generate(key, ai.mode, ai.input)
                setNodeText(node, result, onSuccess = null)
            } catch (e: Exception) {
                Log.w(TAG, "Groq failed", e)
                Toast.makeText(applicationContext, "AI failed: ${e.message}", Toast.LENGTH_LONG).show()
                // Trigger hata do, original text wapas rakho — loop na bane.
                setNodeText(node, ai.input, onSuccess = null)
            } finally {
                isAiProcessing = false
            }
        }
    }

    private fun performReplacement(
        node: AccessibilityNodeInfo,
        currentText: String,
        match: ReplaceHelper.MatchResult
    ) {
        val newText = ReplaceHelper.buildReplacedText(currentText, match)
        setNodeText(node, newText) {
            serviceScope.launch {
                withContext(Dispatchers.IO) {
                    db.commandDao().incrementUseCount(match.command.id, System.currentTimeMillis())
                }
            }
        }
    }

    private fun performQuickCopy(
        node: AccessibilityNodeInfo,
        currentText: String,
        match: ReplaceHelper.QuickCopyMatch
    ) {
        // Copy everything before ".." to clipboard, then clear the entire field
        clipboardManager.setPrimaryClip(
            ClipData.newPlainText("Quick Copy", match.fullTextToCopy)
        )
        setNodeText(node, ReplaceHelper.buildAfterQuickCopy(), onSuccess = null)
    }

    private fun performQuickPaste(
        node: AccessibilityNodeInfo,
        currentText: String,
        match: ReplaceHelper.QuickPasteMatch
    ) {
        val clipText = clipboardManager.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(applicationContext)
            ?.toString()
            ?: run {
                node.recycle()
                return
            }

        val newText = ReplaceHelper.buildAfterQuickPaste(currentText, match, clipText)
        setNodeText(node, newText, onSuccess = null)
    }

    /** Shared helper: sets [newText] on [node] via the accessibility API, moves the cursor to the end, and recycles the node. */
    private fun setNodeText(node: AccessibilityNodeInfo, newText: String, onSuccess: (() -> Unit)?) {
        isReplacing = true
        try {
            val arguments = Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            )
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

            if (success) {
                val selectionArgs = Bundle()
                selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newText.length)
                selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newText.length)
                node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
                onSuccess?.invoke()
            } else {
                Log.w(TAG, "Failed to set text via ACTION_SET_TEXT")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing replacement", e)
        } finally {
            node.recycle()
            isReplacing = false
        }
    }

    private fun maybeRefreshCache() {
        val now = System.currentTimeMillis()
        if (now - lastCacheRefresh > cacheTtlMs) {
            refreshCache()
        }
    }

    private fun refreshCache() {
        lastCacheRefresh = System.currentTimeMillis()
        serviceScope.launch {
            val list = withContext(Dispatchers.IO) {
                db.commandDao().getAllCommandsSync()
            }
            commandCache = list
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "AyX Paste accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.coroutineContext[Job]?.cancel()
    }

    companion object {
        private const val TAG = "CopyAccessibility"
    }
}
