package com.autodroid.teachitback.api

import com.autodroid.teachitback.model.MessageEntity

interface AIService {
    suspend fun sendMessage(messages: List<MessageEntity>, context: String): String
    suspend fun processFileContent(content: String, context: String): String
}
