package com.autodroid.trader.app.ui.tradeplan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.app.R
import com.autodroid.trader.app.ui.tradeplan.StockDetailItem

class StockDetailAdapter(
    private var items: List<StockDetailItem>,
    private val onTimeframeChanged: (String) -> Unit
) : RecyclerView.Adapter<StockDetailViewHolder>() {

    fun updateItems(newItems: List<StockDetailItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    companion object {
        const val TYPE_DAILY_INFO = 0
        const val TYPE_KLINE = 1
        const val TYPE_MACD = 2
        const val TYPE_VOLUME = 3
        const val TYPE_NEWS = 4
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is StockDetailItem.DailyInfo -> TYPE_DAILY_INFO
            is StockDetailItem.KLine -> TYPE_KLINE
            is StockDetailItem.MACD -> TYPE_MACD
            is StockDetailItem.Volume -> TYPE_VOLUME
            is StockDetailItem.News -> TYPE_NEWS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockDetailViewHolder {
        val view = when (viewType) {
            TYPE_DAILY_INFO -> LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stock_daily, parent, false)
            TYPE_KLINE -> LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stock_kline, parent, false)
            TYPE_MACD -> LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stock_macd, parent, false)
            TYPE_VOLUME -> LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stock_volume, parent, false)
            TYPE_NEWS -> LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stock_news, parent, false)
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
        
        return when (viewType) {
            TYPE_DAILY_INFO -> StockDetailViewHolder.DailyInfoViewHolder(view)
            TYPE_KLINE -> StockDetailViewHolder.KLineViewHolder(view)
            TYPE_MACD -> StockDetailViewHolder.MACDViewHolder(view)
            TYPE_VOLUME -> StockDetailViewHolder.VolumeViewHolder(view)
            TYPE_NEWS -> StockDetailViewHolder.NewsViewHolder(view)
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: StockDetailViewHolder, position: Int) {
        when (val item = items[position]) {
            is StockDetailItem.DailyInfo -> {
                (holder as StockDetailViewHolder.DailyInfoViewHolder).bind(item)
            }
            is StockDetailItem.KLine -> {
                (holder as StockDetailViewHolder.KLineViewHolder).bind(item, onTimeframeChanged)
            }
            is StockDetailItem.MACD -> {
                (holder as StockDetailViewHolder.MACDViewHolder).bind(item)
            }
            is StockDetailItem.Volume -> {
                (holder as StockDetailViewHolder.VolumeViewHolder).bind(item)
            }
            is StockDetailItem.News -> {
                (holder as StockDetailViewHolder.NewsViewHolder).bind(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
