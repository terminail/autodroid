package com.autodroid.teachitback.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * AI服务响应数据类
 * 包含AI回复内容和处理信息
 */
@Parcelize
data class AIServiceResponse(
    val content: String,
    val processInfo: AIProcessInfo
) : Parcelable {
    override fun describeContents(): Int = 0
}
