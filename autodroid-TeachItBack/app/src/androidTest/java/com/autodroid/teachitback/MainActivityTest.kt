package com.autodroid.teachitback

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.autodroid.teachitback.R
import com.autodroid.teachitback.ui.TopicsFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Before
    fun setup() {
        
    }

    @Test
    fun testMainActivityIsDisplayed() {
        launchFragmentInContainer<TopicsFragment>()

        onView(withId(R.id.fragment_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBottomNavigationIsDisplayed() {
        launchFragmentInContainer<TopicsFragment>()

        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testTopicsTabIsSelected() {
        launchFragmentInContainer<TopicsFragment>()

        onView(withId(R.id.navigation_topics))
            .check(matches(isSelected()))
    }

    @Test
    fun testWhyTabIsDisplayed() {
        launchFragmentInContainer<TopicsFragment>()

        onView(withId(R.id.navigation_why))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSettingsTabIsDisplayed() {
        launchFragmentInContainer<TopicsFragment>()

        onView(withId(R.id.navigation_settings))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToWhyFragment() {
        launchFragmentInContainer<TopicsFragment>()

        onView(withId(R.id.navigation_why))
            .perform(click())

        onView(withText("Teach It Back"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToSettingsFragment() {
        launchFragmentInContainer<TopicsFragment>()

        onView(withId(R.id.navigation_settings))
            .perform(click())

        onView(withText("设置"))
            .check(matches(isDisplayed()))
    }
}