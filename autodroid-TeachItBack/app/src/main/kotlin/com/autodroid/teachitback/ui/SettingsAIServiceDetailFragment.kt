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
import com.autodroid.teachitback.MainActivity
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.config.AIServiceStatus
import com.autodroid.teachitback.databinding.FragmentSettingsAiServiceDetailBinding
import com.autodroid.teachitback.di.ViewModelFactoryProvider
import com.autodroid.teachitback.helper.AIServiceConfigHelper
import com.autodroid.teachitback.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI服务配置Fragment
 * 支持所有AI服务类型的统一配置界面
 *
 * 使用方式：
 * val fragment = SettingsAIServiceDetailFragment()
 * val args = Bundle().apply {
 *     putString("config_type", "tencent-hunyuan") // 或 "deepseek", "minimax", "baichuan"
 * }
 * fragment.arguments = args
 */
class SettingsAIServiceDetailFragment : Fragment() {

    private var _binding: FragmentSettingsAiServiceDetailBinding? = null
    private val binding get() = _binding!!

    private fun safeBinding(): FragmentSettingsAiServiceDetailBinding? = _binding

    // 当前配置类型
    private lateinit var currentConfig: AIServiceConfig

    // ViewModel实例
    private lateinit var viewModel: SettingsViewModel

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

        // 初始化 ViewModel（使用 Activity 范围的 ViewModel 以便与 SettingsFragment 共享数据）
        val factory = ViewModelFactoryProvider.getFactory()
        viewModel = ViewModelProvider(requireActivity(), factory)[SettingsViewModel::class.java]

        // 获取配置类型参数
        val configType = arguments?.getString("config_type") ?: "tencent-hunyuan"

        // 创建对应的配置实例
        currentConfig = when (configType) {
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

        // 设置标题
        val activity = requireActivity()
        if (activity is MainActivity) {
            activity.setToolbarTitle("${currentConfig.displayName} 配置")
            activity.showBackButton(true)
        }

        // 观察 ViewModel 数据
        observeViewModel()

        // 加载已保存的配置
        loadSavedConfig()

        // 设置UI
        setupUI()

        // 设置AI服务能力
        setupCapabilities()

        // 设置获取API Key链接
        setupGetApiKeyLink()

        // 设置按钮监听器
        setupEnableSwitch()
        setupSaveButton()
        setupTestButton()
    }

    /**
     * 观察 ViewModel 数据变化
     */
    private fun observeViewModel() {
        viewModel.aiServiceConfigs.observe(viewLifecycleOwner) { configs ->
            android.util.Log.d("SettingsAIServiceDetailFragment", "aiServiceConfigs updated, size: ${configs?.size}, currentConfig.id: ${currentConfig.id}")
            configs?.get(currentConfig.id)?.let { savedConfig ->
                android.util.Log.d("SettingsAIServiceDetailFragment", "Found saved config for ${currentConfig.id}, apiKey: ${savedConfig.apiKey.take(10)}...")
                // 使用已保存的配置更新 UI
                updateUIWithSavedConfig(savedConfig)
                // 更新服务状态显示（显示上次保存的状态）
                updateServiceStatusDisplay(savedConfig.status)
            } ?: run {
                android.util.Log.d("SettingsAIServiceDetailFragment", "No saved config found for ${currentConfig.id}")
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            safeBinding()?.let { binding ->
                binding.saveButton.isEnabled = !isLoading
                binding.testConnectionButton.isEnabled = !isLoading
                if (isLoading) {
                    binding.saveButton.text = "保存中..."
                } else {
                    binding.saveButton.text = "保存配置"
                }
            }
        }
    }

    /**
     * 加载已保存的配置
     */
    private fun loadSavedConfig() {
        // 通过观察 aiServiceConfigs 来获取已保存的配置
        // 当 SettingsViewModel 加载配置时，UI 会自动更新
    }

    /**
     * 设置UI界面
     */
    private fun setupUI() {
        // 使用AIServiceConfigHelper动态设置配置字段的可见性
        AIServiceConfigHelper.setupAIServiceConfigUI(
            secretIdField = binding.secretIdField,
            apiKeyField = binding.apiKeyField,
            baseUrlField = binding.baseUrlInput,
            regionField = binding.regionInput,
            modelField = binding.modelField,
            config = currentConfig
        )

        // 设置Base URL和Region为只读显示
        binding.baseUrlInput.text = currentConfig.baseUrl
        binding.regionInput.text = currentConfig.region

        // 设置Model下拉框选项
        val models = AIServiceConfigHelper.getModelList(currentConfig)
        val adapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            models
        )
        binding.modelInput.setAdapter(adapter)

        // 设置模型选择监听器，显示模型描述
        binding.modelInput.setOnItemClickListener { _, _, position, _ ->
            val selectedModel = models[position]
            val description = AIServiceConfigHelper.getModelDescription(currentConfig, selectedModel)
            if (description.isNotEmpty()) {
                binding.modelDescription.text = description
                binding.modelDescription.visibility = android.view.View.VISIBLE
            } else {
                binding.modelDescription.visibility = android.view.View.GONE
            }
        }

        // 初始使用默认配置值
        binding.apiKeyInput.setText(currentConfig.apiKey)
        binding.secretIdInput.setText(currentConfig.secretId)
        binding.modelInput.setText(currentConfig.model, false)

        // 初始显示当前模型的描述
        val initialDescription = AIServiceConfigHelper.getModelDescription(currentConfig, currentConfig.model)
        if (initialDescription.isNotEmpty()) {
            binding.modelDescription.text = initialDescription
            binding.modelDescription.visibility = android.view.View.VISIBLE
        }
    }

