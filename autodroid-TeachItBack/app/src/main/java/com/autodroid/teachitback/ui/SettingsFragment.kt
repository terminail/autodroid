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
import com.autodroid.teachitback.repository.SettingsRepository
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
        viewModel = ViewModelProvider(requireActivity())[SettingsViewModel::class.java]

        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
    }

    private fun setupRecyclerView() {
        settingsAdapter = SettingsAdapter { item ->
            handleSettingsItemClick(item)
        }

        binding.settingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = settingsAdapter
        }
    }

    private fun handleSettingsItemClick(item: com.autodroid.teachitback.ui.adapter.SettingsItem) {
        when (item) {
            is com.autodroid.teachitback.ui.adapter.SettingsItem.DoubaoAIServiceItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_doubao)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.DeepSeekAIServiceItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_deepseek)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.MinimaxAIServiceItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_minimax)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.KimiAIServiceItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_kimi)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.OpenAIServiceItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_openai)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.ErnieAIServiceItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_ernie)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.QwenAIServiceItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_qwen)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.ZhipuAIServiceItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_zhipu)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.SparkAIServiceItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_spark)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.HunyuanAIServiceItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_hunyuan)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.BaichuanAIServiceItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_baichuan)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.LingyiAIServiceItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_lingyi)
            }
            is com.autodroid.teachitback.ui.adapter.SettingsItem.JieyueAIServiceItem -> {
                findNavController().navigate(R.id.action_nav_settings_to_jieyue)
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

    private fun observeViewModel() {
        viewModel.settingsItems.observe(viewLifecycleOwner) { items ->
            settingsAdapter.submitList(items)
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
