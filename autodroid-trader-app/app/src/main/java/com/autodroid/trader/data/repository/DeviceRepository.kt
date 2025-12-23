package com.autodroid.trader.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.autodroid.trader.MyApplication
import com.autodroid.trader.data.dao.DeviceEntity
import com.autodroid.trader.data.database.DeviceProvider
import com.autodroid.trader.managers.DeviceManager
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
class DeviceRepository private constructor(app: MyApplication) {

    private val TAG = "DeviceRepository"
    private val context = app
    private val myApplication = app
    private val deviceProvider = DeviceProvider.getInstance(app)
    private val gson = Gson() // Gson实例用于JSON序列化/反序列化

    /**
     * 获取或更新当前设备
     * 根据"本地优先"设计理念，主动检查设备状态并更新本地数据库
     */
    fun getAndSyncDevice(serialNo: String): LiveData<DeviceEntity?> {
        try {
            // 启动异步任务更新设备信息
            syncDevice(serialNo)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing device: ${e.message}")
        }
        return deviceProvider.getDeviceById(serialNo)
    }

    /**
     * 获取设备信息（不进行网络同步）
     */
    fun getDeviceById(serialNo: String): LiveData<DeviceEntity?> {
        return deviceProvider.getDeviceById(serialNo)
    }

