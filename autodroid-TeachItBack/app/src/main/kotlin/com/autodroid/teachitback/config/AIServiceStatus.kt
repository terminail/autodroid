package com.autodroid.teachitback.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * AI服务状态类
 * 用于记录AI服务的当前状态信息
 */
@Parcelize
data class AIServiceStatus(
    val code: Int = 200, // 状态码，如200表示正常，402表示余额不足等
    val description: String = "正常", // 状态描述
    val lastUpdated: Long = System.currentTimeMillis() // 状态更新时间
) : Parcelable {
    companion object {
        // 预定义状态常量
        val STATUS_NOT_CHECKED = AIServiceStatus(0, "未检查")
        val STATUS_OK = AIServiceStatus(200, "正常")
        val STATUS_BALANCE_INSUFFICIENT = AIServiceStatus(402, "余额不足")
        val STATUS_API_KEY_INVALID = AIServiceStatus(401, "API Key无效")
        val STATUS_SERVICE_UNAVAILABLE = AIServiceStatus(503, "服务不可用")
        val STATUS_RATE_LIMIT = AIServiceStatus(429, "请求频率限制")
        val STATUS_NETWORK_ERROR = AIServiceStatus(500, "网络错误")
        val STATUS_CONFIG_ERROR = AIServiceStatus(400, "配置错误")
        val STATUS_MODEL_NOT_LOADED = AIServiceStatus(404, "模型未加载")
        
        // 根据状态码获取对应的状态对象
        fun fromCode(code: Int, description: String? = null): AIServiceStatus {
            return when (code) {
                200 -> STATUS_OK.copy(description = description ?: "正常")
                402 -> STATUS_BALANCE_INSUFFICIENT.copy(description = description ?: "余额不足")
                401 -> STATUS_API_KEY_INVALID.copy(description = description ?: "API Key无效")
                503 -> STATUS_SERVICE_UNAVAILABLE.copy(description = description ?: "服务不可用")
                429 -> STATUS_RATE_LIMIT.copy(description = description ?: "请求频率限制")
                500 -> STATUS_NETWORK_ERROR.copy(description = description ?: "网络错误")
                400 -> STATUS_CONFIG_ERROR.copy(description = description ?: "配置错误")
                404 -> STATUS_MODEL_NOT_LOADED.copy(description = description ?: "模型未加载")
                else -> AIServiceStatus(code, description ?: "未知状态")
            }
        }
    }
    
    // 判断状态是否正常
    val isOk: Boolean get() = code == 200
    val isError: Boolean get() = code != 200
    
    // 判断是否需要用户干预
    val requiresUserAction: Boolean get() = code in listOf(402, 401, 400)
}