package com.assistant.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.assistant.app.repository.AssistantRepository
import com.assistant.app.repository.VoiceRepository
import com.assistant.app.repository.VoiceSettingsRepository

/**
 * Factory for creating [AssistantViewModel] instances with constructor dependencies.
 */
class AssistantViewModelFactory(
    private val repository: AssistantRepository,
    private val voiceRepository: VoiceRepository,
    private val voiceSettingsRepository: VoiceSettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssistantViewModel::class.java)) {
            return AssistantViewModel(
                repository = repository,
                voiceRepository = voiceRepository,
                voiceSettingsRepository = voiceSettingsRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