    /**
     * 使用已保存的配置更新 UI
     */
    private fun updateUIWithSavedConfig(savedConfig: AIServiceConfig) {
        android.util.Log.d("SettingsAIServiceDetailFragment", "updateUIWithSavedConfig called, apiKey: ${savedConfig.apiKey.take(10)}...")
        // 更新 currentConfig，确保后续操作使用正确的配置
        currentConfig = savedConfig

        // 更新输入框
        binding.apiKeyInput.setText(savedConfig.apiKey)
        binding.secretIdInput.setText(savedConfig.secretId)
        binding.modelInput.setText(savedConfig.model, false)
        android.util.Log.d("SettingsAIServiceDetailFragment", "Updated apiKeyInput text")

        // 更新模型描述
        val description = AIServiceConfigHelper.getModelDescription(savedConfig, savedConfig.model)
        if (description.isNotEmpty()) {
            binding.modelDescription.text = description
            binding.modelDescription.visibility = android.view.View.VISIBLE
        } else {
            binding.modelDescription.visibility = android.view.View.GONE
        }

        // 更新开关状态和文本显示
        val isEnabled = savedConfig.isEnabled
        isUpdatingSwitchProgrammatically = true
        binding.enableServiceSwitch.isChecked = isEnabled
        isUpdatingSwitchProgrammatically = false
        updateUIForEnabledState(isEnabled)
    }

    /**
     * 设置AI服务能力
     */
    private fun setupCapabilities() {
        val caps = currentConfig.capabilities

        binding.capBasicChat.visibility = if (caps.supportBasicChat) View.VISIBLE else View.GONE
        binding.capFileProcessing.visibility = if (caps.supportFileProcessing) View.VISIBLE else View.GONE
        binding.capMindMap.visibility = if (caps.supportMindMapGeneration) View.VISIBLE else View.GONE
        binding.capLearningAnalysis.visibility = if (caps.supportLearningAnalysis) View.VISIBLE else View.GONE
        binding.capSocratic.visibility = if (caps.supportSocraticQuestioning) View.VISIBLE else View.GONE
        binding.capAnswerEval.visibility = if (caps.supportAnswerEvaluation) View.VISIBLE else View.GONE
        binding.capDocumentParsing.visibility = if (caps.supportDocumentParsing) View.VISIBLE else View.GONE
        binding.capConceptExtraction.visibility = if (caps.supportConceptExtraction) View.VISIBLE else View.GONE
        binding.capKnowledgeGraph.visibility = if (caps.supportKnowledgeGraph) View.VISIBLE else View.GONE
        binding.capLongText.visibility = if (caps.supportLongText) View.VISIBLE else View.GONE
        binding.capMultimodal.visibility = if (caps.supportMultimodal) View.VISIBLE else View.GONE
        binding.capEducation.visibility = if (caps.supportEducation) View.VISIBLE else View.GONE
        binding.capCodeGeneration.visibility = if (caps.supportCodeGeneration) View.VISIBLE else View.GONE
        binding.capMath.visibility = if (caps.supportMath) View.VISIBLE else View.GONE
        binding.capCreativeWriting.visibility = if (caps.supportCreativeWriting) View.VISIBLE else View.GONE
        binding.capImageAnalysis.visibility = if (caps.supportImageAnalysis) View.VISIBLE else View.GONE
        binding.capImageGeneration.visibility = if (caps.supportImageGeneration) View.VISIBLE else View.GONE
        binding.capAudioProcessing.visibility = if (caps.supportAudioProcessing) View.VISIBLE else View.GONE
        binding.capVideoAnalysis.visibility = if (caps.supportVideoAnalysis) View.VISIBLE else View.GONE
        binding.capRag.visibility = if (caps.supportRAG) View.VISIBLE else View.GONE
    }

