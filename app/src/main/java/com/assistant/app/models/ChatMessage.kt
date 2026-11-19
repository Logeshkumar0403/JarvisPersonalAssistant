package com.assistant.app.models

/**
 * Identifies the sender of a message in the conversation.
 */
enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Status indicator for an individual message bubble.
 */
enum class MessageStatus {
    SENT,
    PROCESSING,
    NOT_CONNECTED,
    ERROR
}

/**
 * Domain model representing a chat message bubble in the conversation.
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT
)
