package com.autodroid.trader.data.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.autodroid.trader.data.database.DeviceProvider
import com.autodroid.trader.data.dao.DeviceEntity
import com.autodroid.trader.network.ApiClient
import com.autodroid.trader.network.DeviceCreateRequest
import com.autodroid.trader.network.DeviceCreateResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设备仓库类
 * 提供对设备数据的统一访问接口，协调数据源和业务逻辑
 */
class DeviceRepository private constructor(application: Application) {
    
    private val deviceProvider = DeviceProvider.getInstance(application)
    private val context = application.applicationContext
    private val apiClient = ApiClient.getInstance()
    
    /**
     * 更新当前设备信息
     * 根据"本地优先"设计理念，主动检查设备状态并更新本地数据库
     */
    private fun updateCurrentDevice() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 获取当前设备信息
                val currentDevice = deviceProvider.getCurrentDevice().value
                Log.d("DeviceRepository", "updateCurrentDevice: currentDevice = $currentDevice")
                if (currentDevice != null) {
                    Log.d("DeviceRepository", "updateCurrentDevice: 设备 ${currentDevice.name} 在线状态 = ${currentDevice.isOnline}")
                    // 如果设备已连接到服务器，检查服务器状态
                    if (currentDevice.isOnline) {
                        try {
                            Log.d("DeviceRepository", "准备调用API获取设备信息: ${currentDevice.udid}")
                            // 调用API获取设备信息
                            val deviceInfoResponse = apiClient.getDeviceInfo(currentDevice.udid)
                            Log.d("DeviceRepository", "API调用成功，返回: $deviceInfoResponse")
                            
                            // 根据服务器返回的信息更新本地数据库
                            if (deviceInfoResponse != null) {
                                val updatedDevice = currentDevice.copy(
                                    isOnline = true,
                                    usbDebugEnabled = deviceInfoResponse.usb_debug_enabled ?: false,
                                    wifiDebugEnabled = deviceInfoResponse.wifi_debug_enabled ?: false,
                                    debugCheckStatus = deviceInfoResponse.debug_check_status ?: "UNKNOWN",
                                    debugCheckMessage = deviceInfoResponse.debug_check_message,
                                    debugCheckTime = deviceInfoResponse.debug_check_time?.let {
                                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                            .parse(it)?.time 
                                    },
                                    updatedAt = System.currentTimeMillis()
                                )
                                deviceProvider.updateDevice(updatedDevice)
                                Log.d("DeviceRepository", "设备信息同步成功: ${currentDevice.name}")
                            } else {
                                // 服务器不可用，断开连接
                                deviceProvider.updateConnectionStatus(currentDevice.udid, false, System.currentTimeMillis())
                                Log.w("DeviceRepository", "设备信息获取失败: ${currentDevice.name}")
                            }
                        } catch (e: Exception) {
                            Log.e("DeviceRepository", "检查设备${currentDevice.name}状态时出错: ${e.message}")
                            // 检查失败时断开连接
                            deviceProvider.updateConnectionStatus(currentDevice.udid, false, System.currentTimeMillis())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DeviceRepository", "检查设备状态时出错: ${e.message}")
            }
        }
    }

    /**
     * 更新设备信息
     */
    suspend fun update(deviceEntity: DeviceEntity) {
        deviceProvider.updateDevice(deviceEntity)
    }
    
    /**
     * 获取所有设备
     */
    fun getAllDevices(): LiveData<List<DeviceEntity>> {
        return deviceProvider.getAllDevices()
    }
    
    /**
     * 获取或更新当前设备
     * 根据"本地优先"设计理念，主动检查设备状态并更新本地数据库
     */
    fun getOrUpdateCurrentDevice(): LiveData<DeviceEntity?> {
        // 启动异步任务更新设备信息
        updateCurrentDevice()
        return deviceProvider.getCurrentDevice()
    }

    /**
     * 根据设备ID获取设备
     */
    suspend fun getDeviceById(id: String): DeviceEntity? {
        return withContext(Dispatchers.IO) {
            deviceProvider.getDeviceById(id)
        }
    }
    
