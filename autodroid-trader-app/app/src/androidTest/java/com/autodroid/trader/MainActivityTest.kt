package com.autodroid.trader

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @Test
    fun testMainActivityLaunches() {
        // Launch the activity using ActivityScenario
        ActivityScenario.launch(MainActivity::class.java)
        // The test passes if the activity launches without crashing
    }
}
