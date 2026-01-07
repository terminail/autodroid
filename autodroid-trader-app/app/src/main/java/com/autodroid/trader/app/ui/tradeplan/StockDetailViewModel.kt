package com.autodroid.trader.app.ui.tradeplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodroid.trader.app.ui.tradeplan.StockDetailItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StockDetailViewModel : ViewModel() {

    private val _stockDetailItems = MutableStateFlow<List<StockDetailItem>>(emptyList())
    val stockDetailItems: StateFlow<List<StockDetailItem>> = _stockDetailItems

    private val _currentTimeframe = MutableStateFlow("1m")
    val currentTimeframe: StateFlow<String> = _currentTimeframe

    init {
        loadStockDetailData()
    }

    fun changeTimeframe(timeframe: String) {
        _currentTimeframe.value = timeframe
        loadKLineData(timeframe)
    }

    private fun loadStockDetailData() {
        viewModelScope.launch {
            val items = mutableListOf<StockDetailItem>()

            items.add(
                StockDetailItem.DailyInfo(
                    price = 12.34,
                    changePercent = 2.56,
                    changeAmount = 0.31,
                    volume = 12345678,
                    turnover = 152345678.90,
                    open = 12.10,
                    close = 12.34,
                    high = 12.50,
                    low = 12.05
                )
            )

            items.add(
                StockDetailItem.KLine(
                    timeframe = _currentTimeframe.value,
                    data = generateSampleKLineData()
                )
            )

            items.add(
                StockDetailItem.MACD(
                    data = generateSampleMACDData()
                )
            )

            items.add(
                StockDetailItem.Volume(
                    data = generateSampleVolumeData()
                )
            )

            items.add(
                StockDetailItem.News(
                    newsList = listOf(
                        StockDetailItem.News.NewsItem(
                            title = "公司发布2024年第三季度财报，净利润同比增长15%",
                            time = "2024-12-24 10:30:00",
                            url = null
                        ),
                        StockDetailItem.News.NewsItem(
                            title = "行业分析师上调公司评级至\"买入\"",
                            time = "2024-12-23 15:45:00",
                            url = null
                        ),
                        StockDetailItem.News.NewsItem(
                            title = "公司宣布与战略合作伙伴签署重要合作协议",
                            time = "2024-12-22 09:15:00",
                            url = null
                        )
                    )
                )
            )

            _stockDetailItems.value = items
        }
    }

    private fun loadKLineData(timeframe: String) {
        viewModelScope.launch {
            val updatedItems = _stockDetailItems.value.toMutableList()
            val kLineIndex = updatedItems.indexOfFirst { it is StockDetailItem.KLine }
            
            if (kLineIndex >= 0) {
                updatedItems[kLineIndex] = StockDetailItem.KLine(
                    timeframe = timeframe,
                    data = generateSampleKLineData()
                )
                _stockDetailItems.value = updatedItems
            }
        }
    }

    private fun generateSampleKLineData(): List<StockDetailItem.KLine.CandleData> {
        val data = mutableListOf<StockDetailItem.KLine.CandleData>()
        var price = 12.0
        val baseTime = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30

        repeat(30) { i ->
            val open = price
            val change = (Math.random() - 0.5) * 0.5
            val close = open + change
            val high = Math.max(open, close) + Math.random() * 0.2
            val low = Math.min(open, close) - Math.random() * 0.2
            val volume = (1000000 + Math.random() * 5000000).toLong()

            data.add(
                StockDetailItem.KLine.CandleData(
                    timestamp = baseTime + i * 1000L * 60 * 60 * 24,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            )

            price = close
        }

        return data
    }

    private fun generateSampleMACDData(): List<StockDetailItem.MACD.MACDData> {
        val data = mutableListOf<StockDetailItem.MACD.MACDData>()
        val baseTime = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30

        repeat(30) { i ->
            val dif = Math.sin(i * 0.3) * 0.5
            val dea = Math.sin((i - 1) * 0.3) * 0.4
            val macd = 2 * (dif - dea)

            data.add(
                StockDetailItem.MACD.MACDData(
                    timestamp = baseTime + i * 1000L * 60 * 60 * 24,
                    dif = dif,
                    dea = dea,
                    macd = macd
                )
            )
        }

        return data
    }

    private fun generateSampleVolumeData(): List<StockDetailItem.Volume.VolumeData> {
        val data = mutableListOf<StockDetailItem.Volume.VolumeData>()
        val baseTime = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30

        repeat(30) { i ->
            val volume = (1000000 + Math.random() * 5000000).toLong()
            val priceChange = (Math.random() - 0.5) * 0.5

            data.add(
                StockDetailItem.Volume.VolumeData(
                    timestamp = baseTime + i * 1000L * 60 * 60 * 24,
                    volume = volume,
                    price = priceChange
                )
            )
        }

        return data
    }
}
