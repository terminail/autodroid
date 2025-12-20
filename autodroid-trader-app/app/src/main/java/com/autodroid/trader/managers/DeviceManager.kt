// DeviceManager.kt
package com.autodroid.trader.managers

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.data.dao.DeviceEntity
import com.autodroid.trader.data.repository.DeviceRepository
import com.autodroid.trader.network.DeviceRegistrationResponse
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
        val localIp = NetworkUtils.getLocalIpAddress() ?: "Unknown"
        
        android.util.Log.d(TAG, "getLocalDevice: 设备名称 = $deviceName, Android版本 = $androidVersion, 本地IP = $localIp")
        
        // 创建设备信息对象
        val device = DeviceEntity.detailed(
            id = deviceId,
            ip = localIp,
            name = deviceName,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = androidVersion,
            platform = "Android",
            brand = Build.BRAND,
            device = Build.DEVICE,
            product = Build.PRODUCT
        )
        
        android.util.Log.d(TAG, "getLocalDevice: 设备信息创建完成，ID = ${device.id}")
        return device
    }
    val device: DeviceEntity
        get() = getLocalDevice()

    
    /**
     * 向服务器注册设备
     */
    suspend fun registerLocalDeviceWithServer(): DeviceRegistrationResponse {
        android.util.Log.d(TAG, "registerLocalDeviceWithServer: 开始注册本地设备到服务器")
        
        val localDevice = getLocalDevice()
        android.util.Log.d(TAG, "registerLocalDeviceWithServer: 获取到本地设备信息，ID = ${localDevice.id}")
        
        val currentServer = appViewModel.server.value
        
        if (currentServer == null) {
            android.util.Log.e(TAG, "registerLocalDeviceWithServer: 服务器信息为空，无法注册设备")
            throw Exception("请先连接服务器")
        }
        
        android.util.Log.d(TAG, "registerLocalDeviceWithServer: 当前服务器信息 - IP: ${currentServer.ip}, 端口: ${currentServer.port}")
        
        // 使用DeviceRepository向远程服务器注册设备
        // registerDevice方法会处理数据库的保存和更新
        val response = deviceRepository?.registerDevice(localDevice, currentServer.ip, currentServer.port)
            ?: throw Exception("设备仓库未初始化")
            
        android.util.Log.d(TAG, "registerLocalDeviceWithServer: 设备注册完成，响应: $response")
        return response
    }


    companion object {
        private const val TAG = "DeviceManager"
    }
}