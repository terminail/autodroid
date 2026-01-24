package com.autodroid.teachitback.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * AI服务能力配置类
 * 使用布尔属性替代字符串集合，提供类型安全和IDE支持
 */
@Parcelize
data class AIServiceCapability(
    // 基础对话功能
    val supportBasicChat: Boolean = false,
    val supportFileProcessing: Boolean = false,
    val supportMindMapGeneration: Boolean = false,
    val supportLearningAnalysis: Boolean = false,
    val supportSocraticQuestioning: Boolean = false,
    val supportAnswerEvaluation: Boolean = false,
    val supportDocumentParsing: Boolean = false,
    val supportConceptExtraction: Boolean = false,
    val supportKnowledgeGraph: Boolean = false,
    val supportLongText: Boolean = false,
    val supportMultimodal: Boolean = false,
    val supportEducation: Boolean = false,
    val supportCodeGeneration: Boolean = false,
    val supportMath: Boolean = false,
    val supportCreativeWriting: Boolean = false,
    val supportImageAnalysis: Boolean = false,
    val supportImageGeneration: Boolean = false,
    val supportAudioProcessing: Boolean = false,
    val supportVideoAnalysis: Boolean = false,
    val supportRAG: Boolean = false
) : Parcelable {
    
    companion object {
        // 空配置
        val EMPTY = AIServiceCapability()
        
        // 基础对话能力
        val BASIC_CHAT = AIServiceCapability(supportBasicChat = true)
        
        // 完整能力配置
        val FULL_CAPABILITIES = AIServiceCapability(
            supportBasicChat = true,
            supportFileProcessing = true,
            supportMindMapGeneration = true,
            supportLearningAnalysis = true,
            supportSocraticQuestioning = true,
            supportAnswerEvaluation = true,
            supportDocumentParsing = true,
            supportConceptExtraction = true,
            supportKnowledgeGraph = true,
            supportLongText = true,
            supportMultimodal = true,
            supportEducation = true,
            supportCodeGeneration = true,
            supportMath = true,
            supportCreativeWriting = true,
            supportImageAnalysis = true,
            supportImageGeneration = true,
            supportAudioProcessing = true,
            supportVideoAnalysis = true,
            supportRAG = true
        )
    }
    
    // Fluent API 方法 - 支持链式调用
    fun supportBasicChat(value: Boolean = true) = copy(supportBasicChat = value)
    fun supportFileProcessing(value: Boolean = true) = copy(supportFileProcessing = value)
    fun supportMindMapGeneration(value: Boolean = true) = copy(supportMindMapGeneration = value)
    fun supportLearningAnalysis(value: Boolean = true) = copy(supportLearningAnalysis = value)
    fun supportSocraticQuestioning(value: Boolean = true) = copy(supportSocraticQuestioning = value)
    fun supportAnswerEvaluation(value: Boolean = true) = copy(supportAnswerEvaluation = value)
    fun supportDocumentParsing(value: Boolean = true) = copy(supportDocumentParsing = value)
    fun supportConceptExtraction(value: Boolean = true) = copy(supportConceptExtraction = value)
    fun supportKnowledgeGraph(value: Boolean = true) = copy(supportKnowledgeGraph = value)
    fun supportLongText(value: Boolean = true) = copy(supportLongText = value)
    fun supportMultimodal(value: Boolean = true) = copy(supportMultimodal = value)
    fun supportEducation(value: Boolean = true) = copy(supportEducation = value)
    fun supportCodeGeneration(value: Boolean = true) = copy(supportCodeGeneration = value)
    fun supportMath(value: Boolean = true) = copy(supportMath = value)
    fun supportCreativeWriting(value: Boolean = true) = copy(supportCreativeWriting = value)
    fun supportImageAnalysis(value: Boolean = true) = copy(supportImageAnalysis = value)
    fun supportImageGeneration(value: Boolean = true) = copy(supportImageGeneration = value)
    fun supportAudioProcessing(value: Boolean = true) = copy(supportAudioProcessing = value)
    fun supportVideoAnalysis(value: Boolean = true) = copy(supportVideoAnalysis = value)
    fun supportRAG(value: Boolean = true) = copy(supportRAG = value)
}
