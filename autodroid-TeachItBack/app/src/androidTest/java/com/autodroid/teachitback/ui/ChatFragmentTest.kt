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
class ChatFragmentTest {

    @Before
    fun setup() {
        
    }

    @Test
    fun testFragmentIsDisplayed() {
        val fragmentArgs = Bundle().apply {
            putString("topicId", "test-topic-id")
            putString("topicTitle", "Test Topic")
        }
        launchFragmentInContainer<ChatFragment>(fragmentArgs)

        onView(withId(R.id.topic_title))
            .check(matches(withText("Test Topic")))
    }

    @Test
    fun testBackButtonIsDisplayed() {
        val fragmentArgs = Bundle().apply {
            putString("topicId", "test-topic-id")
            putString("topicTitle", "Test Topic")
        }
        launchFragmentInContainer<ChatFragment>(fragmentArgs)

        onView(withId(R.id.back_button))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMessagesRecyclerViewIsDisplayed() {
        val fragmentArgs = Bundle().apply {
            putString("topicId", "test-topic-id")
            putString("topicTitle", "Test Topic")
        }
        launchFragmentInContainer<ChatFragment>(fragmentArgs)

        onView(withId(R.id.messages_recycler_view))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMessageInputIsDisplayed() {
        val fragmentArgs = Bundle().apply {
            putString("topicId", "test-topic-id")
            putString("topicTitle", "Test Topic")
        }
        launchFragmentInContainer<ChatFragment>(fragmentArgs)

        onView(withId(R.id.message_input))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSendButtonIsDisplayed() {
        val fragmentArgs = Bundle().apply {
            putString("topicId", "test-topic-id")
            putString("topicTitle", "Test Topic")
        }
        launchFragmentInContainer<ChatFragment>(fragmentArgs)

        onView(withId(R.id.send_button))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testVoiceButtonIsDisplayed() {
        val fragmentArgs = Bundle().apply {
            putString("topicId", "test-topic-id")
            putString("topicTitle", "Test Topic")
        }
        launchFragmentInContainer<ChatFragment>(fragmentArgs)

        onView(withId(R.id.voice_button))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSendMessage() {
        val fragmentArgs = Bundle().apply {
            putString("topicId", "test-topic-id")
            putString("topicTitle", "Test Topic")
        }
        launchFragmentInContainer<ChatFragment>(fragmentArgs)

        onView(withId(R.id.message_input))
            .perform(typeText("Test message"))

        onView(withId(R.id.send_button))
            .perform(click())
    }

    @Test
    fun testVoiceButtonClick() {
        val fragmentArgs = Bundle().apply {
            putString("topicId", "test-topic-id")
            putString("topicTitle", "Test Topic")
        }
        launchFragmentInContainer<ChatFragment>(fragmentArgs)

        onView(withId(R.id.voice_button))
            .perform(click())
    }
}