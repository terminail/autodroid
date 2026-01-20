package com.autodroid.teachitback

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        // Initialize AI service from preferences
        initializeAI()
    }

    private fun initializeAI() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val apiKey = sharedPreferences.getString("ai_api_key", "") ?: ""
        val model = sharedPreferences.getString("ai_model", "gpt-3.5-turbo") ?: "gpt-3.5-turbo"

        if (apiKey.isNotBlank()) {
            viewModel.initializeAI(apiKey, model)
        }
    }
}