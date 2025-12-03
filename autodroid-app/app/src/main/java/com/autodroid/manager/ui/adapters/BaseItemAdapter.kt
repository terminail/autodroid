package com.autodroid.manager.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.manager.R

class BaseItemAdapter(
    private var items: List<MutableMap<String?, Any?>?>?,
    private val listener: OnItemClickListener?,
    private val itemLayoutRes: Int
) : RecyclerView.Adapter<BaseItemAdapter.ItemViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(item: MutableMap<String?, Any?>?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(itemLayoutRes, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items?.get(position)
        holder.bind(item)
    }

    override fun getItemCount(): Int = items?.size ?: 0

    fun updateItems(newItems: List<MutableMap<String?, Any?>?>?) {
        this.items = newItems
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView? = itemView.findViewById(R.id.item_title)
        private val subtitleTextView: TextView? = itemView.findViewById(R.id.item_subtitle)
        private val statusTextView: TextView? = itemView.findViewById(R.id.item_status)

        fun bind(item: MutableMap<String?, Any?>?) {
            // Bind common fields
            titleTextView?.text = item?.get("title")?.toString() ?: "Unknown"
            subtitleTextView?.text = item?.get("subtitle")?.toString() ?: ""
            statusTextView?.text = item?.get("status")?.toString() ?: ""

            itemView.setOnClickListener {
                listener?.onItemClick(item)
            }
        }
    }
}