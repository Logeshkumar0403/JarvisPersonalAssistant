package com.assistant.app.models

/**
 * High-level state of the assistant UI and background processing.
 * Models Idle, Listening, Processing, and Speaking as explicit states.
 */
enum class AssistantStatus {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}
