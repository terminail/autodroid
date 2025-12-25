package com.autodroid.trader.ui.tradeplan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.R
import com.autodroid.trader.ui.tradeplan.StockDetailItem

class NewsAdapter(
    private var newsList: List<StockDetailItem.News.NewsItem>
) : RecyclerView.Adapter<NewsAdapter.NewsItemViewHolder>() {

    fun updateItems(newNewsList: List<StockDetailItem.News.NewsItem>) {
        newsList = newNewsList
        notifyDataSetChanged()
    }

    class NewsItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView: TextView = view.findViewById(R.id.news_title)
        private val timeView: TextView = view.findViewById(R.id.news_time)

        fun bind(item: StockDetailItem.News.NewsItem) {
            titleView.text = item.title
            timeView.text = item.time
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsItemViewHolder, position: Int) {
        holder.bind(newsList[position])
    }

    override fun getItemCount(): Int = newsList.size
}
