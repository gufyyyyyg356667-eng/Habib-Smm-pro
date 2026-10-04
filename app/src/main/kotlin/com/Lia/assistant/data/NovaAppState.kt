package com.Lia.assistant.data

import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NovaAppState(ctx: Context) {
    private val appCtx = ctx.applicationContext

    var themeMode by mutableStateOf(
        NovaPreferences.getString(appCtx, NovaPreferences.Keys.THEME_MODE, "DARK")
    )
        private set

    var reducedMotion by mutableStateOf(
        NovaPreferences.getBoolean(appCtx, NovaPreferences.Keys.REDUCED_MOTION, false)
    )
        private set

    var orbStyle by mutableStateOf(
        NovaPreferences.getString(appCtx, NovaPreferences.Keys.ORB_STYLE, "NOVA")
    )
        private set

    var personality by mutableStateOf(
        runCatching {
            Personality.valueOf(NovaPreferences.getString(appCtx, NovaPreferences.Keys.PERSONALITY, "NORMAL"))
        }.getOrDefault(Personality.NORMAL)
    )
        private set

    var assistantName by mutableStateOf(
        NovaPreferences.getString(appCtx, NovaPreferences.Keys.ASSISTANT_NAME, AssistantBrand.NAME)
    )
        private set

    var userName by mutableStateOf(
        NovaPreferences.getString(appCtx, NovaPreferences.Keys.USER_NAME, "")
    )
        private set

    var language by mutableStateOf(
        runCatching {
            LanguagePreference.valueOf(NovaPreferences.getString(appCtx, NovaPreferences.Keys.LANGUAGE, "AUTO"))
        }.getOrDefault(LanguagePreference.AUTO)
    )
        private set

    var voiceName by mutableStateOf(
        NovaPreferences.getString(appCtx, NovaPreferences.Keys.VOICE_NAME, "Aoede")
    )
        private set

    var speechSpeed by mutableStateOf(
        NovaPreferences.getFloat(appCtx, NovaPreferences.Keys.SPEECH_SPEED, 1.0f)
    )
        private set

    var voiceVolume by mutableStateOf(
        NovaPreferences.getFloat(appCtx, NovaPreferences.Keys.VOICE_VOLUME, 1.0f)
    )
        private set

    var hasOnboarded by mutableStateOf(
        NovaPreferences.getBoolean(appCtx, NovaPreferences.Keys.HAS_ONBOARDED, false)
    )
        private set

    // Apply methods
    fun applyThemeMode(mode: String) {
        themeMode = mode
        NovaPreferences.putString(appCtx, NovaPreferences.Keys.THEME_MODE, mode)
    }

    fun applyReducedMotion(value: Boolean) {
        reducedMotion = value
        NovaPreferences.putBoolean(appCtx, NovaPreferences.Keys.REDUCED_MOTION, value)
    }

    fun applyOrbStyle(style: String) {
        orbStyle = style
        NovaPreferences.putString(appCtx, NovaPreferences.Keys.ORB_STYLE, style)
    }

    fun applyPersonality(p: Personality) {
        personality = p
        NovaPreferences.putString(appCtx, NovaPreferences.Keys.PERSONALITY, p.name)
        CoroutineScope(Dispatchers.IO).launch {
            PersonalityRepository.setPersonality(appCtx, p)
        }
    }

    fun applyAssistantName(name: String) {
        assistantName = name.ifBlank { AssistantBrand.NAME }
        NovaPreferences.putString(appCtx, NovaPreferences.Keys.ASSISTANT_NAME, assistantName)
        CoroutineScope(Dispatchers.IO).launch {
            PersonalityRepository.setAssistantName(appCtx, assistantName)
        }
    }

    fun applyUserName(name: String) {
        userName = name
        NovaPreferences.putString(appCtx, NovaPreferences.Keys.USER_NAME, name)
    }

    fun applyLanguage(lang: LanguagePreference) {
        language = lang
        NovaPreferences.putString(appCtx, NovaPreferences.Keys.LANGUAGE, lang.name)
        CoroutineScope(Dispatchers.IO).launch {
            PersonalityRepository.setLanguage(appCtx, lang)
        }
    }

    fun applyVoiceName(name: String) {
        voiceName = name
        NovaPreferences.putString(appCtx, NovaPreferences.Keys.VOICE_NAME, name)
        CoroutineScope(Dispatchers.IO).launch {
            PersonalityRepository.setVoiceName(appCtx, name)
        }
    }

    fun applySpeechSpeed(speed: Float) {
        speechSpeed = speed
        NovaPreferences.putFloat(appCtx, NovaPreferences.Keys.SPEECH_SPEED, speed)
    }

    fun applyVoiceVolume(vol: Float) {
        voiceVolume = vol
        NovaPreferences.putFloat(appCtx, NovaPreferences.Keys.VOICE_VOLUME, vol)
    }

    fun applyOnboarded() {
        hasOnboarded = true
        NovaPreferences.putBoolean(appCtx, NovaPreferences.Keys.HAS_ONBOARDED, true)
    }
}
