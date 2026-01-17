package com.autodroid.guardiansdk.ui.settings.model

sealed class SettingItem {
    // 监护人1-5，每个最多5个监护人
    data class GuardianItem1(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false,
        val isPlaceholder: Boolean = false
    ) : SettingItem()

    data class GuardianItem2(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false,
        val isPlaceholder: Boolean = false
    ) : SettingItem()

    data class GuardianItem3(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false,
        val isPlaceholder: Boolean = false
    ) : SettingItem()

    data class GuardianItem4(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false,
        val isPlaceholder: Boolean = false
    ) : SettingItem()

    data class GuardianItem5(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false,
        val isPlaceholder: Boolean = false
    ) : SettingItem()

    data class VolumeKeyAlarmMode(
        val enabled: Boolean = true,
        val normalHoldTime: Int = 5,
        val emergencyHoldTime: Int = 10
    ) : SettingItem()

    data class FloatingWindowAlarmMode(
        val enabled: Boolean = true,
        val normalHoldTime: Int = 5,
        val normalOpacity: Int = 50,
        val emergencyHoldTime: Int = 10,
        val emergencyOpacity: Int = 80
    ) : SettingItem()

    data class ShakePhoneAlarmMode(
        val enabled: Boolean = true,
        val normalSensitivity: Int = 5,
        val emergencySensitivity: Int = 8
    ) : SettingItem()

    data class InactivityAlarmMode(
        val enabled: Boolean = true,
        val normalInactivityTime: Int = 60,
        val emergencyInactivityTime: Int = 30
    ) : SettingItem()

    data class AlarmMessagePassword(
        val password: String = ""
    ) : SettingItem()

    data class AlarmRecordingMode(
        val enabled: Boolean = true,
        val duration: Int = 5,
        val segmentDuration: Int = 2
    ) : SettingItem()

    data class AlarmEmailSettings(
        val enabled: Boolean = false,
        val emailAddress: String = "",
        val smtpHost: String = "smtp.gmail.com",
        val smtpPort: Int = 587,
        val useTls: Boolean = true
    ) : SettingItem()

    data class HiddenSecureUI(
        val enabled: Boolean = false,
        val doorPassphrase: String = "小兔子乖乖把门开开"
    ) : SettingItem()

    data class TestMode(
        val isEnabled: Boolean = false,
        val practiceCount: Int = 0,
        val lastPracticeTime: String = "",
        val isSimulateNotification: Boolean = true
    ) : SettingItem()
}
