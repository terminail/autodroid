package com.autodroid.teachitback.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * AI服务配置需求类
 * 使用布尔属性替代字符串集合，提供类型安全和清晰的配置需求管理
 */
@Parcelize
data class AIServiceRequiredFields(
    val requireApiKey: Boolean = false,
    val requireSecretId: Boolean = false,
    val requireBaseUrl: Boolean = false,
    val requireRegion: Boolean = false,
    val requireModel: Boolean = false
) : Parcelable {
    
    companion object {
        // 无需任何字段
        val NO_REQUIRED_FIELDS = AIServiceRequiredFields()
        
        // 仅需要 API Key
        val API_KEY_ONLY = AIServiceRequiredFields(requireApiKey = true)
        
        // 需要 API Key 和 Base URL
        val API_AND_URL = AIServiceRequiredFields(
            requireApiKey = true,
            requireBaseUrl = true
        )
        
        // 完整的云服务配置需求
        val CLOUD_SERVICE = AIServiceRequiredFields(
            requireApiKey = true,
            requireSecretId = true,
            requireBaseUrl = true,
            requireRegion = true,
            requireModel = true
        )
    }
}
