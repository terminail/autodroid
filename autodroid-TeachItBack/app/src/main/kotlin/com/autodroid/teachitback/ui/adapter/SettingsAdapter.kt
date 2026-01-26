package com.autodroid.teachitback.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.teachitback.R

/**
 * SettingsFragment适配器
 * 专门处理设置界面的异构数据显示
 */
class SettingsAdapter(
    private val onItemClick: ((SettingsItem) -> Unit)? = null,
    private var aiServiceTestStatus: Map<String, Boolean>? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private var items: List<SettingsItem> = emptyList()
    
    /**
     * 更新AI服务测试连接状态
     */
    fun updateAIServiceTestStatus(newStatus: Map<String, Boolean>) {
        aiServiceTestStatus = newStatus
        notifyDataSetChanged()
    }
    
    fun submitList(newItems: List<SettingsItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        return items[position].getType()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        
        return when (viewType) {
            SettingsItem.TYPE_SECTION_HEADER -> {
                val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                SectionHeaderViewHolder(view)
            }
            SettingsItem.TYPE_DARK_MODE_SWITCH_ITEM -> {
                val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                DarkModeSwitchViewHolder(view)
            }
            SettingsItem.TYPE_AUTO_SAVE_SWITCH_ITEM -> {
                val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                AutoSaveSwitchViewHolder(view)
            }
            SettingsItem.TYPE_LANGUAGE_SETTING_ITEM -> {
                val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                LanguageSettingViewHolder(view)
            }
            SettingsItem.TYPE_BACKUP_DATA_ITEM -> {
                val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                BackupDataViewHolder(view)
            }
            SettingsItem.TYPE_RESTORE_DATA_ITEM -> {
                val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                RestoreDataViewHolder(view)
            }
            SettingsItem.TYPE_CLEAR_ALL_DATA_BUTTON_ITEM -> {
                val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                ClearAllDataButtonViewHolder(view)
            }
            SettingsItem.TYPE_VERSION_INFO_ITEM -> {
                val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                VersionInfoViewHolder(view)
            }
            SettingsItem.TYPE_HELP_AND_FEEDBACK_ITEM -> {
                val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                HelpAndFeedbackViewHolder(view)
            }
            SettingsItem.TYPE_TENCENTCLOUD_AI_SERVICE_ITEM,
            SettingsItem.TYPE_DOUBAO_AI_SERVICE_ITEM,
            SettingsItem.TYPE_DEEPSEEK_AI_SERVICE_ITEM,
            SettingsItem.TYPE_MINIMAX_AI_SERVICE_ITEM,
            SettingsItem.TYPE_KIMI_AI_SERVICE_ITEM,
            SettingsItem.TYPE_OPENAI_AI_SERVICE_ITEM,
            SettingsItem.TYPE_ERNIE_AI_SERVICE_ITEM,
            SettingsItem.TYPE_QWEN_AI_SERVICE_ITEM,
            SettingsItem.TYPE_ZHIPU_AI_SERVICE_ITEM,
            SettingsItem.TYPE_SPARK_AI_SERVICE_ITEM,
            SettingsItem.TYPE_HUNYUAN_AI_SERVICE_ITEM,
            SettingsItem.TYPE_BAICHUAN_AI_SERVICE_ITEM,
            SettingsItem.TYPE_LINGYI_AI_SERVICE_ITEM,
            SettingsItem.TYPE_JIEYUE_AI_SERVICE_ITEM,
            SettingsItem.TYPE_CHATGLM_AI_SERVICE_ITEM,
            SettingsItem.TYPE_TINYBERT_AI_SERVICE_ITEM -> {
                val view = inflater.inflate(R.layout.item_setting_ai_service, parent, false)
                AIServiceViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        
        when (holder) {
            is SectionHeaderViewHolder -> holder.bind(item as SettingsItem.SectionHeaderItem)
            is DarkModeSwitchViewHolder -> holder.bind(item as SettingsItem.DarkModeSwitchItem)
            is AutoSaveSwitchViewHolder -> holder.bind(item as SettingsItem.AutoSaveSwitchItem)
            is LanguageSettingViewHolder -> holder.bind(item as SettingsItem.LanguageSettingItem)
            is BackupDataViewHolder -> holder.bind(item as SettingsItem.BackupDataItem)
            is RestoreDataViewHolder -> holder.bind(item as SettingsItem.RestoreDataItem)
            is ClearAllDataButtonViewHolder -> holder.bind(item as SettingsItem.ClearAllDataButtonItem)
            is VersionInfoViewHolder -> holder.bind(item as SettingsItem.VersionInfoItem)
            is HelpAndFeedbackViewHolder -> holder.bind(item as SettingsItem.HelpAndFeedbackItem)
            is AIServiceViewHolder -> {
                val serviceId = when (item) {
                    is SettingsItem.TencentCloudAIServiceItem -> "tencent-hunyuan"
                    is SettingsItem.DoubaoAIServiceItem -> "doubao"
                    is SettingsItem.DeepSeekAIServiceItem -> "deepseek"
                    is SettingsItem.MinimaxAIServiceItem -> "minimax"
                    is SettingsItem.KimiAIServiceItem -> "kimi"
                    is SettingsItem.OpenAIServiceItem -> "openai"
                    is SettingsItem.ErnieAIServiceItem -> "ernie"
                    is SettingsItem.QwenAIServiceItem -> "qwen"
                    is SettingsItem.ZhipuAIServiceItem -> "zhipu"
                    is SettingsItem.SparkAIServiceItem -> "spark"
                    is SettingsItem.HunyuanAIServiceItem -> "hunyuan"
                    is SettingsItem.BaichuanAIServiceItem -> "baichuan"
                    is SettingsItem.LingyiAIServiceItem -> "lingyi"
                    is SettingsItem.JieyueAIServiceItem -> "jieyue"
                    is SettingsItem.ChatGLMAIServiceItem -> "chatglm"
                    is SettingsItem.TinyBERTAIServiceItem -> "tinybert"
                    else -> ""
                }
                
                val testStatus = aiServiceTestStatus?.get(serviceId)
                
                when (item) {
                    is SettingsItem.DoubaoAIServiceItem -> holder.bind("豆包", item.isEnabled, item, testStatus)
                    is SettingsItem.DeepSeekAIServiceItem -> holder.bind("DeepSeek", item.isEnabled, item, testStatus)
                    is SettingsItem.MinimaxAIServiceItem -> holder.bind("MiniMax", item.isEnabled, item, testStatus)
                    is SettingsItem.KimiAIServiceItem -> holder.bind("Kimi", item.isEnabled, item, testStatus)
                    is SettingsItem.OpenAIServiceItem -> holder.bind("OpenAI", item.isEnabled, item, testStatus)
                    is SettingsItem.ErnieAIServiceItem -> holder.bind("文心一言", item.isEnabled, item, testStatus)
                    is SettingsItem.QwenAIServiceItem -> holder.bind("通义千问", item.isEnabled, item, testStatus)
                    is SettingsItem.ZhipuAIServiceItem -> holder.bind("智谱AI", item.isEnabled, item, testStatus)
                    is SettingsItem.SparkAIServiceItem -> holder.bind("讯飞星火", item.isEnabled, item, testStatus)
                    is SettingsItem.HunyuanAIServiceItem -> holder.bind("混元", item.isEnabled, item, testStatus)
                    is SettingsItem.BaichuanAIServiceItem -> holder.bind("百川", item.isEnabled, item, testStatus)
                    is SettingsItem.LingyiAIServiceItem -> holder.bind("零一万物", item.isEnabled, item, testStatus)
                    is SettingsItem.JieyueAIServiceItem -> holder.bind("阶跃", item.isEnabled, item, testStatus)
                    is SettingsItem.ChatGLMAIServiceItem -> holder.bind("ChatGLM", item.isEnabled, item, testStatus)
                    is SettingsItem.TinyBERTAIServiceItem -> holder.bind("TinyBERT", item.isEnabled, item, testStatus)
                    is SettingsItem.TencentCloudAIServiceItem -> holder.bind("腾讯云知识引擎", item.enabled, item, testStatus)
                    else -> holder.bind("未知AI服务", false, item, testStatus)
                }
            }
        }
    }
    
    override fun getItemCount(): Int = items.size
    
    /**
     * 分类标题ViewHolder
     */
    class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        
        fun bind(item: SettingsItem.SectionHeaderItem) {
            textView.text = "=== ${item.title} ==="
        }
    }
    
    /**
     * 深色模式开关ViewHolder
     */
    inner class DarkModeSwitchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        
        fun bind(item: SettingsItem.DarkModeSwitchItem) {
            val status = if (item.isChecked) "[ON]" else "[OFF]"
            textView.text = "$status 深色模式\n${item.subtitle}"
            
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
    
    /**
     * 自动保存开关ViewHolder
     */
    inner class AutoSaveSwitchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        
        fun bind(item: SettingsItem.AutoSaveSwitchItem) {
            val status = if (item.isChecked) "[ON]" else "[OFF]"
            textView.text = "$status 自动保存\n${item.subtitle}"
            
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
    
    /**
     * 语言设置ViewHolder
     */
    inner class LanguageSettingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        
        fun bind(item: SettingsItem.LanguageSettingItem) {
            textView.text = "语言设置\n${item.subtitle}"
            
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
    
    /**
     * 备份数据ViewHolder
     */
    inner class BackupDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        
        fun bind(item: SettingsItem.BackupDataItem) {
            textView.text = "备份数据\n${item.subtitle}"
            
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
    
    /**
     * 恢复数据ViewHolder
     */
    inner class RestoreDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        
        fun bind(item: SettingsItem.RestoreDataItem) {
            textView.text = "恢复数据\n${item.subtitle}"
            
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
    
    /**
     * 清除所有数据按钮ViewHolder
     */
    inner class ClearAllDataButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        
        fun bind(item: SettingsItem.ClearAllDataButtonItem) {
            val prefix = if (item.isDestructive) "[!]" else "[>]"
            textView.text = "$prefix 清除所有数据"
            
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
    
    /**
     * 版本信息ViewHolder
     */
    inner class VersionInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        
        fun bind(item: SettingsItem.VersionInfoItem) {
            textView.text = "版本信息\n${item.version}"
            
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
    
    /**
     * 帮助与反馈ViewHolder
     */
    inner class HelpAndFeedbackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        
        fun bind(item: SettingsItem.HelpAndFeedbackItem) {
            textView.text = "帮助与反馈\n${item.subtitle}"
            
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
    
    /**
     * AI服务项ViewHolder
     */
    inner class AIServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView? = itemView.findViewById(R.id.setting_title)
        private val descriptionTextView: TextView? = itemView.findViewById(R.id.setting_description)
        private val statusTextView: TextView? = itemView.findViewById(R.id.setting_status)
        
        fun bind(serviceName: String, isEnabled: Boolean, item: SettingsItem, testStatus: Boolean? = null) {
            // 第一行：服务名称
            titleTextView?.text = serviceName
            
            // 第二行：服务状态、配置状态和测试连接状态
            val serviceStatus = if (isEnabled) "☑️服务已启用" else "☐服务未启用"
            val configStatus = if (isEnabled) "☑️" else "☐"
            val testConnectionStatus = if (testStatus == true) "☑️测试连接" else "☐测试连接"
            
            val detailText = when (item) {
                is SettingsItem.TencentCloudAIServiceItem -> "$serviceStatus，SecretID, $configStatus Api Key，腾讯云AI服务，$testConnectionStatus"
                is SettingsItem.DoubaoAIServiceItem -> "$serviceStatus，Api Key，字节跳动AI助手，$testConnectionStatus"
                is SettingsItem.DeepSeekAIServiceItem -> "$serviceStatus，Api Key，深度求索AI，$testConnectionStatus"
                is SettingsItem.MinimaxAIServiceItem -> "$serviceStatus，Api Key，稀宇科技，$testConnectionStatus"
                is SettingsItem.KimiAIServiceItem -> "$serviceStatus，Api Key，月之暗面，$testConnectionStatus"
                is SettingsItem.OpenAIServiceItem -> "$serviceStatus，Api Key，OpenAI，$testConnectionStatus"
                is SettingsItem.ErnieAIServiceItem -> "$serviceStatus，Api Key，百度AI，$testConnectionStatus"
                is SettingsItem.QwenAIServiceItem -> "$serviceStatus，Api Key，阿里巴巴AI，$testConnectionStatus"
                is SettingsItem.ZhipuAIServiceItem -> "$serviceStatus，Api Key，智谱AI，$testConnectionStatus"
                is SettingsItem.SparkAIServiceItem -> "$serviceStatus，Api Key，科大讯飞，$testConnectionStatus"
                is SettingsItem.HunyuanAIServiceItem -> "$serviceStatus，Api Key，腾讯混元，$testConnectionStatus"
                is SettingsItem.BaichuanAIServiceItem -> "$serviceStatus，Api Key，百川智能，$testConnectionStatus"
                is SettingsItem.LingyiAIServiceItem -> "$serviceStatus，Api Key，零一万物，$testConnectionStatus"
                is SettingsItem.JieyueAIServiceItem -> "$serviceStatus，Api Key，阶跃星辰，$testConnectionStatus"
                is SettingsItem.ChatGLMAIServiceItem -> "$serviceStatus，${item.modelSize}模型，清华开源，${if(item.isModelDownloaded) "已下载" else "未下载"}"
                is SettingsItem.TinyBERTAIServiceItem -> "$serviceStatus，${item.modelSize}模型，设备端推理，${if(item.isModelDownloaded) "已下载" else "未下载"}"
                else -> "$serviceStatus，未知服务，$testConnectionStatus"
            }
            
            descriptionTextView?.text = detailText
            
            // 隐藏状态文本视图，因为我们已经在描述中显示了状态
            statusTextView?.visibility = View.GONE
            
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}
