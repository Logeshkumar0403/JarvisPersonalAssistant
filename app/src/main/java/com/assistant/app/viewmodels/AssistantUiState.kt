package com.assistant.app.viewmodels

import com.assistant.app.models.AssistantStatus
import com.assistant.app.models.ChatMessage
import com.assistant.app.models.VoiceSettings

/**
 * UI State for the Assistant Home screen and Voice Engine.
 */
data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val status: AssistantStatus = AssistantStatus.IDLE,
    val errorMessage: String? = null,
    val hasAudioPermission: Boolean = false,
    val showPermissionRationale: Boolean = false,
    val showPermanentlyDeniedDialog: Boolean = false,
    val showTestControls: Boolean = false,
    val currentlySpeakingMessageId: String? = null,
    val voiceSettings: VoiceSettings = VoiceSettings()
) {
    val isSendingDisabled: Boolean
        get() = inputText.isBlank() || status == AssistantStatus.PROCESSING

    val isListening: Boolean
        get() = status == AssistantStatus.LISTENING

    val isProcessing: Boolean
        get() = status == AssistantStatus.PROCESSING

    val isSpeaking: Boolean
        get() = status == AssistantStatus.SPEAKING

    val isError: Boolean
        get() = status == AssistantStatus.ERROR
}