    /**
     * 更新当前设备信息
     * 根据"本地优先"设计理念，主动检查设备状态并更新本地数据库
     */
    private fun syncDevice(serialNo: String) {
        val apiClient =
            myApplication.getApiClient() ?: throw Exception("API客户端未初始化")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("DeviceRepository", "准备调用API获取设备信息: $serialNo")
                // 调用API获取设备信息
                val deviceInfoResponse =
                    apiClient.getDeviceInfo(serialNo)
                Log.d("DeviceRepository", "API调用成功，返回: $deviceInfoResponse")

                // 首先获取现有设备信息
                val existingDevice = try {
                    deviceProvider.getDeviceByIdSync(serialNo)
                } catch (e: Exception) {
                    Log.e("DeviceRepository", "获取现有设备信息失败: ${e.message}")
                    null
                }

                // 根据服务器返回的信息更新本地数据库
                // 使用Gson将应用列表转换为JSON字符串
                val appsJson = deviceInfoResponse.apps?.let { apps ->
                    try {
                        gson.toJson(apps)
                    } catch (e: Exception) {
                        Log.e("DeviceRepository", "Error converting apps to JSON: ${e.message}")
                        existingDevice?.apps
                    }
                } ?: existingDevice?.apps

                // 如果存在现有设备，只更新服务器返回的字段，保留其他现有信息
                val updatedDevice = if (existingDevice != null) {
                    existingDevice.copy(
                        udid = deviceInfoResponse.udid ?: existingDevice.udid,
                        name = deviceInfoResponse.name ?: existingDevice.name, // 更新设备名称，优先使用服务器返回的名称
                        model = deviceInfoResponse.model ?: existingDevice.model,
                        manufacturer = deviceInfoResponse.manufacturer ?: existingDevice.manufacturer,
                        androidVersion = deviceInfoResponse.androidVersion ?: existingDevice.androidVersion,
                        apiLevel = deviceInfoResponse.apiLevel ?: existingDevice.apiLevel,
                        platform = deviceInfoResponse.platform ?: existingDevice.platform,
                        brand = deviceInfoResponse.brand ?: existingDevice.brand,
                        device = deviceInfoResponse.device ?: existingDevice.device,
                        product = deviceInfoResponse.product ?: existingDevice.product,
                        ip = deviceInfoResponse.ip ?: existingDevice.ip,
                        screenWidth = deviceInfoResponse.screenWidth ?: existingDevice.screenWidth,
                        screenHeight = deviceInfoResponse.screenHeight ?: existingDevice.screenHeight,
                        isOnline = true,
                        usbDebugEnabled = deviceInfoResponse.usbDebugEnabled ?: existingDevice.usbDebugEnabled,
                        wifiDebugEnabled = deviceInfoResponse.wifiDebugEnabled ?: existingDevice.wifiDebugEnabled,
                        checkStatus = deviceInfoResponse.checkStatus ?: existingDevice.checkStatus,
                        checkMessage = deviceInfoResponse.checkMessage ?: existingDevice.checkMessage,
                        checkTime = deviceInfoResponse.checkTime?.let {
                            java.text.SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                java.util.Locale.getDefault()
                            )
                                .parse(it)?.time
                        } ?: existingDevice.checkTime,
                        apps = appsJson,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    // 如果没有现有设备，创建新设备
                    // 尝试从本地设备管理器获取设备名称，如果没有则使用服务器返回的名称
                    val localDeviceName = try {
                        val appViewModel = myApplication.getAppViewModel()
                        val deviceManager = DeviceManager.getInstance(context, appViewModel)
                        deviceManager.localDevice.name
                    } catch (e: Exception) {
                        Log.e("DeviceRepository", "获取本地设备名称失败: ${e.message}")
                        null
                    }
                    
                    DeviceEntity(
                        serialNo = serialNo,
                        udid = deviceInfoResponse.udid,
                        name = localDeviceName ?: deviceInfoResponse.name ?: "Unknown Device",
                        model = deviceInfoResponse.model,
                        manufacturer = deviceInfoResponse.manufacturer,
                        androidVersion = deviceInfoResponse.androidVersion ?: "Unknown",
                        apiLevel = deviceInfoResponse.apiLevel,
                        platform = deviceInfoResponse.platform ?: "Android",
                        brand = deviceInfoResponse.brand,
                        device = deviceInfoResponse.device,
                        product = deviceInfoResponse.product,
                        ip = deviceInfoResponse.ip,
                        screenWidth = deviceInfoResponse.screenWidth,
                        screenHeight = deviceInfoResponse.screenHeight,
                        isOnline = true,
                        usbDebugEnabled = deviceInfoResponse.usbDebugEnabled ?: false,
                        wifiDebugEnabled = deviceInfoResponse.wifiDebugEnabled ?: false,
                        checkStatus = deviceInfoResponse.checkStatus ?: "UNKNOWN",
                        checkMessage = deviceInfoResponse.checkMessage,
                        checkTime = deviceInfoResponse.checkTime?.let {
                            java.text.SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                java.util.Locale.getDefault()
                            )
                                .parse(it)?.time
                        },
                        apps = appsJson,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                deviceProvider.updateDevice(updatedDevice)
                Log.d("DeviceRepository", "设备信息同步成功: $serialNo")
            } catch (e: Exception) {
                Log.e("DeviceRepository", "检查设备$serialNo 状态时出错: ${e.message}")
                // 检查失败时断开连接，更新设备状态为离线
                try {
                    val existingDevice = deviceProvider.getDeviceByIdSync(serialNo)
                    if (existingDevice != null) {
                        val offlineDevice = existingDevice.copy(
                            isOnline = false,
                            updatedAt = System.currentTimeMillis()
                        )
                        deviceProvider.updateDevice(offlineDevice)
                    }
                } catch (ex: Exception) {
                    Log.e("DeviceRepository", "更新设备离线状态失败: ${ex.message}")
                }

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
     * @return 更新后的设备实体
     */
    suspend fun registerDevice(deviceEntity: DeviceEntity): DeviceEntity? {
        val apiClient =
            myApplication.getApiClient() ?: throw Exception("API客户端未初始化")

        return withContext(Dispatchers.IO) {
            try {
                Log.d("DeviceRepository", "registerDevice: 开始注册设备到服务器")
                Log.d(
                    "DeviceRepository",
                    "registerDevice: 设备信息 - 序列号: ${deviceEntity.serialNo}, 名称: ${deviceEntity.name}, IP: ${deviceEntity.ip}"
                )

                // 创建设备注册请求
                // 只包含序列号，其他属性将由服务器通过ADB获取
                val registrationRequest = DeviceCreateRequest(
                    serialNo = deviceEntity.serialNo // 使用序列号作为主键发送到服务器
                )

                Log.d("DeviceRepository", "registerDevice: 设备注册请求已创建")

                // 调用API注册设备
                val deviceCreateResponse =
                    apiClient.registerDevice(registrationRequest)
                Log.d("DeviceRepository", "registerDevice: API调用完成，响应: $deviceCreateResponse")

                // 根据服务器响应更新本地设备信息
                val updatedDevice = if (deviceCreateResponse.success) {
                    Log.d("DeviceRepository", "registerDevice: 设备注册成功，开始更新本地数据库")

                    // 从响应中获取设备信息，转换为DeviceEntity
                    // 如果响应中包含设备信息，则使用响应中的信息更新本地设备
                    // 否则使用本地设备信息
                    try {
                        // 尝试从响应中获取设备ID等信息更新本地设备
                        val newDevice = deviceEntity.copy(
                            udid = deviceCreateResponse.serialNo ?: deviceEntity.udid,
                            userId = deviceCreateResponse.device?.let { "user_${it.serialNo}" }
                                ?: deviceEntity.userId,
                            name = deviceCreateResponse.device?.name ?: deviceEntity.name,
                            model = deviceCreateResponse.device?.model ?: deviceEntity.model,
                            manufacturer = deviceCreateResponse.device?.manufacturer
                                ?: deviceEntity.manufacturer,
                            androidVersion = deviceCreateResponse.device?.androidVersion
                                ?: deviceEntity.androidVersion,
                            apiLevel = deviceCreateResponse.device?.apiLevel
                                ?: deviceEntity.apiLevel,
                            platform = deviceCreateResponse.device?.platform
                                ?: deviceEntity.platform,
                            brand = deviceCreateResponse.device?.brand ?: deviceEntity.brand,
                            device = deviceCreateResponse.device?.device ?: deviceEntity.device,
                            product = deviceCreateResponse.device?.product ?: deviceEntity.product,
                            ip = deviceCreateResponse.device?.ip ?: deviceEntity.ip,
                            screenWidth = deviceCreateResponse.device?.screenWidth
                                ?: deviceEntity.screenWidth,
                            screenHeight = deviceCreateResponse.device?.screenHeight
                                ?: deviceEntity.screenHeight,
                            isOnline = deviceCreateResponse.device?.isOnline() ?: true,
                            updatedAt = System.currentTimeMillis()
                        )

                        // 保存更新后的设备信息到本地数据库
                        deviceProvider.updateDevice(newDevice)
                        Log.d(
                            "DeviceRepository",
                            "registerDevice: 本地数据库已更新，设备: ${newDevice.name}"
                        )
                        
                        // 通知AppViewModel刷新设备信息
                        try {
                            val appViewModel = MyApplication.getInstance().getAppViewModel()
                            appViewModel.refreshDeviceInfo()
                            Log.d("DeviceRepository", "registerDevice: 已通知AppViewModel刷新设备信息")
                        } catch (e: Exception) {
                            Log.e("DeviceRepository", "registerDevice: 通知AppViewModel刷新失败: ${e.message}")
                        }
                        newDevice
                    } catch (e: Exception) {
                        Log.w(
                            "DeviceRepository",
                            "registerDevice: 更新本地设备信息失败，返回原始设备信息: ${e.message}"
                        )
                        val fallbackDevice = deviceEntity.copy(
                            isOnline = true,
                            updatedAt = System.currentTimeMillis()
                        )
                        deviceProvider.updateDevice(fallbackDevice)
                        fallbackDevice
                    }
                } else {
                    Log.w(
                        "DeviceRepository",
                        "registerDevice: 设备注册失败，响应消息: ${deviceCreateResponse.message}"
                    )
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
     * @return 更新后的设备实体
     */
    suspend fun checkDeviceWithServer(deviceId: String): DeviceEntity? {
        val apiClient = myApplication.getApiClient() ?: throw Exception("API客户端未初始化")

        return withContext(Dispatchers.IO) {
            try {
                Log.d("DeviceRepository", "checkDeviceWithServer: 开始检查设备调试权限")
                Log.d("DeviceRepository", "checkDeviceWithServer: 设备序列号: $deviceId")

                // 调用API检查设备调试权限
                val response = apiClient.checkDevice(deviceId)
                    ?: throw Exception("API调用失败")
                Log.d("DeviceRepository", "checkDeviceWithServer: API调用完成，响应: $response")

                // 获取现有设备信息
                val existingDevice = try {
                    // 使用同步方法从数据库获取设备信息
                    deviceProvider.getDeviceByIdSync(deviceId) ?: run {
                        // 如果数据库中没有设备信息，尝试从本地设备管理器获取
                        Log.d("DeviceRepository", "checkDeviceWithServer: 数据库中没有设备信息，尝试从本地设备管理器获取")
                        try {
                            val appViewModel = myApplication.getAppViewModel()
                            val deviceManager = DeviceManager.getInstance(context, appViewModel)
                            deviceManager.localDevice
                        } catch (e: Exception) {
                            Log.e("DeviceRepository", "checkDeviceWithServer: 从本地设备管理器获取设备信息失败: ${e.message}")
                            // 最后的备用方案，创建一个基本的设备对象
                            DeviceEntity(
                                serialNo = deviceId,
                                name = "Unknown Device",
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DeviceRepository", "checkDeviceWithServer: 获取设备信息失败: ${e.message}")
                    // 如果获取失败，尝试从本地设备管理器获取
                    try {
                        val appViewModel = myApplication.getAppViewModel()
                        val deviceManager = DeviceManager.getInstance(context, appViewModel)
                        deviceManager.localDevice
                    } catch (ex: Exception) {
                        Log.e("DeviceRepository", "checkDeviceWithServer: 从本地设备管理器获取设备信息也失败: ${ex.message}")
                        // 最后的备用方案，创建一个基本的设备对象
                        DeviceEntity(
                            serialNo = deviceId,
                            name = "Unknown Device",
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                }

                // 根据服务器响应更新本地设备信息
                val updatedDevice = if (response.success) {
                    // 将服务器返回的已安装应用列表转换为JSON字符串
                    val appsJson = response.installedApps?.let { apps ->
                        try {
                            gson.toJson(apps)
                        } catch (e: Exception) {
                            Log.e("DeviceRepository", "Error converting installed apps to JSON: ${e.message}")
                            existingDevice.apps
                        }
                    } ?: existingDevice.apps
                    
                    // 如果服务器返回了设备详细信息，使用这些信息更新本地设备
                    if (response.deviceInfo != null) {
                        val deviceInfo = response.deviceInfo
                        existingDevice.copy(
                            udid = deviceInfo.udid ?: existingDevice.udid,
                            name = deviceInfo.name ?: existingDevice.name,
                            model = deviceInfo.model ?: existingDevice.model,
                            manufacturer = deviceInfo.manufacturer ?: existingDevice.manufacturer,
                            androidVersion = deviceInfo.androidVersion ?: existingDevice.androidVersion,
                            apiLevel = deviceInfo.apiLevel ?: existingDevice.apiLevel,
                            platform = deviceInfo.platform ?: existingDevice.platform,
                            brand = deviceInfo.brand ?: existingDevice.brand,
                            device = deviceInfo.device ?: existingDevice.device,
                            product = deviceInfo.product ?: existingDevice.product,
                            ip = deviceInfo.ip ?: existingDevice.ip,
                            screenWidth = deviceInfo.screenWidth ?: existingDevice.screenWidth,
                            screenHeight = deviceInfo.screenHeight ?: existingDevice.screenHeight,
                            isOnline = true,
                            usbDebugEnabled = response.usbDebugEnabled,
                            wifiDebugEnabled = response.wifiDebugEnabled,
                            checkStatus = "SUCCESS",
                            checkMessage = response.message,
                            checkTime = response.checkTime?.let {
                            try {
                                // 使用更灵活的解析方式，处理ISO格式日期
                                // 先移除末尾的Z，然后解析
                                val dateWithoutZ = it.removeSuffix("Z")
                                val formats = arrayOf(
                                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                                    "yyyy-MM-dd'T'HH:mm:ss.SSSSS",
                                    "yyyy-MM-dd'T'HH:mm:ss.SSSS",
                                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                                    "yyyy-MM-dd'T'HH:mm:ss.SS",
                                    "yyyy-MM-dd'T'HH:mm:ss.S",
                                    "yyyy-MM-dd'T'HH:mm:ss"
                                )
                                
                                for (format in formats) {
                                    try {
                                        return@let java.text.SimpleDateFormat(
                                            format,
                                            java.util.Locale.getDefault()
                                        ).parse(dateWithoutZ)?.time
                                    } catch (e: Exception) {
                                        // 继续尝试下一个格式
                                    }
                                }
                                
                                // 如果所有格式都失败，返回当前时间
                                System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                        } ?: System.currentTimeMillis(),
                            apps = appsJson,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        // 如果没有设备详细信息，只更新检查状态
                        existingDevice.copy(
                            isOnline = true,
                            usbDebugEnabled = response.usbDebugEnabled,
                            wifiDebugEnabled = response.wifiDebugEnabled,
                            checkStatus = "SUCCESS",
                            checkMessage = response.message,
                            checkTime = response.checkTime?.let {
                                java.text.SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    java.util.Locale.getDefault()
                                )
                                    .parse(it)?.time
                            } ?: System.currentTimeMillis(),
                            apps = appsJson,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                } else {
                    existingDevice.copy(
                        isOnline = false,
                        checkStatus = "FAILED",
                        checkMessage = response.message,
                        checkTime = System.currentTimeMillis(),
                        apps = existingDevice.apps,
                        updatedAt = System.currentTimeMillis()
                    )
                }

                // 更新本地数据库
                deviceProvider.updateDevice(updatedDevice)
                Log.d(
                    "DeviceRepository",
                    "checkDeviceWithServer: 设备信息已更新: ${updatedDevice.name}"
                )

                // 返回更新后的设备实体
                return@withContext updatedDevice

            } catch (e: Exception) {
                Log.e(
                    "DeviceRepository",
                    "checkDeviceWithServer: 检查设备调试权限失败: ${e.message}",
                    e
                )

                // 获取现有设备信息并标记为检查失败
                val deviceLiveData = deviceProvider.getDeviceById(deviceId)
                val existingDevice = deviceLiveData.value
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
        fun getInstance(application: MyApplication): DeviceRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DeviceRepository(application)
                INSTANCE = instance
                instance
            }
        }
    }
}