package com.assistant.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.assistant.app.models.AssistantResponse
import com.assistant.app.models.AssistantStatus
import com.assistant.app.models.VoiceLanguage
import com.assistant.app.repository.AssistantRepository
import com.assistant.app.repository.VoiceRepository
import com.assistant.app.repository.VoiceSettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the assistant chat interface, voice engine interactions,
 * runtime permissions, and settings.
 */
class AssistantViewModel(
    private val repository: AssistantRepository,
    private val voiceRepository: VoiceRepository,
    private val voiceSettingsRepository: VoiceSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var simulatedProcessingJob: Job? = null

    init {
        // Collect conversation messages from repository
        viewModelScope.launch {
            repository.messages.collect { messageList ->
                _uiState.update { it.copy(messages = messageList) }
            }
        }

        // Collect voice engine status (Listening, Speaking, Idle, Error)
        viewModelScope.launch {
            voiceRepository.voiceStatus.collect { voiceStatus ->
                _uiState.update { current ->
                    if (current.status == AssistantStatus.PROCESSING && voiceStatus == AssistantStatus.IDLE) {
                        current
                    } else {
                        current.copy(status = voiceStatus)
                    }
                }
            }
        }

        // Collect active speaking message ID
        viewModelScope.launch {
            voiceRepository.currentlySpeakingMessageId.collect { speakingId ->
                _uiState.update { it.copy(currentlySpeakingMessageId = speakingId) }
            }
        }

        // Collect voice errors
        viewModelScope.launch {
            voiceRepository.voiceError.collect { error ->
                if (error != null) {
                    _uiState.update {
                        it.copy(
                            status = AssistantStatus.ERROR,
                            errorMessage = error.message
                        )
                    }
                }
            }
        }

        // Collect voice settings
        viewModelScope.launch {
            voiceSettingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(voiceSettings = settings) }
            }
        }
    }

    /**
     * Updates text input field as the user types.
     */
    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * Sends the current user input through the repository.
     * Enforces mutual exclusion by stopping any active speech recognition or playback.
     * Appends user message, receives honest placeholder response, and triggers auto-TTS if enabled.
     */
    fun sendMessage() {
        val query = _uiState.value.inputText.trim()
        if (query.isBlank() || _uiState.value.status == AssistantStatus.PROCESSING) return

        // Stop any active speech sessions before sending
        voiceRepository.cancelListening()
        voiceRepository.stopSpeaking()

        // Clear input and enter PROCESSING state
        _uiState.update {
            it.copy(
                inputText = "",
                status = AssistantStatus.PROCESSING,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val response = repository.sendUserMessage(query)
                when (response) {
                    is AssistantResponse.Success -> {
                        _uiState.update { it.copy(status = AssistantStatus.IDLE) }
                        handleAutoVoiceResponse(response.message)
                    }
                    is AssistantResponse.NotConnected -> {
                        _uiState.update { it.copy(status = AssistantStatus.IDLE) }
                        handleAutoVoiceResponse(response.reason)
                    }
                    is AssistantResponse.Error -> {
                        _uiState.update {
                            it.copy(
                                status = AssistantStatus.ERROR,
                                errorMessage = response.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        status = AssistantStatus.ERROR,
                        errorMessage = e.localizedMessage ?: "Unexpected error during communication"
                    )
                }
            }
        }
    }

    /**
     * When autoVoiceResponse is enabled in settings, reads newly received assistant message aloud.
     * User messages are NEVER spoken aloud.
     */
    private fun handleAutoVoiceResponse(assistantText: String) {
        if (_uiState.value.voiceSettings.autoVoiceResponse) {
            val lastAssistantMsg = _uiState.value.messages.lastOrNull {
                it.sender == com.assistant.app.models.MessageSender.ASSISTANT
            }
            val messageId = lastAssistantMsg?.id ?: java.util.UUID.randomUUID().toString()
            speakMessage(messageId = messageId, text = assistantText)
        }
    }

    /**
     * Microphone activation button handler.
     * - If currently listening: stops listening.
     * - If currently speaking: stops playback.
     * - If idle and permission granted: starts speech recognition.
     */
    fun onMicClicked() {
        when (_uiState.value.status) {
            AssistantStatus.LISTENING -> {
                voiceRepository.stopListening()
                _uiState.update { it.copy(status = AssistantStatus.IDLE) }
            }
            AssistantStatus.SPEAKING -> {
                voiceRepository.stopSpeaking()
                if (_uiState.value.hasAudioPermission) {
                    startListening()
                } else {
                    _uiState.update { it.copy(status = AssistantStatus.IDLE, currentlySpeakingMessageId = null) }
                }
            }
            else -> {
                if (_uiState.value.hasAudioPermission) {
                    startListening()
                }
            }
        }
    }

    /**
     * Initiates real Speech-To-Text listening.
     * Inserts recognized text into the input field for user review and editing.
     * NEVER auto-sends.
     */
    fun startListening() {
        _uiState.update {
            it.copy(
                status = AssistantStatus.LISTENING,
                errorMessage = null
            )
        }
        voiceRepository.startListening(
            onResult = { recognizedText ->
                val current = _uiState.value.inputText
                val updated = if (current.isBlank()) recognizedText else "$current $recognizedText"
                _uiState.update {
                    it.copy(
                        inputText = updated,
                        status = AssistantStatus.IDLE
                    )
                }
            },
            onPartialResult = {
                // Keep UI updated on partial speech recognition if desired
            }
        )
    }

    /**
     * Toggles speech playback for a specific assistant message.
     */
    fun speakMessage(messageId: String, text: String) {
        if (_uiState.value.isSpeaking && _uiState.value.currentlySpeakingMessageId == messageId) {
            voiceRepository.stopSpeaking()
            _uiState.update { it.copy(status = AssistantStatus.IDLE, currentlySpeakingMessageId = null) }
            return
        }

        _uiState.update {
            it.copy(
                status = AssistantStatus.SPEAKING,
                currentlySpeakingMessageId = messageId,
                errorMessage = null
            )
        }
        voiceRepository.speak(
            text = text,
            messageId = messageId,
            onDone = {
                _uiState.update {
                    if (it.currentlySpeakingMessageId == messageId) {
                        it.copy(status = AssistantStatus.IDLE, currentlySpeakingMessageId = null)
                    } else it
                }
            }
        )
    }

    /**
     * Stops current speech output.
     */
    fun stopPlayback() {
        voiceRepository.stopSpeaking()
        _uiState.update { it.copy(status = AssistantStatus.IDLE, currentlySpeakingMessageId = null) }
    }

    /**
     * Invoked when runtime permission check detects permission is already granted.
     */
    fun onPermissionAlreadyGranted() {
        _uiState.update { it.copy(hasAudioPermission = true) }
    }

    /**
     * Handles the result of the runtime permission request contract.
     */
    fun onPermissionResult(isGranted: Boolean, shouldShowRationale: Boolean) {
        if (isGranted) {
            _uiState.update {
                it.copy(
                    hasAudioPermission = true,
                    showPermissionRationale = false,
                    showPermanentlyDeniedDialog = false,
                    errorMessage = null
                )
            }
            startListening()
        } else {
            _uiState.update {
                it.copy(
                    hasAudioPermission = false,
                    showPermissionRationale = shouldShowRationale,
                    showPermanentlyDeniedDialog = !shouldShowRationale,
                    status = AssistantStatus.IDLE
                )
            }
        }
    }

    fun dismissRationaleDialog() {
        _uiState.update { it.copy(showPermissionRationale = false) }
    }

    fun dismissPermanentlyDeniedDialog() {
        _uiState.update { it.copy(showPermanentlyDeniedDialog = false) }
    }

    // --- Voice Settings Modifiers (Used by Settings Screen) ---

    fun setAutoVoiceResponse(enabled: Boolean) {
        voiceSettingsRepository.setAutoVoiceResponse(enabled)
        _uiState.update { it.copy(voiceSettings = it.voiceSettings.copy(autoVoiceResponse = enabled)) }
    }

    fun setSpeechLanguage(language: VoiceLanguage) {
        voiceSettingsRepository.setLanguage(language)
        _uiState.update { it.copy(voiceSettings = it.voiceSettings.copy(language = language)) }
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        voiceSettingsRepository.setSpeechRate(clamped)
        _uiState.update { it.copy(voiceSettings = it.voiceSettings.copy(speechRate = clamped)) }
    }

    fun setSpeechPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        voiceSettingsRepository.setPitch(clamped)
        _uiState.update { it.copy(voiceSettings = it.voiceSettings.copy(pitch = clamped)) }
    }

    /**
     * Plays a test voice sample using current voice settings.
     */
    fun testVoice() {
        if (_uiState.value.isSpeaking && _uiState.value.currentlySpeakingMessageId == "test_voice") {
            stopPlayback()
            return
        }

        val sampleText = when (_uiState.value.voiceSettings.language) {
            VoiceLanguage.TAMIL -> "வணக்கம்! இது உங்கள் தனிப்பட்ட உதவியாளரின் குரல் சோதனை."
            VoiceLanguage.ENGLISH -> "Hello! This is a test of your personal assistant's voice."
            VoiceLanguage.AUTO -> "Hello! வணக்கம்! This is a voice test of your personal assistant."
        }
        speakMessage(messageId = "test_voice", text = sampleText)
    }

    // --- Lifecycle Awareness ---

    /**
     * Cancels or pauses all voice work when the application goes to the background.
     */
    fun onAppBackgrounded() {
        voiceRepository.onAppBackgrounded()
        _uiState.update {
            if (it.status == AssistantStatus.LISTENING || it.status == AssistantStatus.SPEAKING) {
                it.copy(status = AssistantStatus.IDLE, currentlySpeakingMessageId = null)
            } else it
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRepository.shutdown()
    }

    // --- State Exercising & Testing (Requirement: reachable through ViewModel state) ---

    fun toggleTestControls() {
        _uiState.update { it.copy(showTestControls = !it.showTestControls) }
    }

    fun triggerTestListeningState() {
        voiceRepository.stopSpeaking()
        _uiState.update {
            it.copy(
                status = AssistantStatus.LISTENING,
                errorMessage = null
            )
        }
    }

    fun triggerTestProcessingState() {
        simulatedProcessingJob?.cancel()
        _uiState.update {
            it.copy(
                status = AssistantStatus.PROCESSING,
                errorMessage = null
            )
        }
        simulatedProcessingJob = viewModelScope.launch {
            delay(3000)
            _uiState.update {
                if (it.status == AssistantStatus.PROCESSING) {
                    it.copy(status = AssistantStatus.IDLE)
                } else it
            }
        }
    }

    fun triggerTestErrorState(customMessage: String? = null) {
        val errorText = customMessage
            ?: "Simulated error: Assistant connection unreachable or timed out."
        _uiState.update {
            it.copy(
                status = AssistantStatus.ERROR,
                errorMessage = errorText
            )
        }
    }

    fun resetStatusToIdle() {
        simulatedProcessingJob?.cancel()
        voiceRepository.cancelListening()
        voiceRepository.stopSpeaking()
        voiceRepository.clearVoiceError()
        _uiState.update {
            it.copy(
                status = AssistantStatus.IDLE,
                errorMessage = null,
                currentlySpeakingMessageId = null
            )
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            repository.clearMessages()
            resetStatusToIdle()
        }
    }
}
