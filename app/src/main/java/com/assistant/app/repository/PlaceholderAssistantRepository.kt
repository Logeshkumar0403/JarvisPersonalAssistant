package com.assistant.app.repository

import com.assistant.app.database.dao.MessageDao
import com.assistant.app.database.entities.MessageEntity
import com.assistant.app.models.AssistantResponse
import com.assistant.app.models.ChatMessage
import com.assistant.app.models.MessageSender
import com.assistant.app.models.MessageStatus
import com.assistant.app.services.AiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Phase 1 placeholder implementation of [AssistantRepository].
 * Routes user queries through [AiService] which returns an explicit
 * [AssistantResponse.NotConnected] result, and maintains the conversation list.
 * Integrates with Room's [MessageDao] to verify persistence scaffolding.
 */
class PlaceholderAssistantRepository(
    private val aiService: AiService,
    private val messageDao: MessageDao? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : AssistantRepository {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun sendUserMessage(text: String): AssistantResponse {
        // 1. Record the user message
        val userMessage = ChatMessage(
            text = text,
            sender = MessageSender.USER,
            status = MessageStatus.SENT
        )
        appendMessage(userMessage)

        // 2. Delegate to the AI service seam (returns NotConnected in Phase 1)
        val response = aiService.generateResponse(text)

        // 3. Record the honest, non-fake assistant response
        val assistantMessage = when (response) {
            is AssistantResponse.Success -> {
                ChatMessage(
                    text = response.message,
                    sender = MessageSender.ASSISTANT,
                    status = MessageStatus.SENT
                )
            }
            is AssistantResponse.NotConnected -> {
                ChatMessage(
                    text = response.reason,
                    sender = MessageSender.ASSISTANT,
                    status = MessageStatus.NOT_CONNECTED
                )
            }
            is AssistantResponse.Error -> {
                ChatMessage(
                    text = response.message,
                    sender = MessageSender.ASSISTANT,
                    status = MessageStatus.ERROR
                )
            }
        }
        appendMessage(assistantMessage)

        return response
    }

    override suspend fun clearMessages() {
        _messages.value = emptyList()
        scope.launch {
            try {
                messageDao?.clearAllMessages()
            } catch (_: Exception) {}
        }
    }

    override fun addLocalMessage(message: ChatMessage) {
        appendMessage(message)
    }

    private fun appendMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
        // Persist to Room database foundation
        scope.launch {
            try {
                messageDao?.insertMessage(
                    MessageEntity(
                        id = message.id,
                        text = message.text,
                        sender = message.sender.name,
                        timestamp = message.timestamp,
                        status = message.status.name
                    )
                )
            } catch (_: Exception) {}
        }
    }
}
