package com.assistant.app.repository

import com.assistant.app.database.dao.ConversationDao
import com.assistant.app.database.entities.ConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Seam interface for conversation sessions / threads (Phase 4).
 */
interface ConversationRepository {
    fun getAllConversations(): Flow<List<ConversationEntity>>
    suspend fun createConversation(id: String, title: String)
    suspend fun deleteConversation(id: String)
}

/**
 * Room-backed implementation of [ConversationRepository].
 */
class RoomConversationRepository(
    private val conversationDao: ConversationDao
) : ConversationRepository {

    override fun getAllConversations(): Flow<List<ConversationEntity>> =
        conversationDao.getAllConversations()

    override suspend fun createConversation(id: String, title: String) {
        conversationDao.insertConversation(
            ConversationEntity(id = id, title = title)
        )
    }

    override suspend fun deleteConversation(id: String) {
        conversationDao.deleteConversationById(id)
    }
}
