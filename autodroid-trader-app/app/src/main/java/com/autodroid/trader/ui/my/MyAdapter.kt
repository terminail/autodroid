package com.autodroid.trader.ui.my

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.R

class MyAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_LOGIN = 0
        const val TYPE_LOGOUT = 1
        const val TYPE_USER_QR_CODE = 2
    }

    private val items = mutableListOf<MyItem>()

    // 点击监听器接口
    interface OnItemClickListener {
        fun onLoginClick()
        fun onLogoutClick()
        fun onUserQRCodeClick()
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun updateItems(newItems: List<MyItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_LOGIN -> {
                val view = inflater.inflate(R.layout.item_login, parent, false)
                LoginViewHolder(view)
            }
            TYPE_LOGOUT -> {
                val view = inflater.inflate(R.layout.item_logout, parent, false)
                LogoutViewHolder(view)
            }
            TYPE_USER_QR_CODE -> {
                val view = inflater.inflate(R.layout.item_user_qrcode, parent, false)
                UserQRCodeViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is LoginViewHolder -> holder.bind(item as MyItem.ItemLogin)
            is LogoutViewHolder -> holder.bind(item as MyItem.ItemLogout)
            is UserQRCodeViewHolder -> holder.bind(item as MyItem.ItemUserQRCode)
        }
    }

    override fun getItemCount(): Int = items.size

    // ViewHolder 类定义
    inner class LoginViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemContainer: LinearLayout = itemView.findViewById(R.id.item_container)
        private val titleText: TextView = itemView.findViewById(R.id.item_title)
        private val descriptionText: TextView = itemView.findViewById(R.id.item_description)

        init {
            itemContainer.setOnClickListener {
                listener?.onLoginClick()
            }
        }

        fun bind(item: MyItem.ItemLogin) {
            titleText.text = item.title
            descriptionText.text = item.description
        }
    }

    inner class LogoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemContainer: LinearLayout = itemView.findViewById(R.id.item_container)
        private val titleText: TextView = itemView.findViewById(R.id.item_title)
        private val descriptionText: TextView = itemView.findViewById(R.id.item_description)

        init {
            itemContainer.setOnClickListener {
                listener?.onLogoutClick()
            }
        }

        fun bind(item: MyItem.ItemLogout) {
            titleText.text = item.title
            descriptionText.text = item.description
        }
    }

    inner class UserQRCodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemContainer: LinearLayout = itemView.findViewById(R.id.item_container)
        private val titleText: TextView = itemView.findViewById(R.id.item_title)
        private val descriptionText: TextView = itemView.findViewById(R.id.item_description)

        init {
            itemContainer.setOnClickListener {
                listener?.onUserQRCodeClick()
            }
        }

        fun bind(item: MyItem.ItemUserQRCode) {
            titleText.text = item.title
            descriptionText.text = item.description
        }
    }
}