package com.autodroid.teachitback.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.autodroid.teachitback.MainActivity
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.config.AIServiceStatus
import com.autodroid.teachitback.databinding.FragmentSettingsAiServiceDetailBinding
import com.autodroid.teachitback.di.ViewModelFactoryProvider
import com.autodroid.teachitback.helper.AIServiceConfigHelper
import com.autodroid.teachitback.viewmodel.SettingsAIServiceDetailViewModel
import kotlinx.coroutines.launch

/**
 * AI服务配置Fragment
 * 支持所有AI服务类型的统一配置界面
 * 
 * 传统方式：不使用 LiveData，使用回调接口
 */
class SettingsAIServiceDetailFragment : Fragment() {

    private var _binding: FragmentSettingsAiServiceDetailBinding? = null
    private val binding get() = _binding!!

    // 默认配置（从参数创建）
    private lateinit var defaultConfig: AIServiceConfig
    
    // 当前配置ID
    private lateinit var configId: String
    
    // 当前加载的配置（从数据库加载）
    private var loadedConfig: AIServiceConfig? = null

    // ViewModel实例
    private lateinit var viewModel: SettingsAIServiceDetailViewModel

    // 标志：是否由程序更新开关状态（避免触发监听器）
    private var isUpdatingSwitchProgrammatically = false

    // API Key 获取地址映射
    private val apiKeyUrls = mapOf(
        "tencent-hunyuan" to "https://cloud.tencent.com/product/hunyuan",
        "deepseek" to "https://platform.deepseek.com",
        "minimax" to "https://platform.minimaxi.com",
        "baichuan" to "https://platform.baichuan-ai.com",
        "kimi" to "https://platform.moonshot.cn",
        "openai" to "https://platform.openai.com",
        "ernie" to "https://console.bce.baidu.com/qianfan",
        "qwen" to "https://dashscope.aliyuncs.com",
        "zhipu" to "https://open.bigmodel.cn",
        "spark" to "https://console.xfyun.cn/services/spark",
        "hunyuan" to "https://cloud.tencent.com/product/hunyuan",
        "doubao" to "https://developer.doubao.com",
        "lingyi" to "https://open.lingyiwanwu.com",
        "jieyue" to "https://open.jieyuesx.com",
        "chatglm" to "https://github.com/THUDM/ChatGLM-6B",
        "tinybert" to "https://github.com/huawei-noah/TinyBERT",
        "glm-4.7-flash" to "https://open.bigmodel.cn"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsAiServiceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 获取配置类型参数
        val configType = arguments?.getString("config_type") ?: "tencent-hunyuan"
        
        // 创建默认配置
        defaultConfig = createDefaultConfig(configType)
        configId = defaultConfig.id

        // 设置标题
        val activity = requireActivity()
        if (activity is MainActivity) {
            activity.setToolbarTitle("${defaultConfig.displayName} 配置")
            activity.showBackButton(true)
        }

        // 初始化 ViewModel
        val factory = ViewModelFactoryProvider.getFactory()
        viewModel = ViewModelProvider(this, factory)[SettingsAIServiceDetailViewModel::class.java]
        
        // 初始化 ViewModel 的配置 ID
        viewModel.initConfigId(configId)

        // 设置基础UI（不需要动态数据的部分）
        setupStaticUI()
        
        // 加载配置并更新UI
        loadConfigAndUpdateUI()
        
        // 设置按钮监听器
        setupListeners()
    }
    
    /**
     * 加载配置并更新UI
     */
    private fun loadConfigAndUpdateUI() {
        lifecycleScope.launch {
            try {
                loadedConfig = viewModel.loadConfig()
                val config = loadedConfig ?: defaultConfig
                updateUIWithConfig(config)
            } catch (e: Exception) {
                // 加载失败，使用默认配置
                updateUIWithConfig(defaultConfig)
            }
        }
    }
    
