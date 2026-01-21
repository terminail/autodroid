package com.autodroid.teachitback.ui

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.autodroid.teachitback.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsFragmentTest {

    @Before
    fun setup() {
        
    }

    @Test
    fun testFragmentIsDisplayed() {
        launchFragmentInContainer<SettingsFragment>()

        onView(withText("设置"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testApiKeyInputIsDisplayed() {
        launchFragmentInContainer<SettingsFragment>()

        onView(withId(R.id.api_key_input))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testModelInputIsDisplayed() {
        launchFragmentInContainer<SettingsFragment>()

        onView(withId(R.id.model_input))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSaveButtonIsDisplayed() {
        launchFragmentInContainer<SettingsFragment>()

        onView(withId(R.id.save_button))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSaveSettings() {
        launchFragmentInContainer<SettingsFragment>()

        onView(withId(R.id.api_key_input))
            .perform(typeText("test-api-key"))

        onView(withId(R.id.model_input))
            .perform(typeText("gpt-3.5-turbo"))

        onView(withId(R.id.save_button))
            .perform(click())
    }

    @Test
    fun testSaveSettingsWithEmptyApiKey() {
        launchFragmentInContainer<SettingsFragment>()

        onView(withId(R.id.save_button))
            .perform(click())

        onView(withText("API密钥不能为空"))
            .check(matches(isDisplayed()))
    }
}