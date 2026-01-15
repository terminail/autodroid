package com.autodroid.guardiansdk.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.ui.settings.adapter.SettingAdapter
import com.autodroid.guardiansdk.ui.settings.model.SettingItem

/**
 * 我的设置界面
 * 显示所有设置项的列表，点击弹出对应的修改对话框
 * 包括：我的监护人、报警模式、位置密码本等设置项和历史记录
 */
class SettingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var settingAdapter: SettingAdapter

    companion object {
        fun newInstance() = SettingFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.guardian_fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        loadSettingItems()
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        settingAdapter = SettingAdapter()
        settingAdapter.setOnItemClickListener(object : SettingAdapter.OnItemClickListener {
            override fun onMyGuardianClick(index: Int) {
                showMyGuardianDialog(index)
            }

            override fun onVolumeKeyAlarmModeClick() {
                showVolumeKeyAlarmModeDialog()
            }

            override fun onFloatingWindowAlarmModeClick() {
                showFloatingWindowAlarmModeDialog()
            }

            override fun onShakePhoneAlarmModeClick() {
                showShakePhoneAlarmModeDialog()
            }

            override fun onPasswordBookClick() {
                showPasswordBookDialog()
            }

            override fun onFloatingWindowClick() {
                showFloatingWindowDialog()
            }

            override fun onWipeAlarmInfoClick() {
                showWipeAlarmInfoDialog()
            }

            override fun onDoorPassphraseClick() {
                showDoorPassphraseDialog()
            }

            override fun onTestModeClick() {
                showTestModeDialog()
            }
        })

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = settingAdapter
        }
    }

    /**
     * 加载设置项列表，包含所有要求的设置项和历史记录
     */
    private fun loadSettingItems() {
        val settingItems = mutableListOf<SettingItem>()

        // 添加我的监护人设置项（最多5个）
        for (index in 1..5) {
            settingItems.add(SettingItem.MyGuardian(index = index))
        }

        // 添加报警模式相关设置
        settingItems.add(SettingItem.VolumeKeyAlarmMode())
        settingItems.add(SettingItem.FloatingWindowAlarmMode())
        settingItems.add(SettingItem.ShakePhoneAlarmMode())

        // 添加其他常规设置
        settingItems.add(SettingItem.PasswordBook())
        settingItems.add(SettingItem.FloatingWindow())
        settingItems.add(SettingItem.WipeAlarmInfo())
        settingItems.add(SettingItem.DoorPassphrase())
        settingItems.add(SettingItem.TestMode())

        // 添加历史记录（示例数据）
        settingItems.add(SettingItem.AlarmHistory(
            time = "12:34",
            description = "长按音量键报警，报警信息：有人打我，位置xxxx，xxxx"
        ))

        settingItems.add(SettingItem.GuardianQueryHistory(
            time = "11:22",
            guardianName = "爸爸",
            queryContent = "在哪里",
            responseContent = "我的位置xxxx，xxxx"
        ))

        settingItems.add(SettingItem.AlarmHistory(
            time = "09:15",
            description = "摇动手机报警，报警信息：遇到危险，请帮助我，位置xxxx，xxxx"
        ))

        settingItems.add(SettingItem.GuardianQueryHistory(
            time = "08:45",
            guardianName = "妈妈",
            queryContent = "现在安全吗？",
            responseContent = "目前安全，在公司"
        ))

        settingAdapter.updateItems(settingItems)
    }

    /**
     * 显示我的监护人设置对话框
     */
    private fun showMyGuardianDialog(index: Int) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_my_guardian, null)

        AlertDialog.Builder(requireContext())
            .setTitle("我的监护人${index}")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存监护人设置
                saveMyGuardianSettings(dialogView, index)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 显示音量键报警模式设置对话框
     */
    private fun showVolumeKeyAlarmModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_volume_key_alarm_mode, null)

        AlertDialog.Builder(requireContext())
            .setTitle("音量键报警模式设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存音量键报警模式设置
                saveVolumeKeyAlarmModeSettings(dialogView)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 显示浮动窗口报警模式设置对话框
     */
    private fun showFloatingWindowAlarmModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_floating_window_alarm_mode, null)

        AlertDialog.Builder(requireContext())
            .setTitle("浮动窗口报警模式设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存浮动窗口报警模式设置
                saveFloatingWindowAlarmModeSettings(dialogView)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 显示摇动手机报警模式设置对话框
     */
    private fun showShakePhoneAlarmModeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_shake_phone_alarm_mode, null)

        AlertDialog.Builder(requireContext())
            .setTitle("摇动手机报警模式设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存摇动手机报警模式设置
                saveShakePhoneAlarmModeSettings(dialogView)
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
            .inflate(R.layout.guardian_dialog_password_book, null)

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
            .inflate(R.layout.guardian_dialog_floating_window, null)

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
     * 显示擦除报警信息设置对话框
     */
    private fun showWipeAlarmInfoDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_wipe_alarm_info, null)

        AlertDialog.Builder(requireContext())
            .setTitle("擦除报警信息设置")
            .setView(dialogView)
            .setPositiveButton("确定") { dialog, _ ->
                // 保存擦除报警信息设置
                saveWipeAlarmInfoSettings(dialogView)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 显示短信开门密语设置对话框
     */
    private fun showDoorPassphraseDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_door_passphrase, null)

        AlertDialog.Builder(requireContext())
            .setTitle("短信开门密语设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                // 保存短信开门密语设置
                saveDoorPassphraseSettings(dialogView)
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
            .inflate(R.layout.guardian_dialog_test_mode, null)

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

    // 各种保存方法（待实现）
    private fun saveMyGuardianSettings(dialogView: View, index: Int) {}
    private fun saveVolumeKeyAlarmModeSettings(dialogView: View) {}
    private fun saveFloatingWindowAlarmModeSettings(dialogView: View) {}
    private fun saveShakePhoneAlarmModeSettings(dialogView: View) {}
    private fun savePasswordBookSettings(dialogView: View) {}
    private fun saveFloatingWindowSettings(dialogView: View) {}
    private fun saveWipeAlarmInfoSettings(dialogView: View) {}
    private fun saveDoorPassphraseSettings(dialogView: View) {}
    private fun saveTestModeSettings(dialogView: View) {}

    override fun onDestroyView() {
        super.onDestroyView()
    }
}