    /**
     * 创建默认配置
     */
    private fun createDefaultConfig(configType: String): AIServiceConfig {
        return when (configType) {
            "tencent-hunyuan" -> AIServiceConfig.TencentHunyuanConfig()
            "deepseek" -> AIServiceConfig.DeepSeekConfig()
            "kimi" -> AIServiceConfig.KimiConfig()
            "minimax" -> AIServiceConfig.MiniMaxConfig()
            "baichuan" -> AIServiceConfig.BaichuanConfig()
            "openai" -> AIServiceConfig.OpenAIConfig()
            "ernie" -> AIServiceConfig.ErnieConfig()
            "qwen" -> AIServiceConfig.QwenConfig()
            "zhipu" -> AIServiceConfig.ZhipuConfig()
            "spark" -> AIServiceConfig.SparkConfig()
            "hunyuan" -> AIServiceConfig.HunyuanConfig()
            "doubao" -> AIServiceConfig.DoubaoConfig()
            "lingyi" -> AIServiceConfig.LingyiConfig()
            "jieyue" -> AIServiceConfig.JieyueConfig()
            "chatglm" -> AIServiceConfig.ChatGLMConfig()
            "tinybert" -> AIServiceConfig.TinyBERTConfig()
            else -> AIServiceConfig.TencentHunyuanConfig()
        }
    }

    /**
     * 设置基础UI（静态部分）
     */
    private fun setupStaticUI() {
        // 使用AIServiceConfigHelper动态设置配置字段的可见性
        AIServiceConfigHelper.setupAIServiceConfigUI(
            secretIdField = binding.secretIdField,
            apiKeyField = binding.apiKeyField,
            baseUrlField = binding.baseUrlInput,
            regionField = binding.regionInput,
            modelField = binding.modelField,
            config = defaultConfig
        )

        // 设置Base URL和Region为只读显示
        binding.baseUrlInput.text = defaultConfig.baseUrl
        binding.regionInput.text = defaultConfig.region

        // 设置Model下拉框选项
        val models = AIServiceConfigHelper.getModelList(defaultConfig)
        val adapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            models
        )
        binding.modelInput.setAdapter(adapter)

        // 设置获取API Key链接
        binding.getApiKeyLink.setOnClickListener {
            val url = apiKeyUrls[configId] ?: return@setOnClickListener
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

    }

    /**
     * 根据配置更新UI
     */
    private fun updateUIWithConfig(config: AIServiceConfig) {
        android.util.Log.d("SettingsAIServiceDetailFragment", "Updating UI with config: ${config.id}, apiKey: ${config.apiKey.take(10)}...")
        
        // 更新输入框
        binding.apiKeyInput.setText(config.apiKey)
        binding.secretIdInput.setText(config.secretId)
        binding.modelInput.setText(config.model, false)

        // 更新模型描述
        val description = AIServiceConfigHelper.getModelDescription(config, config.model)
        if (description.isNotEmpty()) {
            binding.modelDescription.text = description
            binding.modelDescription.visibility = View.VISIBLE
        } else {
            binding.modelDescription.visibility = View.GONE
        }

        // 更新开关状态
        val isEnabled = config.isEnabled
        isUpdatingSwitchProgrammatically = true
        binding.enableServiceSwitch.isChecked = isEnabled
        isUpdatingSwitchProgrammatically = false
        
        // 更新UI状态
        updateUIForEnabledState(isEnabled)
        
        // 更新服务状态显示
        updateServiceStatusDisplay(config.status)
    }

    /**
     * 设置按钮监听器
     */
    private fun setupListeners() {
        // 启用开关监听器
        binding.enableServiceSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchProgrammatically) return@setOnCheckedChangeListener

            // 获取当前配置并更新
            val currentConfig = buildConfigFromUI()
            val updatedConfig = updateConfigEnabled(currentConfig, isChecked)
            
            // 保存配置（使用回调方式）
            viewModel.saveConfig(
                config = updatedConfig,
                onSuccess = {
                    Toast.makeText(requireContext(), "配置保存成功", Toast.LENGTH_SHORT).show()
                },
                onError = { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                },
                onLoading = { isLoading ->
                    binding.saveButton.isEnabled = !isLoading
                    binding.testConnectionButton.isEnabled = !isLoading
                    binding.saveButton.text = if (isLoading) "保存中..." else "保存配置"
                }
            )
            
