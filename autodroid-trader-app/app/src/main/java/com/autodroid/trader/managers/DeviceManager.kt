// DeviceManager.kt
package com.autodroid.trader.managers

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.data.dao.DeviceEntity
import com.autodroid.trader.data.repository.DeviceRepository
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
     * 获取设备UDID (唯一标识符)
     * 注意：此方法现在主要作为备用标识符，主标识符为设备序列号
     * 
     * @param context 应用程序上下文
     * @return 设备UDID字符串，URL安全且不包含问题字符
     */
    private fun getDeviceUDID(context: Context): String {
        return try {
            // 使用设备属性组合创建唯一标识符，避免ANDROID_ID隐私问题
            val deviceId = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.VERSION.RELEASE}_${Build.ID}"
            
            // 确保UDID是URL安全的，移除问题字符
            makeSerialNoUrlSafe(deviceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device UDID", e)
            "Unknown-UDID"
        }
    }
    
    /**
     * 使序列号URL安全，移除问题字符
     */
    private fun makeSerialNoUrlSafe(serialNo: String): String {
        // 移除可能在URL中导致问题的字符
        return serialNo.replace(":", "")
                  .replace("/", "")
                  .replace("\\", "")
                  .replace("?", "")
                  .replace("&", "")
                  .replace("=", "")
                  .replace("#", "")
                  .replace("%", "")
                  .replace(" ", "")
    }
    
    /**
     * 通过系统属性获取设备序列号
     * 这个方法尝试通过系统属性获取与 adb devices 相同的序列号
     */
    private fun getSerialFromSystemProperty(): String? {
        return try {
            // 尝试多个系统属性路径
            val serialProperties = listOf(
                "ro.serialno",      // 最常见的序列号属性
                "sys.serialnumber", // 备用序列号属性
                "ro.boot.serialno"  // 启动时的序列号属性
            )
            
            for (prop in serialProperties) {
                val serial = getSystemProperty(prop)
                if (!serial.isNullOrEmpty() && serial != "unknown") {
                    Log.d(TAG, "通过系统属性 $prop 获取到序列号: $serial")
                    return serial
                }
            }
            
            Log.d(TAG, "无法通过系统属性获取序列号")
            null
        } catch (e: Exception) {
            Log.e(TAG, "获取系统属性时出错: ${e.message}", e)
            null
        }
    }
    
    /**
     * 获取系统属性
     * 注意：此方法使用反射访问内部API，可能不适用于所有设备或Android版本
     * 已包含多种错误处理和回退机制
     */
    private fun getSystemProperty(property: String): String? {
        return try {
            // 尝试使用反射调用 SystemProperties.get
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            val result = get.invoke(null, property) as? String
            
            // 验证结果是否有效
            if (result.isNullOrEmpty() || result == "unknown") {
                Log.w(TAG, "系统属性 $property 返回空值或unknown")
                null
            } else {
                Log.d(TAG, "成功获取系统属性 $property: $result")
                result
            }
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "无法找到SystemProperties类，可能是Android版本兼容性问题")
            null
        } catch (e: NoSuchMethodException) {
            Log.w(TAG, "无法找到SystemProperties.get方法，可能是Android版本兼容性问题")
            null
        } catch (e: Exception) {
            Log.e(TAG, "获取系统属性 $property 时出错: ${e.message}", e)
            null
        }
    }

    /**
     * 获取本地设备信息
     */
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    private fun getLocalDevice(): DeviceEntity {
        Log.d(TAG, "getLocalDevice: 开始获取本地设备信息")
        
        // 获取设备序列号作为主键
        val serialNo = getSerialNo()
        
        // 获取设备UDID作为备用标识符
        val udid = getUdid()
        
        Log.d(TAG, "getLocalDevice: 设备序列号 = $serialNo")
        Log.d(TAG, "getLocalDevice: 设备UDID = $udid")
        
        // 获取设备基本信息
        val deviceName = Build.MODEL
        val androidVersion = Build.VERSION.RELEASE
        val apiLevel = Build.VERSION.SDK_INT
        val localIp = NetworkUtils.getLocalIpAddress() ?: "Unknown"
        
        // 获取屏幕尺寸
        val screenWidth: Int
        val screenHeight: Int
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 使用新的API (Android 11+)
            val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val bounds = windowManager?.currentWindowMetrics?.bounds
            screenWidth = bounds?.width() ?: 0
            screenHeight = bounds?.height() ?: 0
        } else {
            // 使用已弃用的API (Android 10及以下)
            @Suppress("DEPRECATION")
            val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
        }
        
        Log.d(TAG, "getLocalDevice: 设备名称 = $deviceName, Android版本 = $androidVersion, API级别 = $apiLevel, 本地IP = $localIp")
        Log.d(TAG, "getLocalDevice: 屏幕尺寸 = ${screenWidth}x${screenHeight}")
        
        // 创建设备信息对象
        val device = DeviceEntity.detailed(
            serialNo = serialNo ?: "unknown_serial",
            udid = udid,
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
        
        Log.d(TAG, "getLocalDevice: 设备信息创建完成，序列号 = ${device.serialNo}, UDID = ${device.udid}")
        return device
    }

    private fun getUdid(): String = try {
        // 使用设备属性组合创建唯一标识符，避免ANDROID_ID隐私问题
        if (context != null) {
            getDeviceUDID(context)
        } else {
            "unknown_udid"
        }
    } catch (e: Exception) {
        Log.e(TAG, "获取设备UDID时出错: ${e.message}", e)
        "unknown_udid"
    }

    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    private fun getSerialNo(): String? = try {
        // 尝试多种方式获取设备序列号
        when {
            // 1. 优先使用系统属性获取序列号 (不需要特殊权限)
            getSerialFromSystemProperty()?.isNotEmpty() == true -> {
                Log.d(TAG, "通过系统属性获取设备序列号")
                getSerialFromSystemProperty()
            }
            // 2. 尝试使用 Build.getSerial() (需要 READ_PRIVILEGED_PHONE_STATE 权限)
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && // 只在Android 10以下尝试
                    Build.getSerial() != null &&
                    Build.getSerial() != "unknown" -> {
                Log.d(TAG, "使用 Build.getSerial() 获取设备序列号")
                Build.getSerial()
            }
            // 3. 最后使用组合UDID作为序列号
            else -> {
                Log.d(TAG, "使用组合UDID作为设备序列号")
                if (context != null) {
                    getDeviceUDID(context)
                } else {
                    "unknown_device_id"
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "获取设备序列号时出错: ${e.message}", e)
        if (context != null) {
            getDeviceUDID(context)
        } else {
            "unknown_device_id"
        }
    }

    val device: DeviceEntity
        @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
        get() = getLocalDevice()

    
    /**
     * 向服务器注册设备
     * @return 注册后的设备实体信息
     */
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    suspend fun registerLocalDeviceWithServer(): DeviceEntity {
        Log.d(TAG, "registerLocalDeviceWithServer: 开始注册本地设备到服务器")
        
        val localDevice = getLocalDevice()
        Log.d(TAG, "registerLocalDeviceWithServer: 获取到本地设备信息，序列号 = ${localDevice.serialNo}, UDID = ${localDevice.udid}")
        
        val currentServer = appViewModel.server.value
        
        if (currentServer == null) {
            Log.e(TAG, "registerLocalDeviceWithServer: 服务器信息为空，无法注册设备")
            throw Exception("请先连接服务器")
        }
        
        Log.d(TAG, "registerLocalDeviceWithServer: 当前服务器信息 - IP: ${currentServer.ip}, 端口: ${currentServer.port}")
        
        // 使用DeviceRepository向远程服务器注册设备
        // registerDevice方法会处理API调用和本地数据库同步，并返回更新后的DeviceEntity
        val updatedDevice = deviceRepository?.registerDevice(localDevice)
            ?: throw Exception("设备仓库未初始化")
            
        Log.d(TAG, "registerLocalDeviceWithServer: 设备注册完成，设备信息已更新")
        return updatedDevice
    }


    /**
     * 请求服务器检查设备调试权限、安装支持的应用等情况
     * @return 服务器检查后的设备实体信息
     */
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    suspend fun checkLocalDeviceWithServer(): DeviceEntity {
        Log.d(TAG, "checkLocalDeviceWithServer: 开始请求服务器检查设备状态")
        
        // 获取本地设备信息
        val localDevice = getLocalDevice()
        Log.d(TAG, "checkLocalDeviceWithServer: 获取到本地设备信息，序列号 = ${localDevice.serialNo}")
        
        val currentServer = appViewModel.server.value
        
        if (currentServer == null) {
            Log.e(TAG, "checkLocalDeviceWithServer: 服务器信息为空，无法检查设备状态")
            throw Exception("请先连接服务器")
        }
        
        Log.d(TAG, "checkLocalDeviceWithServer: 当前服务器信息 - IP: ${currentServer.ip}, 端口: ${currentServer.port}")
        
        try {
            // 调用DeviceRepository向服务器请求设备检查，Repository负责API调用和本地数据库同步
            val updatedDevice = deviceRepository?.checkDeviceWithServer(localDevice.serialNo)
                ?: throw Exception("设备仓库未初始化")
            
            Log.d(TAG, "checkLocalDeviceWithServer: 服务器检查完成，设备状态已更新")
            return updatedDevice
            
        } catch (e: Exception) {
            Log.e(TAG, "checkLocalDeviceWithServer: 服务器检查失败: ${e.message}", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "DeviceManager"
    }
}