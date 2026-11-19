package com.assistant.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.assistant.app.database.dao.ConversationDao
import com.assistant.app.database.dao.MessageDao
import com.assistant.app.database.entities.ConversationEntity
import com.assistant.app.database.entities.MessageEntity

/**
 * Room database foundation for the Assistant application.
 * Provides the persistence foundation for messages and conversations.
 */
@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        private const val DATABASE_NAME = "assistant_database.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
