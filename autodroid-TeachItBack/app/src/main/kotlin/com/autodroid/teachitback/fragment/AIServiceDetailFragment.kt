package com.autodroid.teachitback.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.autodroid.teachitback.config.AIServiceConfig
import com.autodroid.teachitback.databinding.FragmentAiServiceDetailBinding
import com.autodroid.teachitback.helper.AIServiceConfigHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI服务配置Fragment
 * 支持所有AI服务类型的统一配置界面
 *
 * 使用方式：
 * val fragment = AIServiceDetailFragment()
 * val args = Bundle().apply {
 *     putString("config_type", "tencent-hunyuan") // 或 "deepseek", "minimax", "baichuan"
 * }
 * fragment.arguments = args
 */
class AIServiceDetailFragment : Fragment() {

    private var _binding: FragmentAiServiceDetailBinding? = null
    private val binding get() = _binding!!

    // 当前配置类型
    private lateinit var currentConfig: AIServiceConfig

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiServiceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 获取配置类型参数
        val configType = arguments?.getString("config_type") ?: "tencent-hunyuan"

        // 创建对应的配置实例
        currentConfig = when (configType) {
            "tencent-hunyuan" -> AIServiceConfig.TencentHunyuanConfig()
            "deepseek" -> AIServiceConfig.DeepSeekConfig()
            "minimax" -> AIServiceConfig.MiniMaxConfig()
            "baichuan" -> AIServiceConfig.BaichuanConfig()
            else -> AIServiceConfig.TencentHunyuanConfig()
        }

        // 设置UI
        setupUI()

        // 设置按钮监听器
        setupSaveButton()
        setupTestButton()
    }

    /**
     * 设置UI界面
     */
    private fun setupUI() {
        // 设置标题
        binding.serviceTitle.text = "${currentConfig.displayName} 配置"

        // 使用AIServiceConfigHelper动态设置配置字段的可见性
        AIServiceConfigHelper.setupAIServiceConfigUI(
            secretIdField = binding.secretIdField,
            apiKeyField = binding.apiKeyField,
            baseUrlField = binding.baseUrlField,
            regionField = binding.regionField,
            modelField = binding.modelField,
            config = currentConfig
        )

        // 设置默认值
        binding.baseUrlInput.setText(currentConfig.baseUrl)
        binding.regionInput.setText(currentConfig.region)
        binding.modelInput.setText(currentConfig.model)
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
            val secretId = binding.secretIdInput.text.toString()
            val apiKey = binding.apiKeyInput.text.toString()
            val baseUrl = binding.baseUrlInput.text.toString()
            val region = binding.regionInput.text.toString()
            val model = binding.modelInput.text.toString()

            // 验证必填字段
            if (currentConfig.requiredFields.requireApiKey && apiKey.isBlank()) {
                Toast.makeText(requireContext(), "API Key不能为空", Toast.LENGTH_SHORT).show()
                return
            }

            // 创建更新后的配置
            val updatedConfig = AIServiceConfigHelper.getConfigData(
                secretId = secretId,
                apiKey = apiKey,
                baseUrl = baseUrl,
                region = region,
                model = model,
                baseConfig = currentConfig
            )

            // 保存到数据库（这里简化处理，实际应该调用ViewModel或Repository）
            saveToDatabase(updatedConfig)

            // 显示成功消息
            Toast.makeText(requireContext(), "配置保存成功", Toast.LENGTH_SHORT).show()

            // 返回上一页
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
        val apiKey = binding.apiKeyInput.text.toString()

        if (apiKey.isBlank()) {
            Toast.makeText(requireContext(), "请先填写API Key", Toast.LENGTH_SHORT).show()
            return
        }

        // 显示加载状态
        binding.testConnectionButton.isEnabled = false
        binding.testConnectionButton.text = "测试中..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 创建临时配置用于测试
                val testConfig = AIServiceConfigHelper.getConfigData(
                    secretId = binding.secretIdInput.text.toString(),
                    apiKey = apiKey,
                    baseUrl = binding.baseUrlInput.text.toString(),
                    region = binding.regionInput.text.toString(),
                    model = binding.modelInput.text.toString(),
                    baseConfig = currentConfig
                )

                // 测试连接（这里简化处理，实际应该调用AI服务的testConnection方法）
                val isSuccess = testConnectionWithConfig(testConfig)

                withContext(Dispatchers.Main) {
                    if (isSuccess) {
                        Toast.makeText(
                            requireContext(),
                            "连接成功！",
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
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "测试失败: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.testConnectionButton.isEnabled = true
                    binding.testConnectionButton.text = "测试连接"
                }
            }
        }
    }

    /**
     * 保存配置到数据库（简化实现）
     */
    private fun saveToDatabase(config: AIServiceConfig) {
        // 实际实现：调用SharedPreferences或Room数据库存储配置
        // 这里仅作为示例
        val sharedPref = requireContext().getSharedPreferences("ai_services", 0)
        with(sharedPref.edit()) {
            putString("${config.id}_api_key", config.apiKey)
            putString("${config.id}_secret_id", config.secretId)
            putString("${config.id}_base_url", config.baseUrl)
            putString("${config.id}_region", config.region)
            putString("${config.id}_model", config.model)
            apply()
        }
    }

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
        fun newInstance(configType: String): AIServiceDetailFragment {
            return AIServiceDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("config_type", configType)
                }
            }
        }
    }
}
