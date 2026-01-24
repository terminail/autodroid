package com.autodroid.teachitback.repository

import com.autodroid.teachitback.database.SettingDao
import com.autodroid.teachitback.model.SettingEntity
import com.autodroid.teachitback.ui.adapter.SettingsItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 设置项仓库
 * 负责SettingsItem的序列化、反序列化和数据库操作
 */
class SettingsRepository(private val settingDao: SettingDao) {
    
    private val gson = Gson()
    
    /**
     * 获取所有设置项
     */
    fun getAllSettings(): Flow<List<SettingsItem>> {
        return settingDao.getAllSettings().map { entities ->
            entities.mapNotNull { entity ->
                deserializeSettingItem(entity.value)
            }
        }
    }
    
    /**
     * 根据key获取设置项
     */
    fun getSettingByKey(key: String): Flow<SettingsItem?> {
        return settingDao.getSettingByKey(key).map { entity ->
            entity?.let { deserializeSettingItem(it.value) }
        }
    }
    
    /**
     * 同步获取设置项
     */
    suspend fun getSettingByKeySync(key: String): SettingsItem? {
        val entity = settingDao.getSettingByKeySync(key)
        return entity?.let { deserializeSettingItem(it.value) }
    }
    
    /**
     * 保存设置项
     */
    suspend fun saveSetting(key: String, item: SettingsItem) {
        val json = serializeSettingItem(item)
        val entity = SettingEntity(
            key = key,
            value = json,
            lastUpdated = System.currentTimeMillis()
        )
        settingDao.insertSetting(entity)
    }
    
    /**
     * 删除设置项
     */
    suspend fun deleteSetting(key: String) {
        settingDao.deleteSettingByKey(key)
    }
    
    /**
     * 删除所有设置项
     */
    suspend fun deleteAllSettings() {
        settingDao.deleteAllSettings()
    }
    
    /**
     * 序列化SettingsItem为JSON字符串
     */
    private fun serializeSettingItem(item: SettingsItem): String {
        val type = when (item) {
            is SettingsItem.SectionHeaderItem -> "SectionHeaderItem"
            is SettingsItem.DarkModeSwitchItem -> "DarkModeSwitchItem"
            is SettingsItem.AutoSaveSwitchItem -> "AutoSaveSwitchItem"
            is SettingsItem.LanguageSettingItem -> "LanguageSettingItem"
            is SettingsItem.BackupDataItem -> "BackupDataItem"
            is SettingsItem.RestoreDataItem -> "RestoreDataItem"
            is SettingsItem.ClearAllDataButtonItem -> "ClearAllDataButtonItem"
            is SettingsItem.VersionInfoItem -> "VersionInfoItem"
            is SettingsItem.HelpAndFeedbackItem -> "HelpAndFeedbackItem"
            is SettingsItem.DoubaoAIServiceItem -> "DoubaoAIServiceItem"
            is SettingsItem.DeepSeekAIServiceItem -> "DeepSeekAIServiceItem"
            is SettingsItem.MinimaxAIServiceItem -> "MinimaxAIServiceItem"
            is SettingsItem.KimiAIServiceItem -> "KimiAIServiceItem"
            is SettingsItem.OpenAIServiceItem -> "OpenAIServiceItem"
            is SettingsItem.ErnieAIServiceItem -> "ErnieAIServiceItem"
            is SettingsItem.QwenAIServiceItem -> "QwenAIServiceItem"
            is SettingsItem.ZhipuAIServiceItem -> "ZhipuAIServiceItem"
            is SettingsItem.SparkAIServiceItem -> "SparkAIServiceItem"
            is SettingsItem.HunyuanAIServiceItem -> "HunyuanAIServiceItem"
            is SettingsItem.BaichuanAIServiceItem -> "BaichuanAIServiceItem"
            is SettingsItem.LingyiAIServiceItem -> "LingyiAIServiceItem"
            is SettingsItem.JieyueAIServiceItem -> "JieyueAIServiceItem"
            is SettingsItem.TencentCloudApiKeyItem -> "TencentCloudApiKeyItem"
        }
        
        val itemJson = gson.toJson(item)
        return """{"type":"$type","data":$itemJson}"""
    }
    
    /**
     * 反序列化JSON字符串为SettingsItem
     */
    private fun deserializeSettingItem(json: String): SettingsItem? {
        return try {
            val wrapperType = object : TypeToken<Map<String, Any>>() {}.type
            val wrapper: Map<String, Any> = gson.fromJson(json, wrapperType)
            
            val type = wrapper["type"] as? String ?: return null
            val dataJson = gson.toJson(wrapper["data"])
            
            when (type) {
                "SectionHeaderItem" -> gson.fromJson(dataJson, SettingsItem.SectionHeaderItem::class.java)
                "DarkModeSwitchItem" -> gson.fromJson(dataJson, SettingsItem.DarkModeSwitchItem::class.java)
                "AutoSaveSwitchItem" -> gson.fromJson(dataJson, SettingsItem.AutoSaveSwitchItem::class.java)
                "LanguageSettingItem" -> gson.fromJson(dataJson, SettingsItem.LanguageSettingItem::class.java)
                "BackupDataItem" -> gson.fromJson(dataJson, SettingsItem.BackupDataItem::class.java)
                "RestoreDataItem" -> gson.fromJson(dataJson, SettingsItem.RestoreDataItem::class.java)
                "ClearAllDataButtonItem" -> gson.fromJson(dataJson, SettingsItem.ClearAllDataButtonItem::class.java)
                "VersionInfoItem" -> gson.fromJson(dataJson, SettingsItem.VersionInfoItem::class.java)
                "HelpAndFeedbackItem" -> gson.fromJson(dataJson, SettingsItem.HelpAndFeedbackItem::class.java)
                "DoubaoAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.DoubaoAIServiceItem::class.java)
                "DeepSeekAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.DeepSeekAIServiceItem::class.java)
                "MinimaxAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.MinimaxAIServiceItem::class.java)
                "KimiAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.KimiAIServiceItem::class.java)
                "OpenAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.OpenAIServiceItem::class.java)
                "ErnieAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.ErnieAIServiceItem::class.java)
                "QwenAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.QwenAIServiceItem::class.java)
                "ZhipuAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.ZhipuAIServiceItem::class.java)
                "SparkAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.SparkAIServiceItem::class.java)
                "HunyuanAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.HunyuanAIServiceItem::class.java)
                "BaichuanAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.BaichuanAIServiceItem::class.java)
                "LingyiAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.LingyiAIServiceItem::class.java)
                "JieyueAIServiceItem" -> gson.fromJson(dataJson, SettingsItem.JieyueAIServiceItem::class.java)
                "TencentCloudApiKeyItem" -> gson.fromJson(dataJson, SettingsItem.TencentCloudApiKeyItem::class.java)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
