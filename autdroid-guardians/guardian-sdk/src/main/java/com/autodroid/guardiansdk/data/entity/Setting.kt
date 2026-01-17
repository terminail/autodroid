package com.autodroid.guardiansdk.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * 应用设置实体
 * 替代传统的SharedPreferences，提供更好的扩展性和类型安全
 */
@Entity(tableName = "settings")
data class Setting(
    @PrimaryKey
    val key: String,                    // 设置项键名
    val value: String,                  // 设置项值（字符串形式）
    val type: SettingType = SettingType.STRING, // 设置项类型
    val description: String = "",       // 设置项描述
    val category: String = "general",   // 设置项分类
    val isEncrypted: Boolean = false,   // 是否加密存储
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 设置项类型枚举
 */
enum class SettingType {
    STRING,     // 字符串类型
    INT,        // 整数类型
    LONG,       // 长整数类型
    FLOAT,      // 浮点数类型
    BOOLEAN,    // 布尔类型
    JSON        // JSON对象类型
}

/**
 * 设置项键名常量
 */
object SettingKeys {
    // 报警触发模式相关
    const val ALARM_VOLUME_KEY_HOLD_TIME = "alarm_volume_key_hold_time"
    const val ALARM_FLOATING_WINDOW_HOLD_TIME = "alarm_floating_window_hold_time"
    const val ALARM_SHAKE_SENSITIVITY = "alarm_shake_sensitivity"
    
    // 浮动窗口相关
    const val FLOATING_WINDOW_SIZE = "floating_window_size"
    const val FLOATING_WINDOW_OPACITY = "floating_window_opacity"
    const val FLOATING_WINDOW_POSITION_X = "floating_window_position_x"
    const val FLOATING_WINDOW_POSITION_Y = "floating_window_position_y"
    
    // 测试模式相关
    const val TEST_MODE_ENABLED = "test_mode_enabled"
    const val TEST_MODE_PRACTICE_COUNT = "test_mode_practice_count"
    const val TEST_MODE_LAST_PRACTICE_TIME = "test_mode_last_practice_time"
    
    // 监护人相关
    const val GUARDIAN_CONTACT_COUNT = "guardian_contact_count"
    const val GUARDIAN_CONTACT_LAST_SYNC = "guardian_contact_last_sync"
    
    // 密码本相关
    const val PASSWORD_BOOK_ENABLED = "password_book_enabled"
    const val PASSWORD_BOOK_LAST_SYNC = "password_book_last_sync"
    const val PASSWORD_BOOK_DATA = "password_book_data"
    
    // 应用设置相关
    const val APP_NAME = "app_name"
    const val APP_ICON_PATH = "app_icon_path"
    const val APP_VERSION = "app_version"
    const val APP_LAST_UPDATE = "app_last_update"
    
    // 安全相关
    const val LAST_ALARM_TIME = "last_alarm_time"
    const val TOTAL_ALARM_COUNT = "total_alarm_count"
}

/**
 * 设置项类型转换器
 */
class SettingTypeConverter {
    @TypeConverter
    fun fromSettingType(type: SettingType): String = type.name
    
    @TypeConverter
    fun toSettingType(name: String): SettingType = SettingType.valueOf(name)
}