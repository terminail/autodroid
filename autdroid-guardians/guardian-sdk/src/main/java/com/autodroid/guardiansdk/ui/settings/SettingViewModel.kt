package com.autodroid.guardiansdk.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.autodroid.guardiansdk.data.database.GuardianDatabase
import com.autodroid.guardiansdk.data.entity.Setting
import com.autodroid.guardiansdk.data.entity.Contact
import com.autodroid.guardiansdk.data.entity.ContactType
import com.autodroid.guardiansdk.ui.settings.model.SettingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置页面的ViewModel
 * 负责加载和更新设置项数据（报警触发模式、报警密码等）
 */
class SettingViewModel(private val database: GuardianDatabase) : ViewModel() {
    
    private val _settingItems = MutableLiveData<List<SettingItem>>()
    val settingItems: LiveData<List<SettingItem>> = _settingItems
    
    private val settingDao = database.settingDao()
    private val contactDao = database.contactDao()
    
    init {
        observeDataChanges()
    }
    
    /**
     * 监听数据变化
     */
    private fun observeDataChanges() {
        viewModelScope.launch {
            combine(
                contactDao.observeContactsByType(ContactType.GUARDIAN),
                settingDao.observeAllSettings()
            ) { guardians, settings ->
                buildSettingItems(guardians, settings)
            }.collect { items ->
                _settingItems.postValue(items)
            }
        }
    }
    
    /**
     * 构建设置项列表
     */
    private fun buildSettingItems(guardians: List<Contact>, settings: List<Setting>): List<SettingItem> {
        val settingItems = mutableListOf<SettingItem>()
        
        // 生成5个监护人项，如果数据库中的联系人不足，用占位符
        for (i in 1..5) {
            val guardian = guardians.find { it.orderIndex == i }
            if (guardian != null) {
                when (i) {
                    1 -> settingItems.add(SettingItem.GuardianItem1(
                        phoneNumber = guardian.phoneNumber,
                        name = guardian.name,
                        isPrimary = guardian.isPrimary,
                        isPlaceholder = false
                    ))
                    2 -> settingItems.add(SettingItem.GuardianItem2(
                        phoneNumber = guardian.phoneNumber,
                        name = guardian.name,
                        isPrimary = guardian.isPrimary,
                        isPlaceholder = false
                    ))
                    3 -> settingItems.add(SettingItem.GuardianItem3(
                        phoneNumber = guardian.phoneNumber,
                        name = guardian.name,
                        isPrimary = guardian.isPrimary,
                        isPlaceholder = false
                    ))
                    4 -> settingItems.add(SettingItem.GuardianItem4(
                        phoneNumber = guardian.phoneNumber,
                        name = guardian.name,
                        isPrimary = guardian.isPrimary,
                        isPlaceholder = false
                    ))
                    5 -> settingItems.add(SettingItem.GuardianItem5(
                        phoneNumber = guardian.phoneNumber,
                        name = guardian.name,
                        isPrimary = guardian.isPrimary,
                        isPlaceholder = false
                    ))
                }
            } else {
                when (i) {
                    1 -> settingItems.add(SettingItem.GuardianItem1(
                        phoneNumber = "",
                        name = "请设置监护人${i}",
                        isPrimary = false,
                        isPlaceholder = true
                    ))
                    2 -> settingItems.add(SettingItem.GuardianItem2(
                        phoneNumber = "",
                        name = "请设置监护人${i}",
                        isPrimary = false,
                        isPlaceholder = true
                    ))
                    3 -> settingItems.add(SettingItem.GuardianItem3(
                        phoneNumber = "",
                        name = "请设置监护人${i}",
                        isPrimary = false,
                        isPlaceholder = true
                    ))
                    4 -> settingItems.add(SettingItem.GuardianItem4(
                        phoneNumber = "",
                        name = "请设置监护人${i}",
                        isPrimary = false,
                        isPlaceholder = true
                    ))
                    5 -> settingItems.add(SettingItem.GuardianItem5(
                        phoneNumber = "",
                        name = "请设置监护人${i}",
                        isPrimary = false,
                        isPlaceholder = true
                    ))
                }
            }
        }
        
        // 添加报警触发模式相关设置，加载实际数据
        settingItems.addAll(loadAlarmModeSettings(settings))
        
        // 添加录音模式
        val recordingEnabled = settings.find { it.key == "recording_enabled" }?.value?.toBoolean() ?: true
        val recordingDuration = settings.find { it.key == "recording_duration" }?.value?.toIntOrNull() ?: 5
        val segmentDuration = settings.find { it.key == "recording_segment_duration" }?.value?.toIntOrNull() ?: 2
        settingItems.add(SettingItem.AlarmRecordingMode(
            enabled = recordingEnabled,
            duration = recordingDuration,
            segmentDuration = segmentDuration
        ))
        
        // 添加邮件配置
        val emailEnabled = settings.find { it.key == "email_enabled" }?.value?.toBoolean() ?: false
        val emailAddress = settings.find { it.key == "email_address" }?.value ?: ""
        val smtpHost = settings.find { it.key == "smtp_host" }?.value ?: "smtp.gmail.com"
        val smtpPort = settings.find { it.key == "smtp_port" }?.value?.toIntOrNull() ?: 587
        val useTls = settings.find { it.key == "smtp_tls" }?.value?.toBoolean() ?: true
        settingItems.add(SettingItem.AlarmEmailSettings(
            enabled = emailEnabled,
            emailAddress = emailAddress,
            smtpHost = smtpHost,
            smtpPort = smtpPort,
            useTls = useTls
        ))
        
        // 添加报警信息密码
        val secureBookSetting = settings.find { it.key == "secure_book" }
        val passwordText = if (secureBookSetting != null) {
            val secureBook = com.autodroid.guardiansdk.utils.SecureBookUtils.decodeSecureBook(secureBookSetting.value)
            secureBook.getAllCharacters().joinToString("")
        } else {
            com.autodroid.guardiansdk.data.model.SecureBook.getDefault().getAllCharacters().joinToString("")
        }
        settingItems.add(SettingItem.AlarmMessagePassword(password = passwordText))
        
        // 添加Ping设置
        settingItems.add(loadPingSettings(settings))
        
        // 添加隐秘界面设置
        val hiddenUIEnabled = settings.find { it.key == "hidden_secure_ui_enabled" }?.value?.toBoolean() ?: false
        val doorPassphrase = settings.find { it.key == "door_passphrase" }?.value ?: "小兔子乖乖把门开开"
        settingItems.add(SettingItem.HiddenSecureUI(
            enabled = hiddenUIEnabled,
            doorPassphrase = doorPassphrase
        ))
        
        // 添加测试模式设置
        val testModeEnabled = settings.find { it.key == "test_mode_enabled" }?.value?.toBoolean() ?: false
        settingItems.add(SettingItem.TestMode(isEnabled = testModeEnabled))
        
        return settingItems
    }
    
