package com.autodroid.teachitback.ui

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.autodroid.teachitback.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhyFragmentTest {

    @Before
    fun setup() {
        
    }

    @Test
    fun testFragmentIsDisplayed() {
        launchFragmentInContainer<WhyFragment>()

        onView(withText("Teach It Back"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAppIntroductionIsDisplayed() {
        launchFragmentInContainer<WhyFragment>()

        onView(withText("Teach It Back"))
            .check(matches(isDisplayed()))
    }
}