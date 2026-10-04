package com.Lia.assistant.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("assistant_settings")

object PersonalityRepository {
    private val KEY_PERSONALITY = stringPreferencesKey("personality")
    private val KEY_LANGUAGE = stringPreferencesKey("language")
    private val KEY_ASSISTANT_NAME = stringPreferencesKey("assistant_name")
    private val KEY_VOICE_NAME = stringPreferencesKey("voice_name")

    fun personalityFlow(ctx: Context): Flow<Personality> =
        ctx.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                val name = prefs[KEY_PERSONALITY]
                    ?: NovaPreferences.getString(ctx, NovaPreferences.Keys.PERSONALITY, Personality.NORMAL.name)
                runCatching { Personality.valueOf(name) }.getOrDefault(Personality.NORMAL)
            }

    fun languageFlow(ctx: Context): Flow<LanguagePreference> =
        ctx.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                val name = prefs[KEY_LANGUAGE]
                    ?: NovaPreferences.getString(ctx, NovaPreferences.Keys.LANGUAGE, LanguagePreference.AUTO.name)
                runCatching { LanguagePreference.valueOf(name) }.getOrDefault(LanguagePreference.AUTO)
            }

    fun assistantNameFlow(ctx: Context): Flow<String> =
        ctx.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                prefs[KEY_ASSISTANT_NAME]
                    ?: NovaPreferences.getString(ctx, NovaPreferences.Keys.ASSISTANT_NAME, AssistantBrand.NAME)
            }

    fun voiceNameFlow(ctx: Context): Flow<String> =
        ctx.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                prefs[KEY_VOICE_NAME]
                    ?: NovaPreferences.getString(ctx, NovaPreferences.Keys.VOICE_NAME, "Aoede")
            }

    suspend fun setPersonality(ctx: Context, personality: Personality) {
        ctx.dataStore.edit { it[KEY_PERSONALITY] = personality.name }
        NovaPreferences.putString(ctx, NovaPreferences.Keys.PERSONALITY, personality.name)
        refreshVoiceSession(ctx)
    }

    suspend fun setLanguage(ctx: Context, language: LanguagePreference) {
        ctx.dataStore.edit { it[KEY_LANGUAGE] = language.name }
        NovaPreferences.putString(ctx, NovaPreferences.Keys.LANGUAGE, language.name)
        refreshVoiceSession(ctx)
    }

    suspend fun setAssistantName(ctx: Context, name: String) {
        ctx.dataStore.edit { it[KEY_ASSISTANT_NAME] = name }
        NovaPreferences.putString(ctx, NovaPreferences.Keys.ASSISTANT_NAME, name)
        refreshVoiceSession(ctx)
    }

    suspend fun setVoiceName(ctx: Context, voiceName: String) {
        ctx.dataStore.edit { it[KEY_VOICE_NAME] = voiceName }
        NovaPreferences.putString(ctx, NovaPreferences.Keys.VOICE_NAME, voiceName)
        refreshVoiceSession(ctx)
    }

    private fun refreshVoiceSession(ctx: Context) {
        // Late binding to avoid circular dependency at init time
        com.Lia.assistant.voice.VoiceSessionManager.refreshInstructions(ctx)
    }
}
