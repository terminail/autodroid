package com.autodroid.guardiansdk.ui.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.autodroid.guardiansdk.data.database.GuardianDatabase
import com.autodroid.guardiansdk.data.dao.ContactDao
import com.autodroid.guardiansdk.data.dao.SettingDao
import com.autodroid.guardiansdk.data.entity.Setting
import com.autodroid.guardiansdk.data.entity.SettingType
import com.autodroid.guardiansdk.ui.settings.model.SettingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SettingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Mock
    private lateinit var database: GuardianDatabase

    @Mock
    private lateinit var contactDao: ContactDao

    @Mock
    private lateinit var settingDao: SettingDao

    private lateinit var viewModel: SettingViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(database.contactDao()).thenReturn(contactDao)
        whenever(database.settingDao()).thenReturn(settingDao)
        whenever(contactDao.observeContactsByType(org.mockito.kotlin.any())).thenReturn(emptyFlow())
        whenever(settingDao.observeAllSettings()).thenReturn(emptyFlow())

        viewModel = SettingViewModel(database)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testBuildSettingItemsWithEmptySettingsReturnsDefaultValues() = runTest {
        val guardians = emptyList<com.autodroid.guardiansdk.data.entity.Contact>()
        val settings = emptyList<Setting>()

        val result = viewModel.buildSettingItems(guardians, settings)

        assert(result.isNotEmpty())
        assert(result.size >= 10)
    }

    @Test
    fun testBuildSettingItemsWithVolumeKeySettingsReturnsCorrectItem() = runTest {
        val guardians = emptyList<com.autodroid.guardiansdk.data.entity.Contact>()
        val settings = listOf(
            Setting("volume_key_alarm_enabled", "true", SettingType.BOOLEAN, "", "alarm"),
            Setting("volume_key_normal_hold_time", "5", SettingType.INT, "", "alarm"),
            Setting("volume_key_emergency_hold_time", "10", SettingType.INT, "", "alarm")
        )

        val result = viewModel.buildSettingItems(guardians, settings)

        val volumeKeyItem = result.filterIsInstance<SettingItem.VolumeKeyAlarmMode>().firstOrNull()
        assert(volumeKeyItem != null)
        assert(volumeKeyItem!!.enabled)
        assert(volumeKeyItem.normalHoldTime == 5)
        assert(volumeKeyItem.emergencyHoldTime == 10)
    }

    @Test
    fun testBuildSettingItemsWithFloatingWindowSettingsReturnsCorrectItem() = runTest {
        val guardians = emptyList<com.autodroid.guardiansdk.data.entity.Contact>()
        val settings = listOf(
            Setting("floating_window_alarm_enabled", "true", SettingType.BOOLEAN, "", "alarm"),
            Setting("floating_window_normal_hold_time", "5", SettingType.INT, "", "alarm"),
            Setting("floating_window_normal_opacity", "50", SettingType.INT, "", "alarm"),
            Setting("floating_window_emergency_hold_time", "10", SettingType.INT, "", "alarm"),
            Setting("floating_window_emergency_opacity", "80", SettingType.INT, "", "alarm")
        )

        val result = viewModel.buildSettingItems(guardians, settings)

        val floatingWindowItem = result.filterIsInstance<SettingItem.FloatingWindowAlarmMode>().firstOrNull()
        assert(floatingWindowItem != null)
        assert(floatingWindowItem!!.enabled)
        assert(floatingWindowItem.normalHoldTime == 5)
        assert(floatingWindowItem.normalOpacity == 50)
        assert(floatingWindowItem.emergencyHoldTime == 10)
        assert(floatingWindowItem.emergencyOpacity == 80)
    }

    @Test
    fun testBuildSettingItemsWithShakePhoneSettingsReturnsCorrectItem() = runTest {
        val guardians = emptyList<com.autodroid.guardiansdk.data.entity.Contact>()
        val settings = listOf(
            Setting("shake_alarm_enabled", "true", SettingType.BOOLEAN, "", "alarm"),
            Setting("shake_normal_sensitivity", "5", SettingType.INT, "", "alarm"),
            Setting("shake_emergency_sensitivity", "8", SettingType.INT, "", "alarm")
        )

        val result = viewModel.buildSettingItems(guardians, settings)

        val shakePhoneItem = result.filterIsInstance<SettingItem.ShakePhoneAlarmMode>().firstOrNull()
        assert(shakePhoneItem != null)
        assert(shakePhoneItem!!.enabled)
        assert(shakePhoneItem.normalSensitivity == 5)
        assert(shakePhoneItem.emergencySensitivity == 8)
    }

    @Test
    fun testBuildSettingItemsWithInactivitySettingsReturnsCorrectItem() = runTest {
        val guardians = emptyList<com.autodroid.guardiansdk.data.entity.Contact>()
        val settings = listOf(
            Setting("inactivity_alarm_enabled", "true", SettingType.BOOLEAN, "", "alarm"),
            Setting("inactivity_normal_time", "60", SettingType.INT, "", "alarm"),
            Setting("inactivity_emergency_time", "30", SettingType.INT, "", "alarm")
        )

        val result = viewModel.buildSettingItems(guardians, settings)

        val inactivityItem = result.filterIsInstance<SettingItem.InactivityAlarmMode>().firstOrNull()
        assert(inactivityItem != null)
        assert(inactivityItem!!.enabled)
        assert(inactivityItem.normalInactivityTime == 60)
        assert(inactivityItem.emergencyInactivityTime == 30)
    }

    @Test
    fun testBuildSettingItemsWithRecordingSettingsReturnsCorrectItem() = runTest {
        val guardians = emptyList<com.autodroid.guardiansdk.data.entity.Contact>()
        val settings = listOf(
            Setting("recording_enabled", "true", SettingType.BOOLEAN, "", "recording"),
            Setting("recording_duration", "5", SettingType.INT, "", "recording"),
            Setting("recording_segment_duration", "2", SettingType.INT, "", "recording")
        )

        val result = viewModel.buildSettingItems(guardians, settings)

        val recordingItem = result.filterIsInstance<SettingItem.AlarmRecordingMode>().firstOrNull()
        assert(recordingItem != null)
        assert(recordingItem!!.enabled)
        assert(recordingItem.duration == 5)
        assert(recordingItem.segmentDuration == 2)
    }

    @Test
    fun testBuildSettingItemsWithEmailSettingsReturnsCorrectItem() = runTest {
        val guardians = emptyList<com.autodroid.guardiansdk.data.entity.Contact>()
        val settings = listOf(
            Setting("email_enabled", "true", SettingType.BOOLEAN, "", "email"),
            Setting("email_address", "test@example.com", SettingType.STRING, "", "email"),
            Setting("smtp_host", "smtp.gmail.com", SettingType.STRING, "", "email"),
            Setting("smtp_port", "587", SettingType.INT, "", "email"),
            Setting("smtp_tls", "true", SettingType.BOOLEAN, "", "email")
        )

        val result = viewModel.buildSettingItems(guardians, settings)

        val emailItem = result.filterIsInstance<SettingItem.AlarmEmailSettings>().firstOrNull()
        assert(emailItem != null)
        assert(emailItem!!.enabled)
        assert(emailItem.emailAddress == "test@example.com")
        assert(emailItem.smtpHost == "smtp.gmail.com")
        assert(emailItem.smtpPort == 587)
        assert(emailItem.useTls)
    }

    @Test
    fun testBuildSettingItemsWithPingSettingsReturnsCorrectItem() = runTest {
        val guardians = emptyList<com.autodroid.guardiansdk.data.entity.Contact>()
        val settings = listOf(
            Setting("ping_enabled", "true", SettingType.BOOLEAN, "", "ping"),
            Setting("ping_check_interval", "30", SettingType.INT, "", "ping"),
            Setting("ping_email_retry_count", "3", SettingType.INT, "", "ping"),
            Setting("ping_email_timeout", "60", SettingType.INT, "", "ping"),
            Setting("ping_use_sms_fallback", "true", SettingType.BOOLEAN, "", "ping")
        )

        val result = viewModel.buildSettingItems(guardians, settings)

        val pingItem = result.filterIsInstance<SettingItem.PingSettings>().firstOrNull()
        assert(pingItem != null)
        assert(pingItem!!.enabled)
        assert(pingItem.checkInterval == 30)
        assert(pingItem.emailRetryCount == 3)
        assert(pingItem.emailTimeout == 60)
        assert(pingItem.useSmsFallback)
    }

    @Test
    fun testBuildSettingItemsWithHiddenUISettingsReturnsCorrectItem() = runTest {
        val guardians = emptyList<com.autodroid.guardiansdk.data.entity.Contact>()
        val settings = listOf(
            Setting("hidden_secure_ui_enabled", "true", SettingType.BOOLEAN, "", "ui"),
            Setting("door_passphrase", "小兔子乖乖把门开开", SettingType.STRING, "", "ui")
        )

        val result = viewModel.buildSettingItems(guardians, settings)

        val hiddenUIItem = result.filterIsInstance<SettingItem.HiddenSecureUI>().firstOrNull()
        assert(hiddenUIItem != null)
        assert(hiddenUIItem!!.enabled)
        assert(hiddenUIItem.doorPassphrase == "小兔子乖乖把门开开")
    }

    @Test
    fun testBuildSettingItemsWithTestModeSettingsReturnsCorrectItem() = runTest {
        val guardians = emptyList<com.autodroid.guardiansdk.data.entity.Contact>()
        val settings = listOf(
            Setting("test_mode_enabled", "true", SettingType.BOOLEAN, "", "test"),
            Setting("test_mode_practice_count", "5", SettingType.INT, "", "test"),
            Setting("test_mode_last_practice_time", "2024-01-20 10:30:00", SettingType.STRING, "", "test")
        )

        val result = viewModel.buildSettingItems(guardians, settings)

        val testModeItem = result.filterIsInstance<SettingItem.TestMode>().firstOrNull()
        assert(testModeItem != null)
        assert(testModeItem!!.isEnabled)
        assert(testModeItem.practiceCount == 5)
        assert(testModeItem.lastPracticeTime == "2024-01-20 10:30:00")
    }

    @Test
    fun testBuildSettingItemsWithGuardiansReturnsCorrectItems() = runTest {
        val guardians = listOf(
            com.autodroid.guardiansdk.data.entity.Contact(
                phoneNumber = "13800138000",
                name = "张三",
                type = com.autodroid.guardiansdk.data.entity.ContactType.GUARDIAN,
                relationship = "监护人",
                isPrimary = true,
                orderIndex = 1
            ),
            com.autodroid.guardiansdk.data.entity.Contact(
                phoneNumber = "13900139000",
                name = "李四",
                type = com.autodroid.guardiansdk.data.entity.ContactType.GUARDIAN,
                relationship = "监护人",
                isPrimary = false,
                orderIndex = 2
            )
        )
        val settings = emptyList<Setting>()

        val result = viewModel.buildSettingItems(guardians, settings)

        val guardian1Item = result.filterIsInstance<SettingItem.GuardianItem1>().firstOrNull()
        val guardian2Item = result.filterIsInstance<SettingItem.GuardianItem2>().firstOrNull()

        assert(guardian1Item != null)
        assert(guardian1Item!!.name == "张三")
        assert(guardian1Item.phoneNumber == "13800138000")
        assert(guardian1Item.isPrimary)

        assert(guardian2Item != null)
        assert(guardian2Item!!.name == "李四")
        assert(guardian2Item.phoneNumber == "13900139000")
        assert(!guardian2Item.isPrimary)
    }

    @Test
    fun testBuildSettingItemsWithMissingGuardiansReturnsPlaceholderItems() = runTest {
        val guardians = emptyList<com.autodroid.guardiansdk.data.entity.Contact>()
        val settings = emptyList<Setting>()

        val result = viewModel.buildSettingItems(guardians, settings)

        val guardian1Item = result.filterIsInstance<SettingItem.GuardianItem1>().firstOrNull()
        val guardian2Item = result.filterIsInstance<SettingItem.GuardianItem2>().firstOrNull()

        assert(guardian1Item != null)
        assert(guardian1Item!!.isPlaceholder)
        assert(guardian1Item.name == "请设置监护人1")

        assert(guardian2Item != null)
        assert(guardian2Item!!.isPlaceholder)
        assert(guardian2Item.name == "请设置监护人2")
    }

    @Test
    fun testBuildSettingItemsDefaultValuesAreCorrect() = runTest {
        val guardians = emptyList<com.autodroid.guardiansdk.data.entity.Contact>()
        val settings = emptyList<Setting>()

        val result = viewModel.buildSettingItems(guardians, settings)

        val volumeKeyItem = result.filterIsInstance<SettingItem.VolumeKeyAlarmMode>().firstOrNull()
        assert(volumeKeyItem != null)
        assert(volumeKeyItem!!.enabled)
        assert(volumeKeyItem.normalHoldTime == 5)
        assert(volumeKeyItem.emergencyHoldTime == 10)

        val floatingWindowItem = result.filterIsInstance<SettingItem.FloatingWindowAlarmMode>().firstOrNull()
        assert(floatingWindowItem != null)
        assert(floatingWindowItem!!.enabled)
        assert(floatingWindowItem.normalHoldTime == 5)
        assert(floatingWindowItem.normalOpacity == 50)
        assert(floatingWindowItem.emergencyHoldTime == 10)
        assert(floatingWindowItem.emergencyOpacity == 80)

        val shakePhoneItem = result.filterIsInstance<SettingItem.ShakePhoneAlarmMode>().firstOrNull()
        assert(shakePhoneItem != null)
        assert(shakePhoneItem!!.enabled)
        assert(shakePhoneItem.normalSensitivity == 5)
        assert(shakePhoneItem.emergencySensitivity == 8)

        val inactivityItem = result.filterIsInstance<SettingItem.InactivityAlarmMode>().firstOrNull()
        assert(inactivityItem != null)
        assert(inactivityItem!!.enabled)
        assert(inactivityItem.normalInactivityTime == 60)
        assert(inactivityItem.emergencyInactivityTime == 30)
    }
}
