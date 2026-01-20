package com.autodroid.guardiansdk.ui.settings

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.autodroid.guardiansdk.R
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingFragmentUITest {

    @Before
    fun setup() {
        launchFragmentInContainer<SettingFragment>()
    }

    @Test
    fun recyclerView_isDisplayed() {
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun recyclerView_hasChildren() {
        onView(withId(R.id.recyclerView))
            .check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun guardianItem1_isDisplayed() {
        onView(withId(R.id.recyclerView))
            .check(matches(hasDescendant(withText("请设置监护人1"))))
    }

    @Test
    fun volumeKeyAlarmMode_isDisplayed() {
        onView(withId(R.id.recyclerView))
            .check(matches(hasDescendant(withText("报警触发模式1"))))
    }

    @Test
    fun floatingWindowAlarmMode_isDisplayed() {
        onView(withId(R.id.recyclerView))
            .check(matches(hasDescendant(withText("报警触发模式2"))))
    }

    @Test
    fun shakePhoneAlarmMode_isDisplayed() {
        onView(withId(R.id.recyclerView))
            .check(matches(hasDescendant(withText("报警触发模式3"))))
    }

    @Test
    fun inactivityAlarmMode_isDisplayed() {
        onView(withId(R.id.recyclerView))
            .check(matches(hasDescendant(withText("报警触发模式4"))))
    }

    @Test
    fun alarmRecordingMode_isDisplayed() {
        onView(withId(R.id.recyclerView))
            .check(matches(hasDescendant(withText("报警录音模式"))))
    }

    @Test
    fun alarmEmailSettings_isDisplayed() {
        onView(withId(R.id.recyclerView))
            .check(matches(hasDescendant(withText("报警邮件设置"))))
    }

    @Test
    fun pingSettings_isDisplayed() {
        onView(withId(R.id.recyclerView))
            .check(matches(hasDescendant(withText("Ping响应设置"))))
    }

    @Test
    fun hiddenSecureUI_isDisplayed() {
        onView(withId(R.id.recyclerView))
            .check(matches(hasDescendant(withText("隐秘界面"))))
    }

    @Test
    fun testMode_isDisplayed() {
        onView(withId(R.id.recyclerView))
            .check(matches(hasDescendant(withText("测试模式"))))
    }

    @Test
    fun passwordBook_isDisplayed() {
        onView(withId(R.id.recyclerView))
            .check(matches(hasDescendant(withText("密码本"))))
    }

    @Test
    fun alarmHistory_isDisplayed() {
        onView(withId(R.id.recyclerView))
            .check(matches(hasDescendant(withText("报警历史"))))
    }

    @Test
    fun clickGuardianItem1_opensDialog() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("请设置监护人1"))))
            .perform(click())
        
        onView(withText("报警触发模式-音量键设置"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickVolumeKeyAlarmMode_opensDialog() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("报警触发模式1"))))
            .perform(click())
        
        onView(withText("报警触发模式-音量键设置"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickFloatingWindowAlarmMode_opensDialog() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("报警触发模式2"))))
            .perform(click())
        
        onView(withText("报警触发模式-浮动窗口设置"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickShakePhoneAlarmMode_opensDialog() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("报警触发模式3"))))
            .perform(click())
        
        onView(withText("报警触发模式-摇动手机设置"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickInactivityAlarmMode_opensDialog() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("报警触发模式4"))))
            .perform(click())
        
        onView(withText("报警触发模式-长时间未使用设置"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickAlarmRecordingMode_opensDialog() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("报警录音模式"))))
            .perform(click())
        
        onView(withText("报警录音模式设置"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickAlarmEmailSettings_opensDialog() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("报警邮件设置"))))
            .perform(click())
        
        onView(withText("报警邮件设置"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickPingSettings_opensDialog() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("Ping响应设置"))))
            .perform(click())
        
        onView(withText("Ping响应设置"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickHiddenSecureUI_opensDialog() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("隐秘界面"))))
            .perform(click())
        
        onView(withText("隐秘界面设置"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickTestMode_opensDialog() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("测试模式"))))
            .perform(click())
        
        onView(withText("测试模式设置"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickPasswordBook_opensDialog() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("密码本"))))
            .perform(click())
        
        onView(withText("密码本"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun dialogHasPositiveButton() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("报警触发模式1"))))
            .perform(click())
        
        onView(withText("保存"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun dialogHasNegativeButton() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("报警触发模式1"))))
            .perform(click())
        
        onView(withText("取消"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clickPositiveButton_closesDialog() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("报警触发模式1"))))
            .perform(click())
        
        onView(withText("保存"))
            .perform(click())
        
        onView(withText("报警触发模式-音量键设置"))
            .check(doesNotExist())
    }

    @Test
    fun clickNegativeButton_closesDialog() {
        onView(allOf(withId(R.id.recyclerView), hasDescendant(withText("报警触发模式1"))))
            .perform(click())
        
        onView(withText("取消"))
            .perform(click())
        
        onView(withText("报警触发模式-音量键设置"))
            .check(doesNotExist())
    }

    @Test
    fun recyclerView_isScrollable() {
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
            .check(matches(isScrollable()))
    }
}
