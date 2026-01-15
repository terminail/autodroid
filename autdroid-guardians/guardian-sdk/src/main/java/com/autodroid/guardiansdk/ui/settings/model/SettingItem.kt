package com.autodroid.guardiansdk.ui.settings.model

/**
 * 设置项数据模型 - 异构item设计
 */
sealed class SettingItem {
    
    /**
     * 紧急联系人设置项
     */
    data class EmergencyContacts(
        val contacts: List<EmergencyContact> = emptyList(),
        val isSynced: Boolean = false
    ) : SettingItem() {
        companion object
    }

    /**
     * 报警模式设置项
     */
    data class AlarmMode(
        val volumeKeyHoldTime: Int = 5, // 音量键长按时间（秒）
        val floatingWindowHoldTime: Int = 5, // 浮动窗口长按时间（秒）
        val shakeSensitivity: Int = 3 // 摇晃灵敏度
    ) : SettingItem() {
        companion object
    }

    /**
     * 位置密码本设置项
     */
    data class PasswordBook(
        val isEnabled: Boolean = true,
        val passwordMap: Map<String, String> = emptyMap(),
        val lastSyncTime: String = ""
    ) : SettingItem() {
        companion object
    }

    /**
     * 浮动窗口设置项
     */
    data class FloatingWindow(
        val size: Int = 10, // 窗口大小（像素）
        val opacity: Int = 10, // 透明度（%）
        val positionX: Int = 100, // 位置X坐标
        val positionY: Int = 100  // 位置Y坐标
    ) : SettingItem() {
        companion object
    }

    /**
     * 测试模式设置项
     */
    data class TestMode(
        val isEnabled: Boolean = false,
        val practiceCount: Int = 0,
        val lastPracticeTime: String = ""
    ) : SettingItem() {
        companion object
    }

    /**
     * 紧急擦除设置项
     */
    data class EmergencyWipe(
        val isConfirmed: Boolean = false,
        val wipeTriggerCount: Int = 0 // 触发擦除的次数
    ) : SettingItem() {
        companion object
    }
}

/**
 * 紧急联系人数据模型
 */
data class EmergencyContact(
    val id: Int,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isPrimary: Boolean = false
)