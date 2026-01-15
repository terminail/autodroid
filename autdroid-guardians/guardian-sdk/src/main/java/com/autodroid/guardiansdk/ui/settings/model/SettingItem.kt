package com.autodroid.guardiansdk.ui.settings.model

sealed class SettingItem {
    data class MyGuardian(
        val index: Int,
        val name: String = "",
        val phone: String = "",
        val isAdded: Boolean = false
    ) : SettingItem()

    data class VolumeKeyAlarmMode(
        val isEnabled: Boolean = true,
        val holdTime: Int = 5
    ) : SettingItem()

    data class FloatingWindowAlarmMode(
        val isEnabled: Boolean = true,
        val holdTime: Int = 5
    ) : SettingItem()

    data class ShakePhoneAlarmMode(
        val isEnabled: Boolean = true,
        val sensitivity: Int = 3
    ) : SettingItem()

    data class PasswordBook(
        val isEnabled: Boolean = true,
        val passwordMap: Map<String, String> = emptyMap(),
        val lastSyncTime: String = ""
    ) : SettingItem()

    data class FloatingWindow(
        val isEnabled: Boolean = true,
        val size: Int = 10,
        val opacity: Int = 10,
        val positionX: Int = 100,
        val positionY: Int = 100
    ) : SettingItem()

    data class WipeAlarmInfo(
        val isEnabled: Boolean = false
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
}
