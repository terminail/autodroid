package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.SettingItemOpenaiDetailBinding
import com.autodroid.teachitback.utils.AIServiceProvider
import com.autodroid.teachitback.MainActivity

class AISettingsFragment : Fragment() {
    
    companion object {
        private const val ARG_SERVICE = "ai_service"
        
        fun newInstance(service: AIServiceProvider): AISettingsFragment {
            val fragment = AISettingsFragment()
            val args = Bundle()
            args.putSerializable(ARG_SERVICE, service)
            fragment.arguments = args
            return fragment
        }
    }
    
    private var _binding: SettingItemOpenaiDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var aiService: AIServiceProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingItemOpenaiDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        arguments?.getSerializable(ARG_SERVICE)?.let { 
            aiService = it as AIServiceProvider
        }

        setupToolbar()
        setupSaveButton()
    }

    private fun setupToolbar() {
        (requireActivity() as? MainActivity)?.setToolbarTitle("${aiService.name} 配置")
        (requireActivity() as? MainActivity)?.showBackButton(true)
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "${aiService.name}配置已保存", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}