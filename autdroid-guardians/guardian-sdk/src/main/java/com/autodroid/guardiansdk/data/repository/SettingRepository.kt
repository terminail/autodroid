package com.autodroid.guardiansdk.data.repository

import com.autodroid.guardiansdk.data.dao.SettingDao
import com.autodroid.guardiansdk.data.entity.Setting
import com.autodroid.guardiansdk.data.entity.SettingType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 设置项仓库
 * 提供类似SharedPreferences的便捷接口，但基于数据库存储
 */
class SettingRepository(private val settingDao: SettingDao) {

    /**
     * 获取字符串设置
     */
    suspend fun getString(key: String, defaultValue: String = ""): String {
        return settingDao.getSetting(key)?.value ?: defaultValue
    }

    /**
     * 设置字符串设置
     */
    suspend fun putString(key: String, value: String, description: String = "", category: String = "general") {
        val setting = Setting(
            key = key,
            value = value,
            type = SettingType.STRING,
            description = description,
            category = category
        )
        settingDao.insertOrUpdate(setting)
    }

    /**
     * 获取整数设置
     */
    suspend fun getInt(key: String, defaultValue: Int = 0): Int {
        return settingDao.getSetting(key)?.value?.toIntOrNull() ?: defaultValue
    }

    /**
     * 设置整数设置
     */
    suspend fun putInt(key: String, value: Int, description: String = "", category: String = "general") {
        val setting = Setting(
            key = key,
            value = value.toString(),
            type = SettingType.INT,
            description = description,
            category = category
        )
        settingDao.insertOrUpdate(setting)
    }

    /**
     * 获取长整数设置
     */
    suspend fun getLong(key: String, defaultValue: Long = 0L): Long {
        return settingDao.getSetting(key)?.value?.toLongOrNull() ?: defaultValue
    }

    /**
     * 设置长整数设置
     */
    suspend fun putLong(key: String, value: Long, description: String = "", category: String = "general") {
        val setting = Setting(
            key = key,
            value = value.toString(),
            type = SettingType.LONG,
            description = description,
            category = category
        )
        settingDao.insertOrUpdate(setting)
    }

    /**
     * 获取布尔设置
     */
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return settingDao.getSetting(key)?.value?.toBooleanStrictOrNull() ?: defaultValue
    }

    /**
     * 设置布尔设置
     */
    suspend fun putBoolean(key: String, value: Boolean, description: String = "", category: String = "general") {
        val setting = Setting(
            key = key,
            value = value.toString(),
            type = SettingType.BOOLEAN,
            description = description,
            category = category
        )
        settingDao.insertOrUpdate(setting)
    }

    /**
     * 获取浮点数设置
     */
    suspend fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return settingDao.getSetting(key)?.value?.toFloatOrNull() ?: defaultValue
    }

    /**
     * 设置浮点数设置
     */
    suspend fun putFloat(key: String, value: Float, description: String = "", category: String = "general") {
        val setting = Setting(
            key = key,
            value = value.toString(),
            type = SettingType.FLOAT,
            description = description,
            category = category
        )
        settingDao.insertOrUpdate(setting)
    }

    /**
     * 删除设置项
     */
    suspend fun remove(key: String) {
        settingDao.deleteSetting(key)
    }

    /**
     * 检查设置项是否存在
     */
    suspend fun contains(key: String): Boolean {
        return settingDao.getSetting(key) != null
    }

    /**
     * 清空所有设置项
     */
    suspend fun clear() {
        settingDao.deleteAllSettings()
    }

    /**
     * 获取所有设置项
     */
    suspend fun getAllSettings(): List<Setting> {
        return settingDao.getAllSettings()
    }

    /**
     * 根据分类获取设置项
     */
    suspend fun getSettingsByCategory(category: String): List<Setting> {
        return settingDao.getSettingsByCategory(category)
    }

    /**
     * 监听设置项变化
     */
    fun observeSetting(key: String): Flow<String?> {
        return settingDao.observeSetting(key).map { it?.value }
    }

    /**
     * 监听分类设置项变化
     */
    fun observeSettingsByCategory(category: String): Flow<List<Setting>> {
        return settingDao.observeSettingsByCategory(category)
    }

    /**
     * 初始化默认设置项
     */
    suspend fun initializeDefaultSettings() {
        // 报警模式默认设置
        if (!contains(SettingKeys.ALARM_VOLUME_KEY_HOLD_TIME)) {
            putInt(SettingKeys.ALARM_VOLUME_KEY_HOLD_TIME, 5, "音量键长按报警时间（秒）", "alarm")
        }
        if (!contains(SettingKeys.ALARM_FLOATING_WINDOW_HOLD_TIME)) {
            putInt(SettingKeys.ALARM_FLOATING_WINDOW_HOLD_TIME, 5, "浮动窗口长按报警时间（秒）", "alarm")
        }
        if (!contains(SettingKeys.ALARM_SHAKE_SENSITIVITY)) {
            putInt(SettingKeys.ALARM_SHAKE_SENSITIVITY, 3, "摇晃报警灵敏度", "alarm")
        }

        // 浮动窗口默认设置
        if (!contains(SettingKeys.FLOATING_WINDOW_SIZE)) {
            putInt(SettingKeys.FLOATING_WINDOW_SIZE, 10, "浮动窗口大小（像素）", "floating_window")
        }
        if (!contains(SettingKeys.FLOATING_WINDOW_OPACITY)) {
            putInt(SettingKeys.FLOATING_WINDOW_OPACITY, 10, "浮动窗口透明度（%）", "floating_window")
        }
        if (!contains(SettingKeys.FLOATING_WINDOW_POSITION_X)) {
            putInt(SettingKeys.FLOATING_WINDOW_POSITION_X, 100, "浮动窗口X坐标", "floating_window")
        }
        if (!contains(SettingKeys.FLOATING_WINDOW_POSITION_Y)) {
            putInt(SettingKeys.FLOATING_WINDOW_POSITION_Y, 100, "浮动窗口Y坐标", "floating_window")
        }

        // 测试模式默认设置
        if (!contains(SettingKeys.TEST_MODE_ENABLED)) {
            putBoolean(SettingKeys.TEST_MODE_ENABLED, false, "测试模式是否启用", "test_mode")
        }
        if (!contains(SettingKeys.TEST_MODE_PRACTICE_COUNT)) {
            putInt(SettingKeys.TEST_MODE_PRACTICE_COUNT, 0, "测试模式练习次数", "test_mode")
        }

        // 密码本默认设置
        if (!contains(SettingKeys.PASSWORD_BOOK_ENABLED)) {
            putBoolean(SettingKeys.PASSWORD_BOOK_ENABLED, true, "位置密码本是否启用", "password_book")
        }

        // 应用设置默认值
        if (!contains(SettingKeys.APP_NAME)) {
            putString(SettingKeys.APP_NAME, "记事本守卫", "应用显示名称", "app")
        }
        if (!contains(SettingKeys.APP_VERSION)) {
            putString(SettingKeys.APP_VERSION, "v1.0.0", "应用版本号", "app")
        }
    }
}