    /**
     * 插入或更新设备
     */
    suspend fun insertOrUpdateDevice(deviceEntity: DeviceEntity): String {
        return withContext(Dispatchers.IO) {
            try {
                // 检查设备是否已存在
                val existingDevice = deviceProvider.getDeviceById(deviceEntity.udid)
                
                if (existingDevice != null) {
                    // 更新现有设备
                    deviceProvider.updateDevice(deviceEntity)
                    Log.d("DeviceRepository", "设备已更新: ${deviceEntity.name}")
                    return@withContext "设备已更新: ${deviceEntity.name}"
                } else {
                    // 插入新设备
                    deviceProvider.insertOrUpdateDevice(deviceEntity)
                    Log.d("DeviceRepository", "新设备已添加: ${deviceEntity.name}")
                    return@withContext "新设备已添加: ${deviceEntity.name}"
                }
            } catch (e: Exception) {
                Log.e("DeviceRepository", "插入或更新设备失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 删除设备
     */
    suspend fun deleteDevice(deviceId: String) {
        withContext(Dispatchers.IO) {
            deviceProvider.deleteDeviceById(deviceId)
        }
    }
    
    /**
     * 向服务器注册设备
     */
    suspend fun registerDevice(deviceEntity: DeviceEntity, apiEndpoint: String): DeviceCreateResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DeviceRepository", "registerDevice: 开始注册设备到服务器")
                Log.d("DeviceRepository", "registerDevice: 设备信息 - ID: ${deviceEntity.udid}, 名称: ${deviceEntity.name}, IP: ${deviceEntity.ip}")
                Log.d("DeviceRepository", "registerDevice: API端点: $apiEndpoint")
                
                // 设置API端点
                apiClient.setApiEndpoint(apiEndpoint)
                
                // 创建设备注册请求
                val registrationRequest = DeviceCreateRequest(
                    udid = deviceEntity.udid,
                    name = deviceEntity.name ?: "Unknown Device",
                    model = deviceEntity.model,
                    manufacturer = deviceEntity.manufacturer,
                    android_version = deviceEntity.androidVersion,
                    api_level = deviceEntity.apiLevel,
                    platform = deviceEntity.platform ?: "Android",
                    brand = deviceEntity.brand,
                    device = deviceEntity.device,
                    product = deviceEntity.product,
                    ip = deviceEntity.ip,
                    screen_width = deviceEntity.screenWidth,
                    screen_height = deviceEntity.screenHeight
                )
                
                Log.d("DeviceRepository", "registerDevice: 设备注册请求已创建")
                
                // 调用API注册设备
                val response = apiClient.registerDevice(registrationRequest)
                Log.d("DeviceRepository", "registerDevice: API调用完成，响应: $response")
                
                // 注册成功后更新本地数据库
                if (response.success) {
                    Log.d("DeviceRepository", "registerDevice: 设备注册成功，开始更新本地数据库")
                    val updatedDevice = deviceEntity.copy(
                        isOnline = true,
                        updatedAt = System.currentTimeMillis()
                    )
                    deviceProvider.updateDevice(updatedDevice)
                    Log.d("DeviceRepository", "registerDevice: 本地数据库已更新，设备: ${deviceEntity.name}")
                } else {
                    Log.w("DeviceRepository", "registerDevice: 设备注册失败，响应消息: ${response.message}")
                }
                
                return@withContext response
            } catch (e: Exception) {
                Log.e("DeviceRepository", "registerDevice: 注册设备失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 检查设备调试权限
     */
    suspend fun checkDeviceDebugPermissions(deviceId: String, apiEndpoint: String): com.autodroid.trader.network.DeviceDebugCheckResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DeviceRepository", "checkDeviceDebugPermissions: 开始检查设备调试权限")
                Log.d("DeviceRepository", "checkDeviceDebugPermissions: 设备ID: $deviceId")
                Log.d("DeviceRepository", "checkDeviceDebugPermissions: API端点: $apiEndpoint")
                
                // 设置API端点
                apiClient.setApiEndpoint(apiEndpoint)
                
                // 调用API检查设备调试权限
                val response = apiClient.checkDeviceDebugPermissions(deviceId)
                Log.d("DeviceRepository", "checkDeviceDebugPermissions: API调用完成，响应: $response")
                
                // 更新本地设备信息
                if (response.success) {
                    val existingDevice = deviceProvider.getDeviceById(deviceId)
                    if (existingDevice != null) {
                        val updatedDevice = existingDevice.copy(
                            isOnline = true,
                            usbDebugEnabled = response.usb_debug_enabled,
                            wifiDebugEnabled = response.wifi_debug_enabled,
                            debugCheckStatus = "SUCCESS",
                            debugCheckMessage = response.message,
                            debugCheckTime = response.check_time?.let {
                                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                    .parse(it)?.time 
                            } ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        deviceProvider.updateDevice(updatedDevice)
                        Log.d("DeviceRepository", "设备调试权限信息已更新: ${updatedDevice.name}")
                    }
                }
                
                return@withContext response
            } catch (e: Exception) {
                Log.e("DeviceRepository", "checkDeviceDebugPermissions: 检查设备调试权限失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 更新设备连接状态
     */
    suspend fun updateConnectionStatus(deviceId: String, isConnected: Boolean) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            deviceProvider.updateConnectionStatus(deviceId, isConnected, now)
        }
    }
    
    /**
     * 断开所有设备的连接
     */
    suspend fun disconnectAllDevices() {
        withContext(Dispatchers.IO) {
            deviceProvider.disconnectAllDevices()
        }
    }
    
    /**
     * 获取设备数量
     */
    suspend fun getDeviceCount(): Int {
        return withContext(Dispatchers.IO) {
            deviceProvider.getDeviceCount()
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: DeviceRepository? = null
        
        /**
         * 获取设备仓库实例（单例模式）
         */
        fun getInstance(application: Application): DeviceRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DeviceRepository(application)
                INSTANCE = instance
                instance
            }
        }
    }
}