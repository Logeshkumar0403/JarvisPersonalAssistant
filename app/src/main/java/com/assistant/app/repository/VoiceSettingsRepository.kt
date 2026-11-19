package com.assistant.app.repository

import android.content.Context
import android.content.SharedPreferences
import com.assistant.app.models.VoiceLanguage
import com.assistant.app.models.VoiceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository interface for managing and persisting voice settings.
 */
interface VoiceSettingsRepository {
    val settings: StateFlow<VoiceSettings>
    fun setAutoVoiceResponse(enabled: Boolean)
    fun setLanguage(language: VoiceLanguage)
    fun setSpeechRate(rate: Float)
    fun setPitch(pitch: Float)
}

/**
 * SharedPreferences-backed implementation of [VoiceSettingsRepository].
 * Ensures fast, synchronous, persistent storage across application launches.
 */
class DefaultVoiceSettingsRepository(
    context: Context,
    prefsName: String = "assistant_voice_settings"
) : VoiceSettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    override val settings: StateFlow<VoiceSettings> = _settings.asStateFlow()

    private fun loadSettings(): VoiceSettings {
        val autoResponse = prefs.getBoolean(KEY_AUTO_VOICE_RESPONSE, false)
        val langStr = prefs.getString(KEY_SPEECH_LANGUAGE, VoiceLanguage.AUTO.name)
        val language = try {
            VoiceLanguage.valueOf(langStr ?: VoiceLanguage.AUTO.name)
        } catch (_: Exception) {
            VoiceLanguage.AUTO
        }
        val rate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        val pitch = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)

        return VoiceSettings(
            autoVoiceResponse = autoResponse,
            language = language,
            speechRate = rate,
            pitch = pitch
        )
    }

    override fun setAutoVoiceResponse(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_VOICE_RESPONSE, enabled).apply()
        _settings.value = _settings.value.copy(autoVoiceResponse = enabled)
    }

    override fun setLanguage(language: VoiceLanguage) {
        prefs.edit().putString(KEY_SPEECH_LANGUAGE, language.name).apply()
        _settings.value = _settings.value.copy(language = language)
    }

    override fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_SPEECH_RATE, clamped).apply()
        _settings.value = _settings.value.copy(speechRate = clamped)
    }

    override fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_SPEECH_PITCH, clamped).apply()
        _settings.value = _settings.value.copy(pitch = clamped)
    }

    companion object {
        private const val KEY_AUTO_VOICE_RESPONSE = "key_auto_voice_response"
        private const val KEY_SPEECH_LANGUAGE = "key_speech_language"
        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"
    }
}
