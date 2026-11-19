package com.assistant.app.repository

import com.assistant.app.models.AssistantResponse
import com.assistant.app.models.ChatMessage
import kotlinx.coroutines.flow.StateFlow

/**
 * Seam interface for assistant interactions and conversation history.
 * Exposed through this interface so future phases can swap implementations
 * without touching the ViewModel or UI.
 */
interface AssistantRepository {

    /**
     * Flow of active conversation messages.
     */
    val messages: StateFlow<List<ChatMessage>>

    /**
     * Sends a user query to the assistant backend, updates conversation state,
     * and returns the resulting [AssistantResponse].
     */
    suspend fun sendUserMessage(text: String): AssistantResponse

    /**
     * Clears all active messages.
     */
    suspend fun clearMessages()

    /**
     * Injects a local message (useful for testing or system notices).
     */
    fun addLocalMessage(message: ChatMessage)
}
