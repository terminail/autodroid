package com.autodroid.trader.ui.tradeplan

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.R


sealed class StockDetailViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    class DailyInfoViewHolder(view: View) : StockDetailViewHolder(view) {
        private val priceView: TextView = view.findViewById(R.id.stock_daily_price)
        private val changePercentView: TextView = view.findViewById(R.id.stock_daily_change_percent)
        private val changeAmountView: TextView = view.findViewById(R.id.stock_daily_change_amount)
        private val volumeView: TextView = view.findViewById(R.id.stock_daily_volume)
        private val turnoverView: TextView = view.findViewById(R.id.stock_daily_turnover)
        private val openView: TextView = view.findViewById(R.id.stock_daily_open)
        private val closeView: TextView = view.findViewById(R.id.stock_daily_close)
        private val highView: TextView = view.findViewById(R.id.stock_daily_high)
        private val lowView: TextView = view.findViewById(R.id.stock_daily_low)

        fun bind(item: StockDetailItem.DailyInfo) {
            priceView.text = String.format("%.2f", item.price)
            
            val changePercentText = if (item.changePercent >= 0) "+${String.format("%.2f", item.changePercent)}%" else "${String.format("%.2f", item.changePercent)}%"
            changePercentView.text = changePercentText
            changePercentView.setTextColor(itemView.context.getColor(
                if (item.changePercent >= 0) android.R.color.holo_red_dark else android.R.color.holo_green_dark
            ))
            
            val changeAmountText = if (item.changeAmount >= 0) "+${String.format("%.2f", item.changeAmount)}" else "${String.format("%.2f", item.changeAmount)}"
            changeAmountView.text = changeAmountText
            changeAmountView.setTextColor(itemView.context.getColor(
                if (item.changeAmount >= 0) android.R.color.holo_red_dark else android.R.color.holo_green_dark
            ))
            
            volumeView.text = formatVolume(item.volume)
            turnoverView.text = formatTurnover(item.turnover)
            openView.text = String.format("%.2f", item.open)
            closeView.text = String.format("%.2f", item.close)
            highView.text = String.format("%.2f", item.high)
            lowView.text = String.format("%.2f", item.low)
        }

        private fun formatVolume(volume: Long): String {
            return when {
                volume >= 100000000 -> String.format("%.2f亿", volume / 100000000.0)
                volume >= 10000 -> String.format("%.2f万", volume / 10000.0)
                else -> volume.toString()
            }
        }