    /**
     * 设置获取API Key链接
     */
    private fun setupGetApiKeyLink() {
        val configType = arguments?.getString("config_type") ?: "tencent-hunyuan"
        val apiKeyUrl = apiKeyUrls[configType] ?: ""

        binding.getApiKeyLink.text = "获取 API KEY: $apiKeyUrl （长按复制）"

        // 点击打开浏览器
        binding.getApiKeyLink.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apiKeyUrl))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "无法打开链接", Toast.LENGTH_SHORT).show()
            }
        }

        // 长按复制链接
        binding.getApiKeyLink.setOnLongClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("API Key URL", apiKeyUrl)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "链接已复制到剪贴板", Toast.LENGTH_SHORT).show()
            true
        }
    }

    /**
     * 设置启用状态开关
     */
    private fun setupEnableSwitch() {
        // 初始状态：从当前配置获取启用状态
        val isEnabled = currentConfig.isEnabled
        binding.enableServiceSwitch.isChecked = isEnabled
        
        // 更新文本显示
        updateUIForEnabledState(isEnabled)

        // 监听开关状态变化
        binding.enableServiceSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 如果是由程序更新开关状态，跳过处理
            if (isUpdatingSwitchProgrammatically) return@setOnCheckedChangeListener

            // 更新配置的启用状态
            val config = currentConfig
            val updatedConfig = when (config) {
                is AIServiceConfig.DoubaoConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.DeepSeekConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.MiniMaxConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.BaichuanConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.KimiConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.OpenAIConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.ErnieConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.QwenConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.ZhipuConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.SparkConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.HunyuanConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.LingyiConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.JieyueConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.ChatGLMConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.TinyBERTConfig -> config.copy(isEnabled = isChecked)
                is AIServiceConfig.TencentHunyuanConfig -> config.copy(isEnabled = isChecked)
            }

            // 更新当前配置引用
            currentConfig = updatedConfig

            // 保存配置
            viewModel.saveAIServiceConfig(updatedConfig)

            // 更新UI状态
            updateUIForEnabledState(isChecked)
        }
    }

    /**
     * 根据启用状态更新UI
     */
    private fun updateUIForEnabledState(isEnabled: Boolean) {
        // 更新文本显示
        binding.enableServiceText.text = if (isEnabled) "服务已启用" else "服务未启用"

        val alpha = if (isEnabled) 1.0f else 0.5f

        // 设置所有配置字段的透明度
        binding.secretIdField.alpha = alpha
        binding.apiKeyField.alpha = alpha
        binding.baseUrlInput.alpha = alpha
        binding.regionInput.alpha = alpha
        binding.modelField.alpha = alpha

        // 设置按钮的启用状态
        binding.secretIdInput.isEnabled = isEnabled
        binding.apiKeyInput.isEnabled = isEnabled
        binding.modelInput.isEnabled = isEnabled
        binding.testConnectionButton.isEnabled = isEnabled
        binding.saveButton.isEnabled = isEnabled
    }

    /**
     * 设置保存按钮
     */
    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            saveConfig()
        }
    }

    /**
     * 设置测试按钮
     */
    private fun setupTestButton() {
        binding.testConnectionButton.setOnClickListener {
            binding.apiKeyField.error = null
            checkServiceStatus()
        }
    }

    /**
     * 格式化时间为微信风格（如"刚刚"、"1分钟前"、"3小时前"、"12-26"等）
     */
    private fun formatTimeWeChatStyle(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        // 转换为分钟
        val minutes = diff / 60000
        if (minutes < 1) return "刚刚"

        // 转换为小时
        val hours = minutes / 60
        if (hours < 1) return "${minutes}分钟前"

        // 转换为天
        val days = hours / 24
        if (days < 1) return "${hours}小时前"

        // 超过1天，显示日期
        val date = java.util.Date(timestamp)
        val calendar = java.util.Calendar.getInstance()
        calendar.time = date

        val currentCalendar = java.util.Calendar.getInstance()
        val currentYear = currentCalendar.get(java.util.Calendar.YEAR)
        val year = calendar.get(java.util.Calendar.YEAR)

        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        return if (year == currentYear) {
            // 同一年，只显示月-日
            String.format("%d-%02d", month, day)
        } else {
            // 不同年份，显示年-月-日
            String.format("%d-%02d-%02d", year, month, day)
        }
    }

    /**
     * 更新服务状态显示
     */
    private fun updateServiceStatusDisplay(status: AIServiceStatus) {
        safeBinding()?.let { binding ->
            binding.serviceStatusContainer.visibility = View.VISIBLE
            binding.serviceStatusDescription.text = status.description

            // 根据状态码设置颜色
            val color = when (status.code) {
                0 -> "#9E9E9E" // 灰色 - 未检查
                200 -> "#4CAF50" // 绿色 - 正常
                in 400..499 -> "#FF9800" // 橙色 - 客户端错误
                in 500..599 -> "#F44336" // 红色 - 服务器错误
                else -> "#9E9E9E" // 灰色 - 未知状态
            }
            binding.serviceStatusDescription.setTextColor(android.graphics.Color.parseColor(color))

            // 更新时间
            binding.serviceStatusLastUpdated.text = formatTimeWeChatStyle(status.lastUpdated)
        }
    }

    /**
     * 保存配置
     */
    private fun saveConfig() {
        try {
            // 获取输入值
            val secretId = binding.secretIdInput.text?.toString() ?: ""
            val apiKey = binding.apiKeyInput.text?.toString() ?: ""
            val model = binding.modelInput.text?.toString() ?: ""

            // 验证必填字段
            if (currentConfig.requiredFields.requireApiKey && apiKey.isBlank()) {
                binding.apiKeyField.error = "请先填写API Key"
                return
            }

            // 创建更新后的配置（Base URL和Region使用默认值，不允许修改）
            val updatedConfig = AIServiceConfigHelper.getConfigData(
                secretId = secretId,
                apiKey = apiKey,
                baseUrl = currentConfig.baseUrl,
                region = currentConfig.region,
                model = model,
                baseConfig = currentConfig
            )

            // 使用 SettingsViewModel 保存配置
            viewModel.saveAIServiceConfig(updatedConfig)

            // 显示成功消息并返回上一页
            Toast.makeText(requireContext(), "配置保存成功", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "保存失败: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * 检查服务状态（用户点击按钮时调用）
     */
    private fun checkServiceStatus() {
        safeBinding()?.let { binding ->
            val apiKey = binding.apiKeyInput.text?.toString() ?: ""

            // 对于需要API Key的服务，检查API Key是否填写
            if (currentConfig.requiredFields.requireApiKey && apiKey.isBlank()) {
                binding.apiKeyField.error = "请先填写API Key"
                return
            }

            binding.testConnectionButton.isEnabled = false
            binding.testConnectionButton.text = "检查中..."

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (!isAdded || _binding == null) {
                        return@launch
                    }

                    val testConfig = AIServiceConfigHelper.getConfigData(
                        secretId = binding.secretIdInput.text?.toString() ?: "",
                        apiKey = apiKey,
                        baseUrl = currentConfig.baseUrl,
                        region = currentConfig.region,
                        model = binding.modelInput.text?.toString() ?: "",
                        baseConfig = currentConfig
                    )

                    // 使用统一的checkAndUpdateAiServiceStatus方法检查服务状态
                    val repository = com.autodroid.teachitback.di.AppContainer.getSettingsRepository()
                    val updatedConfig = repository.checkAndUpdateAiServiceStatus(testConfig)

                    withContext(Dispatchers.Main) {
                        safeBinding()?.let { binding ->
                            // 更新状态显示
                            updateServiceStatusDisplay(updatedConfig.status)

                            if (updatedConfig.status.isOk) {
                                Toast.makeText(
                                    requireContext(),
                                    "服务状态: ${updatedConfig.status.description}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "服务异常: ${updatedConfig.status.description}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        safeBinding()?.let { binding ->
                            Toast.makeText(
                                requireContext(),
                                "检查失败: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        safeBinding()?.let { binding ->
                            binding.testConnectionButton.isEnabled = true
                            binding.testConnectionButton.text = "检查服务状态"
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * 创建Fragment实例的便捷方法
         */
        fun newInstance(configType: String): SettingsAIServiceDetailFragment {
            return SettingsAIServiceDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("config_type", configType)
                }
            }
        }
    }
}
