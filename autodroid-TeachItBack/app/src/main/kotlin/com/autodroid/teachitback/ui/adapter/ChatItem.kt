package com.autodroid.teachitback.ui.adapter

import com.autodroid.teachitback.model.MindMapNode
import com.autodroid.teachitback.model.MessageEntity

/**
 * ChatFragment异构数据项的密封类
 * 支持聊天界面中的所有数据类型：消息、AI响应、MindMap、文件等
 */
sealed class ChatItem {
    
    abstract fun getType(): Int
    
    companion object {
        const val TYPE_USER_MESSAGE = 0
        const val TYPE_AI_MESSAGE = 1
        const val TYPE_MINDMAP = 2
        const val TYPE_FILE = 3
        const val TYPE_SYSTEM = 4
    }
    
    /**
     * 用户消息项
     */
    data class UserMessageItem(
        val message: MessageEntity
    ) : ChatItem() {
        override fun getType(): Int = TYPE_USER_MESSAGE
    }
    
    /**
     * AI消息项
     */
    data class AIMessageItem(
        val message: MessageEntity
    ) : ChatItem() {
        override fun getType(): Int = TYPE_AI_MESSAGE
    }
    
    /**
     * MindMap显示项
     */
    data class MindMapDisplayItem(
        val mindMapNodes: List<MindMapNode>,
        val title: String
    ) : ChatItem() {
        override fun getType(): Int = TYPE_MINDMAP
    }
    
    /**
     * 文件项
     */
    data class FileItem(
        val fileName: String,
        val filePath: String,
        val extractedText: String? = null
    ) : ChatItem() {
        override fun getType(): Int = TYPE_FILE
    }
    
    /**
     * 系统消息项
     */
    data class SystemItem(
        val content: String
    ) : ChatItem() {
        override fun getType(): Int = TYPE_SYSTEM
    }
}