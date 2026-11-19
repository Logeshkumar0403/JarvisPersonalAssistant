package com.assistant.app.utils

import android.content.Context
import com.assistant.app.database.AppDatabase
import com.assistant.app.repository.AssistantRepository
import com.assistant.app.repository.ConversationRepository
import com.assistant.app.repository.DefaultVoiceRepository
import com.assistant.app.repository.DefaultVoiceSettingsRepository
import com.assistant.app.repository.PlaceholderAssistantRepository
import com.assistant.app.repository.RoomConversationRepository
import com.assistant.app.repository.VoiceRepository
import com.assistant.app.repository.VoiceSettingsRepository
import com.assistant.app.services.AiService
import com.assistant.app.services.AndroidSpeechRecognitionService
import com.assistant.app.services.AndroidTextToSpeechService
import com.assistant.app.services.PlaceholderAiService
import com.assistant.app.services.SpeechRecognitionService
import com.assistant.app.services.TextToSpeechService

/**
 * Dependency injection container for the application.
 * Uses explicit manual dependency injection:
 * - Simple, zero-reflection, compile-safe wiring
 * - Clean interfaces allowing swap-in of real AI/voice implementations
 * - Lifecycles managed and accessible from Application scope
 */
interface AppContainer {
    val database: AppDatabase
    val aiService: AiService
    val speechRecognitionService: SpeechRecognitionService
    val textToSpeechService: TextToSpeechService
    val voiceSettingsRepository: VoiceSettingsRepository
    val voiceRepository: VoiceRepository
    val assistantRepository: AssistantRepository
    val conversationRepository: ConversationRepository
}

/**
 * Default implementation of [AppContainer] instantiated at Application start.
 */
class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val aiService: AiService by lazy {
        PlaceholderAiService()
    }

    override val speechRecognitionService: SpeechRecognitionService by lazy {
        AndroidSpeechRecognitionService(context.applicationContext)
    }

    override val textToSpeechService: TextToSpeechService by lazy {
        AndroidTextToSpeechService(context.applicationContext)
    }

    override val voiceSettingsRepository: VoiceSettingsRepository by lazy {
        DefaultVoiceSettingsRepository(context.applicationContext)
    }

    override val voiceRepository: VoiceRepository by lazy {
        DefaultVoiceRepository(
            speechRecognitionService = speechRecognitionService,
            textToSpeechService = textToSpeechService,
            voiceSettingsRepository = voiceSettingsRepository
        )
    }

    override val assistantRepository: AssistantRepository by lazy {
        PlaceholderAssistantRepository(
            aiService = aiService,
            messageDao = database.messageDao()
        )
    }

    override val conversationRepository: ConversationRepository by lazy {
        RoomConversationRepository(database.conversationDao())
    }
}
