package com.autodroid.trader.model

import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * 交易计划状态枚举
 */
enum class TradePlanStatus(val value: String) {
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
 * OHLCV 数据类
 */
data class Ohlcv(
    val open: Double? = null,
    val high: Double? = null,
    val low: Double? = null,
    val close: Double? = null,
    val volume: Double? = null
)

/**
 * Trade plan list item data class
 */
data class TradePlan(
    val id: String? = null,
    val script_id: String? = null,
    val name: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val description: String? = null,
    val exchange: String? = null,
    val symbol: String? = null,
    val symbol_name: String? = null,
    val ohlcv: Ohlcv? = null,
    val change_percent: Double? = null,
    val data: JsonObject? = null,
    val status: String? = null,
    val executionStatus: String? = null,
    val executionResult: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    companion object {
        private val gson = Gson()

        fun fromJson(json: String): TradePlan {
            return gson.fromJson(json, TradePlan::class.java)
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
            val timePart = isoTime.substring(11, 16)
            timePart
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
