package com.autodroid.guardiansdk.ui.settings.model

sealed class SettingItem {
    // 报警联系人1-5，每个最多5个报警联系人
    data class GuardianItem1(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false
    ) : SettingItem()

    data class GuardianItem2(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false
    ) : SettingItem()

    data class GuardianItem3(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false
    ) : SettingItem()

    data class GuardianItem4(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false
    ) : SettingItem()

    data class GuardianItem5(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false
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

    data class DoorPassphrase(
        val isEnabled: Boolean = false,
        val passphrase: String = "open_door"
    ) : SettingItem()

    data class TestMode(
        val isEnabled: Boolean = false,
        val practiceCount: Int = 0,
        val lastPracticeTime: String = "",
        val isSimulateNotification: Boolean = true
    ) : SettingItem()

    data class AlarmHistory(
        val time: String,
        val description: String
    ) : SettingItem()

    data class GuardianQueryHistory(
        val time: String,
        val guardianName: String,
        val queryContent: String,
        val responseContent: String
    ) : SettingItem()

    data class AlarmMessage(
        val normalMessage: String = "准备去麦当劳",
        val emergencyMessage: String = "准备去肯德基",
        val isEnabled: Boolean = true
    ) : SettingItem()
}
