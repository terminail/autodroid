package com.autodroid.guardiansdk.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.data.model.GuardianInfo

/**
 * 监护人列表适配器 - 根据监护人数量动态调整视图类型
 * 少于等于5个时使用大卡片视图，多于5个时使用列表视图
 */
class GuardianAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var guardians = listOf<GuardianInfo>()
    private var viewType = VIEW_TYPE_LIST // Default to list view

    companion object {
        private const val VIEW_TYPE_LARGE_CARD = 1
        private const val VIEW_TYPE_LIST = 2
    }

    fun updateData(guardianList: List<GuardianInfo>) {
        this.guardians = guardianList
        // Switch view type based on count
        this.viewType = if (guardianList.size <= 5) {
            VIEW_TYPE_LARGE_CARD
        } else {
            VIEW_TYPE_LIST
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LARGE_CARD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_guardian_large_card, parent, false)
                LargeCardViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_guardian_list, parent, false)
                ListViewViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val guardian = guardians[position]
        
        when (holder) {
            is LargeCardViewHolder -> holder.bind(guardian, position)
            is ListViewViewHolder -> holder.bind(guardian)
        }
    }

    override fun getItemCount(): Int = guardians.size

    // ViewHolder for large card view
    class LargeCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tv_guardian_name)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val lastContactText: TextView = itemView.findViewById(R.id.tv_last_contact_time)
        private val locationText: TextView = itemView.findViewById(R.id.tv_location)
        private val alarmMessageText: TextView = itemView.findViewById(R.id.tv_alarm_message)

        fun bind(guardian: GuardianInfo, position: Int) {
            nameText.text = guardian.name
            lastContactText.text = "最后联系: ${guardian.lastContactTime}"
            locationText.text = guardian.lastLocation
            alarmMessageText.text = guardian.lastAlarmMessage
            
            // Set default avatar or load from URI if available
            avatarImage.setImageResource(R.drawable.ic_people)
        }
    }

    // ViewHolder for list view (like WeChat)
    class ListViewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tv_guardian_name)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val lastContactText: TextView = itemView.findViewById(R.id.tv_last_contact_time)
        private val locationText: TextView = itemView.findViewById(R.id.tv_location)
        private val alarmMessageText: TextView = itemView.findViewById(R.id.tv_alarm_message)

        fun bind(guardian: GuardianInfo) {
            nameText.text = guardian.name
            lastContactText.text = guardian.lastContactTime
            locationText.text = guardian.lastLocation
            alarmMessageText.text = guardian.lastAlarmMessage
            
            // Set default avatar or load from URI if available
            avatarImage.setImageResource(R.drawable.ic_people)
        }
    }
}