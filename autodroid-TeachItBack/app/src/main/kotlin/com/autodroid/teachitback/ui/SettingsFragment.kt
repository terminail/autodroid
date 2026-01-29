package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.FragmentSettingsBinding
import com.autodroid.teachitback.di.ViewModelFactoryProvider
import com.autodroid.teachitback.ui.adapter.SettingsAdapter
import com.autodroid.teachitback.ui.adapter.SettingsItem
import com.autodroid.teachitback.viewmodel.SettingsViewModel

/**
 * SettingsFragment
 * 
 * 传统方式：不使用 LiveData，使用回调接口
 */
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SettingsViewModel
    private lateinit var settingsAdapter: SettingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 使用自定义的ViewModelFactory来创建ViewModel实例
        val factory = ViewModelFactoryProvider.getFactory()
        viewModel = ViewModelProvider(requireActivity(), factory)[SettingsViewModel::class.java]

        setupRecyclerView()
    }
    
    override fun onResume() {
        super.onResume()
        // 每次进入页面时刷新数据
        loadAllSettings()
    }

    private fun setupRecyclerView() {
        settingsAdapter = SettingsAdapter(
            onItemClick = { item ->
                handleSettingsItemClick(item)
            }
        )

        binding.settingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = settingsAdapter
        }
    }
    
    /**
     * 加载所有设置项（传统回调方式）
     */
    private fun loadAllSettings() {
        viewModel.loadAllSettings(
            onSuccess = { items ->
                settingsAdapter.submitList(items)
            },
            onError = { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            },
            onLoading = { isLoading ->
                // 可以在这里显示/隐藏加载指示器
                // binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        )
    }
    
    private fun handleSettingsItemClick(item: SettingsItem) {
        when (item) {
            is SettingsItem.TencentCloudAIServiceItem -> navigateToAIServiceDetail("tencent-hunyuan")
            is SettingsItem.DoubaoAIServiceItem -> navigateToAIServiceDetail("doubao")
            is SettingsItem.DeepSeekAIServiceItem -> navigateToAIServiceDetail("deepseek")
            is SettingsItem.MinimaxAIServiceItem -> navigateToAIServiceDetail("minimax")
            is SettingsItem.KimiAIServiceItem -> navigateToAIServiceDetail("kimi")
            is SettingsItem.OpenAIServiceItem -> navigateToAIServiceDetail("openai")
            is SettingsItem.ErnieAIServiceItem -> navigateToAIServiceDetail("ernie")
            is SettingsItem.QwenAIServiceItem -> navigateToAIServiceDetail("qwen")
            is SettingsItem.ZhipuAIServiceItem -> navigateToAIServiceDetail("zhipu")
            is SettingsItem.SparkAIServiceItem -> navigateToAIServiceDetail("spark")
            is SettingsItem.HunyuanAIServiceItem -> navigateToAIServiceDetail("hunyuan")
            is SettingsItem.BaichuanAIServiceItem -> navigateToAIServiceDetail("baichuan")
            is SettingsItem.LingyiAIServiceItem -> navigateToAIServiceDetail("lingyi")
            is SettingsItem.JieyueAIServiceItem -> navigateToAIServiceDetail("jieyue")
            is SettingsItem.ChatGLMAIServiceItem -> navigateToAIServiceDetail("chatglm")
            is SettingsItem.TinyBERTAIServiceItem -> navigateToAIServiceDetail("tinybert")
            is SettingsItem.LanguageSettingItem -> navigateToLanguageSettings()
            is SettingsItem.BackupDataItem -> backupData()
            is SettingsItem.RestoreDataItem -> restoreData()
            is SettingsItem.ClearAllDataButtonItem -> clearAllData()
            is SettingsItem.HelpAndFeedbackItem -> navigateToHelpAndFeedback()
            else -> {}
        }
    }

    private fun navigateToAIServiceDetail(configType: String) {
        val bundle = Bundle().apply {
            putString("config_type", configType)
        }
        findNavController().navigate(R.id.action_nav_settings_to_ai_service_detail, bundle)
    }

    private fun navigateToLanguageSettings() {
        // 语言设置页面导航 - 暂时使用 Toast 提示
        Toast.makeText(requireContext(), "语言设置功能开发中...", Toast.LENGTH_SHORT).show()
    }

    private fun backupData() {
        Toast.makeText(requireContext(), "备份功能开发中...", Toast.LENGTH_SHORT).show()
    }

    private fun restoreData() {
        Toast.makeText(requireContext(), "恢复功能开发中...", Toast.LENGTH_SHORT).show()
    }

    private fun clearAllData() {
        Toast.makeText(requireContext(), "清除数据功能开发中...", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToHelpAndFeedback() {
        Toast.makeText(requireContext(), "帮助与反馈功能开发中...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
