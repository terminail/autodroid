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
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.autodroid.manager.R
import com.autodroid.manager.model.ServerInfo
import com.autodroid.manager.model.UserInfo
import com.autodroid.manager.AppViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    // ViewModels
    private lateinit var viewModel: AppViewModel

    // Navigation
    private lateinit var navController: NavController
    private lateinit var bottomNavigation: BottomNavigationView
    
    // Server connection state
    private var isServerConnected = false
    
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
        initializeNavigation()
        
        // Check and request permissions (NetworkService is auto-started in MyApplication)
        checkAndRequestPermissions()
        
        // New flow: Start with DashboardFragment and check server connection first
        checkServerConnectionAndAuthentication()
    }

    private fun initializeViewModels() {
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]
    }

    private fun checkServerConnectionAndAuthentication() {
        Log.d(TAG, "Checking server connection and authentication status")
        
        // Check if we have valid authentication data from LoginActivity
        val isAuthenticatedFromIntent = intent.getBooleanExtra("isAuthenticated", false)

        if (isAuthenticatedFromIntent) {
            // We came from LoginActivity with authentication - update ViewModel
            val userId = intent.getStringExtra("userId")
            val email = intent.getStringExtra("email")
            val token = intent.getStringExtra("token")

            if (userId != null && email != null && token != null) {
                val userInfo = UserInfo(
                    userId = userId,
                    email = email,
                    token = token,
                    isAuthenticated = true
                )
                viewModel.setUserInfo(userInfo)
            }
        }
        
        // Observe server connection status from AppViewModel
        viewModel.serverInfo.observe(this) { serverInfo ->
            Log.d(TAG, "Server info changed: $serverInfo")
            
            val isConnected = serverInfo?.get("connected") as? Boolean ?: false
            
            // Update server connection state
            isServerConnected = isConnected
            
            // Update navigation items based on connection state
            updateNavigationItemsState()
            
            if (isConnected) {
                // Server is connected, check authentication
                val serverHost = serverInfo["ip"] as? String
                val serverPort = serverInfo["port"] as? Int
                
                Log.d(TAG, "Server connected: $serverHost:$serverPort")
                
                // Check if user is authenticated
                if (!viewModel.isAuthenticated()) {
                    Log.d(TAG, "User not authenticated, navigating to LoginActivity")
                    navigateToLoginActivity()
                } else {
                    Log.d(TAG, "User already authenticated, staying in DashboardFragment")
                    // User is authenticated, stay in DashboardFragment
                }
            } else {
                // Server not connected, stay in DashboardFragment for server discovery
                Log.d(TAG, "Server not connected, staying in DashboardFragment for discovery")
                
                // Ensure we're on DashboardFragment when server is disconnected
                if (navController.currentDestination?.id != R.id.nav_dashboard) {
                    navController.navigate(R.id.nav_dashboard)
                }
            }
        }
        
        // DashboardFragment is the primary page for server discovery
        Log.d(TAG, "Starting with DashboardFragment as primary page for server discovery")
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, com.autodroid.manager.auth.activity.LoginActivity::class.java)
        // Use standard activity transition without clearing the task
        startActivity(intent)
        // Don't finish() MainActivity to allow returning to Dashboard
    }
    
    private fun updateNavigationItemsState() {
        val menu = bottomNavigation.menu
        
        // Enable/disable navigation items based on server connection state
        menu.findItem(R.id.nav_workflows).isEnabled = isServerConnected
        menu.findItem(R.id.nav_reports).isEnabled = isServerConnected
        menu.findItem(R.id.nav_orders).isEnabled = isServerConnected
        menu.findItem(R.id.nav_my).isEnabled = isServerConnected
        
        // Dashboard is always enabled for server discovery
        menu.findItem(R.id.nav_dashboard).isEnabled = true
        
        // Update item titles to show connection status
        if (!isServerConnected) {
            menu.findItem(R.id.nav_workflows).title = "Workflows (需要连接)"
            menu.findItem(R.id.nav_reports).title = "Reports (需要连接)"
            menu.findItem(R.id.nav_orders).title = "Orders (需要连接)"
            menu.findItem(R.id.nav_my).title = "My (需要连接)"
        } else {
            menu.findItem(R.id.nav_workflows).title = "Workflows"
            menu.findItem(R.id.nav_reports).title = "Reports"
            menu.findItem(R.id.nav_orders).title = "Orders"
            menu.findItem(R.id.nav_my).title = "My"
        }
    }

    private fun initializeNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController
        bottomNavigation = findViewById(R.id.bottom_navigation)
        NavigationUI.setupWithNavController(bottomNavigation, navController)
        
        // Add navigation listener to intercept navigation when server is disconnected
        bottomNavigation.setOnItemSelectedListener { item ->
            // Check if server is connected for non-dashboard items
            if (!isServerConnected && item.itemId != R.id.nav_dashboard) {
                // Show toast message and prevent navigation
                Toast.makeText(this, "请先连接服务器", Toast.LENGTH_SHORT).show()
                return@setOnItemSelectedListener false
            }
            
            // Allow navigation for dashboard or when server is connected
            NavigationUI.onNavDestinationSelected(item, navController)
            return@setOnItemSelectedListener true
        }
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