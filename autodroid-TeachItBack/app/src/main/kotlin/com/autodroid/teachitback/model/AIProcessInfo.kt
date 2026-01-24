package com.autodroid.teachitback.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * AI服务处理信息数据类
 * 用于记录每次AI调用的详细信息
 */
@Parcelize
data class AIProcessInfo(
    val serviceId: String,
    val serviceName: String,
    val modelUsed: String? = null,
    val processingTime: Long? = null
) : Parcelable
