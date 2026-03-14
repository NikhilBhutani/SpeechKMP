package dev.deviceai.demo

/**
 * Lightweight KMP key-value preferences.
 * Android: SharedPreferences. iOS: NSUserDefaults.
 * Must call [init] on Android before use (in MainActivity.onCreate).
 */
internal expect object AppPrefs {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

internal const val PREF_ACTIVE_VOICE_ID = "active_voice_id"
internal const val PREF_ACTIVE_CHAT_ID  = "active_chat_id"
