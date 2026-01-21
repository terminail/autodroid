package com.autodroid.teachitback.ui

import android.os.Bundle
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
class TopicsFragmentTest {

    @Before
    fun setup() {
        
    }

    @Test
    fun testFragmentIsDisplayed() {
        launchFragmentInContainer<TopicsFragment>()

        onView(withId(R.id.topics_recycler_view))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testEmptyStateIsDisplayed() {
        launchFragmentInContainer<TopicsFragment>()

        onView(withId(R.id.empty_state))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAddTopicButtonIsDisplayed() {
        launchFragmentInContainer<TopicsFragment>()

        onView(withId(R.id.add_topic_button))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNewTopicTitleInputIsDisplayed() {
        launchFragmentInContainer<TopicsFragment>()

        onView(withId(R.id.new_topic_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNewTopicDescriptionInputIsDisplayed() {
        launchFragmentInContainer<TopicsFragment>()

        onView(withId(R.id.new_topic_description))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAddTopicButtonClick() {
        launchFragmentInContainer<TopicsFragment>()

        onView(withId(R.id.new_topic_title))
            .perform(typeText("Test Topic"))

        onView(withId(R.id.add_topic_button))
            .perform(click())
    }
}