        private fun formatTurnover(turnover: Double): String {
            return when {
                turnover >= 100000000 -> String.format("%.2f亿", turnover / 100000000.0)
                turnover >= 10000 -> String.format("%.2f万", turnover / 10000.0)
                else -> String.format("%.2f", turnover)
            }
        }
    }

    class KLineViewHolder(view: View) : StockDetailViewHolder(view) {
        private val chart = view.findViewById<CandleStickChart>(R.id.kline_chart)
        private val timeframeButtons = listOf(
            view.findViewById<TextView>(R.id.kline_timeframe_1m),
            view.findViewById<TextView>(R.id.kline_timeframe_5m),
            view.findViewById<TextView>(R.id.kline_timeframe_30m),
            view.findViewById<TextView>(R.id.kline_timeframe_60m),
            view.findViewById<TextView>(R.id.kline_timeframe_120m),
            view.findViewById<TextView>(R.id.kline_timeframe_day),
            view.findViewById<TextView>(R.id.kline_timeframe_week),
            view.findViewById<TextView>(R.id.kline_timeframe_month)
        )

        private var currentTimeframe = "1m"
        private var onTimeframeChanged: ((String) -> Unit)? = null

        fun bind(item: StockDetailItem.KLine, onTimeframeChanged: (String) -> Unit) {
            this.onTimeframeChanged = onTimeframeChanged
            currentTimeframe = item.timeframe
            updateTimeframeButtons()
            setupChart()
            loadData(item.data)
        }

        private fun updateTimeframeButtons() {
            val timeframeMap = mapOf(
                "1m" to 0, "5m" to 1, "30m" to 2, "60m" to 3,
                "120m" to 4, "day" to 5, "week" to 6, "month" to 7
            )
            
            timeframeButtons.forEachIndexed { index, button ->
                val isSelected = timeframeMap[currentTimeframe] == index
                button.setTextColor(itemView.context.getColor(
                    if (isSelected) android.R.color.white else android.R.color.black
                ))
                button.setBackgroundColor(itemView.context.getColor(
                    if (isSelected) android.R.color.holo_blue_light else android.R.color.darker_gray
                ))
                
                button.setOnClickListener {
                    val newTimeframe = timeframeMap.entries.firstOrNull { it.value == index }?.key ?: "1m"
                    if (newTimeframe != currentTimeframe) {
                        currentTimeframe = newTimeframe
                        updateTimeframeButtons()
                        onTimeframeChanged?.invoke(newTimeframe)
                    }
                }
            }
        }

        private fun setupChart() {
            chart.description.isEnabled = false
            chart.setDrawGridBackground(false)
            chart.setPinchZoom(true)
            chart.isScaleYEnabled = false
            chart.isDragEnabled = true
            chart.legend.isEnabled = false
            chart.axisRight.isEnabled = false
            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        }

        private fun loadData(data: List<StockDetailItem.KLine.CandleData>) {
            val entries = data.mapIndexed { index, candle ->
                CandleEntry(
                    index.toFloat(),
                    candle.high.toFloat(),
                    candle.low.toFloat(),
                    candle.open.toFloat(),
                    candle.close.toFloat()
                )
            }

            val dataSet = CandleDataSet(entries, "K线")
            dataSet.color = itemView.context.getColor(android.R.color.holo_red_dark)
            dataSet.shadowColor = itemView.context.getColor(android.R.color.darker_gray)
            dataSet.shadowWidth = 0.7f
            dataSet.decreasingColor = itemView.context.getColor(android.R.color.holo_green_dark)
            dataSet.decreasingPaintStyle = android.graphics.Paint.Style.FILL
            dataSet.increasingColor = itemView.context.getColor(android.R.color.holo_red_dark)
            dataSet.increasingPaintStyle = android.graphics.Paint.Style.FILL
            dataSet.neutralColor = itemView.context.getColor(android.R.color.darker_gray)

            val candleData = CandleData(dataSet)
            chart.data = candleData
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    }

    class MACDViewHolder(view: View) : StockDetailViewHolder(view) {
        private val chart = view.findViewById<LineChart>(R.id.macd_chart)

        fun bind(item: StockDetailItem.MACD) {
            setupChart()
            loadData(item.data)
        }

        private fun setupChart() {
            chart.description.isEnabled = false
            chart.setDrawGridBackground(false)
            chart.setPinchZoom(true)
            chart.isScaleYEnabled = false
            chart.isDragEnabled = true
            chart.legend.isEnabled = false
            chart.axisRight.isEnabled = false
            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        }

        private fun loadData(data: List<StockDetailItem.MACD.MACDData>) {
            val difEntries = data.mapIndexed { index, macd ->
                Entry(index.toFloat(), macd.dif.toFloat())
            }
            val deaEntries = data.mapIndexed { index, macd ->
                Entry(index.toFloat(), macd.dea.toFloat())
            }
            val macdEntries = data.mapIndexed { index, macd ->
                Entry(index.toFloat(), macd.macd.toFloat())
            }

            val difDataSet = LineDataSet(difEntries, "DIF")
            difDataSet.color = itemView.context.getColor(android.R.color.holo_orange_dark)
            difDataSet.setDrawCircles(false)
            difDataSet.lineWidth = 1f

            val deaDataSet = LineDataSet(deaEntries, "DEA")
            deaDataSet.color = itemView.context.getColor(android.R.color.holo_blue_dark)
            deaDataSet.setDrawCircles(false)
            deaDataSet.lineWidth = 1f

            val macdDataSet = LineDataSet(macdEntries, "MACD")
            macdDataSet.color = itemView.context.getColor(android.R.color.holo_purple)
            macdDataSet.setDrawCircles(false)
            macdDataSet.lineWidth = 1f

            val lineData = LineData(difDataSet, deaDataSet, macdDataSet)
            chart.data = lineData
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    }

    class VolumeViewHolder(view: View) : StockDetailViewHolder(view) {
        private val chart = view.findViewById<BarChart>(R.id.volume_chart)

        fun bind(item: StockDetailItem.Volume) {
            setupChart()
            loadData(item.data)
        }

        private fun setupChart() {
            chart.description.isEnabled = false
            chart.setDrawGridBackground(false)
            chart.setPinchZoom(true)
            chart.isScaleYEnabled = false
            chart.isDragEnabled = true
            chart.legend.isEnabled = false
            chart.axisRight.isEnabled = false
            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        }

        private fun loadData(data: List<StockDetailItem.Volume.VolumeData>) {
            val entries = data.mapIndexed { index, volume ->
                BarEntry(index.toFloat(), volume.volume.toFloat())
            }

            val dataSet = BarDataSet(entries, "成交量")
            val colors = data.map { volumeData ->
                itemView.context.getColor(
                    if (volumeData.price >= 0) android.R.color.holo_red_dark else android.R.color.holo_green_dark
                )
            }
            dataSet.colors = colors

            val barData = BarData(dataSet)
            chart.data = barData
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    }

    class NewsViewHolder(view: View) : StockDetailViewHolder(view) {
        private val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.news_recycler_view)

        fun bind(item: StockDetailItem.News) {
            val adapter = NewsAdapter(item.newsList)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(itemView.context)
        }
    }
}
