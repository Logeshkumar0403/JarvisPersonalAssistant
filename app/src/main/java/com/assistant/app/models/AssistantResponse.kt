package com.assistant.app.models

/**
 * Result boundary returned by the AssistantRepository and AiService.
 * In Phase 1, the placeholder returns [NotConnected] to represent an honest,
 * non-fake state rather than inventing artificial responses.
 */
sealed interface AssistantResponse {
    /**
     * Successful response from an active AI service (Phase 2+).
     */
    data class Success(val message: String) : AssistantResponse

    /**
     * Explicit honest result indicating the backend AI is not connected in this phase.
     */
    data class NotConnected(
        val reason: String = "AI service not yet connected (Phase 2). Request successfully passed through the architecture seam."
    ) : AssistantResponse

    /**
     * An error occurred while communicating with the assistant service.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : AssistantResponse
}
