package com.assistant.app.services

import com.assistant.app.models.AssistantResponse

/**
 * Seam interface for AI engines (Phase 2).
 * Implementations may integrate with cloud providers (Gemini, Claude, OpenAI)
 * or local on-device LLMs.
 */
interface AiService {
    /**
     * Sends a user query to the AI engine and returns an [AssistantResponse].
     */
    suspend fun generateResponse(prompt: String): AssistantResponse
}
