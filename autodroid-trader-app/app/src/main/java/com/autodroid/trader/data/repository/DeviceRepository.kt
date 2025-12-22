package com.autodroid.trader.data.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.autodroid.trader.data.database.DeviceProvider
import com.autodroid.trader.data.dao.DeviceEntity
import com.autodroid.trader.network.ApiClient
import com.autodroid.trader.network.DeviceCreateRequest
import com.google.gson.Gson
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
    private val gson = Gson() // Gson实例用于JSON序列化/反序列化

    /**
     * 获取或更新当前设备
     * 根据"本地优先"设计理念，主动检查设备状态并更新本地数据库
     */
    fun getAndSyncCurrentDevice(): LiveData<DeviceEntity?> {
        // 启动异步任务更新设备信息
        updateCurrentDevice()
        return deviceProvider.getCurrentDevice()
    }

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
                            Log.d("DeviceRepository", "准备调用API获取设备信息: ${currentDevice.serialNo}")
                            // 调用API获取设备信息
                            val deviceInfoResponse = apiClient.getDeviceInfo(currentDevice.serialNo)
                            Log.d("DeviceRepository", "API调用成功，返回: $deviceInfoResponse")
                            
                            // 根据服务器返回的信息更新本地数据库
                            // 使用Gson将应用列表转换为JSON字符串
                            val appsJson = deviceInfoResponse.apps?.let { apps ->
                                try {
                                    gson.toJson(apps)
                                } catch (e: Exception) {
                                    Log.e("DeviceRepository", "Error converting apps to JSON: ${e.message}")
                                    null
                                }
                            }

                            val updatedDevice = currentDevice.copy(
                                isOnline = true,
                                usbDebugEnabled = deviceInfoResponse.usb_debug_enabled ?: false,
                                wifiDebugEnabled = deviceInfoResponse.wifi_debug_enabled ?: false,
                                checkStatus = deviceInfoResponse.check_status ?: "UNKNOWN",
                                checkMessage = deviceInfoResponse.check_message,
                                checkTime = deviceInfoResponse.check_time?.let {
                                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                        .parse(it)?.time
                                },
                                apps = appsJson,
                                updatedAt = System.currentTimeMillis()
                            )
                            deviceProvider.updateDevice(updatedDevice)
                            Log.d("DeviceRepository", "设备信息同步成功: ${currentDevice.name}")
                        } catch (e: Exception) {
                            Log.e("DeviceRepository", "检查设备${currentDevice.name}状态时出错: ${e.message}")
                            // 检查失败时断开连接
                            deviceProvider.updateConnectionStatus(currentDevice.serialNo, false, System.currentTimeMillis())
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
     * 向服务器注册设备并更新本地数据库
     * @param deviceEntity 设备实体
     * @param apiEndpoint API端点
     * @return 更新后的设备实体
     */
    suspend fun registerDevice(deviceEntity: DeviceEntity, apiEndpoint: String): DeviceEntity {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DeviceRepository", "registerDevice: 开始注册设备到服务器")
                Log.d("DeviceRepository", "registerDevice: 设备信息 - 序列号: ${deviceEntity.serialNo}, 名称: ${deviceEntity.name}, IP: ${deviceEntity.ip}")
                Log.d("DeviceRepository", "registerDevice: API端点: $apiEndpoint")
                
                // 设置API端点
                apiClient.setApiEndpoint(apiEndpoint)
                
                // 创建设备注册请求
                val registrationRequest = DeviceCreateRequest(
                    serialNo = deviceEntity.serialNo, // 使用序列号作为主键发送到服务器
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
                
                // 根据服务器响应更新本地设备信息
                val updatedDevice = if (response.success) {
                    Log.d("DeviceRepository", "registerDevice: 设备注册成功，开始更新本地数据库")
                    
                    // 从响应中获取设备信息，转换为DeviceEntity
                    // 如果响应中包含设备信息，则使用响应中的信息更新本地设备
                    // 否则使用本地设备信息
                    try {
                        // 尝试从响应中获取设备ID等信息更新本地设备
                        val newDevice = deviceEntity.copy(
                            udid = response.serialNo ?: deviceEntity.udid,
                            userId = response.device?.let { "user_${it.serialNo}" } ?: deviceEntity.userId,
                            name = response.device?.name ?: deviceEntity.name,
                            model = response.device?.model ?: deviceEntity.model,
                            manufacturer = response.device?.manufacturer ?: deviceEntity.manufacturer,
                            androidVersion = response.device?.android_version ?: deviceEntity.androidVersion,
                            apiLevel = response.device?.api_level ?: deviceEntity.apiLevel,
                            platform = response.device?.platform ?: deviceEntity.platform,
                            brand = response.device?.brand ?: deviceEntity.brand,
                            device = response.device?.device ?: deviceEntity.device,
                            product = response.device?.product ?: deviceEntity.product,
                            ip = response.device?.ip ?: deviceEntity.ip,
                            screenWidth = response.device?.screen_width ?: deviceEntity.screenWidth,
                            screenHeight = response.device?.screen_height ?: deviceEntity.screenHeight,
                            isOnline = response.device?.isOnline() ?: true,
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        // 保存更新后的设备信息到本地数据库
                        deviceProvider.updateDevice(newDevice)
                        Log.d("DeviceRepository", "registerDevice: 本地数据库已更新，设备: ${newDevice.name}")
                        newDevice
                    } catch (e: Exception) {
                        Log.w("DeviceRepository", "registerDevice: 更新本地设备信息失败，返回原始设备信息: ${e.message}")
                        val fallbackDevice = deviceEntity.copy(
                            isOnline = true,
                            updatedAt = System.currentTimeMillis()
                        )
                        deviceProvider.updateDevice(fallbackDevice)
                        fallbackDevice
                    }
                } else {
                    Log.w("DeviceRepository", "registerDevice: 设备注册失败，响应消息: ${response.message}")
                    // 注册失败时，标记设备为离线状态
                    val failedDevice = deviceEntity.copy(
                        isOnline = false,
                        updatedAt = System.currentTimeMillis()
                    )
                    deviceProvider.updateDevice(failedDevice)
                    failedDevice
                }
                
                return@withContext updatedDevice
            } catch (e: Exception) {
                Log.e("DeviceRepository", "registerDevice: 注册设备失败: ${e.message}", e)
                
                // 注册失败时，标记设备为离线状态
                val failedDevice = deviceEntity.copy(
                    isOnline = false,
                    updatedAt = System.currentTimeMillis()
                )
                deviceProvider.updateDevice(failedDevice)
                
                throw e
            }
        }
    }
    
    /**
     * 检查设备调试权限并同步到本地数据库
     * @param deviceId 设备ID
     * @param apiEndpoint API端点
     * @return 更新后的设备实体
     */
    suspend fun checkDeviceWithServer(deviceId: String, apiEndpoint: String): DeviceEntity {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DeviceRepository", "checkDeviceWithServer: 开始检查设备调试权限")
                Log.d("DeviceRepository", "checkDeviceWithServer: 设备序列号: $deviceId")
                Log.d("DeviceRepository", "checkDeviceWithServer: API端点: $apiEndpoint")
                
                // 设置API端点
                apiClient.setApiEndpoint(apiEndpoint)
                
                // 调用API检查设备调试权限
                val response = apiClient.checkDevice(deviceId)
                Log.d("DeviceRepository", "checkDeviceWithServer: API调用完成，响应: $response")
                
                // 获取现有设备信息
                val existingDevice = deviceProvider.getDeviceById(deviceId)
                    ?: throw Exception("设备不存在: $deviceId")
                
                // 根据服务器响应更新本地设备信息
                val updatedDevice = if (response.success) {
                    existingDevice.copy(
                        isOnline = true,
                        usbDebugEnabled = response.usb_debug_enabled,
                        wifiDebugEnabled = response.wifi_debug_enabled,
                        checkStatus = "SUCCESS",
                        checkMessage = response.message,
                        checkTime = response.check_time?.let {
                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                .parse(it)?.time 
                        } ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    existingDevice.copy(
                        isOnline = false,
                        checkStatus = "FAILED",
                        checkMessage = response.message,
                        checkTime = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
                
                // 更新本地数据库
                deviceProvider.updateDevice(updatedDevice)
                Log.d("DeviceRepository", "checkDeviceWithServer: 设备信息已更新: ${updatedDevice.name}")
                
                // 返回更新后的设备实体
                return@withContext updatedDevice
                
            } catch (e: Exception) {
                Log.e("DeviceRepository", "checkDeviceWithServer: 检查设备调试权限失败: ${e.message}", e)
                
                // 获取现有设备信息并标记为检查失败
                val existingDevice = deviceProvider.getDeviceById(deviceId)
                if (existingDevice != null) {
                    val failedDevice = existingDevice.copy(
                        isOnline = false,
                        checkStatus = "FAILED",
                        checkMessage = "服务器检查失败: ${e.message}",
                        checkTime = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    deviceProvider.updateDevice(failedDevice)
                    return@withContext failedDevice
                }
                
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