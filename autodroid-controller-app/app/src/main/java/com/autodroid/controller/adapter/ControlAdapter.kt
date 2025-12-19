package com.autodroid.controller.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.controller.R
import com.autodroid.controller.model.AppiumStatus
import com.autodroid.controller.model.ControlItem

class ControlAdapter(
    private val onTestMingYongBaoClick: (ControlItem.TestControlItem) -> Unit,
    private val onTestGetPageXmlClick: (ControlItem.TestControlItem) -> Unit,
    private val onSettingsClick: (ControlItem.SettingsItem) -> Unit,
    private val onCheckAppiumStatusClick: (ControlItem.AppiumStatusItem) -> Unit,
    private val onClearAppiumInfoClick: (ControlItem.AppiumStatusItem) -> Unit,
    private val onShareAdbCommandsClick: (ControlItem.AppiumStatusItem) -> Unit,
    private val onAppiumAppInfoClick: (ControlItem.AppiumStatusItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TEST_CONTROL = 1
        private const val VIEW_TYPE_SETTINGS = 2
        private const val VIEW_TYPE_APPIUM_STATUS = 3
    }
    
    private var items: List<ControlItem> = emptyList()

    fun setData(newItems: List<ControlItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ControlItem.HeaderItem -> VIEW_TYPE_HEADER
            is ControlItem.TestControlItem -> VIEW_TYPE_TEST_CONTROL
            is ControlItem.SettingsItem -> VIEW_TYPE_SETTINGS
            is ControlItem.AppiumStatusItem -> VIEW_TYPE_APPIUM_STATUS
            else -> throw IllegalArgumentException("Unknown item type: ${items[position]}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_TEST_CONTROL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_test_control, parent, false)
                TestControlViewHolder(view)
            }
            VIEW_TYPE_SETTINGS -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_settings, parent, false)
                SettingsViewHolder(view)
            }
            VIEW_TYPE_APPIUM_STATUS -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_appium, parent, false)
                AppiumStatusViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ControlItem.HeaderItem -> (holder as HeaderViewHolder).bind(item)
            is ControlItem.TestControlItem -> (holder as TestControlViewHolder).bind(item)
            is ControlItem.SettingsItem -> (holder as SettingsViewHolder).bind(item)
            is ControlItem.AppiumStatusItem -> (holder as AppiumStatusViewHolder).bind(item)
            else -> throw IllegalArgumentException("Unknown item type: ${items[position]}")
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView as TextView
        
        fun bind(header: ControlItem.HeaderItem) {
            titleTextView.text = header.title
        }
    }

    inner class TestControlViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnTestMingYongBao: Button = itemView.findViewById(R.id.btnTestMingYongBao)
        private val btnTestGetPageXml: Button = itemView.findViewById(R.id.btnTestGetPageXml)
        
        fun bind(testControl: ControlItem.TestControlItem) {
            btnTestMingYongBao.setOnClickListener {
                onTestMingYongBaoClick(testControl)
            }
            btnTestGetPageXml.setOnClickListener {
                onTestGetPageXmlClick(testControl)
            }
        }
    }

    inner class SettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnOpenSettings: Button = itemView.findViewById(R.id.btnOpenSettings)
        private val btnOpenAppSettings: Button = itemView.findViewById(R.id.btnOpenAppSettings)
        
        fun bind(settings: ControlItem.SettingsItem) {
            btnOpenSettings.setOnClickListener {
                onSettingsClick(settings)
            }
            btnOpenAppSettings.setOnClickListener {
                onSettingsClick(settings)
            }
        }
    }



    inner class AppiumStatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProcessStatus: TextView = itemView.findViewById(R.id.tvProcessStatus)
        private val tvPortStatus: TextView = itemView.findViewById(R.id.tvPortStatus)
        private val tvHttpStatus: TextView = itemView.findViewById(R.id.tvHttpStatus)
        private val tvUIA2Version: TextView = itemView.findViewById(R.id.tvUIA2Version)
        private val tvUIA2Port: TextView = itemView.findViewById(R.id.tvUIA2Port)
        private val tvSessionId: TextView = itemView.findViewById(R.id.tvSessionId)
        private val tvServiceMessage: TextView = itemView.findViewById(R.id.tvServiceMessage)
        private val btnCheckStatus: Button = itemView.findViewById(R.id.btnCheckStatus)
        private val btnClearInfo: Button = itemView.findViewById(R.id.btnClearInfo)
        private val btnShareAdbCommands: Button = itemView.findViewById(R.id.btnShareAdbCommands)
        private val btnAppiumAppInfo: Button = itemView.findViewById(R.id.btnAppiumAppInfo)
        
        fun bind(appiumStatus: ControlItem.AppiumStatusItem) {
            tvProcessStatus.text = appiumStatus.processStatus
            tvPortStatus.text = appiumStatus.portStatus
            tvHttpStatus.text = appiumStatus.httpStatus
            tvUIA2Version.text = appiumStatus.version
            tvUIA2Port.text = appiumStatus.port
            tvSessionId.text = appiumStatus.sessionId
            tvServiceMessage.text = appiumStatus.message
            
            btnCheckStatus.setOnClickListener {
                onCheckAppiumStatusClick(appiumStatus)
            }
            btnClearInfo.setOnClickListener {
                onClearAppiumInfoClick(appiumStatus)
            }
            btnShareAdbCommands.setOnClickListener {
                onShareAdbCommandsClick(appiumStatus)
            }
            btnAppiumAppInfo.setOnClickListener {
                onAppiumAppInfoClick(appiumStatus)
            }
        }
    }
}