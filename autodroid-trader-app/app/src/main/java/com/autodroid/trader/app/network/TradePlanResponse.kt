package com.autodroid.trader.app.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName



/**
 * Trade plan list item data class
 */
data class TradePlanResponse(
    val id: String? = null,
    @SerializedName("script_id")
    val script_id: String? = null,
    val name: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val description: String? = null,
    val exchange: String? = null,
    val symbol: String? = null,
    @SerializedName("symbol_name")
    val symbol_name: String? = null,
    val ohlcv: Ohlcv? = null,
    @SerializedName("change_percent")
    val change_percent: Double? = null,
    val data: JsonObject? = null,
    val status: String? = null,
    val executable: Boolean? = null,
    @SerializedName("executionStatus")
    val executionStatus: String? = null,
    @SerializedName("execution_result")
    val executionResult: String? = null,
    @SerializedName("started_at")
    val startTime: String? = null,
    @SerializedName("ended_at")
    val endTime: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) {
    companion object {
        private val gson = Gson()

        fun fromJson(json: String): TradePlanResponse {
            return gson.fromJson(json, TradePlanResponse::class.java)
        }
    }

    fun toJson(): String {
        return gson.toJson(this)
    }

    fun getDisplayTime(): String {
        return createdAt?.let { formatTime(it) } ?: ""
    }

    fun getDisplayInfoLine1(): String {
        val parts = mutableListOf<String>()
        symbol?.let { parts.add(it) }
        symbol_name?.let { parts.add(it) }
        ohlcv?.close?.let { parts.add("收盘价: ${"%.2f".format(it)}") }
        return parts.joinToString(" | ")
    }

    fun getDisplayInfoLine2(): String {
        val parts = mutableListOf<String>()
        change_percent?.let {
            val sign = if (it >= 0) "+" else ""
            parts.add("涨跌幅: ${sign}${"%.2f".format(it)}%")
        }
        ohlcv?.volume?.let { parts.add("成交量: ${formatVolume(it)}") }
        return parts.joinToString(" | ")
    }

    private fun formatTime(isoTime: String): String {
        return try {
            val datePart = isoTime.substring(0, 10)
            val timePart = isoTime.substring(11, 16)
            val hour = timePart.substring(0, 2).toInt()
            val period = if (hour < 12) "上午" else "下午"
            val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val displayTime = String.format("%d:%s", displayHour, timePart.substring(3))
            "$period$displayTime"
        } catch (e: Exception) {
            isoTime
        }
    }

    private fun formatVolume(volume: Double): String {
        return when {
            volume >= 1_000_000_000 -> "%.2fB".format(volume / 1_000_000_000)
            volume >= 1_000_000 -> "%.2fM".format(volume / 1_000_000)
            volume >= 1_000 -> "%.2fK".format(volume / 1_000)
            else -> "%.2f".format(volume)
        }
    }
}


/**
 * 交易计划状态枚举
 */
enum class TradePlanStatus(val value: String) {
    ALL("ALL"),
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    EXECUTING("EXECUTING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    companion object {
        fun fromValue(value: String?): TradePlanStatus {
            return values().find { it.value == value } ?: PENDING
        }
    }
}

/**
 * 执行结果枚举
 */
enum class ExecutionResult(val value: String) {
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    STOPPED("STOPPED");

    companion object {
        fun fromValue(value: String?): ExecutionResult {
            return values().find { it.value == value } ?: FAILED
        }
    }
}

/**
 * OHLCV 数据类
 */
data class Ohlcv(
    val open: Double? = null,
    val high: Double? = null,
    val low: Double? = null,
    val close: Double? = null,
    val volume: Double? = null
)
