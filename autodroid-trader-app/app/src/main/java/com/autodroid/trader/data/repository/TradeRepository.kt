package com.autodroid.trader.data.repository

import com.autodroid.trader.model.TradeData

class TradeRepository private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: TradeRepository? = null
        
        fun getInstance(): TradeRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TradeRepository()
                INSTANCE = instance
                instance
            }
        }
    }
    
    suspend fun getLatestTradeData(): TradeData {
        // 模拟获取最新交易数据
        return TradeData(
            price = "123.45",
            changePercent = "+2.34",
            volume = "123456"
        )
    }
}