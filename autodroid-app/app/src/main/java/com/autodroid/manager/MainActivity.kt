// MainActivity.kt
package com.autodroid.manager

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.autodroid.manager.R
import com.autodroid.manager.auth.viewmodel.AuthViewModel
import com.autodroid.manager.model.DiscoveredServer
import com.autodroid.manager.service.NetworkService
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

    // Network service
    private var networkService: NetworkService? = null
    private var isNetworkServiceBound = false

    // Navigation
    private lateinit var navController: NavController
    private lateinit var bottomNavigation: BottomNavigationView

    // Service connection for NetworkService
    private val networkServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as NetworkService.LocalBinder
            networkService = binder.service
            isNetworkServiceBound = true

            // Set up server discovery callback
            networkService?.setServerDiscoveryCallback(object : NetworkService.ServerDiscoveryCallback {
                override fun onServerDiscovered(server: DiscoveredServer?) {
                    if (server != null) {
                        runOnUiThread {
                            // Create unified serverInfo object with all server properties
                            val serverInfo = mutableMapOf<String?, Any?>()
                            serverInfo["name"] = server.serviceName
                            serverInfo["ip"] = server.host
                            serverInfo["port"] = server.port
                            serverInfo["connected"] = true
                            
                            viewModel.setServerInfo(serverInfo)
                            Toast.makeText(this@MainActivity, "Server discovered: ${server.host}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onServerLost(server: DiscoveredServer?) {
                    if (server != null) {
                        runOnUiThread {
                            // Update serverInfo to mark as disconnected
                            val currentServerInfo = viewModel.serverInfo.value?.toMutableMap() ?: mutableMapOf()
                            currentServerInfo["connected"] = false
                            viewModel.setServerInfo(currentServerInfo)
                            Toast.makeText(this@MainActivity, "Server lost: ${server.host}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isNetworkServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViewModels()
        checkAuthentication()
        initializeNavigation()
        
        // Check and request permissions before starting network service
        if (checkAndRequestPermissions()) {
            startNetworkService()
        }
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

    private fun startNetworkService() {
        val intent = Intent(this, NetworkService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, networkServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isNetworkServiceBound) {
            unbindService(networkServiceConnection)
            isNetworkServiceBound = false
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // Permissions granted, start network service
                    startNetworkService()
                } else {
                    // Permissions denied, show message
                    Toast.makeText(this, "Permissions required for mDNS discovery", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()
        
        // Check location permission for mDNS discovery (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
            return false
        }
        
        return true
    }
}