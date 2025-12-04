// MainActivity.kt
package com.autodroid.manager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.autodroid.manager.R
import com.autodroid.manager.auth.viewmodel.AuthViewModel
import com.autodroid.manager.service.DiscoveryStatusManager
import com.autodroid.manager.viewmodel.AppViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    // ViewModels
    private lateinit var viewModel: AppViewModel
    private lateinit var authViewModel: AuthViewModel

    // Navigation
    private lateinit var navController: NavController
    private lateinit var bottomNavigation: BottomNavigationView
    
    // Activity Result API launcher for permissions
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            // All permissions granted (NetworkService is auto-started in MyApplication)
            Log.d(TAG, "All permissions granted for mDNS discovery")
        } else {
            // Some permissions denied, show message
            Toast.makeText(this, "Permissions required for mDNS discovery", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViewModels()
        checkAuthentication()
        initializeNavigation()
        
        // Check and request permissions (NetworkService is auto-started in MyApplication)
        checkAndRequestPermissions()
    }

    private fun initializeViewModels() {
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
    }

    private fun checkAuthentication() {
        // Check if we have valid authentication data from LoginActivity
        val isAuthenticatedFromIntent = intent.getBooleanExtra("isAuthenticated", false)

        if (isAuthenticatedFromIntent) {
            // We came from LoginActivity with authentication - don't redirect back!
            val userId = intent.getStringExtra("userId")
            val email = intent.getStringExtra("email")
            val token = intent.getStringExtra("token")

            // Update the ViewModel with the authentication data
            if (userId != null && email != null && token != null) {
                authViewModel.setIsAuthenticated(true)
                authViewModel.setUserId(userId)
                authViewModel.setEmail(email)
                authViewModel.setToken(token)
            }
            return // Stay in MainActivity
        }

        // Only redirect if not authenticated AND we didn't come from LoginActivity
        if (!authViewModel.isAuthenticated()) {
            Log.d(TAG, "Not authenticated, redirecting to LoginActivity")
            val intent = Intent(this, com.autodroid.manager.auth.activity.LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun initializeNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController
        bottomNavigation = findViewById(R.id.bottom_navigation)
        NavigationUI.setupWithNavController(bottomNavigation, navController)
    }

    override fun onDestroy() {
        super.onDestroy()
        // NetworkService now handles its own lifecycle
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()
        
        // Check location permission for mDNS discovery (Android 6.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        
        // Check network state permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_NETWORK_STATE)
        }
        
        // Check internet permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.INTERNET)
        }
        
        // Check WiFi state permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_WIFI_STATE)
        }
        
        if (permissionsNeeded.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsNeeded.toTypedArray())
            return false
        }
        
        return true
    }
}