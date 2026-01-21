package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.FragmentSettingsBinding
import com.autodroid.teachitback.ui.adapter.SettingsAdapter
import com.autodroid.teachitback.utils.DataInitializer

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

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

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val settingsAdapter = SettingsAdapter { item ->
            when (item) {
                is com.autodroid.teachitback.ui.adapter.SettingsItem.AIService -> {
                    // 根据AI服务类型导航到对应的设置页面
                    when (item.service.id) {
                        "doubao" -> findNavController().navigate(R.id.action_nav_settings_to_doubao)
                        "ernie" -> findNavController().navigate(R.id.action_nav_settings_to_ernie)
                        "qwen" -> findNavController().navigate(R.id.action_nav_settings_to_qwen)
                        "deepseek" -> findNavController().navigate(R.id.action_nav_settings_to_deepseek)
                        "zhipu" -> findNavController().navigate(R.id.action_nav_settings_to_zhipu)
                        "spark" -> findNavController().navigate(R.id.action_nav_settings_to_spark)
                        "minimax" -> findNavController().navigate(R.id.action_nav_settings_to_minimax)
                        "kimi" -> findNavController().navigate(R.id.action_nav_settings_to_kimi)
                        "hunyuan" -> findNavController().navigate(R.id.action_nav_settings_to_hunyuan)
                        "baichuan" -> findNavController().navigate(R.id.action_nav_settings_to_baichuan)
                        "lingyi" -> findNavController().navigate(R.id.action_nav_settings_to_lingyi)
                        "jieyue" -> findNavController().navigate(R.id.action_nav_settings_to_jieyue)
                        else -> {
                            // 其他AI服务的通用设置页面
                            // 暂时使用默认导航，后续可以添加通用设置页面
                            findNavController().navigate(R.id.action_nav_settings_to_doubao)
                        }
                    }
                }
                is com.autodroid.teachitback.ui.adapter.SettingsItem.OtherSetting -> {
                    when (item.type) {
                        "about" -> findNavController().navigate(R.id.action_nav_settings_to_about)
                        "preferences" -> findNavController().navigate(R.id.action_nav_settings_to_preferences)
                        else -> {}
                    }
                }
                else -> {}
            }
        }

        // 创建设置项列表
        val items = mutableListOf<com.autodroid.teachitback.ui.adapter.SettingsItem>()
        
        // AI服务提供商
        items.add(com.autodroid.teachitback.ui.adapter.SettingsItem.Header("AI 服务"))
        DataInitializer.getAIServiceProviders().forEach { service ->
            items.add(com.autodroid.teachitback.ui.adapter.SettingsItem.AIService(service))
        }
        
        items.add(com.autodroid.teachitback.ui.adapter.SettingsItem.Divider)
        
        // 其他设置
        items.add(com.autodroid.teachitback.ui.adapter.SettingsItem.Header("其他"))
        items.add(com.autodroid.teachitback.ui.adapter.SettingsItem.OtherSetting("关于", "应用信息、版本号和帮助", "about"))
        items.add(com.autodroid.teachitback.ui.adapter.SettingsItem.OtherSetting("个人偏好", "界面主题、语言设置等", "preferences"))

        binding.settingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = settingsAdapter
        }
        
        settingsAdapter.submitList(items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
