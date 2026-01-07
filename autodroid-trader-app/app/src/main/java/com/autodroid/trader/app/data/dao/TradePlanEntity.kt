package com.autodroid.trader.app.data.dao

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.autodroid.trader.app.network.Ohlcv
import com.autodroid.trader.app.network.TradePlanResponse
import com.google.gson.JsonObject

/**
 * 交易计划信息实体类
 * 用于Room数据库持久化存储交易计划信息
 * 字段顺序与TradePlanResponse保持一致，便于对比和维护
 */
@Entity(tableName = "trade_plans")
data class TradePlanEntity(
    // ========== 服务器端字段（与TradePlanResponse一致） ==========
    
    @PrimaryKey
    var id: String,
    var script_id: String? = null,
    var name: String? = null,
    var title: String? = null,
    var subtitle: String? = null,
    var description: String? = null,
    var exchange: String? = null,
    var symbol: String? = null,
    var symbol_name: String? = null,
    var ohlcv: Ohlcv? = null,
    var change_percent: Double? = null,
    var data: JsonObject? = null,
    var status: String? = null,
    var executable: Boolean? = null,
    var executionStatus: String? = null,
    var executionResult: String? = null,
    var startTime: String? = null,
    var endTime: String? = null,
    var createdAt: String? = null,
    var updatedAt: String? = null
) {
    constructor() : this(
        id = "",
        script_id = null,
        name = null,
        title = null,
        subtitle = null,
        description = null,
        exchange = null,
        symbol = null,
        symbol_name = null,
        ohlcv = null,
        change_percent = null,
        data = null,
        status = null,
        executable = null,
        executionStatus = null,
        executionResult = null,
        startTime = null,
        endTime = null,
        createdAt = null,
        updatedAt = null
    )
    
    fun getDisplayName(): String = title ?: name ?: id
    
    fun getDisplayDescription(): String = subtitle ?: description ?: ""
    
    fun getDisplayTime(): String {
        return try {
            createdAt?.let { formatTime(it) } ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun formatTime(isoTime: String): String {
        return try {
            val datePart = isoTime.substring(0, 10)
            val timePart = isoTime.substring(11, 16)
            
            val year = datePart.substring(0, 4).toInt()
            val month = datePart.substring(5, 7).toInt() - 1
            val day = datePart.substring(8, 10).toInt()
            val hour = timePart.substring(0, 2).toInt()
            
            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, month, day, 0, 0, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            
            val today = java.util.Calendar.getInstance()
            today.set(java.util.Calendar.HOUR_OF_DAY, 0)
            today.set(java.util.Calendar.MINUTE, 0)
            today.set(java.util.Calendar.SECOND, 0)
            today.set(java.util.Calendar.MILLISECOND, 0)
            
            val diffDays = ((today.timeInMillis - calendar.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
            
            return when {
                diffDays == 0 -> {
                    val period = if (hour < 12) "上午" else "下午"
                    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                    val displayTime = String.format("%d:%s", displayHour, timePart.substring(3))
                    "$period$displayTime"
                }
                diffDays == 1 -> "昨天"
                diffDays == 2 -> "前天"
                diffDays in 3..5 -> {
                    val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                    val weekdays = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
                    weekdays[dayOfWeek - 1]
                }
                else -> "${month + 1}月${day}日"
            }
        } catch (e: Exception) {
            isoTime
        }
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
    
    private fun formatVolume(volume: Double): String {
        return when {
            volume >= 1_000_000_000 -> "%.2fB".format(volume / 1_000_000_000)
            volume >= 1_000_000 -> "%.2fM".format(volume / 1_000_000)
            volume >= 1_000 -> "%.2fK".format(volume / 1_000)
            else -> "%.2f".format(volume)
        }
    }
    
    companion object {
        fun fromTradePlan(tradePlanResponse: TradePlanResponse): TradePlanEntity {
            val entity = TradePlanEntity(
                id = tradePlanResponse.id ?: java.util.UUID.randomUUID().toString(),
                script_id = tradePlanResponse.script_id,
                name = tradePlanResponse.name,
                title = tradePlanResponse.title,
                subtitle = tradePlanResponse.subtitle,
                description = tradePlanResponse.description,
                exchange = tradePlanResponse.exchange,
                symbol = tradePlanResponse.symbol,
                symbol_name = tradePlanResponse.symbol_name,
                ohlcv = tradePlanResponse.ohlcv,
                change_percent = tradePlanResponse.change_percent,
                data = tradePlanResponse.data,
                status = tradePlanResponse.status,
                executable = tradePlanResponse.executable,
                executionStatus = tradePlanResponse.executionStatus,
                executionResult = tradePlanResponse.executionResult,
                startTime = tradePlanResponse.startTime,
                endTime = tradePlanResponse.endTime,
                createdAt = tradePlanResponse.createdAt,
                updatedAt = tradePlanResponse.updatedAt
            )
            android.util.Log.d("TradePlanEntity", "fromTradePlan: 创建实体 id=${entity.id}, name=${entity.name}, status=${entity.status}")
            return entity
        }
        
        fun detailed(
            id: String,
            script_id: String? = null,
            name: String? = null,
            title: String? = null,
            subtitle: String? = null,
            description: String? = null,
            exchange: String? = null,
            symbol: String? = null,
            symbol_name: String? = null,
            ohlcv: Ohlcv? = null,
            change_percent: Double? = null,
            data: JsonObject? = null,
            status: String? = null,
            executable: Boolean? = null,
            executionStatus: String? = null,
            executionResult: String? = null,
            startTime: String? = null,
            endTime: String? = null,
            createdAt: String? = null,
            updatedAt: String? = null
        ): TradePlanEntity {
            return TradePlanEntity(
                id = id,
                script_id = script_id,
                name = name,
                title = title,
                subtitle = subtitle,
                description = description,
                exchange = exchange,
                symbol = symbol,
                symbol_name = symbol_name,
                ohlcv = ohlcv,
                change_percent = change_percent,
                data = data,
                status = status,
                executable = executable,
                executionStatus = executionStatus,
                executionResult = executionResult,
                startTime = startTime,
                endTime = endTime,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
}
