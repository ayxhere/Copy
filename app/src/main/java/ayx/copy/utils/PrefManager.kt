package ayx.copy.utils

import android.content.Context
import android.content.SharedPreferences

class PrefManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("copyme_prefs", Context.MODE_PRIVATE)

    var autoReplaceEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_REPLACE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_REPLACE, value).apply()

    var autoPasteEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PASTE, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_PASTE, value).apply()

    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK, value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    var groqApiKey: String
        get() = prefs.getString(KEY_GROQ_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_GROQ_KEY, value).apply()

    var aiEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AI_ENABLED, value).apply()

    var lastSelectedCategory: String
        get() = prefs.getString(KEY_LAST_CATEGORY, "all") ?: "all"
        set(value) = prefs.edit().putString(KEY_LAST_CATEGORY, value).apply()

    companion object {
        private const val KEY_AUTO_REPLACE   = "auto_replace_enabled"
        private const val KEY_AUTO_PASTE      = "auto_paste_enabled"
        private const val KEY_APP_LOCK        = "app_lock_enabled"
        private const val KEY_FIRST_LAUNCH    = "first_launch"
        private const val KEY_LAST_CATEGORY   = "last_category"
        private const val KEY_GROQ_KEY        = "groq_api_key"
        private const val KEY_AI_ENABLED      = "ai_enabled"
    }
}
