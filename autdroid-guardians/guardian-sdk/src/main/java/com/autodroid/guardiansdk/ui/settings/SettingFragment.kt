package com.autodroid.guardiansdk.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.databinding.FragmentSettingBinding
import com.autodroid.guardiansdk.ui.settings.adapter.SettingAdapter
import com.autodroid.guardiansdk.ui.settings.model.SettingItem

/**
 * 隐秘设置列表界面
 * 显示所有设置项的列表，点击弹出对应的修改对话框
 */
class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingAdapter: SettingAdapter

    companion object {
        fun newInstance() = SettingFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadSettingItems()
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        settingAdapter = SettingAdapter()
        settingAdapter.setOnItemClickListener(object : SettingAdapter.OnItemClickListener {
            override fun onEmergencyContactsClick() {
                showEmergencyContactDialog()
            }
            override fun onAlarmModeClick() {
                showAlarmModeDialog()
            }
            override fun onPasswordBookClick() {
                showPasswordBookDialog()
            }
            override fun onFloatingWindowClick() {
                showFloatingWindowDialog()
            }
            override fun onTestModeClick() {
                showTestModeDialog()
            }
            override fun onEmergencyWipeClick() {
                showEmergencyWipeDialog()
            }
        })
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = settingAdapter
        }
    }

    /**
     * 加载设置项列表
     */
    private fun loadSettingItems() {
        val settingItems = listOf<SettingItem>(
            SettingItem.EmergencyContacts(),
            SettingItem.AlarmMode(),
            SettingItem.PasswordBook(),
            SettingItem.FloatingWindow(),
            SettingItem.TestMode(),
            SettingItem.EmergencyWipe()
        )
        
        settingAdapter.updateItems(settingItems)
    }

    /**
     * 显示紧急联系人设置对话框
     */
    private fun showEmergencyContactDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_emergency_contact, null)
        
        AlertDialog.Builder(requireContext())
            .setTitle("紧急联系人设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存联系人设置
                saveEmergencyContactSettings(dialogView)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 显示报警模式设置对话框
     */
    private fun showAlarmModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_alarm_mode, null)
        
        AlertDialog.Builder(requireContext())
            .setTitle("报警模式设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存报警模式设置
                saveAlarmModeSettings(dialogView)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 显示位置密码本设置对话框
     */
    private fun showPasswordBookDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_password_book, null)
        
        AlertDialog.Builder(requireContext())
            .setTitle("位置密码本设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存密码本设置
                savePasswordBookSettings(dialogView)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 显示浮动窗口设置对话框
     */
    private fun showFloatingWindowDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_floating_window, null)
        
        AlertDialog.Builder(requireContext())
            .setTitle("浮动窗口设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存浮动窗口设置
                saveFloatingWindowSettings(dialogView)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 显示测试模式设置对话框
     */
    private fun showTestModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_test_mode, null)
        
        AlertDialog.Builder(requireContext())
            .setTitle("测试模式设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存测试模式设置
                saveTestModeSettings(dialogView)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 显示紧急擦除设置对话框
     */
    private fun showEmergencyWipeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_emergency_wipe, null)
        
        AlertDialog.Builder(requireContext())
            .setTitle("紧急擦除设置")
            .setView(dialogView)
            .setPositiveButton("确认擦除") { dialog, _ ->
                // 执行紧急擦除
                performEmergencyWipe()
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // 各种保存方法（待实现）
    private fun saveEmergencyContactSettings(dialogView: View) {}
    private fun saveAlarmModeSettings(dialogView: View) {}
    private fun savePasswordBookSettings(dialogView: View) {}
    private fun saveFloatingWindowSettings(dialogView: View) {}
    private fun saveTestModeSettings(dialogView: View) {}
    private fun performEmergencyWipe() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}