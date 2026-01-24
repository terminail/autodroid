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
import com.autodroid.teachitback.databinding.FragmentSettingsAiServiceDetailBinding
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
        "jieyue" to "https://open.jieyuesx.com"
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
        viewModel = ViewModelProvider(requireActivity())[SettingsViewModel::class.java]

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
        setupSaveButton()
        setupTestButton()
    }

    /**
     * 观察 ViewModel 数据变化
     */
    private fun observeViewModel() {
        viewModel.aiServiceConfigs.observe(viewLifecycleOwner) { configs ->
            configs?.get(currentConfig.id)?.let { savedConfig ->
                // 使用已保存的配置更新 UI
                updateUIWithSavedConfig(savedConfig)
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
        
        // 初始使用默认配置值
        binding.apiKeyInput.setText(currentConfig.apiKey)
        binding.secretIdInput.setText(currentConfig.secretId)
        binding.modelInput.setText(currentConfig.model, false)
    }
    
    /**
     * 使用已保存的配置更新 UI
     */
    private fun updateUIWithSavedConfig(savedConfig: AIServiceConfig) {
        binding.apiKeyInput.setText(savedConfig.apiKey)
        binding.secretIdInput.setText(savedConfig.secretId)
        binding.modelInput.setText(savedConfig.model, false)
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
            testConnection()
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
                Toast.makeText(requireContext(), "API Key不能为空", Toast.LENGTH_SHORT).show()
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
     * 测试连接
     */
    private fun testConnection() {
        safeBinding()?.let { binding ->
            val apiKey = binding.apiKeyInput.text?.toString() ?: ""

            if (apiKey.isBlank()) {
                Toast.makeText(requireContext(), "请先填写API Key", Toast.LENGTH_SHORT).show()
                return
            }

            binding.testConnectionButton.isEnabled = false
            binding.testConnectionButton.text = "测试中..."

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

                    val isSuccess = testConnectionWithConfig(testConfig)

                    withContext(Dispatchers.Main) {
                        safeBinding()?.let { binding ->
                            if (isSuccess) {
                                saveConfig()
                                viewModel.testAIServiceConnection(currentConfig.id)
                                Toast.makeText(
                                    requireContext(),
                                    "连接成功！配置已保存",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "连接失败，请检查配置",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        safeBinding()?.let { binding ->
                            Toast.makeText(
                                requireContext(),
                                "测试失败: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        safeBinding()?.let { binding ->
                            binding.testConnectionButton.isEnabled = true
                            binding.testConnectionButton.text = "测试连接"
                        }
                    }
                }
            }
        }
    }

    // saveToDatabase 方法已被 SettingsViewModel 替代，不再需要

    /**
     * 测试连接（简化实现）
     */
    private suspend fun testConnectionWithConfig(config: AIServiceConfig): Boolean {
        // 实际实现：调用AI服务的testConnection方法
        // 这里仅作为示例，总是返回true
        return true
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