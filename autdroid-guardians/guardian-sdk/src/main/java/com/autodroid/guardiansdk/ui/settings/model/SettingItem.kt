package com.autodroid.guardiansdk.ui.settings.model

/**
 * 设置项数据模型 - 异构item设计
 */
sealed class SettingItem {

    /**
     * 我的监护人设置项（最多5个）
     */
    data class MyGuardian(
        val index: Int, // 1-5
        val name: String = "",
        val phone: String = "",
        val isAdded: Boolean = false
    ) : SettingItem() {
        companion object
    }

    /**
     * 音量键报警模式设置项
     */
    data class VolumeKeyAlarmMode(
        val isEnabled: Boolean = true,
        val holdTime: Int = 5 // 音量键长按时间（秒）
    ) : SettingItem() {
        companion object
    }

    /**
     * 浮动窗口报警模式设置项
     */
    data class FloatingWindowAlarmMode(
        val isEnabled: Boolean = true,
        val holdTime: Int = 5 // 浮动窗口长按时间（秒）
    ) : SettingItem() {
        companion object
    }

    /**
     * 摇动手机报警模式设置项
     */
    data class ShakePhoneAlarmMode(
        val isEnabled: Boolean = true,
        val sensitivity: Int = 3 // 摇晃灵敏度
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
        val isEnabled: Boolean = true,
        val size: Int = 10, // 窗口大小（像素）
        val opacity: Int = 10, // 透明度（%）
        val positionX: Int = 100, // 位置X坐标
        val positionY: Int = 100  // 位置Y坐标
    ) : SettingItem() {
        companion object
    }

    /**
     * 擦除报警信息设置项
     */
    data class WipeAlarmInfo(
        val isEnabled: Boolean = false
    ) : SettingItem() {
        companion object
    }

    /**
     * 开门密语设置项
     */
    data class DoorPassphrase(
        val isEnabled: Boolean = false,
        val passphrase: String = "open_door"
    ) : SettingItem() {
        companion object
    }

    /**
     * 测试模式设置项
     */
    data class TestMode(
        val isEnabled: Boolean = false,
        val practiceCount: Int = 0,
        val lastPracticeTime: String = "",
        val isSimulateNotification: Boolean = true // 是否模拟发送通知
    ) : SettingItem() {
        companion object
    }

    /**
     * 报警历史记录项
     */
    data class AlarmHistory(
        val time: String,
        val description: String
    ) : SettingItem() {
        companion object
    }

    /**
     * 监护人查询历史记录项
     */
    data class GuardianQueryHistory(
        val time: String,
        val guardianName: String,
        val queryContent: String,
        val responseContent: String
    ) : SettingItem() {
        companion object
    }
}

/**
 * 紧急联系人数据模型 (保留原有用于兼容性)
 */
data class EmergencyContact(
    val id: Int,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isPrimary: Boolean = false
)