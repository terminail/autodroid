package com.autodroid.trader.ui.tradeplan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.R
import com.autodroid.trader.model.TradePlan

class TradePlanAdapter(
    private var items: MutableList<TradePlan>?,
    private val listener: OnItemClickListener?
) : RecyclerView.Adapter<TradePlanAdapter.ViewHolder>() {
    
    interface OnItemClickListener {
        fun onItemClick(item: TradePlan?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_trade_plan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items!![position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return if (items != null) items!!.size else 0
    }

    fun updateItems(newItems: MutableList<TradePlan>?) {
        this.items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView
        private val nameView: TextView
        private val timeView: TextView
        private val infoLine1View: TextView
        private val infoLine2View: TextView
        private val statusView: TextView

        init {
            iconView = itemView.findViewById(R.id.trade_plan_icon)
            nameView = itemView.findViewById(R.id.trade_plan_name)
            timeView = itemView.findViewById(R.id.trade_plan_time)
            infoLine1View = itemView.findViewById(R.id.trade_plan_info_line1)
            infoLine2View = itemView.findViewById(R.id.trade_plan_info_line2)
            statusView = itemView.findViewById(R.id.trade_plan_status)

            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(items!![position])
                }
            }
        }

        fun bind(item: TradePlan) {
            nameView.text = item.name ?: item.title ?: ""
            timeView.text = item.getDisplayTime()
            infoLine1View.text = item.getDisplayInfoLine1()
            infoLine2View.text = item.getDisplayInfoLine2()
            statusView.text = item.status ?: "PENDING"
        }
    }
}
