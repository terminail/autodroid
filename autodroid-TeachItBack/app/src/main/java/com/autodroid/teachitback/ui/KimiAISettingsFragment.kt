package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.SettingItemKimiDetailBinding
import com.autodroid.teachitback.MainActivity

class KimiAISettingsFragment : Fragment() {
    private var _binding: SettingItemKimiDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingItemKimiDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupSaveButton()
    }

    private fun setupToolbar() {
        (requireActivity() as? MainActivity)?.setToolbarTitle("Kimi 配置")
        (requireActivity() as? MainActivity)?.showBackButton(true)
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            val apiKey = binding.apiKeyInput.text.toString()
            val baseUrl = binding.baseUrlInput.text.toString()
            
            android.widget.Toast.makeText(requireContext(), "Kimi配置已保存", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}