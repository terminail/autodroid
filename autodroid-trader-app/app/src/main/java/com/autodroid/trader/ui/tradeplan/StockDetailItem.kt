package com.autodroid.trader.ui.tradeplan

sealed class StockDetailItem {
    data class DailyInfo(
        val price: Double,
        val changePercent: Double,
        val changeAmount: Double,
        val volume: Long,
        val turnover: Double,
        val open: Double,
        val close: Double,
        val high: Double,
        val low: Double
    ) : StockDetailItem()

    data class KLine(
        val timeframe: String,
        val data: List<CandleData>
    ) : StockDetailItem() {
        data class CandleData(
            val timestamp: Long,
            val open: Double,
            val high: Double,
            val low: Double,
            val close: Double,
            val volume: Long
        )
    }

    data class MACD(
        val data: List<MACDData>
    ) : StockDetailItem() {
        data class MACDData(
            val timestamp: Long,
            val dif: Double,
            val dea: Double,
            val macd: Double
        )
    }

    data class Volume(
        val data: List<VolumeData>
    ) : StockDetailItem() {
        data class VolumeData(
            val timestamp: Long,
            val volume: Long,
            val price: Double
        )
    }

    data class News(
        val newsList: List<NewsItem>
    ) : StockDetailItem() {
        data class NewsItem(
            val title: String,
            val time: String,
            val url: String?
        )
    }
}