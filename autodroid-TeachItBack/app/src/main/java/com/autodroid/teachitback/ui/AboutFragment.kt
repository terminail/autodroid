package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupContent()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        binding.toolbar.title = "关于应用"
    }

    private fun setupContent() {
        binding.appVersion.text = "版本 1.0.0"
        binding.appDescription.text = "TeachItBack - 基于苏格拉底和费曼学习方法的AI学习助手\n\n帮助用户通过对话式学习掌握复杂概念，提升学习效率。"
        binding.developerInfo.text = "开发者: Autodroid Team"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}