    /**
     * 加载所有设置项（保留用于兼容性）
     */
    fun loadSettingItems() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val guardians = contactDao.getContactsByType(ContactType.GUARDIAN)
                val settings = settingDao.getAllSettings()
                _settingItems.value = buildSettingItems(guardians, settings)
            } catch (e: Exception) {
                _settingItems.value = emptyList()
            }
        }
    }
    
    /**
     * 加载报警触发模式设置项的实际数据
     */
    private fun loadAlarmModeSettings(settings: List<Setting>): List<SettingItem> {
        val alarmModeSettings = mutableListOf<SettingItem>()
        
        // 音量键报警触发模式
        val volumeKeyEnabled = settings.find { it.key == "volume_key_alarm_enabled" }?.value?.toBoolean() ?: true
        val normalTime = settings.find { it.key == "volume_key_normal_hold_time" }?.value?.toIntOrNull() ?: 5
        val emergencyTime = settings.find { it.key == "volume_key_emergency_hold_time" }?.value?.toIntOrNull() ?: 10
        
        val volumeKeySetting = SettingItem.VolumeKeyAlarmMode(
            enabled = volumeKeyEnabled,
            normalHoldTime = normalTime,
            emergencyHoldTime = emergencyTime
        )
        alarmModeSettings.add(volumeKeySetting)
        
        // 浮动窗口报警触发模式
        val floatingWindowEnabled = settings.find { it.key == "floating_window_alarm_enabled" }?.value?.toBoolean() ?: true
        val floatingNormalTime = settings.find { it.key == "floating_window_normal_hold_time" }?.value?.toIntOrNull() ?: 5
        val floatingNormalOpacity = settings.find { it.key == "floating_window_normal_opacity" }?.value?.toIntOrNull() ?: 50
        val floatingEmergencyTime = settings.find { it.key == "floating_window_emergency_hold_time" }?.value?.toIntOrNull() ?: 10
        val floatingEmergencyOpacity = settings.find { it.key == "floating_window_emergency_opacity" }?.value?.toIntOrNull() ?: 80
        
        val floatingWindowSetting = SettingItem.FloatingWindowAlarmMode(
            enabled = floatingWindowEnabled,
            normalHoldTime = floatingNormalTime,
            normalOpacity = floatingNormalOpacity,
            emergencyHoldTime = floatingEmergencyTime,
            emergencyOpacity = floatingEmergencyOpacity
        )
        alarmModeSettings.add(floatingWindowSetting)
        
        // 摇动手机报警触发模式
        val shakeEnabled = settings.find { it.key == "shake_alarm_enabled" }?.value?.toBoolean() ?: true
        val normalSensitivity = settings.find { it.key == "shake_normal_sensitivity" }?.value?.toIntOrNull() ?: 5
        val emergencySensitivity = settings.find { it.key == "shake_emergency_sensitivity" }?.value?.toIntOrNull() ?: 8
        
        val shakePhoneSetting = SettingItem.ShakePhoneAlarmMode(
            enabled = shakeEnabled,
            normalSensitivity = normalSensitivity,
            emergencySensitivity = emergencySensitivity
        )
        alarmModeSettings.add(shakePhoneSetting)
        
        // 长时间未使用手机报警触发模式
        val inactivityEnabled = settings.find { it.key == "inactivity_alarm_enabled" }?.value?.toBoolean() ?: true
        val normalInactivityTime = settings.find { it.key == "inactivity_normal_time" }?.value?.toIntOrNull() ?: 60
        val emergencyInactivityTime = settings.find { it.key == "inactivity_emergency_time" }?.value?.toIntOrNull() ?: 30
        
        val inactivitySetting = SettingItem.InactivityAlarmMode(
            enabled = inactivityEnabled,
            normalInactivityTime = normalInactivityTime,
            emergencyInactivityTime = emergencyInactivityTime
        )
        alarmModeSettings.add(inactivitySetting)
        
        return alarmModeSettings
    }

    /**
     * 更新音量键报警触发模式设置
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
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 更新浮动窗口报警触发模式设置
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
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 更新摇动手机报警触发模式设置
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
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 更新长时间未使用手机报警触发模式设置
     */
    fun updateInactivityAlarmMode(enabled: Boolean, normalInactivityTime: Int, emergencyInactivityTime: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = listOf(
                    Setting(
                        key = "inactivity_alarm_enabled",
                        value = enabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "长时间未使用手机报警启用状态",
                        category = "alarm"
                    ),
                    Setting(
                        key = "inactivity_normal_time",
                        value = normalInactivityTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "长时间未使用手机普通报警时间（分钟）",
                        category = "alarm"
                    ),
                    Setting(
                        key = "inactivity_emergency_time",
                        value = emergencyInactivityTime.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "长时间未使用手机紧急报警时间（分钟）",
                        category = "alarm"
                    )
                )
                
                settingDao.insertOrUpdateAll(settings)
                
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
     * 保存监护人
     */
    fun saveGuardianContact(index: Int, name: String, phoneNumber: String, isPrimary: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contactDao = database.contactDao()
                
                // 创建或更新联系人
                val contact = Contact(
                    phoneNumber = phoneNumber,
                    name = name,
                    type = ContactType.GUARDIAN,
                    relationship = "监护人",
                    isPrimary = isPrimary,
                    orderIndex = index
                )
                
                contactDao.insertOrUpdate(contact)
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    /**
     * 获取监护人姓名
     */
    suspend fun getGuardianName(index: Int): String {
        val contactDao = database.contactDao()
        val guardian = contactDao.getGuardianByOrderIndex(index)
        return guardian?.name ?: ""
    }

    /**
     * 获取监护人电话
     */
    suspend fun getGuardianPhoneNumber(index: Int): String {
        val contactDao = database.contactDao()
        val guardian = contactDao.getGuardianByOrderIndex(index)
        return guardian?.phoneNumber ?: ""
    }

    /**
     * 获取监护人是否主要
     */
    suspend fun getGuardianPrimary(index: Int): Boolean {
        val contactDao = database.contactDao()
        val guardian = contactDao.getGuardianByOrderIndex(index)
        return guardian?.isPrimary ?: false
    }

    /**
     * 加载Ping设置
     */
    private fun loadPingSettings(settings: List<Setting>): SettingItem.PingSettings {
        val enabled = settings.find { it.key == "ping_enabled" }?.value?.toBoolean() ?: false
        val checkInterval = settings.find { it.key == "ping_check_interval" }?.value?.toIntOrNull() ?: 30
        val emailRetryCount = settings.find { it.key == "ping_email_retry_count" }?.value?.toIntOrNull() ?: 3
        val emailTimeout = settings.find { it.key == "ping_email_timeout" }?.value?.toIntOrNull() ?: 60
        val useSmsFallback = settings.find { it.key == "ping_use_sms_fallback" }?.value?.toBoolean() ?: true

        return SettingItem.PingSettings(
            enabled = enabled,
            checkInterval = checkInterval,
            emailRetryCount = emailRetryCount,
            emailTimeout = emailTimeout,
            useSmsFallback = useSmsFallback
        )
    }

    /**
     * 获取布尔设置
     */
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return settingDao.getSetting(key)?.value?.toBooleanStrictOrNull() ?: defaultValue
    }

    /**
     * 获取整数设置
     */
    suspend fun getInt(key: String, defaultValue: Int = 0): Int {
        return settingDao.getSetting(key)?.value?.toIntOrNull() ?: defaultValue
    }
    
    /**
     * 更新报警录音模式设置
     */
    fun updateAlarmRecordingMode(enabled: Boolean, duration: Int, segmentDuration: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = listOf(
                    Setting(
                        key = "recording_enabled",
                        value = enabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "报警录音启用状态",
                        category = "alarm"
                    ),
                    Setting(
                        key = "recording_duration",
                        value = duration.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "报警录音时长（分钟）",
                        category = "alarm"
                    ),
                    Setting(
                        key = "recording_segment_duration",
                        value = segmentDuration.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "报警录音分段时长（分钟）",
                        category = "alarm"
                    )
                )
                
                settingDao.insertOrUpdateAll(settings)
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 更新报警邮件设置
     */
    fun updateAlarmEmailSettings(enabled: Boolean, emailAddress: String, emailPassword: String, smtpHost: String, smtpPort: String, useTls: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = listOf(
                    Setting(
                        key = "email_enabled",
                        value = enabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "报警邮件启用状态",
                        category = "alarm"
                    ),
                    Setting(
                        key = "email_address",
                        value = emailAddress,
                        type = com.autodroid.guardiansdk.data.entity.SettingType.STRING,
                        description = "报警邮件地址",
                        category = "alarm"
                    ),
                    Setting(
                        key = "email_password",
                        value = emailPassword,
                        type = com.autodroid.guardiansdk.data.entity.SettingType.STRING,
                        description = "报警邮件密码（加密存储）",
                        category = "alarm"
                    ),
                    Setting(
                        key = "smtp_host",
                        value = smtpHost,
                        type = com.autodroid.guardiansdk.data.entity.SettingType.STRING,
                        description = "SMTP服务器地址",
                        category = "alarm"
                    ),
                    Setting(
                        key = "smtp_port",
                        value = smtpPort,
                        type = com.autodroid.guardiansdk.data.entity.SettingType.STRING,
                        description = "SMTP服务器端口",
                        category = "alarm"
                    ),
                    Setting(
                        key = "smtp_tls",
                        value = useTls.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "是否使用TLS加密",
                        category = "alarm"
                    )
                )
                
                settingDao.insertOrUpdateAll(settings)
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 更新隐秘界面设置
     */
    fun updateHiddenSecureUI(enabled: Boolean, doorPassphrase: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = listOf(
                    Setting(
                        key = "hidden_secure_ui_enabled",
                        value = enabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "隐秘界面启用状态",
                        category = "security"
                    ),
                    Setting(
                        key = "door_passphrase",
                        value = doorPassphrase,
                        type = com.autodroid.guardiansdk.data.entity.SettingType.STRING,
                        description = "隐秘界面开门密语",
                        category = "security"
                    )
                )
                
                settingDao.insertOrUpdateAll(settings)
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 更新测试模式设置
     */
    fun updateTestMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = listOf(
                    Setting(
                        key = "test_mode_enabled",
                        value = enabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "测试模式启用状态",
                        category = "test"
                    )
                )
                
                settingDao.insertOrUpdateAll(settings)
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 更新Ping设置
     */
    fun updatePingSettings(enabled: Boolean, checkInterval: Int, emailRetryCount: Int, emailTimeout: Int, useSmsFallback: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = listOf(
                    Setting(
                        key = "ping_enabled",
                        value = enabled.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "Ping响应启用状态",
                        category = "ping"
                    ),
                    Setting(
                        key = "ping_check_interval",
                        value = checkInterval.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "Ping检查间隔（分钟）",
                        category = "ping"
                    ),
                    Setting(
                        key = "ping_email_retry_count",
                        value = emailRetryCount.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "邮件重试次数",
                        category = "ping"
                    ),
                    Setting(
                        key = "ping_email_timeout",
                        value = emailTimeout.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.INT,
                        description = "邮件超时时间（分钟）",
                        category = "ping"
                    ),
                    Setting(
                        key = "ping_use_sms_fallback",
                        value = useSmsFallback.toString(),
                        type = com.autodroid.guardiansdk.data.entity.SettingType.BOOLEAN,
                        description = "邮件失败时使用短信",
                        category = "ping"
                    )
                )
                
                settingDao.insertOrUpdateAll(settings)
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
}