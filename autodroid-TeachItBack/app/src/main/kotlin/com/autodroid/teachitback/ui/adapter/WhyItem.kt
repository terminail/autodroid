package com.autodroid.teachitback.ui.adapter

import com.autodroid.teachitback.model.TopicEntity

sealed class WhyItem {
    
    abstract fun getType(): Int
    
    companion object {
        const val TYPE_SECTION_HEADER = 0
        const val TYPE_TEXT_CARD = 1
        const val TYPE_PRESET_TOPIC = 2
    }
    
    data class SectionHeaderItem(
        val title: String
    ) : WhyItem() {
        override fun getType(): Int = TYPE_SECTION_HEADER
    }
    
    data class TextCardItem(
        val title: String,
        val content: String
    ) : WhyItem() {
        override fun getType(): Int = TYPE_TEXT_CARD
    }
    
    data class PresetTopicItem(
        val topic: TopicEntity
    ) : WhyItem() {
        override fun getType(): Int = TYPE_PRESET_TOPIC
    }
}
