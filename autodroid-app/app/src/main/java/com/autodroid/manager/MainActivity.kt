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
import com.autodroid.manager.model.Server
import com.autodroid.manager.model.User
import com.autodroid.manager.AppViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    // ViewModels
    private lateinit var appViewModel: AppViewModel

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
        
        // Optimized flow: Start with DashboardFragment for server discovery
        // Login will only be triggered after server connection is established
        setupServerConnectionObserver()
    }

    private fun initializeViewModels() {
        appViewModel = ViewModelProvider(this)[AppViewModel::class.java]
    }

    private fun setupServerConnectionObserver() {
        Log.d(TAG, "Setting up server connection observer")
        
        // Observe server connection status from AppViewModel
        appViewModel.server.observe(this) { server ->
            Log.d(TAG, "Server info changed: $server")
            
            val isConnected = server != null
            
            // Update server connection state
            isServerConnected = isConnected
            
            // Update navigation items based on connection state
            updateNavigationItemsState()
            
            if (isConnected) {
                // Server is connected, check authentication
                val serverName = server?.name ?: "Unknown Server"
                val serverHostname = server?.hostname ?: "Unknown Host"
                val apiEndpoint = server?.api_endpoint ?: "Unknown"
                
                Log.d(TAG, "Server connected: $serverName ($serverHostname) - $apiEndpoint")
                
                // Server is connected, user can stay in Dashboard as long as they want
                // Authentication check will only happen when user tries to access protected pages
                Log.d(TAG, "Server connected, user can stay in DashboardFragment")
                
                // Update navigation items state: enable protected pages
                updateNavigationItemsState()
            } else {
                // Server not connected, stay in DashboardFragment for server discovery
                Log.d(TAG, "Server not connected, staying in DashboardFragment for discovery")
                
                // Ensure we're on DashboardFragment when server is disconnected
                if (navController.currentDestination?.id != R.id.nav_dashboard) {
                    navController.navigate(R.id.nav_dashboard)
                }
            }
        }
        
        // Also observe discovery status for better state management
        appViewModel.discoveryStatus.observe(this) { discoveryStatus ->
            Log.d(TAG, "Discovery status changed: $discoveryStatus")
            
            // Update navigation items based on connection state
            updateNavigationItemsState()
            
            // Get current server connection state
            val connectionState = appViewModel.getServerConnectionState()
            Log.d(TAG, "Current server connection state: $connectionState")
            
            // Handle different connection states
            when (connectionState) {
                AppViewModel.ServerConnectionState.DISCOVERING -> {
                    Log.d(TAG, "Server discovery in progress, staying in DashboardFragment")
                    // Stay in DashboardFragment during discovery
                    if (navController.currentDestination?.id != R.id.nav_dashboard) {
                        navController.navigate(R.id.nav_dashboard)
                    }
                }
                AppViewModel.ServerConnectionState.FAILED -> {
                    Log.d(TAG, "Server discovery failed, staying in DashboardFragment")
                    // Stay in DashboardFragment to show retry options
                    if (navController.currentDestination?.id != R.id.nav_dashboard) {
                        navController.navigate(R.id.nav_dashboard)
                    }
                }
                else -> {
                    // Other states handled by serverInfo observer
                }
            }
        }
        
        // DashboardFragment is the primary page for server discovery
        Log.d(TAG, "Starting with DashboardFragment as primary page for server discovery")
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, com.autodroid.manager.auth.activity.LoginActivity::class.java)
        // Pass authentication state to prevent duplicate observation
        intent.putExtra("isAuthenticated", false)
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
        
        // Add navigation listener to intercept navigation based on server connection and authentication
        setupNavigationInterception()
    }
    
    private fun setupNavigationInterception() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Dashboard always allows navigation
                    NavigationUI.onNavDestinationSelected(item, navController)
                    true
                }
                else -> {
                    // Protected pages: check server connection and authentication
                    if (!isServerConnected) {
                        // Server not connected, show message and prevent navigation
                        Toast.makeText(this, "请先连接服务器", Toast.LENGTH_SHORT).show()
                        false
                    } else if (!appViewModel.isAuthenticated()) {
                        // Server connected but not authenticated, navigate to login
                        Log.d(TAG, "User not authenticated, navigating to LoginActivity")
                        navigateToLoginActivity()
                        false
                    } else {
                        // Connected and authenticated, allow navigation
                        NavigationUI.onNavDestinationSelected(item, navController)
                        true
                    }
                }
            }
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