// MainActivity.kt
package com.autodroid.trader.app

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
import com.autodroid.trader.app.ui.floatwindow.FloatWindowManager
import com.autodroid.trader.app.utils.ServiceUtils
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
        
        // Don't start float window service here - it will be started when app goes to background
        
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
    
    override fun onResume() {
        super.onResume()
        // Hide float window when app is in foreground
        FloatWindowManager.hideFloatWindow(this)
        
        // Check if required services are enabled
        checkRequiredServices()
    }
    
    private fun checkRequiredServices() {
        val aasEnabled = ServiceUtils.isAASEnabled(this)
        val imeEnabled = ServiceUtils.isIMEEnabled(this)
        val imeSetAsDefault = ServiceUtils.isIMESetAsDefault(this)
        
        // Debug logging
        Log.d("ServiceCheck", "AAS Enabled: $aasEnabled")
        Log.d("ServiceCheck", "IME Enabled: $imeEnabled")
        Log.d("ServiceCheck", "IME Set as Default: $imeSetAsDefault")
        
        val missingServices = mutableListOf<String>()
        val servicesNeedingAction = mutableListOf<String>()
        
        if (!aasEnabled) {
            missingServices.add("辅助功能服务 (AAS)")
        }
        if (!imeEnabled) {
            missingServices.add("输入法服务 (IME)")
        } else if (!imeSetAsDefault) {
            servicesNeedingAction.add("将默认输入法改为 Trader IME")
        }
        
        if (missingServices.isNotEmpty() || servicesNeedingAction.isNotEmpty()) {
            Log.d("ServiceCheck", "Missing services: $missingServices")
            Log.d("ServiceCheck", "Services needing action: $servicesNeedingAction")
            showServiceSettingsDialog(missingServices, servicesNeedingAction)
        } else {
            Log.d("ServiceCheck", "All services are properly configured")
        }
    }
    
    private fun showServiceSettingsDialog(missingServices: List<String>, servicesNeedingAction: List<String>) {
        val message = if (missingServices.isNotEmpty() && servicesNeedingAction.isNotEmpty()) {
            "需要配置以下服务以正常使用应用：\n\n" +
            "• ${missingServices.joinToString(", ")} - 需要启用\n" +
            "• ${servicesNeedingAction.joinToString(", ")} - 需要配置\n\n" +
            "请前往设置页面进行配置。"
        } else if (missingServices.isNotEmpty()) {
            "需要启用以下服务以正常使用应用：\n\n" +
            "• ${missingServices.joinToString(", ")}\n\n" +
            "请前往设置页面启用这些服务。"
        } else {
            "需要配置以下服务：\n\n" +
            "• ${servicesNeedingAction.joinToString(", ")}\n\n" +
            "请前往设置页面进行配置。"
        }
        
        // Create an alert dialog to guide the user
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("服务配置")
        builder.setMessage(message)
        
        builder.setPositiveButton("前往设置") { _, _ ->
            // Navigate to the appropriate settings based on what's needed
            when {
                missingServices.any { it.contains("AAS") } && servicesNeedingAction.any { it.contains("IME") } -> {
                    // AAS missing and IME needs to be set as default - show accessibility settings first
                    ServiceUtils.navigateToAASSettings(this)
                }
                missingServices.any { it.contains("AAS") } && missingServices.any { it.contains("IME") } -> {
                    // Both services missing - show accessibility settings first
                    ServiceUtils.navigateToAASSettings(this)
                }
                missingServices.any { it.contains("AAS") } -> {
                    ServiceUtils.navigateToAASSettings(this)
                }
                missingServices.any { it.contains("IME") } -> {
                    ServiceUtils.navigateToIMESettings(this)
                }
                servicesNeedingAction.any { it.contains("IME") } -> {
                    // IME is enabled but not set as default - show input method picker directly
                    ServiceUtils.showInputMethodPicker(this)
                }
                else -> {
                    Toast.makeText(this, "请在设置中配置必需的服务", Toast.LENGTH_LONG).show()
                }
            }
        }
        
        builder.setNegativeButton("稍后") { _, _ ->
            // User chooses to do it later
        }
        
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()
    }
    
    override fun onPause() {
        super.onPause()
        // Show float window when app is in background
        FloatWindowManager.showFloatWindow(this)
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
        
        // Check phone state permission (needed for device serial number)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)
        }
        
        if (permissionsNeeded.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsNeeded.toTypedArray())
            return false
        }
        
        return true
    }
}