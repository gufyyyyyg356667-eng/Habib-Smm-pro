package com.Lia.assistant.data

import android.content.Context
import android.content.SharedPreferences

// ── Brand ──────────────────────────────────────────────────────────────────
object AssistantBrand {
    const val NAME = "Lia"
    const val FULL_NAME = "Lia AI"
    const val TAGLINE = "Your Intelligent AI Companion"
}

// ── API Key ────────────────────────────────────────────────────────────────
object ApiKeyStore {
    private const val PREFS = "nova_prefs"
    private const val KEY_GEMINI = "gemini_api_key"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getKey(ctx: Context): String = prefs(ctx).getString(KEY_GEMINI, "") ?: ""
    fun saveKey(ctx: Context, key: String) { prefs(ctx).edit().putString(KEY_GEMINI, key.trim()).apply() }
    fun hasKey(ctx: Context): Boolean = getKey(ctx).isNotBlank()
    fun clearKey(ctx: Context) { prefs(ctx).edit().remove(KEY_GEMINI).apply() }
}

// ── Preferences ────────────────────────────────────────────────────────────
object NovaPreferences {
    private const val PREFS = "nova_prefs"

    object Keys {
        const val THEME_MODE = "theme_mode"
        const val REDUCED_MOTION = "reduced_motion"
        const val ORB_STYLE = "orb_style"
        const val PERSONALITY = "personality"
        const val USER_NAME = "user_name"
        const val VOICE_INPUT_ENABLED = "voice_input_enabled"
        const val WAKE_WORD_ENABLED = "wake_word_enabled"
        const val LANGUAGE = "language"
        const val HAS_ONBOARDED = "has_onboarded"
        const val VOICE_NAME = "voice_name"
        const val RESPONSE_STYLE = "response_style"
        const val SPEECH_SPEED = "speech_speed"
        const val VOICE_VOLUME = "voice_volume"
        const val ASSISTANT_NAME = "assistant_name"
    }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getString(ctx: Context, key: String, default: String = ""): String =
        prefs(ctx).getString(key, default) ?: default

    fun putString(ctx: Context, key: String, value: String) {
        prefs(ctx).edit().putString(key, value).apply()
    }

    fun getBoolean(ctx: Context, key: String, default: Boolean = false): Boolean =
        prefs(ctx).getBoolean(key, default)

    fun putBoolean(ctx: Context, key: String, value: Boolean) {
        prefs(ctx).edit().putBoolean(key, value).apply()
    }

    fun getFloat(ctx: Context, key: String, default: Float = 0f): Float =
        prefs(ctx).getFloat(key, default)

    fun putFloat(ctx: Context, key: String, value: Float) {
        prefs(ctx).edit().putFloat(key, value).apply()
    }
}
