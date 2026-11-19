package com.assistant.app.services

import com.assistant.app.models.VoiceError
import com.assistant.app.models.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fallback/test placeholder for [SpeechRecognitionService].
 */
class PlaceholderSpeechRecognitionService : SpeechRecognitionService {

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    override fun isAvailable(): Boolean = false

    override fun startListening(
        language: VoiceLanguage,
        onResult: (String) -> Unit,
        onPartialResult: ((String) -> Unit)?,
        onError: (VoiceError) -> Unit
    ) {
        _isListening.value = false
        onError(VoiceError.RecognizerUnavailable)
    }

    override fun stopListening() {
        _isListening.value = false
    }

    override fun cancelListening() {
        _isListening.value = false
    }

    override fun destroy() {
        _isListening.value = false
    }
}
