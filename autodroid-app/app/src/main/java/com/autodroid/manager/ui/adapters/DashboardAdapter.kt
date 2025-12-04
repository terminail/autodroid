package com.autodroid.manager.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.manager.R
import com.autodroid.manager.model.DashboardItem

class DashboardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    companion object {
        const val TYPE_SERVER_CONNECTION = 0
        const val TYPE_WIFI = 1
        const val TYPE_DEVICE = 2
        const val TYPE_APK = 3
    }
    
    private val items = mutableListOf<DashboardItem>()
    
    // 点击监听器接口
    interface OnItemClickListener {
        fun onScanQrCodeClick()
        fun onScanApksClick()
    }
    
    private var listener: OnItemClickListener? = null
    
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }
    
    fun updateItems(newItems: List<DashboardItem>) {
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
            TYPE_SERVER_CONNECTION -> {
                val view = inflater.inflate(R.layout.item_server_connection, parent, false)
                ServerConnectionViewHolder(view)
            }
            TYPE_WIFI -> {
                val view = inflater.inflate(R.layout.item_wifi, parent, false)
                WiFiViewHolder(view)
            }
            TYPE_DEVICE -> {
                val view = inflater.inflate(R.layout.item_device, parent, false)
                DeviceViewHolder(view)
            }
            TYPE_APK -> {
                val view = inflater.inflate(R.layout.item_apk, parent, false)
                ApkViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ServerConnectionViewHolder -> holder.bind(item as DashboardItem.ServerConnectionItem)
            is WiFiViewHolder -> holder.bind(item as DashboardItem.WiFiInfoItem)
            is DeviceViewHolder -> holder.bind(item as DashboardItem.DeviceInfoItem)
            is ApkViewHolder -> holder.bind(item as DashboardItem.ApkInfoItem)
        }
    }
    
    override fun getItemCount(): Int = items.size
    
    // ViewHolder 类定义
    inner class ServerConnectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val connectionStatus: TextView = itemView.findViewById(R.id.connection_status)
        private val serverIp: TextView = itemView.findViewById(R.id.server_ip)
        private val serverPort: TextView = itemView.findViewById(R.id.server_port)
        private val serverStatus: TextView = itemView.findViewById(R.id.server_status)
        private val apiEndpoint: TextView = itemView.findViewById(R.id.api_endpoint)
        private val scanQrButton: Button = itemView.findViewById(R.id.scan_qr_button)
        
        init {
            scanQrButton.setOnClickListener {
                listener?.onScanQrCodeClick()
            }
        }
        
        fun bind(item: DashboardItem.ServerConnectionItem) {
            connectionStatus.text = item.status
            serverIp.text = item.serverIp
            serverPort.text = item.serverPort
            serverStatus.text = item.serverStatus
            apiEndpoint.text = item.apiEndpoint
            scanQrButton.visibility = if (item.showQrButton) View.VISIBLE else View.GONE
        }
    }
    
    inner class WiFiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val wifiName: TextView = itemView.findViewById(R.id.wifi_name)
        private val wifiIp: TextView = itemView.findViewById(R.id.wifi_ip)
        private val wifiStatus: TextView = itemView.findViewById(R.id.wifi_status)
        
        fun bind(item: DashboardItem.WiFiInfoItem) {
            wifiName.text = item.ssid
            wifiIp.text = item.ipAddress
            wifiStatus.text = if (item.isConnected) "Connected" else "Disconnected"
        }
    }
    
    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deviceUdid: TextView = itemView.findViewById(R.id.device_udid)
        private val userId: TextView = itemView.findViewById(R.id.user_id)
        private val deviceName: TextView = itemView.findViewById(R.id.device_name)
        private val platform: TextView = itemView.findViewById(R.id.platform)
        private val deviceModel: TextView = itemView.findViewById(R.id.device_model)
        private val deviceStatus: TextView = itemView.findViewById(R.id.device_status)
        private val connectionTime: TextView = itemView.findViewById(R.id.connection_time)
        
        fun bind(item: DashboardItem.DeviceInfoItem) {
            deviceUdid.text = item.udid
            userId.text = item.userId
            deviceName.text = item.name
            platform.text = item.platform
            deviceModel.text = item.deviceModel
            deviceStatus.text = item.deviceStatus
            connectionTime.text = item.connectionTime
        }
    }
    
    inner class ApkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val scanApksButton: Button = itemView.findViewById(R.id.scan_apks_button)
        
        init {
            scanApksButton.setOnClickListener {
                listener?.onScanApksClick()
            }
        }
        
        fun bind(item: DashboardItem.ApkInfoItem) {
            // APK扫描按钮不需要绑定具体数据
        }
    }
}