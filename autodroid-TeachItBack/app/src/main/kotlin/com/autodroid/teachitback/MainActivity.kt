package com.autodroid.teachitback

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.autodroid.teachitback.utils.DataInitializer
import com.autodroid.teachitback.viewmodel.AppViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: AppViewModel
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[AppViewModel::class.java]
        bottomNav = findViewById(R.id.bottom_navigation)
        
        // Setup toolbar
        setupToolbar()

        // Initialize demo data
        DataInitializer(this).initializeDemoData()

        // Initialize AI service from preferences
        initializeAI()

        // Setup Navigation Component
        setupNavigation()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun initializeAI() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val apiKey = sharedPreferences.getString("ai_api_key", "") ?: ""
        val model = sharedPreferences.getString("ai_model", "gpt-3.5-turbo") ?: "gpt-3.5-turbo"

        if (apiKey.isNotBlank()) {
            viewModel.initializeAI(apiKey, model)
        }
    }

    private fun setupNavigation() {
        // Find NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup bottom navigation with NavController
        bottomNav.setupWithNavController(navController)
        
        // Configure AppBar with NavController
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_topics, R.id.nav_why, R.id.nav_settings)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Setup back button behavior
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Only show bottom navigation for main fragments: topics, why, settings
            when (destination.id) {
                R.id.nav_topics, R.id.nav_why, R.id.nav_settings -> {
                    bottomNav.visibility = android.view.View.VISIBLE
                    // Hide back button for main fragments
                    showBackButton(false)
                }
                else -> {
                    bottomNav.visibility = android.view.View.GONE
                    // Show back button for other fragments
                    showBackButton(true)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    fun showBackButton(show: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(show)
        if (show) {
            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }
}