package com.assistant.app.repository

import com.assistant.app.models.AssistantStatus
import com.assistant.app.models.VoiceError
import com.assistant.app.models.VoiceSettings
import com.assistant.app.services.SpeechRecognitionService
import com.assistant.app.services.TextToSpeechService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repository interface coordinating voice operations (Speech Recognition & TTS).
 * Enforces mutual exclusion (only one voice session runs at a time),
 * lifecycle management, and clean transitions between Idle, Listening, and Speaking states.
 */
interface VoiceRepository {
    val voiceStatus: StateFlow<AssistantStatus>
    val currentlySpeakingMessageId: StateFlow<String?>
    val settings: StateFlow<VoiceSettings>
    val voiceError: StateFlow<VoiceError?>

    fun startListening(
        onResult: (String) -> Unit,
        onPartialResult: ((String) -> Unit)? = null
    )
    fun stopListening()
    fun cancelListening()

    fun speak(
        text: String,
        messageId: String,
        onDone: () -> Unit = {}
    )
    fun stopSpeaking()

    fun testVoice(sampleText: String)
    fun clearVoiceError()
    fun onAppBackgrounded()
    fun shutdown()
}

/**
 * Concrete implementation of [VoiceRepository].
 * Coordinates [SpeechRecognitionService] and [TextToSpeechService] with [VoiceSettingsRepository].
 */
class DefaultVoiceRepository(
    private val speechRecognitionService: SpeechRecognitionService,
    private val textToSpeechService: TextToSpeechService,
    private val voiceSettingsRepository: VoiceSettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) : VoiceRepository {

    private val _voiceStatus = MutableStateFlow(AssistantStatus.IDLE)
    override val voiceStatus: StateFlow<AssistantStatus> = _voiceStatus.asStateFlow()

    private val _voiceError = MutableStateFlow<VoiceError?>(null)
    override val voiceError: StateFlow<VoiceError?> = _voiceError.asStateFlow()

    override val currentlySpeakingMessageId: StateFlow<String?> =
        textToSpeechService.currentlySpeakingMessageId

    override val settings: StateFlow<VoiceSettings> =
        voiceSettingsRepository.settings

    init {
        // Synchronize TTS completion/cancellation back to IDLE
        scope.launch {
            textToSpeechService.isSpeaking.collect { isSpeaking ->
                if (!isSpeaking && _voiceStatus.value == AssistantStatus.SPEAKING) {
                    _voiceStatus.value = AssistantStatus.IDLE
                }
            }
        }
    }

    override fun startListening(
        onResult: (String) -> Unit,
        onPartialResult: ((String) -> Unit)?
    ) {
        // Mutual exclusion: stop any active speech playback before listening
        if (textToSpeechService.isSpeaking.value) {
            textToSpeechService.stop()
        }

        _voiceError.value = null
        _voiceStatus.value = AssistantStatus.LISTENING

        speechRecognitionService.startListening(
            language = settings.value.language,
            onResult = { recognizedText ->
                _voiceStatus.value = AssistantStatus.IDLE
                onResult(recognizedText)
            },
            onPartialResult = onPartialResult,
            onError = { error ->
                _voiceStatus.value = AssistantStatus.ERROR
                _voiceError.value = error
            }
        )
    }

    override fun stopListening() {
        speechRecognitionService.stopListening()
        if (_voiceStatus.value == AssistantStatus.LISTENING) {
            _voiceStatus.value = AssistantStatus.IDLE
        }
    }

    override fun cancelListening() {
        speechRecognitionService.cancelListening()
        if (_voiceStatus.value == AssistantStatus.LISTENING) {
            _voiceStatus.value = AssistantStatus.IDLE
        }
    }

    override fun speak(
        text: String,
        messageId: String,
        onDone: () -> Unit
    ) {
        // If already speaking this message, tapping toggles playback OFF
        if (textToSpeechService.isSpeaking.value &&
            textToSpeechService.currentlySpeakingMessageId.value == messageId
        ) {
            stopSpeaking()
            return
        }

        // Mutual exclusion: cancel any active speech recognition before speaking
        if (speechRecognitionService.isListening.value) {
            speechRecognitionService.cancelListening()
        }

        _voiceError.value = null
        _voiceStatus.value = AssistantStatus.SPEAKING

        val currentSettings = settings.value
        textToSpeechService.speak(
            text = text,
            messageId = messageId,
            language = currentSettings.language,
            pitch = currentSettings.pitch,
            speechRate = currentSettings.speechRate,
            onStart = {
                _voiceStatus.value = AssistantStatus.SPEAKING
            },
            onDone = {
                _voiceStatus.value = AssistantStatus.IDLE
                onDone()
            },
            onError = { error ->
                _voiceStatus.value = AssistantStatus.ERROR
                _voiceError.value = error
            }
        )
    }

    override fun stopSpeaking() {
        textToSpeechService.stop()
        if (_voiceStatus.value == AssistantStatus.SPEAKING) {
            _voiceStatus.value = AssistantStatus.IDLE
        }
    }

    override fun testVoice(sampleText: String) {
        speak(text = sampleText, messageId = "test_voice")
    }

    override fun clearVoiceError() {
        _voiceError.value = null
        if (_voiceStatus.value == AssistantStatus.ERROR) {
            _voiceStatus.value = AssistantStatus.IDLE
        }
    }

    override fun onAppBackgrounded() {
        // Cancel or pause voice work when the app goes into the background
        if (speechRecognitionService.isListening.value) {
            speechRecognitionService.cancelListening()
        }
        if (textToSpeechService.isSpeaking.value) {
            textToSpeechService.stop()
        }
        _voiceStatus.value = AssistantStatus.IDLE
    }

    override fun shutdown() {
        speechRecognitionService.destroy()
        textToSpeechService.shutdown()
        _voiceStatus.value = AssistantStatus.IDLE
    }
}
