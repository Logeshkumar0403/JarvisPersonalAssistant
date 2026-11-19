package com.assistant.app.services

import com.assistant.app.models.VoiceError
import com.assistant.app.models.VoiceLanguage
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for Text-To-Speech (TTS) synthesis.
 * Handles voice configuration, Tamil/English language availability checks,
 * utterance tracking, and stop controls.
 */
interface TextToSpeechService {

    /**
     * Flow tracking whether audio synthesis is actively playing.
     */
    val isSpeaking: StateFlow<Boolean>

    /**
     * Flow tracking the ID of the message currently being spoken aloud, or null if idle.
     */
    val currentlySpeakingMessageId: StateFlow<String?>

    /**
     * Checks whether the TTS engine is initialized and available.
     */
    fun isAvailable(): Boolean

    /**
     * Speaks the given [text] aloud associated with [messageId].
     * Configures pitch, speechRate, and handles Tamil/English language fallbacks.
     */
    fun speak(
        text: String,
        messageId: String,
        language: VoiceLanguage = VoiceLanguage.AUTO,
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (VoiceError) -> Unit = {}
    )

    /**
     * Immediately stops ongoing speech output.
     */
    fun stop()

    /**
     * Releases resources held by the TTS engine.
     */
    fun shutdown()
}
