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
import com.autodroid.manager.service.DiscoveryStatusManager
import com.autodroid.manager.ui.dashboard.DashboardFragment
import com.autodroid.data.repository.ServerRepository
import android.app.Application
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val LOGIN_REQUEST_CODE = 1002
    }

    // ViewModels
    private lateinit var appViewModel: AppViewModel

    // Navigation
    private lateinit var navController: NavController
    private lateinit var bottomNavigation: BottomNavigationView
    
    // Track intended destination when user needs to login
    private var intendedDestination: Int? = null
    
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
        
        // Immediately check current server connection state to handle pre-existing connections
        checkCurrentServerConnectionState()
    }

    private fun initializeViewModels() {
        appViewModel = ViewModelProvider(this)[AppViewModel::class.java]
    }

    private fun setupServerConnectionObserver() {
        Log.d(TAG, "Setting up server connection observer using DiscoveryStatusManager as single source of truth")
        
        // Observe server connection status from AppViewModel (single source of truth)
        appViewModel.server.observe(this) { serverInfo ->
            Log.d(TAG, "Server info changed via AppViewModel: $serverInfo")
            
            // Use AppViewModel as single source of truth for server connection state
            val isConnected = serverInfo?.connected ?: false
            
            // Navigation state is now handled by navigation interception logic
            // No need to manually update navigation items
            
            if (isConnected) {
                // Server is connected, check authentication
                val serverName = serverInfo?.name ?: "Unknown Server"
                val serverHostname = serverInfo?.hostname ?: "Unknown Host"
                val apiEndpoint = serverInfo?.apiEndpoint ?: "Unknown"
                
                Log.d(TAG, "Server connected via AppViewModel: $serverName ($serverHostname) - $apiEndpoint")
                
                // Server is connected, user can stay in Dashboard as long as they want
                // Authentication check will only happen when user tries to access protected pages
                Log.d(TAG, "Server connected, user can stay in DashboardFragment")
                
                // Navigation state is now handled by navigation interception logic
                // No need to manually update navigation items
            } else {
                // Server not connected, stay in DashboardFragment for server discovery
                Log.d(TAG, "Server not connected via AppViewModel, staying in DashboardFragment for discovery")
                
                // Ensure we're on DashboardFragment when server is disconnected
                if (navController.currentDestination?.id != R.id.nav_dashboard) {
                    navController.navigate(R.id.nav_dashboard)
                }
            }
        }
        
        // Also observe discovery status from DiscoveryStatusManager for better state management
        DiscoveryStatusManager.discoveryStatus.observe(this) { discoveryStatus ->
            Log.d(TAG, "Discovery status changed via DiscoveryStatusManager: $discoveryStatus")
            
            // Navigation state is now handled by navigation interception logic
            // No need to manually update navigation items
            
            // Get current server connection state from AppViewModel
            val isConnected = appViewModel.server.value?.connected ?: false
            val isDiscovering = discoveryStatus?.inProgress ?: false
            val isDiscoveryFailed = discoveryStatus?.failed ?: false
            
            // Handle different connection states based on AppViewModel
            when {
                isConnected -> {
                    Log.d(TAG, "Server connected via AppViewModel, allowing navigation")
                    // Server connected, no need to force navigation
                }
                isDiscovering -> {
                    Log.d(TAG, "Server discovery in progress via AppViewModel, staying in DashboardFragment")
                    // Stay in DashboardFragment during discovery
                    if (navController.currentDestination?.id != R.id.nav_dashboard) {
                        navController.navigate(R.id.nav_dashboard)
                    }
                }
                isDiscoveryFailed -> {
                    Log.d(TAG, "Server discovery failed via AppViewModel, staying in DashboardFragment")
                    // Stay in DashboardFragment to show retry options
                    if (navController.currentDestination?.id != R.id.nav_dashboard) {
                        navController.navigate(R.id.nav_dashboard)
                    }
                }
                else -> {
                    Log.d(TAG, "Server disconnected via AppViewModel, staying in DashboardFragment")
                    // Stay in DashboardFragment when server is disconnected
                    if (navController.currentDestination?.id != R.id.nav_dashboard) {
                        navController.navigate(R.id.nav_dashboard)
                    }
                }
            }
        }
        
        // DashboardFragment is the primary page for server discovery
        Log.d(TAG, "Starting with DashboardFragment as primary page for server discovery using DiscoveryStatusManager")
    }
    
    private fun checkCurrentServerConnectionState() {
        Log.d(TAG, "Checking current server connection state on activity creation")
        
        // Use AppViewModel as single source of truth for server connection state
        val isConnected = appViewModel.server.value?.connected ?: false
        
        Log.d(TAG, "Current server connection state: isConnected=$isConnected, serverInfo=${appViewModel.server.value}")
        
        // Navigation state is now handled by navigation interception logic
        // No need to manually update navigation items
        
        if (isConnected) {
            Log.d(TAG, "Server already connected on activity creation, updating navigation state")
        } else {
            Log.d(TAG, "Server not connected on activity creation, staying in DashboardFragment")
        }
    }
    


    private fun navigateToLoginActivity() {
        val intent = Intent(this, com.autodroid.manager.auth.activity.LoginActivity::class.java)
        // Pass authentication state to prevent duplicate observation
        intent.putExtra("isAuthenticated", false)
        // Use startActivityForResult to get login result back
        startActivityForResult(intent, LOGIN_REQUEST_CODE)
        // Don't finish() MainActivity to allow returning to Dashboard
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
        val serverRepository = ServerRepository.getInstance(this.applicationContext as Application)
        
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Dashboard always allows navigation
                    NavigationUI.onNavDestinationSelected(item, navController)
                    true
                }
                else -> {
                    // Protected pages: check server connection and authentication
                    // Use database as single source of truth for server connection state
                    val connectedServer = serverRepository.getConnectedServer().value
                    val isServerConnected = connectedServer?.isConnected ?: false
                    Log.d(TAG, "Navigation interception - Checking server connection from database: isServerConnected=$isServerConnected, serverInfo=$connectedServer")
                    
                    if (!isServerConnected) {
                        // Server not connected, show message and prevent navigation
                        Log.d(TAG, "Server not connected, showing message and preventing navigation")
                        Toast.makeText(this, "请先连接服务器", Toast.LENGTH_SHORT).show()
                        false
                    } else if (!appViewModel.isAuthenticated()) {
                        // Server connected but not authenticated, navigate to login
                        Log.d(TAG, "User not authenticated, navigating to LoginActivity")
                        
                        // Remember the intended destination
                        intendedDestination = item.itemId
                        Log.d(TAG, "Remembering intended destination: $intendedDestination")
                        
                        navigateToLoginActivity()
                        false
                    } else {
                        // Connected and authenticated, allow navigation
                        Log.d(TAG, "Connected and authenticated, allowing navigation to ${item.itemId}")
                        NavigationUI.onNavDestinationSelected(item, navController)
                        true
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Simplified: MainActivity focuses on interception logic only
        // UI state management is handled by individual fragments
        Log.d(TAG, "MainActivity onResume - focusing on interception logic")
        
        // Check if user is now authenticated after returning from LoginActivity
        if (appViewModel.isAuthenticated()) {
            Log.d(TAG, "User is authenticated after returning from LoginActivity")
            
            // If user was trying to access a protected page before login,
            // navigate there now that they're authenticated
            val intendedDestination = getIntendedDestination()
            if (intendedDestination != null && intendedDestination != R.id.nav_dashboard) {
                Log.d(TAG, "Navigating to intended destination: $intendedDestination")
                navController.navigate(intendedDestination)
            }
        }
    }
    
    /**
     * Get the intended destination that user was trying to access before login
     */
    private fun getIntendedDestination(): Int? {
        val destination = intendedDestination
        // Clear the intended destination after retrieving it
        intendedDestination = null
        return destination
    }

    override fun onDestroy() {
        super.onDestroy()
        // NetworkService now handles its own lifecycle
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            LOGIN_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Login successful, checking intended destination")
                    
                    // Navigation state is now handled by navigation interception logic
                    // No need to manually update navigation items
                    
                    // User is now authenticated, navigate to intended destination if any
                    val intendedDestination = getIntendedDestination()
                    if (intendedDestination != null && intendedDestination != R.id.nav_dashboard) {
                        Log.d(TAG, "Navigating to intended destination after login: $intendedDestination")
                        
                        // Check if we're already on the intended destination
                        val currentDestination = navController.currentDestination?.id
                        if (currentDestination != intendedDestination) {
                            navController.navigate(intendedDestination)
                        } else {
                            Log.d(TAG, "Already on intended destination: $intendedDestination")
                        }
                    } else {
                        Log.d(TAG, "No intended destination or already on Dashboard, staying on current page")
                        
                        // Ensure MainActivity is properly brought to foreground after login
                        // This is critical to make sure the app is visible to the user
                        val currentDestination = navController.currentDestination?.id
                        if (currentDestination == null || currentDestination == R.id.nav_dashboard) {
                            Log.d(TAG, "Bringing MainActivity to foreground after successful login")
                            // Force a refresh of the DashboardFragment to ensure UI consistency
                            if (navController.currentDestination?.id == R.id.nav_dashboard) {
                                // Navigation state is now handled by navigation interception logic
                                // No need to manually update navigation items
                                
                                // Force a refresh of the DashboardFragment UI
                                val dashboardFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                                    ?.childFragmentManager?.fragments?.firstOrNull { it is DashboardFragment }
                                if (dashboardFragment is DashboardFragment) {
                                    dashboardFragment.refreshUI()
                                }
                            }
                        }
                    }
                }
            }
        }
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