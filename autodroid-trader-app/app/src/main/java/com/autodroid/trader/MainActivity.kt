// MainActivity.kt
package com.autodroid.trader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.autodroid.trader.R
import com.google.android.material.bottomnavigation.BottomNavigationView

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
    
    // Activity Result API launcher for permissions
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            // All permissions granted
            Log.d(TAG, "All permissions granted")
        } else {
            // Some permissions denied, show message
            Toast.makeText(this, "Permissions required for proper functioning", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViewModels()
        initializeNavigation()
        
        // Check and request permissions
        checkAndRequestPermissions()
        
    }

    private fun initializeViewModels() {
        appViewModel = (application as MyApplication).getAppViewModel()
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