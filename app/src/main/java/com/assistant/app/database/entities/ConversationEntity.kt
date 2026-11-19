package com.assistant.app.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a conversation session or thread (Phase 4 persistence).
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)
