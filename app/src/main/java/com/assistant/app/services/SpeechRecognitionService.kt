package com.assistant.app.services

import com.assistant.app.models.VoiceError
import com.assistant.app.models.VoiceLanguage
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for speech-to-text recognition service.
 * Supports configurable languages (English, Tamil, Tanglish/Auto), lifecycle management,
 * and structured error reporting.
 */
interface SpeechRecognitionService {

    /**
     * Observable state indicating whether the recognizer is actively listening.
     */
    val isListening: StateFlow<Boolean>

    /**
     * Checks whether speech recognition capabilities are available on the device.
     */
    fun isAvailable(): Boolean

    /**
     * Starts listening for speech input using the specified [language] configuration.
     * Emits final transcribed text via [onResult] or errors via [onError].
     */
    fun startListening(
        language: VoiceLanguage = VoiceLanguage.AUTO,
        onResult: (String) -> Unit,
        onPartialResult: ((String) -> Unit)? = null,
        onError: (VoiceError) -> Unit
    )

    /**
     * Stops listening and processes any audio captured so far.
     */
    fun stopListening()

    /**
     * Cancels the active speech recognition session immediately.
     */
    fun cancelListening()

    /**
     * Cleans up and releases the underlying SpeechRecognizer instance.
     */
    fun destroy()
}
