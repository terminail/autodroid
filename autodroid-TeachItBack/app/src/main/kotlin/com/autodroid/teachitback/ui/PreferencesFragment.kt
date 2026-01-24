package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.FragmentPreferencesBinding

class PreferencesFragment : Fragment() {
    private var _binding: FragmentPreferencesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreferencesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupPreferences()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        binding.toolbar.title = "个人偏好"
    }

    private fun setupPreferences() {
        // 设置主题偏好
        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 保存主题偏好
            val sharedPref = requireContext().getSharedPreferences("preferences", android.content.Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("dark_theme", isChecked).apply()
        }

        // 设置通知偏好
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 保存通知偏好
            val sharedPref = requireContext().getSharedPreferences("preferences", android.content.Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("notifications", isChecked).apply()
        }

        // 设置学习提醒偏好
        binding.remindersSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 保存学习提醒偏好
            val sharedPref = requireContext().getSharedPreferences("preferences", android.content.Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("study_reminders", isChecked).apply()
        }

        // 加载已保存的偏好设置
        loadSavedPreferences()
    }

    private fun loadSavedPreferences() {
        val sharedPref = requireContext().getSharedPreferences("preferences", android.content.Context.MODE_PRIVATE)
        
        binding.themeSwitch.isChecked = sharedPref.getBoolean("dark_theme", false)
        binding.notificationsSwitch.isChecked = sharedPref.getBoolean("notifications", true)
        binding.remindersSwitch.isChecked = sharedPref.getBoolean("study_reminders", true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}