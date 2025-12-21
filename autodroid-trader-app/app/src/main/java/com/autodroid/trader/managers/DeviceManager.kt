// DeviceManager.kt
package com.autodroid.trader.managers

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.data.dao.DeviceEntity
import com.autodroid.trader.data.repository.DeviceRepository
import com.autodroid.trader.network.DeviceCreateResponse
import com.autodroid.trader.utils.NetworkUtils

class DeviceManager(private val context: Context?, private val appViewModel: AppViewModel) {
    private var deviceRepository: DeviceRepository? = null
    
    /**
     * 设置设备仓库
     */
    fun setDeviceRepository(repository: DeviceRepository) {
        this.deviceRepository = repository
    }
    
    /**
     * 获取设备仓库
     */
    fun getDeviceRepository(): DeviceRepository? {
        return this.deviceRepository
    }

    /**
     * 获取本地设备信息
     */
    private fun getLocalDevice(): DeviceEntity {
        android.util.Log.d(TAG, "getLocalDevice: 开始获取本地设备信息")
        
        // 获取设备序列号作为唯一标识
        val deviceId = if (Build.SERIAL != null && Build.SERIAL != "unknown") {
            Build.SERIAL
        } else {
            // 如果序列号不可用，则使用Android ID作为备用
            if (context != null) {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                ) ?: "unknown_device_id"
            } else {
                "unknown_device_id"
            }
        }
        
        // 获取Android设备ID用于日志比较
        val androidId = if (context != null) {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } else {
            "unknown_android_id"
        }
        
        android.util.Log.d(TAG, "getLocalDevice: 设备ID = $deviceId")
        android.util.Log.d(TAG, "getLocalDevice: Android ID = $androidId")
        
        // 获取设备基本信息
        val deviceName = Build.MODEL
        val androidVersion = Build.VERSION.RELEASE
        val apiLevel = Build.VERSION.SDK_INT
        val localIp = NetworkUtils.getLocalIpAddress() ?: "Unknown"
        
        // 获取屏幕尺寸
        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        android.util.Log.d(TAG, "getLocalDevice: 设备名称 = $deviceName, Android版本 = $androidVersion, API级别 = $apiLevel, 本地IP = $localIp")
        android.util.Log.d(TAG, "getLocalDevice: 屏幕尺寸 = ${screenWidth}x${screenHeight}")
        
        // 创建设备信息对象
        val device = DeviceEntity.detailed(
            udid = deviceId,
            ip = localIp,
            name = deviceName,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = androidVersion,
            apiLevel = apiLevel,
            platform = "Android",
            brand = Build.BRAND,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        
        android.util.Log.d(TAG, "getLocalDevice: 设备信息创建完成，ID = ${device.udid}")
        return device
    }
    val device: DeviceEntity
        get() = getLocalDevice()

    
    /**
     * 向服务器注册设备
     */
    suspend fun registerLocalDeviceWithServer(): DeviceCreateResponse {
        android.util.Log.d(TAG, "registerLocalDeviceWithServer: 开始注册本地设备到服务器")
        
        val localDevice = getLocalDevice()
        android.util.Log.d(TAG, "registerLocalDeviceWithServer: 获取到本地设备信息，ID = ${localDevice.udid}")
        
        val currentServer = appViewModel.server.value
        
        if (currentServer == null) {
            android.util.Log.e(TAG, "registerLocalDeviceWithServer: 服务器信息为空，无法注册设备")
            throw Exception("请先连接服务器")
        }
        
        android.util.Log.d(TAG, "registerLocalDeviceWithServer: 当前服务器信息 - IP: ${currentServer.ip}, 端口: ${currentServer.port}")
        
        // 使用DeviceRepository向远程服务器注册设备
        // registerDevice方法会处理数据库的保存和更新
        val response = deviceRepository?.registerDevice(localDevice, currentServer.apiEndpoint())
            ?: throw Exception("设备仓库未初始化")
            
        android.util.Log.d(TAG, "registerLocalDeviceWithServer: 设备注册完成，响应: $response")
        return response
    }


    /**
     * 检查设备调试权限
     */
    suspend fun checkDeviceDebugPermissions(): com.autodroid.trader.network.DeviceDebugCheckResponse {
        android.util.Log.d(TAG, "checkDeviceDebugPermissions: 开始检查设备调试权限")
        
        val localDevice = getLocalDevice()
        android.util.Log.d(TAG, "checkDeviceDebugPermissions: 获取到本地设备信息，ID = ${localDevice.udid}")
        
        val currentServer = appViewModel.server.value
        
        if (currentServer == null) {
            android.util.Log.e(TAG, "checkDeviceDebugPermissions: 服务器信息为空，无法检查调试权限")
            throw Exception("请先连接服务器")
        }
        
        android.util.Log.d(TAG, "checkDeviceDebugPermissions: 当前服务器信息 - IP: ${currentServer.ip}, 端口: ${currentServer.port}")
        
        // 使用DeviceRepository检查设备调试权限
        val response = deviceRepository?.checkDeviceDebugPermissions(localDevice.udid, currentServer.apiEndpoint())
            ?: throw Exception("设备仓库未初始化")
            
        android.util.Log.d(TAG, "checkDeviceDebugPermissions: 调试权限检查完成，响应: $response")
        return response
    }

    companion object {
        private const val TAG = "DeviceManager"
    }
}