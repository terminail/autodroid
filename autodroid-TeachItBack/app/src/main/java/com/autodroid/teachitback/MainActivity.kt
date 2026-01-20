package com.autodroid.teachitback

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.autodroid.teachitback.ui.TopicsFragment
import com.autodroid.teachitback.ui.WhyFragment
import com.autodroid.teachitback.ui.SettingsFragment

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        // Initialize AI service from preferences
        initializeAI()

        // Setup bottom navigation
        setupBottomNavigation()

        // Load initial fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TopicsFragment())
                .commit()
        }
    }

    private fun initializeAI() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val apiKey = sharedPreferences.getString("ai_api_key", "") ?: ""
        val model = sharedPreferences.getString("ai_model", "gpt-3.5-turbo") ?: "gpt-3.5-turbo"

        if (apiKey.isNotBlank()) {
            viewModel.initializeAI(apiKey, model)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.navigation_topics -> TopicsFragment()
                R.id.navigation_why -> WhyFragment()
                R.id.navigation_settings -> SettingsFragment()
                else -> TopicsFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            true
        }
    }
}