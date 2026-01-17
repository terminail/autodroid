package com.autodroid.guardiansdk.ui.settings.utils

import android.os.Parcelable
import android.util.Base64
import com.autodroid.guardiansdk.ui.settings.model.SettingItem
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 设置项工具类
 * 用于序列化和反序列化SettingItem
 */
object SettingItemUtils {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * 序列化SettingItem为JSON字符串
     */
    fun serializeSettingItem(settingItem: SettingItem): String {
        return when (settingItem) {
            is SettingItem.GuardianItem1 -> json.encodeToString(settingItem)
            is SettingItem.GuardianItem2 -> json.encodeToString(settingItem)
            is SettingItem.GuardianItem3 -> json.encodeToString(settingItem)
            is SettingItem.GuardianItem4 -> json.encodeToString(settingItem)
            is SettingItem.GuardianItem5 -> json.encodeToString(settingItem)
            is SettingItem.VolumeKeyAlarmMode -> json.encodeToString(settingItem)
            is SettingItem.FloatingWindowAlarmMode -> json.encodeToString(settingItem)
            is SettingItem.ShakePhoneAlarmMode -> json.encodeToString(settingItem)
            is SettingItem.InactivityAlarmMode -> json.encodeToString(settingItem)
            is SettingItem.AlarmMessagePassword -> json.encodeToString(settingItem)
            is SettingItem.AlarmRecordingMode -> json.encodeToString(settingItem)
            is SettingItem.AlarmEmailSettings -> json.encodeToString(settingItem)
            is SettingItem.HiddenSecureUI -> json.encodeToString(settingItem)
            is SettingItem.TestMode -> json.encodeToString(settingItem)
            is SettingItem.PingSettings -> json.encodeToString(settingItem)
        }
    }
    
    /**
     * 反序列化JSON字符串为SettingItem
     */
    fun deserializeSettingItem(jsonString: String, type: String): SettingItem? {
        return try {
            when (type) {
                "GuardianItem1" -> json.decodeFromString<SettingItem.GuardianItem1>(jsonString)
                "GuardianItem2" -> json.decodeFromString<SettingItem.GuardianItem2>(jsonString)
                "GuardianItem3" -> json.decodeFromString<SettingItem.GuardianItem3>(jsonString)
                "GuardianItem4" -> json.decodeFromString<SettingItem.GuardianItem4>(jsonString)
                "GuardianItem5" -> json.decodeFromString<SettingItem.GuardianItem5>(jsonString)
                "VolumeKeyAlarmMode" -> json.decodeFromString<SettingItem.VolumeKeyAlarmMode>(jsonString)
                "FloatingWindowAlarmMode" -> json.decodeFromString<SettingItem.FloatingWindowAlarmMode>(jsonString)
                "ShakePhoneAlarmMode" -> json.decodeFromString<SettingItem.ShakePhoneAlarmMode>(jsonString)
                "InactivityAlarmMode" -> json.decodeFromString<SettingItem.InactivityAlarmMode>(jsonString)
                "AlarmMessagePassword" -> json.decodeFromString<SettingItem.AlarmMessagePassword>(jsonString)
                "AlarmRecordingMode" -> json.decodeFromString<SettingItem.AlarmRecordingMode>(jsonString)
                "AlarmEmailSettings" -> json.decodeFromString<SettingItem.AlarmEmailSettings>(jsonString)
                "HiddenSecureUI" -> json.decodeFromString<SettingItem.HiddenSecureUI>(jsonString)
                "TestMode" -> json.decodeFromString<SettingItem.TestMode>(jsonString)
                "PingSettings" -> json.decodeFromString<SettingItem.PingSettings>(jsonString)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取SettingItem的类型名称
     */
    fun getSettingItemType(settingItem: SettingItem): String {
        return when (settingItem) {
            is SettingItem.GuardianItem1 -> "GuardianItem1"
            is SettingItem.GuardianItem2 -> "GuardianItem2"
            is SettingItem.GuardianItem3 -> "GuardianItem3"
            is SettingItem.GuardianItem4 -> "GuardianItem4"
            is SettingItem.GuardianItem5 -> "GuardianItem5"
            is SettingItem.VolumeKeyAlarmMode -> "VolumeKeyAlarmMode"
            is SettingItem.FloatingWindowAlarmMode -> "FloatingWindowAlarmMode"
            is SettingItem.ShakePhoneAlarmMode -> "ShakePhoneAlarmMode"
            is SettingItem.InactivityAlarmMode -> "InactivityAlarmMode"
            is SettingItem.AlarmMessagePassword -> "AlarmMessagePassword"
            is SettingItem.AlarmRecordingMode -> "AlarmRecordingMode"
            is SettingItem.AlarmEmailSettings -> "AlarmEmailSettings"
            is SettingItem.HiddenSecureUI -> "HiddenSecureUI"
            is SettingItem.TestMode -> "TestMode"
            is SettingItem.PingSettings -> "PingSettings"
        }
    }
    
    /**
     * 序列化整个设置项列表为JSON字符串
     */
    fun serializeSettingItems(settingItems: List<SettingItem>): String {
        val serializedItems = settingItems.map { settingItem ->
            SettingItemWrapper(
                type = getSettingItemType(settingItem),
                data = serializeSettingItem(settingItem)
            )
        }
        return json.encodeToString(serializedItems)
    }
    
    /**
     * 反序列化JSON字符串为设置项列表
     */
    fun deserializeSettingItems(jsonString: String): List<SettingItem> {
        return try {
            val wrappers = json.decodeFromString<List<SettingItemWrapper>>(jsonString)
            wrappers.mapNotNull { wrapper ->
                deserializeSettingItem(wrapper.data, wrapper.type)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 设置项包装器
     */
    @Serializable
    private data class SettingItemWrapper(
        val type: String,
        val data: String
    )
}