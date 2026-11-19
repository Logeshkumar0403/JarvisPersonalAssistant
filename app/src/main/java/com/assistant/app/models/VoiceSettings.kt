package com.assistant.app.models

/**
 * Persisted configuration settings for voice interactions.
 */
data class VoiceSettings(
    val autoVoiceResponse: Boolean = false,
    val language: VoiceLanguage = VoiceLanguage.AUTO,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f
)
