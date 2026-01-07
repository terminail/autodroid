package com.autodroid.trader.app.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

object ServiceUtils {
    
    private const val AAS_SERVICE_NAME = "com.autodroid.trader.aas.service.UIRecorderAccessibilityService"
    private const val IME_SERVICE_NAME = "TraderIME"
    
    fun isAASEnabled(context: Context): Boolean {
        val service = "com.autodroid.trader.aas/$AAS_SERVICE_NAME"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service) == true
    }
    
    fun isIMEEnabled(context: Context): Boolean {
        val service = "com.autodroid.trader.ime/$IME_SERVICE_NAME"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_INPUT_METHODS
        )
        // Use exact match with the correct format (with dot before service name)
        val exactService = "com.autodroid.trader.ime/.$IME_SERVICE_NAME"
        
        // Debug logging
        android.util.Log.d("ServiceUtils", "IME Enabled Services: $enabledServices")
        android.util.Log.d("ServiceUtils", "Looking for exact service: $exactService")
        android.util.Log.d("ServiceUtils", "Looking for service: $service")
        
        val result = enabledServices?.contains(exactService) == true || enabledServices?.contains(service) == true
        android.util.Log.d("ServiceUtils", "IME Enabled result: $result")
        return result
    }
    
    fun isIMESetAsDefault(context: Context): Boolean {
        val defaultIME = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        val expectedIME = "com.autodroid.trader.ime/.$IME_SERVICE_NAME"
        
        // Debug logging
        android.util.Log.d("ServiceUtils", "Default IME: $defaultIME")
        android.util.Log.d("ServiceUtils", "Expected IME: $expectedIME")
        
        val result = defaultIME == expectedIME
        android.util.Log.d("ServiceUtils", "IME Set as Default result: $result")
        return result
    }
    
    fun navigateToAASSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
            Toast.makeText(context, "Please enable Trader AAS in Accessibility Settings", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open Accessibility Settings", Toast.LENGTH_LONG).show()
        }
    }
    
    fun navigateToIMESettings(context: Context) {
        try {
            // Open settings page where user can find "语言和输入法" menu
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
            Toast.makeText(context, "请在设置中找到'语言和输入法'，然后启用'Trader IME'输入法", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开设置", Toast.LENGTH_LONG).show()
        }
    }
    
    // Navigate to default input method settings
    fun navigateToDefaultInputMethodSettings(context: Context) {
        try {
            // Open settings page where user can find "语言和输入法" menu
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
            Toast.makeText(context, "请在设置中找到'语言和输入法'，点击'默认键盘'，然后选择'Trader IME'", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开设置", Toast.LENGTH_LONG).show()
        }
    }
    
    // Direct method to show input method picker (if available)
    fun showInputMethodPicker(context: Context) {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
            Toast.makeText(context, "请选择'Trader IME'作为默认输入法", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Fallback to settings navigation
            navigateToDefaultInputMethodSettings(context)
        }
    }
}