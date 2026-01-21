package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.autodroid.teachitback.databinding.SettingItemLingyiDetailBinding
import com.autodroid.teachitback.MainActivity

class LingyiAISettingsFragment : Fragment() {
    
    private var _binding: SettingItemLingyiDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingItemLingyiDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        loadSettings()
        setupClickListeners()
    }

    private fun setupToolbar() {
        (requireActivity() as? MainActivity)?.setToolbarTitle("零一万物 配置")
        (requireActivity() as? MainActivity)?.showBackButton(true)
    }

    private fun loadSettings() {
        val apiKey = "" // TODO: Load from SharedPreferences
        val baseUrl = "https://open.lingyiwanwu.com"
        
        binding.apiKeyInput.setText(apiKey)
        binding.baseUrlInput.setText(baseUrl)
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        val apiKey = binding.apiKeyInput.text.toString().trim()
        val baseUrl = binding.baseUrlInput.text.toString().trim()
        
        // TODO: Save to SharedPreferences
        
        // Show success message
        android.widget.Toast.makeText(requireContext(), "零一万物配置已保存", android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}