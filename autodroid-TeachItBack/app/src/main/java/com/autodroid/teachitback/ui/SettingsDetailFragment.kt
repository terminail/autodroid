package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.autodroid.teachitback.MainActivity
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.FragmentSettingsDetailBinding
import com.autodroid.teachitback.viewmodel.AppViewModel

class SettingsDetailFragment : Fragment() {
    private var _binding: FragmentSettingsDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AppViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AppViewModel::class.java]

        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        // Set toolbar title to "AI 设置"
        setToolbarTitle("AI 设置")

        binding.saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun setToolbarTitle(title: String) {
        (requireActivity() as? MainActivity)?.supportActionBar?.title = title
    }

    private fun loadSettings() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val apiKey = sharedPreferences.getString("ai_api_key", "") ?: ""
        val model = sharedPreferences.getString("ai_model", "gpt-3.5-turbo") ?: "gpt-3.5-turbo"

        binding.apiKeyInput.setText(apiKey)
        binding.modelInput.setText(model)
    }

    private fun saveSettings() {
        val apiKey = binding.apiKeyInput.text.toString().trim()
        val model = binding.modelInput.text.toString().trim()

        if (apiKey.isEmpty()) {
            Toast.makeText(requireContext(), "API密钥不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        sharedPreferences.edit()
            .putString("ai_api_key", apiKey)
            .putString("ai_model", model)
            .apply()

        viewModel.initializeAI(apiKey, model)

        Toast.makeText(requireContext(), "设置已保存", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
