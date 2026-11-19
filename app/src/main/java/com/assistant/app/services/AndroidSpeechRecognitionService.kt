package com.assistant.app.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.assistant.app.models.VoiceError
import com.assistant.app.models.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Concrete implementation of [SpeechRecognitionService] utilizing Android's [SpeechRecognizer].
 * Configures free-form models, language preferences (English, Tamil, Tanglish/Auto),
 * and maps low-level system error codes to structured [VoiceError] domain states.
 */
class AndroidSpeechRecognitionService(
    private val context: Context
) : SpeechRecognitionService {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var activeOnResult: ((String) -> Unit)? = null
    private var activeOnPartialResult: ((String) -> Unit)? = null
    private var activeOnError: ((VoiceError) -> Unit)? = null
    private var isExplicitCancellation = false

    override fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun startListening(
        language: VoiceLanguage,
        onResult: (String) -> Unit,
        onPartialResult: ((String) -> Unit)?,
        onError: (VoiceError) -> Unit
    ) {
        mainHandler.post {
            if (!isAvailable()) {
                _isListening.value = false
                onError(VoiceError.RecognizerUnavailable)
                return@post
            }

            activeOnResult = onResult
            activeOnPartialResult = onPartialResult
            activeOnError = onError
            isExplicitCancellation = false

            try {
                // Ensure previous recognizer instance is torn down cleanly
                tearDownRecognizer()

                val recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
                    speechRecognizer = it
                }

                recognizer.setRecognitionListener(createRecognitionListener())

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    when (language) {
                        VoiceLanguage.TAMIL -> {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ta-IN")
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ta-IN")
                        }
                        VoiceLanguage.ENGLISH -> {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                        }
                        VoiceLanguage.AUTO -> {
                            // Support English, Tamil, and Tanglish (Tamil-English mixed speech)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                            putExtra(
                                "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",
                                arrayOf("ta-IN", "en-US", "en-IN")
                            )
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ta-IN")
                        }
                    }
                }

                recognizer.startListening(intent)
                _isListening.value = true
            } catch (e: Exception) {
                _isListening.value = false
                onError(VoiceError.Other("Failed to start speech recognizer: ${e.localizedMessage}"))
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            _isListening.value = false
        }
    }

    override fun cancelListening() {
        mainHandler.post {
            isExplicitCancellation = true
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            _isListening.value = false
        }
    }

    override fun destroy() {
        mainHandler.post {
            tearDownRecognizer()
            _isListening.value = false
        }
    }

    private fun tearDownRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            speechRecognizer = null
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(errorCode: Int) {
                _isListening.value = false

                if (isExplicitCancellation && errorCode == SpeechRecognizer.ERROR_CLIENT) {
                    return
                }

                val voiceError = when (errorCode) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceError.PermissionDenied
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    SpeechRecognizer.ERROR_SERVER -> VoiceError.NetworkError
                    SpeechRecognizer.ERROR_NO_MATCH -> VoiceError.NoSpeechDetected
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceError.RecognitionTimeout
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        tearDownRecognizer()
                        VoiceError.Other("Speech recognition service was busy. Please try again.")
                    }
                    SpeechRecognizer.ERROR_AUDIO -> VoiceError.Other("Audio recording failed. Check microphone.")
                    SpeechRecognizer.ERROR_CLIENT -> {
                        if (isExplicitCancellation) VoiceError.UserCancelled
                        else VoiceError.Other("Client error during recognition ($errorCode).")
                    }
                    else -> VoiceError.Other("Recognition error encountered ($errorCode).")
                }

                activeOnError?.invoke(voiceError)
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull { it.isNotBlank() }

                if (!recognizedText.isNullOrBlank()) {
                    activeOnResult?.invoke(recognizedText)
                } else {
                    activeOnError?.invoke(VoiceError.NoSpeechDetected)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull { it.isNotBlank() }
                if (!partialText.isNullOrBlank()) {
                    activeOnPartialResult?.invoke(partialText)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
