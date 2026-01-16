package com.autodroid.guardiansdk.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodroid.guardiansdk.data.database.GuardianDatabase
import com.autodroid.guardiansdk.data.entity.Setting
import com.autodroid.guardiansdk.ui.settings.model.SettingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 设置页面的ViewModel
 * 负责加载和更新设置项数据（报警模式、报警密码等）
 */
class SettingViewModel(private val database: GuardianDatabase) : ViewModel() {
    
    private val _settingItems = MutableStateFlow<List<SettingItem>>(emptyList())
    val settingItems: StateFlow<List<SettingItem>> = _settingItems.asStateFlow()
    
    private val settingDao = database.settingDao()
    
    /**
     * 加载所有设置项
     */
    fun loadSettingItems() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settingItems = mutableListOf<SettingItem>()
                
                // 加载5个报警联系人数据
                val guardian1Name = settingDao.getSetting("guardian1_name")?.value ?: ""
                val guardian2Name = settingDao.getSetting("guardian2_name")?.value ?: ""
                val guardian3Name = settingDao.getSetting("guardian3_name")?.value ?: ""
                val guardian4Name = settingDao.getSetting("guardian4_name")?.value ?: ""
                val guardian5Name = settingDao.getSetting("guardian5_name")?.value ?: ""

                settingItems.add(SettingItem.GuardianItem1(
                    phoneNumber = settingDao.getSetting("guardian1_phone")?.value ?: "",
                    name = guardian1Name,
                    isPrimary = settingDao.getSetting("guardian1_primary")?.value?.toBoolean() ?: false
                ))
                settingItems.add(SettingItem.GuardianItem2(
                    phoneNumber = settingDao.getSetting("guardian2_phone")?.value ?: "",
                    name = guardian2Name,
                    isPrimary = settingDao.getSetting("guardian2_primary")?.value?.toBoolean() ?: false
                ))
                settingItems.add(SettingItem.GuardianItem3(
                    phoneNumber = settingDao.getSetting("guardian3_phone")?.value ?: "",
                    name = guardian3Name,
                    isPrimary = settingDao.getSetting("guardian3_primary")?.value?.toBoolean() ?: false
                ))
                settingItems.add(SettingItem.GuardianItem4(
                    phoneNumber = settingDao.getSetting("guardian4_phone")?.value ?: "",
                    name = guardian4Name,
                    isPrimary = settingDao.getSetting("guardian4_primary")?.value?.toBoolean() ?: false
                ))
                settingItems.add(SettingItem.GuardianItem5(
                    phoneNumber = settingDao.getSetting("guardian5_phone")?.value ?: "",
                    name = guardian5Name,
                    isPrimary = settingDao.getSetting("guardian5_primary")?.value?.toBoolean() ?: false
                ))
                
                // 添加报警模式相关设置，加载实际数据
                settingItems.addAll(loadAlarmModeSettings())
                
                // 添加报警信息密码
                settingItems.add(SettingItem.AlarmMessage())
                
                // 添加其他常规设置
                settingItems.add(SettingItem.DoorPassphrase())
                settingItems.add(SettingItem.TestMode())
                
                // 添加历史记录（示例数据）
                settingItems.addAll(loadHistoryItems())
                
