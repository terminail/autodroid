package com.autodroid.guardiansdk.ui.settings.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class SettingItem : Parcelable {
    // 监护人1-5，每个最多5个监护人
    @Parcelize
    data class GuardianItem1(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false,
        val isPlaceholder: Boolean = false
    ) : SettingItem()

    @Parcelize
    data class GuardianItem2(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false,
        val isPlaceholder: Boolean = false
    ) : SettingItem()

    @Parcelize
    data class GuardianItem3(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false,
        val isPlaceholder: Boolean = false
    ) : SettingItem()

    @Parcelize
    data class GuardianItem4(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false,
        val isPlaceholder: Boolean = false
    ) : SettingItem()

    @Parcelize
    data class GuardianItem5(
        val phoneNumber: String = "",
        val name: String = "",
        val isPrimary: Boolean = false,
        val isPlaceholder: Boolean = false
    ) : SettingItem()

    @Parcelize
    data class VolumeKeyAlarmMode(
        val enabled: Boolean = true,
        val normalHoldTime: Int = 5,
        val emergencyHoldTime: Int = 10
    ) : SettingItem()

    @Parcelize
    data class FloatingWindowAlarmMode(
        val enabled: Boolean = true,
        val normalHoldTime: Int = 5,
        val normalOpacity: Int = 50,
        val emergencyHoldTime: Int = 10,
        val emergencyOpacity: Int = 80
    ) : SettingItem()

    @Parcelize
    data class ShakePhoneAlarmMode(
        val enabled: Boolean = true,
        val normalSensitivity: Int = 5,
        val emergencySensitivity: Int = 8
    ) : SettingItem()

    @Parcelize
    data class InactivityAlarmMode(
        val enabled: Boolean = true,
        val normalInactivityTime: Int = 60,
        val emergencyInactivityTime: Int = 30
    ) : SettingItem()

    @Parcelize
    data class AlarmMessagePassword(
        val password: String = ""
    ) : SettingItem()

    @Parcelize
    data class AlarmRecordingMode(
        val enabled: Boolean = true,
        val duration: Int = 5,
        val segmentDuration: Int = 2
    ) : SettingItem()

    @Parcelize
    data class AlarmEmailSettings(
        val enabled: Boolean = false,
        val emailAddress: String = "",
        val smtpHost: String = "smtp.gmail.com",
        val smtpPort: Int = 587,
        val useTls: Boolean = true
    ) : SettingItem()

    @Parcelize
    data class HiddenSecureUI(
        val enabled: Boolean = false,
        val doorPassphrase: String = "小兔子乖乖把门开开"
    ) : SettingItem()

    @Parcelize
    data class TestMode(
        val isEnabled: Boolean = false,
        val practiceCount: Int = 0,
        val lastPracticeTime: String = "",
        val isSimulateNotification: Boolean = true
    ) : SettingItem()

    @Parcelize
    data class PingSettings(
        val enabled: Boolean = false,
        val checkInterval: Int = 30,
        val emailRetryCount: Int = 3,
        val emailTimeout: Int = 60,
        val useSmsFallback: Boolean = true
    ) : SettingItem()
}
