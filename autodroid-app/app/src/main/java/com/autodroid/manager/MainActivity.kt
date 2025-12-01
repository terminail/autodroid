// MainActivity.kt
package com.autodroid.manager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
                            viewModel.setServerIp(server.host ?: "")
                            viewModel.setServerConnected(true)
                            Toast.makeText(this@MainActivity, "Server discovered: ${server.host}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onServerLost(server: DiscoveredServer?) {
                    if (server != null) {
                        runOnUiThread {
                            viewModel.setServerConnected(false)
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
        startNetworkService()
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
}