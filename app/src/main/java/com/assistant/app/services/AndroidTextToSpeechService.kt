package com.assistant.app.services

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.assistant.app.models.VoiceError
import com.assistant.app.models.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Concrete implementation of [TextToSpeechService] wrapping Android's [TextToSpeech].
 * Handles language detection (English vs Tamil), graceful voice fallback,
 * utterance tracking, and stop controls.
 */
class AndroidTextToSpeechService(
    private val context: Context
) : TextToSpeechService {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentlySpeakingMessageId = MutableStateFlow<String?>(null)
    override val currentlySpeakingMessageId: StateFlow<String?> = _currentlySpeakingMessageId.asStateFlow()

    private var activeOnStart: (() -> Unit)? = null
    private var activeOnDone: (() -> Unit)? = null
    private var activeOnError: ((VoiceError) -> Unit)? = null

    init {
        initializeTts()
    }

    private fun initializeTts() {
        mainHandler.post {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    setupUtteranceListener()
                } else {
                    isInitialized = false
                }
            }
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post {
                    _isSpeaking.value = true
                    _currentlySpeakingMessageId.value = utteranceId
                    activeOnStart?.invoke()
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    _isSpeaking.value = false
                    _currentlySpeakingMessageId.value = null
                    activeOnDone?.invoke()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    _isSpeaking.value = false
                    _currentlySpeakingMessageId.value = null
                    activeOnError?.invoke(VoiceError.Other("TTS playback error for utterance $utteranceId"))
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post {
                    _isSpeaking.value = false
                    _currentlySpeakingMessageId.value = null
                    activeOnError?.invoke(VoiceError.Other("TTS playback error ($errorCode)"))
                }
            }
        })
    }

    override fun isAvailable(): Boolean = isInitialized && tts != null

    override fun speak(
        text: String,
        messageId: String,
        language: VoiceLanguage,
        pitch: Float,
        speechRate: Float,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (VoiceError) -> Unit
    ) {
        mainHandler.post {
            val engine = tts
            if (!isInitialized || engine == null) {
                _isSpeaking.value = false
                _currentlySpeakingMessageId.value = null
                onError(VoiceError.TtsUnavailable)
                return@post
            }

            activeOnStart = onStart
            activeOnDone = onDone
            activeOnError = onError

            // Configure target locale based on language and text content
            val targetLocale = resolveLocale(language, text, onError)

            try {
                engine.setLanguage(targetLocale)
                engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
                engine.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))

                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, messageId)
                }

                _currentlySpeakingMessageId.value = messageId
                val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, messageId)

                if (result == TextToSpeech.ERROR) {
                    _isSpeaking.value = false
                    _currentlySpeakingMessageId.value = null
                    onError(VoiceError.Other("TTS engine failed to queue utterance."))
                }
            } catch (e: Exception) {
                _isSpeaking.value = false
                _currentlySpeakingMessageId.value = null
                onError(VoiceError.Other("TTS error: ${e.localizedMessage}"))
            }
        }
    }

    private fun resolveLocale(
        language: VoiceLanguage,
        text: String,
        onError: (VoiceError) -> Unit
    ): Locale {
        val engine = tts ?: return Locale.getDefault()
        val tamilLocale = Locale("ta", "IN")
        val englishLocale = Locale("en", "US")

        val wantsTamil = when (language) {
            VoiceLanguage.TAMIL -> true
            VoiceLanguage.ENGLISH -> false
            VoiceLanguage.AUTO -> containsTamilCharacters(text)
        }

        if (wantsTamil) {
            val availability = engine.isLanguageAvailable(tamilLocale)
            if (availability == TextToSpeech.LANG_AVAILABLE ||
                availability == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                availability == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            ) {
                return tamilLocale
            } else {
                // Tamil voice missing: fall back gracefully and inform user
                onError(VoiceError.TamilVoiceUnavailable)
                return englishLocale
            }
        }

        // English / default check
        val englishAvailability = engine.isLanguageAvailable(englishLocale)
        return if (englishAvailability >= TextToSpeech.LANG_AVAILABLE) {
            englishLocale
        } else {
            Locale.getDefault()
        }
    }

    private fun containsTamilCharacters(text: String): Boolean {
        // Tamil Unicode range: \u0B80-\u0BFF
        for (char in text) {
            if (char in '\u0B80'..'\u0BFF') {
                return true
            }
        }
        return false
    }

    override fun stop() {
        mainHandler.post {
            try {
                tts?.stop()
            } catch (_: Exception) {}
            _isSpeaking.value = false
            _currentlySpeakingMessageId.value = null
        }
    }

    override fun shutdown() {
        mainHandler.post {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (_: Exception) {
            } finally {
                tts = null
                isInitialized = false
                _isSpeaking.value = false
                _currentlySpeakingMessageId.value = null
            }
        }
    }
}
