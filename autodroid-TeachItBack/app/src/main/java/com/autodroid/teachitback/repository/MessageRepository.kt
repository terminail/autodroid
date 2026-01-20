package com.autodroid.teachitback.repository

import com.autodroid.teachitback.database.MessageDao
import com.autodroid.teachitback.model.MessageEntity
import kotlinx.coroutines.flow.Flow

class MessageRepository(private val messageDao: MessageDao) {
    fun getMessagesByTopic(topicId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesByTopic(topicId)

    suspend fun insertMessage(message: MessageEntity) = messageDao.insertMessage(message)

    suspend fun updateMessage(message: MessageEntity) = messageDao.updateMessage(message)

    suspend fun deleteMessagesByTopic(topicId: String) =
        messageDao.deleteMessagesByTopic(topicId)
}
