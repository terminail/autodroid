package com.autodroid.trader.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.R

class DashboardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_SERVER = 0
        const val TYPE_WIFI = 1
        const val TYPE_DEVICE = 2
    }

    private val items = mutableListOf<DashboardItem>()

    // 点击监听器接口
    interface OnItemClickListener {
        fun onScanQrCodeClick()
        fun onManualInputClick()
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
            TYPE_SERVER -> {
                val view = inflater.inflate(R.layout.item_server, parent, false)
                ServerViewHolder(view)
            }
            TYPE_WIFI -> {
                val view = inflater.inflate(R.layout.item_wifi, parent, false)
                WiFiViewHolder(view)
            }
            TYPE_DEVICE -> {
                val view = inflater.inflate(R.layout.item_device, parent, false)
                DeviceViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ServerViewHolder -> holder.bind(item as DashboardItem.ItemServer)
            is WiFiViewHolder -> holder.bind(item as DashboardItem.ItemWiFi)
            is DeviceViewHolder -> holder.bind(item as DashboardItem.ItemDevice)
        }
    }

    override fun getItemCount(): Int = items.size

    // ViewHolder 类定义
    inner class ServerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val connectionStatus: TextView = itemView.findViewById(R.id.connection_status)
        private val serverStatus: TextView = itemView.findViewById(R.id.server_status)
        private val serverName: TextView = itemView.findViewById(R.id.server_name)
        private val serverHostname: TextView = itemView.findViewById(R.id.server_hostname)
        private val serverPlatform: TextView = itemView.findViewById(R.id.server_platform)
        private val apiEndpoint: TextView = itemView.findViewById(R.id.api_endpoint)
        private val discoveryMethod: TextView = itemView.findViewById(R.id.discovery_method)
        private val scanQrButton: Button = itemView.findViewById(R.id.scan_qr_button)
        private val manualSetButton: Button = itemView.findViewById(R.id.manual_set_button)

        init {
            scanQrButton.setOnClickListener {
                listener?.onScanQrCodeClick()
            }

            manualSetButton.setOnClickListener {
                listener?.onManualInputClick()
            }
        }

        fun bind(item: DashboardItem.ItemServer) {
            connectionStatus.text = item.status
            serverStatus.text = item.serverStatus
            serverName.text = item.serverName
            serverHostname.text = item.hostname
            serverPlatform.text = item.platform
            apiEndpoint.text = item.apiEndpoint
            discoveryMethod.text = item.discoveryMethod

            // 按钮始终显示且始终可以点击
            scanQrButton.visibility = View.VISIBLE
            scanQrButton.isEnabled = true

            manualSetButton.visibility = View.VISIBLE
            manualSetButton.isEnabled = true

        }
    }

    inner class WiFiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val wifiName: TextView = itemView.findViewById(R.id.wifi_name)
        private val wifiIp: TextView = itemView.findViewById(R.id.wifi_ip)
        private val wifiStatus: TextView = itemView.findViewById(R.id.wifi_status)

        fun bind(item: DashboardItem.ItemWiFi) {
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

        fun bind(item: DashboardItem.ItemDevice) {
            deviceUdid.text = item.udid
            userId.text = item.userId
            deviceName.text = item.name
            platform.text = item.platform
            deviceModel.text = item.deviceModel
            deviceStatus.text = item.deviceStatus
            connectionTime.text = item.connectionTime
        }
    }
}