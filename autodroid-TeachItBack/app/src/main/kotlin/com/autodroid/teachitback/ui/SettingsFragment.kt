package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.FragmentSettingsBinding
import com.autodroid.teachitback.di.ViewModelFactoryProvider
import com.autodroid.teachitback.ui.adapter.SettingsAdapter
import com.autodroid.teachitback.viewmodel.SettingsViewModel

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

        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
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

    private fun handleSettingsItemClick(item: com.autodroid.teachitback.ui.adapter.SettingsItem) {
        when (item) {
            is com.autodroid.teachitback.ui.adapter.SettingsItem.TencentCloudAIServiceItem -> {
                navigateToAIServiceDetail("tencent-hunyuan")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.DoubaoAIServiceItem -> {
                navigateToAIServiceDetail("doubao")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.DeepSeekAIServiceItem -> {
                navigateToAIServiceDetail("deepseek")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.MinimaxAIServiceItem -> {
                navigateToAIServiceDetail("minimax")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.KimiAIServiceItem -> {
                navigateToAIServiceDetail("kimi")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.OpenAIServiceItem -> {
                navigateToAIServiceDetail("openai")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.ErnieAIServiceItem -> {
                navigateToAIServiceDetail("ernie")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.QwenAIServiceItem -> {
                navigateToAIServiceDetail("qwen")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.ZhipuAIServiceItem -> {
                navigateToAIServiceDetail("zhipu")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.SparkAIServiceItem -> {
                navigateToAIServiceDetail("spark")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.HunyuanAIServiceItem -> {
                navigateToAIServiceDetail("hunyuan")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.BaichuanAIServiceItem -> {
                navigateToAIServiceDetail("baichuan")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.LingyiAIServiceItem -> {
                navigateToAIServiceDetail("lingyi")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.JieyueAIServiceItem -> {
                navigateToAIServiceDetail("jieyue")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.ChatGLMAIServiceItem -> {
                navigateToAIServiceDetail("chatglm")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.TinyBERTAIServiceItem -> {
                navigateToAIServiceDetail("tinybert")
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.DarkModeSwitchItem -> {
                viewModel.updateSwitchSetting("dark_mode", !item.isChecked)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.AutoSaveSwitchItem -> {
                viewModel.updateSwitchSetting("auto_save", !item.isChecked)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.LanguageSettingItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_preferences)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.BackupDataItem -> {
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.RestoreDataItem -> {
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.ClearAllDataButtonItem -> {
                viewModel.clearAllData()
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.VersionInfoItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_about)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.HelpAndFeedbackItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_about)
            }
            else -> {}
        }
    }

    private fun navigateToAIServiceDetail(configType: String) {
        val bundle = Bundle().apply {
            putString("config_type", configType)
        }
        findNavController().navigate(R.id.action_nav_settings_to_ai_service_detail, bundle)
    }

    private fun observeViewModel() {
        viewModel.settingsItems.observe(viewLifecycleOwner) { items ->
            settingsAdapter.submitList(items)
        }

        viewModel.aiServiceConfigs.observe(viewLifecycleOwner) { _ ->
            // 当AI服务配置发生变化时，重新加载设置项并更新服务状态
            viewModel.loadSettings()
            val serviceStatuses = viewModel.getAIServiceStatuses()
            settingsAdapter.updateAIServiceStatus(serviceStatuses)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