            // 更新UI状态
            updateUIForEnabledState(isChecked)
        }
        
        // 保存按钮
        binding.saveButton.setOnClickListener {
            val config = buildConfigFromUI()
            viewModel.saveConfig(
                config = config,
                onSuccess = {
                    Toast.makeText(requireContext(), "配置保存成功", Toast.LENGTH_SHORT).show()
                },
                onError = { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                },
                onLoading = { isLoading ->
                    binding.saveButton.isEnabled = !isLoading
                    binding.testConnectionButton.isEnabled = !isLoading
                    binding.saveButton.text = if (isLoading) "保存中..." else "保存配置"
                }
            )
        }
        
        // 测试连接按钮
        binding.testConnectionButton.setOnClickListener {
            testConnection()
        }
    }
    
    /**
     * 从UI构建配置对象
     */
    private fun buildConfigFromUI(): AIServiceConfig {
        val apiKey = binding.apiKeyInput.text.toString()
        val secretId = binding.secretIdInput.text.toString()
        val model = binding.modelInput.text.toString()
        val isEnabled = binding.enableServiceSwitch.isChecked
        
        // 使用局部变量避免 smart cast 问题
        val config = loadedConfig ?: defaultConfig
        return when (config) {
            is AIServiceConfig.ZhipuConfig -> config.copy(
                apiKey = apiKey,
                isEnabled = isEnabled
            )
            is AIServiceConfig.DeepSeekConfig -> config.copy(
                apiKey = apiKey,
                isEnabled = isEnabled
            )
            is AIServiceConfig.KimiConfig -> config.copy(
                apiKey = apiKey,
                isEnabled = isEnabled
            )
            is AIServiceConfig.MiniMaxConfig -> config.copy(
                apiKey = apiKey,
                isEnabled = isEnabled
            )
            is AIServiceConfig.BaichuanConfig -> config.copy(
                apiKey = apiKey,
                isEnabled = isEnabled
            )
            is AIServiceConfig.OpenAIConfig -> config.copy(
                apiKey = apiKey,
                isEnabled = isEnabled
            )
            is AIServiceConfig.ErnieConfig -> config.copy(
                apiKey = apiKey,
                secretId = secretId,
                isEnabled = isEnabled
            )
            is AIServiceConfig.QwenConfig -> config.copy(
                apiKey = apiKey,
                isEnabled = isEnabled
            )
            is AIServiceConfig.SparkConfig -> config.copy(
                apiKey = apiKey,
                secretId = secretId,
                isEnabled = isEnabled
            )
            is AIServiceConfig.HunyuanConfig -> config.copy(
                apiKey = apiKey,
                secretId = secretId,
                isEnabled = isEnabled
            )
            is AIServiceConfig.DoubaoConfig -> config.copy(
                apiKey = apiKey,
                isEnabled = isEnabled
            )
            is AIServiceConfig.LingyiConfig -> config.copy(
                apiKey = apiKey,
                isEnabled = isEnabled
            )
            is AIServiceConfig.JieyueConfig -> config.copy(
                apiKey = apiKey,
                isEnabled = isEnabled
            )
            is AIServiceConfig.ChatGLMConfig -> config.copy(
                isEnabled = isEnabled
            )
            is AIServiceConfig.TinyBERTConfig -> config.copy(
                isEnabled = isEnabled
            )
            is AIServiceConfig.TencentHunyuanConfig -> config.copy(
                apiKey = apiKey,
                secretId = secretId,
                isEnabled = isEnabled
            )
            else -> config
        }
    }
    
    /**
     * 更新配置的启用状态
     */
    private fun updateConfigEnabled(config: AIServiceConfig, isEnabled: Boolean): AIServiceConfig {
        return when (config) {
            is AIServiceConfig.ZhipuConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.DeepSeekConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.KimiConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.MiniMaxConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.BaichuanConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.OpenAIConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.ErnieConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.QwenConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.SparkConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.HunyuanConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.DoubaoConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.LingyiConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.JieyueConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.ChatGLMConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.TinyBERTConfig -> config.copy(isEnabled = isEnabled)
            is AIServiceConfig.TencentHunyuanConfig -> config.copy(isEnabled = isEnabled)
            else -> config
        }
    }

    /**
     * 根据启用状态更新UI
     */
    private fun updateUIForEnabledState(isEnabled: Boolean) {
        binding.enableServiceText.text = if (isEnabled) "服务已启用" else "服务已禁用"
        
        // 禁用/启用输入字段
        val alpha = if (isEnabled) 1.0f else 0.5f
        binding.apiKeyField.alpha = alpha
        binding.secretIdField.alpha = alpha
        binding.modelField.alpha = alpha
        
        binding.apiKeyInput.isEnabled = isEnabled
        binding.secretIdInput.isEnabled = isEnabled
        binding.modelInput.isEnabled = isEnabled
    }

    /**
     * 更新服务状态显示
     */
    private fun updateServiceStatusDisplay(status: AIServiceStatus) {
        // 服务状态显示已移除，避免引用不存在的视图
        // 如需显示服务状态，请在布局中添加相应的视图
    }

    /**
     * 测试连接
     */
    private fun testConnection() {
        Toast.makeText(requireContext(), "连接测试功能开发中...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