                _settingItems.value = settingItems
                
            } catch (e: Exception) {
                // 出错时返回空列表
                _settingItems.value = emptyList()
            }
        }
    }
    
    /**
     * 加载报警模式设置项的实际数据
     */
    private suspend fun loadAlarmModeSettings(): List<SettingItem> {
        val alarmModeSettings = mutableListOf<SettingItem>()
        
        // 音量键报警模式
        val volumeKeyEnabled = settingDao.getSetting("volume_key_alarm_enabled")?.value?.toBoolean() ?: true
        val normalTime = settingDao.getSetting("volume_key_normal_hold_time")?.value?.toIntOrNull() ?: 5
        val emergencyTime = settingDao.getSetting("volume_key_emergency_hold_time")?.value?.toIntOrNull() ?: 10
        
        val volumeKeySetting = SettingItem.VolumeKeyAlarmMode(
            enabled = volumeKeyEnabled,
            normalHoldTime = normalTime,
            emergencyHoldTime = emergencyTime
        )
        alarmModeSettings.add(volumeKeySetting)
        
        // 浮动窗口报警模式
        val floatingWindowEnabled = settingDao.getSetting("floating_window_alarm_enabled")?.value?.toBoolean() ?: true
        val floatingNormalTime = settingDao.getSetting("floating_window_normal_hold_time")?.value?.toIntOrNull() ?: 5
        val floatingNormalOpacity = settingDao.getSetting("floating_window_normal_opacity")?.value?.toIntOrNull() ?: 50
        val floatingEmergencyTime = settingDao.getSetting("floating_window_emergency_hold_time")?.value?.toIntOrNull() ?: 10
        val floatingEmergencyOpacity = settingDao.getSetting("floating_window_emergency_opacity")?.value?.toIntOrNull() ?: 80
        
        val floatingWindowSetting = SettingItem.FloatingWindowAlarmMode(
            enabled = floatingWindowEnabled,
            normalHoldTime = floatingNormalTime,
            normalOpacity = floatingNormalOpacity,
            emergencyHoldTime = floatingEmergencyTime,
            emergencyOpacity = floatingEmergencyOpacity
        )
        alarmModeSettings.add(floatingWindowSetting)
        
        // 摇动手机报警模式
        val shakeEnabled = settingDao.getSetting("shake_alarm_enabled")?.value?.toBoolean() ?: true
        val normalSensitivity = settingDao.getSetting("shake_normal_sensitivity")?.value?.toIntOrNull() ?: 5
        val emergencySensitivity = settingDao.getSetting("shake_emergency_sensitivity")?.value?.toIntOrNull() ?: 8
        
        val shakePhoneSetting = SettingItem.ShakePhoneAlarmMode(
            enabled = shakeEnabled,
            normalSensitivity = normalSensitivity,
            emergencySensitivity = emergencySensitivity
        )
        alarmModeSettings.add(shakePhoneSetting)
        
        return alarmModeSettings
    }
    
    /**
     * 加载历史记录项
     */
    private fun loadHistoryItems(): List<SettingItem> {
        return listOf(
            SettingItem.AlarmHistory(
                time = "12:34",
                description = "长按音量键报警，报警信息：有人打我，位置xxxx，xxxx"
            ),
            SettingItem.GuardianQueryHistory(
                time = "11:22",
                guardianName = "爸爸",
                queryContent = "在哪里",
                responseContent = "我的位置xxxx，xxxx"
            ),
            SettingItem.AlarmHistory(
                time = "09:15",
                description = "摇动手机报警，报警信息：遇到危险，请帮助我，位置xxxx，xxxx"
            ),
            SettingItem.GuardianQueryHistory(
                time = "08:45",
                guardianName = "妈妈",
                queryContent = "现在安全吗？",
                responseContent = "目前安全，在公司"
            )
        )
    }
    
    /**
     * 更新音量键报警模式设置
     */
    fun updateVolumeKeyAlarmMode(enabled: Boolean, normalTime: Int, emergencyTime: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = listOf(
                    Setting(
                        key = "volume_key_alarm_enabled",
                        value = enabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "音量键报警启用状态",
                        category = "alarm"
                    ),
                    Setting(
                        key = "volume_key_normal_hold_time",
                        value = normalTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "音量键普通报警长按时间（秒）",
                        category = "alarm"
                    ),
                    Setting(
                        key = "volume_key_emergency_hold_time",
                        value = emergencyTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "音量键紧急报警长按时间（秒）",
                        category = "alarm"
                    )
                )
                
                settingDao.insertOrUpdateAll(settings)
                
                // 重新加载设置项以更新UI
                loadSettingItems()
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 更新浮动窗口报警模式设置
     */
    fun updateFloatingWindowAlarmMode(enabled: Boolean, normalTime: Int, normalOpacity: Int, emergencyTime: Int, emergencyOpacity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = listOf(
                    Setting(
                        key = "floating_window_alarm_enabled",
                        value = enabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "浮动窗口报警启用状态",
                        category = "alarm"
                    ),
                    Setting(
                        key = "floating_window_normal_hold_time",
                        value = normalTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "浮动窗口普通报警长按时间（秒）",
                        category = "alarm"
                    ),
                    Setting(
                        key = "floating_window_normal_opacity",
                        value = normalOpacity.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "浮动窗口普通报警透明度（%）",
                        category = "alarm"
                    ),
                    Setting(
                        key = "floating_window_emergency_hold_time",
                        value = emergencyTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "浮动窗口紧急报警长按时间（秒）",
                        category = "alarm"
                    ),
                    Setting(
                        key = "floating_window_emergency_opacity",
                        value = emergencyOpacity.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "浮动窗口紧急报警透明度（%）",
                        category = "alarm"
                    )
                )
                
                settingDao.insertOrUpdateAll(settings)
                
                // 重新加载设置项以更新UI
                loadSettingItems()
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 更新摇动手机报警模式设置
     */
    fun updateShakePhoneAlarmMode(enabled: Boolean, normalSensitivity: Int, emergencySensitivity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = listOf(
                    Setting(
                        key = "shake_alarm_enabled",
                        value = enabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "摇动手机报警启用状态",
                        category = "alarm"
                    ),
                    Setting(
                        key = "shake_normal_sensitivity",
                        value = normalSensitivity.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "摇动手机普通报警敏感度（1-10级）",
                        category = "alarm"
                    ),
                    Setting(
                        key = "shake_emergency_sensitivity",
                        value = emergencySensitivity.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "摇动手机紧急报警敏感度（1-10级）",
                        category = "alarm"
                    )
                )
                
                settingDao.insertOrUpdateAll(settings)
                
                // 重新加载设置项以更新UI
                loadSettingItems()
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 根据键获取设置项
     */
    suspend fun getSetting(key: String): Setting? {
        return settingDao.getSetting(key)
    }
    
    /**
     * 批量插入或更新设置项
     */
    suspend fun insertOrUpdateAllSettings(settings: List<Setting>) {
        settingDao.insertOrUpdateAll(settings)
    }
    
    /**
     * 插入或更新单个设置项
     */
    suspend fun insertOrUpdateSetting(setting: Setting) {
        settingDao.insertOrUpdate(setting)
    }

    /**
     * 保存报警联系人
     */
    fun saveGuardianContact(index: Int, name: String, phoneNumber: String, isPrimary: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = listOf(
                    Setting(
                        key = "guardian${index}_name",
                        value = name,
                        type = com.autodroid.guardiansdk.data.entity.SettingType.STRING,
                        description = "报警联系人${index}姓名",
                        category = "guardian"
                    ),
                    Setting(
                        key = "guardian${index}_phone",
                        value = phoneNumber,
                        type = com.autodroid.guardiansdk.data.entity.SettingType.STRING,
                        description = "报警联系人${index}电话",
                        category = "guardian"
                    ),
                    Setting(
                        key = "guardian${index}_primary",
                        value = isPrimary.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "报警联系人${index}是否主要联系人",
                        category = "guardian"
                    )
                )
                settingDao.insertOrUpdateAll(settings)

                // 重新加载设置项以更新UI
                loadSettingItems()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    /**
     * 获取报警联系人姓名
     */
    suspend fun getGuardianName(index: Int): String {
        return settingDao.getSetting("guardian${index}_name")?.value ?: ""
    }

    /**
     * 获取报警联系人电话
     */
    suspend fun getGuardianPhoneNumber(index: Int): String {
        return settingDao.getSetting("guardian${index}_phone")?.value ?: ""
    }

    /**
     * 获取报警联系人是否主要
     */
    suspend fun getGuardianPrimary(index: Int): Boolean {
        return settingDao.getSetting("guardian${index}_primary")?.value?.toBoolean() ?: false
    }
}