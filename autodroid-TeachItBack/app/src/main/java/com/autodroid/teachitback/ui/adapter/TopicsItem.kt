package com.autodroid.teachitback.ui.adapter

import com.autodroid.teachitback.model.TopicEntity

/**
 * TopicsFragment异构数据项的密封类
 * 支持话题列表界面中的所有数据类型：话题项、分类标题、添加按钮等
 */
sealed class TopicsItem {
    
    abstract fun getType(): Int
    
    companion object {
        const val TYPE_TOPIC = 0
        const val TYPE_SECTION_HEADER = 1
        const val TYPE_ADD_BUTTON = 2
        const val TYPE_EMPTY_STATE = 3
    }
    
    /**
     * 话题项
     */
    data class TopicItem(
        val topic: TopicEntity
    ) : TopicsItem() {
        override fun getType(): Int = TYPE_TOPIC
    }
    
    /**
     * 分类标题项
     */
    data class SectionHeaderItem(
        val title: String,
        val subtitle: String? = null
    ) : TopicsItem() {
        override fun getType(): Int = TYPE_SECTION_HEADER
    }
    
    /**
     * 添加按钮项
     */
    data class AddButtonItem(
        val label: String = "添加新话题"
    ) : TopicsItem() {
        override fun getType(): Int = TYPE_ADD_BUTTON
    }
    
    /**
     * 空状态项
     */
    data class EmptyStateItem(
        val message: String = "暂无话题，点击添加按钮创建新话题"
    ) : TopicsItem() {
        override fun getType(): Int = TYPE_EMPTY_STATE
    }
}