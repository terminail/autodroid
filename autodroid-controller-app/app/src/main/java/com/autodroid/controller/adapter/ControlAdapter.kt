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
    private val onServiceStartClick: (ControlItem.ServiceControlItem) -> Unit,
    private val onServiceStopClick: (ControlItem.ServiceControlItem) -> Unit,
    private val onServiceCheckStatusClick: (ControlItem.ServiceControlItem) -> Unit,
    private val onTestControlClick: (ControlItem.TestControlItem) -> Unit,
    private val onTestMingYongBaoClick: (ControlItem.TestControlItem) -> Unit,
    private val onTestGetPageXmlClick: (ControlItem.TestControlItem) -> Unit,
    private val onSettingsClick: (ControlItem.SettingsItem) -> Unit,
    private val onCheckAppiumStatusClick: (ControlItem.AppiumControlItem) -> Unit,
    private val onAppiumAppInfoClick: (ControlItem.AppiumControlItem) -> Unit,
    private val onShareToWechatClick: (ControlItem.AppiumControlItem) -> Unit,
    private val onGetUIA2InfoClick: (ControlItem.UIA2StatusItem) -> Unit,
    private val onClearUIA2InfoClick: (ControlItem.UIA2StatusItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ControlItem> = emptyList()

    fun setData(newItems: List<ControlItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ControlItem.HeaderItem -> VIEW_TYPE_HEADER
            is ControlItem.ServiceControlItem -> VIEW_TYPE_SERVICE_CONTROL
            is ControlItem.AppiumControlItem -> VIEW_TYPE_APPIUM_CONTROL
            is ControlItem.TestControlItem -> VIEW_TYPE_TEST_CONTROL
            is ControlItem.SettingsItem -> VIEW_TYPE_SETTINGS
            is ControlItem.UIA2StatusItem -> VIEW_TYPE_UIA2_STATUS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_SERVICE_CONTROL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_service_control, parent, false)
                ServiceControlViewHolder(view)
            }
            VIEW_TYPE_APPIUM_CONTROL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_appium_uia2_control, parent, false)
                AppiumControlViewHolder(view)
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
            VIEW_TYPE_UIA2_STATUS -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_appium_uia2_status, parent, false)
                UIA2StatusViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ControlItem.HeaderItem -> (holder as HeaderViewHolder).bind(item)
            is ControlItem.ServiceControlItem -> (holder as ServiceControlViewHolder).bind(item)
            is ControlItem.AppiumControlItem -> (holder as AppiumControlViewHolder).bind(item)
            is ControlItem.TestControlItem -> (holder as TestControlViewHolder).bind(item)
            is ControlItem.SettingsItem -> (holder as SettingsViewHolder).bind(item)
            is ControlItem.UIA2StatusItem -> (holder as UIA2StatusViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView as TextView
        
        fun bind(header: ControlItem.HeaderItem) {
            titleTextView.text = header.title
        }
    }

    inner class ServiceControlViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvServiceStatus: TextView = itemView.findViewById(R.id.tvServiceStatus)
        private val tvLastCheckTime: TextView = itemView.findViewById(R.id.tvLastCheckTime)
        private val btnStartService: Button = itemView.findViewById(R.id.btnStartService)
        private val btnStopService: Button = itemView.findViewById(R.id.btnStopService)
        private val btnCheckServiceStatus: Button = itemView.findViewById(R.id.btnCheckServiceStatus)
        
        fun bind(serviceControl: ControlItem.ServiceControlItem) {
            // 更新状态显示
            updateServiceStatus(serviceControl.status)
            
            // 更新最后检查时间
            updateLastCheckTime(serviceControl.lastCheckTime)
            
            btnStartService.setOnClickListener {
                onServiceStartClick(serviceControl)
            }
            btnStopService.setOnClickListener {
                onServiceStopClick(serviceControl)
            }
            btnCheckServiceStatus.setOnClickListener {
                onServiceCheckStatusClick(serviceControl)
            }
        }
        
        private fun updateServiceStatus(status: com.autodroid.controller.model.ServiceStatus) {
            when (status) {
                com.autodroid.controller.model.ServiceStatus.RUNNING -> {
                    tvServiceStatus.text = "运行中"
                    tvServiceStatus.setTextColor(itemView.context.getColor(R.color.color_success))
                    btnStartService.isEnabled = false
                    btnStopService.isEnabled = true
                }
                com.autodroid.controller.model.ServiceStatus.STOPPED -> {
                    tvServiceStatus.text = "已停止"
                    tvServiceStatus.setTextColor(itemView.context.getColor(R.color.color_error))
                    btnStartService.isEnabled = true
                    btnStopService.isEnabled = false
                }
                com.autodroid.controller.model.ServiceStatus.STARTING -> {
                    tvServiceStatus.text = "启动中..."
                    tvServiceStatus.setTextColor(itemView.context.getColor(R.color.color_warning))
                    btnStartService.isEnabled = false
                    btnStopService.isEnabled = false
                }
                com.autodroid.controller.model.ServiceStatus.STOPPING -> {
                    tvServiceStatus.text = "停止中..."
                    tvServiceStatus.setTextColor(itemView.context.getColor(R.color.color_warning))
                    btnStartService.isEnabled = false
                    btnStopService.isEnabled = false
                }
            }
        }
        
        private fun updateLastCheckTime(lastCheckTime: String?) {
            tvLastCheckTime.text = lastCheckTime ?: "未检查"
        }
    }

    inner class AppiumControlViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvServerStatus: TextView = itemView.findViewById(R.id.tvServerStatus)
        private val tvLastCheckTime: TextView = itemView.findViewById(R.id.tvLastCheckTime)
        private val tvServerVersion: TextView = itemView.findViewById(R.id.tvServerVersion)
        private val tvServerPort: TextView = itemView.findViewById(R.id.tvServerPort)
        private val tvStartInstructions: TextView = itemView.findViewById(R.id.tvStartInstructions)
        private val btnCheckStatus: Button = itemView.findViewById(R.id.btnCheckAppiumStatus)
        private val btnAppInfo: Button = itemView.findViewById(R.id.btnAppiumAppInfo)
        private val btnShareToWechat: Button = itemView.findViewById(R.id.btnShareAdbCommands)
        
        fun bind(appiumControl: ControlItem.AppiumControlItem) {
            // 更新状态显示
            updateServerStatus(appiumControl.status)
            
            // 更新最后检查时间
            updateLastCheckTime(appiumControl.lastCheckTime)
            
            btnCheckStatus.setOnClickListener {
                // 只检查状态，不触发服务器控制
                onCheckAppiumStatusClick(appiumControl)
            }
            
            btnAppInfo.setOnClickListener {
                // 打开应用信息界面
                onAppiumAppInfoClick(appiumControl)
            }
            
            btnShareToWechat.setOnClickListener {
                // 分享到微信
                onShareToWechatClick(appiumControl)
            }
        }
        
        private fun updateServerStatus(status: AppiumStatus) {
            when (status) {
                AppiumStatus.RUNNING -> {
                    tvServerStatus.text = "运行中"
                    tvServerStatus.setTextColor(itemView.context.getColor(R.color.color_success))
                }
                AppiumStatus.STOPPED -> {
                    tvServerStatus.text = "已停止"
                    tvServerStatus.setTextColor(itemView.context.getColor(R.color.color_error))
                }
                AppiumStatus.STARTING -> {
                    tvServerStatus.text = "启动中..."
                    tvServerStatus.setTextColor(itemView.context.getColor(R.color.color_warning))
                }
                AppiumStatus.STOPPING -> {
                    tvServerStatus.text = "停止中..."
                    tvServerStatus.setTextColor(itemView.context.getColor(R.color.color_warning))
                }
            }
        }
        
        private fun updateLastCheckTime(lastCheckTime: String?) {
            tvLastCheckTime.text = lastCheckTime ?: "未检查"
        }
    }

    inner class TestControlViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnTestTask: Button = itemView.findViewById(R.id.btnTestTask)
        private val btnTestMingYongBao: Button = itemView.findViewById(R.id.btnTestMingYongBao)
        private val btnTestGetPageXml: Button = itemView.findViewById(R.id.btnTestGetPageXml)
        
        fun bind(testControl: ControlItem.TestControlItem) {
            btnTestTask.setOnClickListener {
                onTestControlClick(testControl)
            }
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

    inner class UIA2StatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUIA2Status: TextView = itemView.findViewById(R.id.tvUIA2Status)
        private val tvUIA2Version: TextView = itemView.findViewById(R.id.tvUIA2Version)
        private val tvUIA2Port: TextView = itemView.findViewById(R.id.tvUIA2Port)
        private val tvSessionId: TextView = itemView.findViewById(R.id.tvSessionId)
        private val tvServiceMessage: TextView = itemView.findViewById(R.id.tvServiceMessage)
        private val btnGetUIA2Info: Button = itemView.findViewById(R.id.btnGetUIA2Info)
        private val btnClearUIA2Info: Button = itemView.findViewById(R.id.btnClearUIA2Info)
        
        fun bind(uia2Status: ControlItem.UIA2StatusItem) {
            tvUIA2Status.text = uia2Status.status
            tvUIA2Version.text = uia2Status.version
            tvUIA2Port.text = uia2Status.port
            tvSessionId.text = uia2Status.sessionId
            tvServiceMessage.text = uia2Status.message
            
            btnGetUIA2Info.setOnClickListener {
                onGetUIA2InfoClick(uia2Status)
            }
            btnClearUIA2Info.setOnClickListener {
                onClearUIA2InfoClick(uia2Status)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_SERVICE_CONTROL = 1
        private const val VIEW_TYPE_APPIUM_CONTROL = 2
        private const val VIEW_TYPE_TEST_CONTROL = 3
        private const val VIEW_TYPE_SETTINGS = 4
        private const val VIEW_TYPE_UIA2_STATUS = 5
    }
}