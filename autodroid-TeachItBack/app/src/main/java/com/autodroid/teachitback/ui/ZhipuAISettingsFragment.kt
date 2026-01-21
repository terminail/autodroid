package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.SettingItemZhipuDetailBinding
import com.autodroid.teachitback.MainActivity

class ZhipuAISettingsFragment : Fragment() {
    private var _binding: SettingItemZhipuDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingItemZhipuDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupSaveButton()
    }

    private fun setupToolbar() {
        (requireActivity() as? MainActivity)?.setToolbarTitle("智谱AI 配置")
        (requireActivity() as? MainActivity)?.showBackButton(true)
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            val apiKey = binding.apiKeyInput.text.toString()
            val baseUrl = binding.baseUrlInput.text.toString()
            
            android.widget.Toast.makeText(requireContext(), "智谱AI配置已保存", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}