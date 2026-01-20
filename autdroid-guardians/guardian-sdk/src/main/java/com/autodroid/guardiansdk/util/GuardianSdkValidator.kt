package com.autodroid.guardiansdk.util

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import com.autodroid.guardiansdk.ui.GuardianActivity
import com.autodroid.guardiansdk.service.EmergencyService
import com.autodroid.guardiansdk.service.FloatingWindowService
import com.autodroid.guardiansdk.service.GuardianAccessibilityService
import com.autodroid.guardiansdk.service.AudioRecordingService
import com.autodroid.guardiansdk.sms.receiver.SmsReceiver

data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
    val warnings: List<String>
)

object GuardianSdkValidator {

    private val REQUIRED_PERMISSIONS = listOf(
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.SYSTEM_ALERT_WINDOW,
        android.Manifest.permission.FOREGROUND_SERVICE,
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    private val REQUIRED_COMPONENTS: List<Class<*>> = listOf(
        GuardianActivity::class.java,
        EmergencyService::class.java,
        FloatingWindowService::class.java,
        GuardianAccessibilityService::class.java,
        AudioRecordingService::class.java,
        SmsReceiver::class.java
    )

    fun validate(context: Context): ValidationResult {
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        try {
            val packageManager = context.packageManager
            val packageName = context.packageName

            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_PERMISSIONS
            )

            val registeredActivities = packageInfo.activities?.map { it.name }?.toSet() ?: emptySet()
            val registeredServices = packageInfo.services?.map { it.name }?.toSet() ?: emptySet()
            val registeredReceivers = packageInfo.receivers?.map { it.name }?.toSet() ?: emptySet()
            val declaredPermissions = packageInfo.requestedPermissions?.toSet() ?: emptySet()

            for (component in REQUIRED_COMPONENTS) {
                val componentName = component.name
                when {
                    Activity::class.java.isAssignableFrom(component) -> {
                        if (!registeredActivities.contains(componentName)) {
                            issues.add("缺少组件注册: ${component.simpleName}")
                        }
                    }
                    Service::class.java.isAssignableFrom(component) -> {
                        if (!registeredServices.contains(componentName)) {
                            issues.add("缺少组件注册: ${component.simpleName}")
                        }
                    }
                    BroadcastReceiver::class.java.isAssignableFrom(component) -> {
                        if (!registeredReceivers.contains(componentName)) {
                            issues.add("缺少组件注册: ${component.simpleName}")
                        }
                    }
                }
            }

            for (permission in REQUIRED_PERMISSIONS) {
                if (!declaredPermissions.contains(permission)) {
                    warnings.add("缺少权限声明: ${permission.split(".").last()}")
                }
            }

            if (issues.isEmpty() && warnings.isEmpty()) {
                return ValidationResult(true, emptyList(), listOf("SDK集成验证通过"))
            }

        } catch (e: NameNotFoundException) {
            issues.add("无法获取应用包信息: ${e.message}")
        } catch (e: Exception) {
            issues.add("验证过程中发生错误: ${e.message}")
        }

        return ValidationResult(issues.isEmpty(), issues, warnings)
    }

    fun checkPermissions(context: Context): List<String> {
        val missingPermissions = mutableListOf<String>()

        for (permission in REQUIRED_PERMISSIONS) {
            if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }

        return missingPermissions
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/com.autodroid.guardiansdk.service.GuardianAccessibilityService"
        val enabledServices = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_accessibility_services"
        )

        return enabledServices?.contains(expectedServiceName) ?: false
    }

    fun isOverlayPermissionGranted(context: Context): Boolean {
        return android.provider.Settings.canDrawOverlays(context)
    }
}
