package com.assistant.app.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a persistent conversation message.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val sender: String,
    val timestamp: Long,
    val status: String
)
