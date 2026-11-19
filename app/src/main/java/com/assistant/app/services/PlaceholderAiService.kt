package com.assistant.app.services

import com.assistant.app.models.AssistantResponse
import kotlinx.coroutines.delay

/**
 * Phase 1 placeholder implementation of [AiService].
 * Intentionally returns an explicit [AssistantResponse.NotConnected] rather
 * than generating fabricated AI text.
 */
class PlaceholderAiService : AiService {

    override suspend fun generateResponse(prompt: String): AssistantResponse {
        // Brief simulated round-trip delay to exercise the UI processing state
        delay(700)
        return AssistantResponse.NotConnected(
            reason = "AI service not yet connected (Phase 2). Input was received and processed through the architecture seam."
        )
    }
}
