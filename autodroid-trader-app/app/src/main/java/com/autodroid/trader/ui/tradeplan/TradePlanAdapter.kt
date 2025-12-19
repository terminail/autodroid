// TradePlanAdapter.kt
package com.autodroid.trader.ui.tradeplan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.R
import com.autodroid.trader.model.TradePlan
import com.autodroid.trader.ui.tradeplan.TradePlanAdapter.TradePlanViewHolder

class TradePlanAdapter(
    private var tradeplans: MutableList<TradePlan>?,
    private val listener: OnTradePlanClickListener?
) : RecyclerView.Adapter<TradePlanViewHolder?>() {
    interface OnTradePlanClickListener {
        fun onTradePlanClick(tradePlan: TradePlan?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TradePlanViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_tradeplan, parent, false)
        return TradePlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: TradePlanViewHolder, position: Int) {
        val tradePlan = tradeplans!!.get(position)
        holder.bind(tradePlan)
    }

    override fun getItemCount(): Int {
        return if (tradeplans != null) tradeplans!!.size else 0
    }

    fun updateTradePlans(newTradePlans: MutableList<TradePlan>?) {
        this.tradeplans = newTradePlans
        notifyDataSetChanged()
    }

    inner class TradePlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView
        private val descriptionTextView: TextView
        private val packageTextView: TextView

        init {
            nameTextView = itemView.findViewById<TextView>(R.id.tradeplan_item_name)
            descriptionTextView = itemView.findViewById<TextView>(R.id.tradeplan_item_description)
            packageTextView = itemView.findViewById<TextView>(R.id.tradeplan_item_package)
        }

        fun bind(tradePlan: TradePlan) {
            // Use title if available, otherwise use name
            val displayName = tradePlan.title ?: tradePlan.name ?: "Unknown Trade Plan"
            nameTextView.setText(displayName)

            // Use subtitle if available, otherwise use description
            val displayDescription = tradePlan.subtitle ?: tradePlan.description ?: ""
            if (displayDescription.isEmpty()) {
                descriptionTextView.setVisibility(View.GONE)
            } else {
                descriptionTextView.setVisibility(View.VISIBLE)
                descriptionTextView.setText(displayDescription)
            }

            // Show status if available
            if (!tradePlan.status.isNullOrEmpty()) {
                packageTextView.setText(tradePlan.status)
                packageTextView.setVisibility(View.VISIBLE)
            } else {
                packageTextView.setVisibility(View.GONE)
            }

            itemView.setOnClickListener(View.OnClickListener { v: View? ->
                if (listener != null) {
                    listener.onTradePlanClick(tradePlan)
                }
            })
        }
    }
}