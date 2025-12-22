// DeviceManager.kt
package com.autodroid.trader.managers

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.data.dao.DeviceEntity
import com.autodroid.trader.data.repository.DeviceRepository
import java.io.File

class DeviceManager(private val context: Context?, private val appViewModel: AppViewModel) {
    private var deviceRepository: DeviceRepository? = null
    
    /**
     * 设置设备仓库
     */
    fun setDeviceRepository(repository: DeviceRepository) {
        this.deviceRepository = repository
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
                "ro.boot.serialno",  // 启动时的序列号属性
                "persist.sys.serialno", // 持久化序列号属性
                "ro.product.serial", // 产品序列号属性
                "gsm.device.id",    // GSM设备ID
                "gsm.serial.id",    // GSM序列号
                "ril.serialnumber"  // RIL序列号
            )
            
            for (prop in serialProperties) {
                val serial = getSystemProperty(prop)
                if (!serial.isNullOrEmpty() && serial != "unknown" && serial.length > 5) {
                    Log.d(TAG, "通过系统属性 $prop 获取到序列号: $serial")
                    return serial
                }
            }
            
            // 如果系统属性无法获取序列号，尝试通过读取/proc/cmdline获取
            try {
                val cmdline = File("/proc/cmdline").readText()
                val serialMatch = Regex("androidboot.serialno=([^\\s]+)").find(cmdline)
                if (serialMatch != null) {
                    val serial = serialMatch.groupValues[1]
                    if (serial.isNotEmpty() && serial.length > 5) {
                        Log.d(TAG, "通过/proc/cmdline获取到序列号: $serial")
                        return serial
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "读取/proc/cmdline失败: ${e.message}")
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
     * 优先使用 Runtime.getRuntime().exec() 执行 getprop 命令
     * 如果失败，则使用反射访问内部API
     */
    private fun getSystemProperty(property: String): String? {
        // 首先尝试通过执行 getprop 命令获取属性
        try {
            val process = Runtime.getRuntime().exec("getprop $property")
            val inputStream = process.inputStream
            val reader = inputStream.bufferedReader()
            val value = reader.readLine()
            reader.close()
            inputStream.close()
            process.waitFor()
            
            if (!value.isNullOrEmpty() && value != "unknown") {
                Log.d(TAG, "通过getprop命令获取系统属性 $property: $value")
                return value
            }
        } catch (e: Exception) {
            Log.w(TAG, "通过getprop命令获取系统属性 $property 失败: ${e.message}")
        }
        
        // 如果 getprop 命令失败，尝试使用反射
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            val result = get.invoke(null, property) as? String
            
            // 验证结果是否有效
            if (result.isNullOrEmpty() || result == "unknown") {
                Log.w(TAG, "系统属性 $property 返回空值或unknown")
                null
            } else {
                Log.d(TAG, "通过反射获取系统属性 $property: $result")
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
    @RequiresPermission("android.permission.READ_PHONE_STATE")
    private fun getLocalDevice(): DeviceEntity {
        Log.d(TAG, "getLocalDevice: 开始获取本地设备信息")
        
        // 获取设备序列号作为主键
        val serialNo = getSerialNo()
        
        // 获取设备UDID作为备用标识符
        val udid = getUdid()
        
        // 获取设备名称
        val deviceName = getDeviceName()
        
        Log.d(TAG, "getLocalDevice: 设备序列号 = $serialNo")
        Log.d(TAG, "getLocalDevice: 设备UDID = $udid")
        Log.d(TAG, "getLocalDevice: 设备名称 = $deviceName")

        // 创建设备信息对象
        val device = DeviceEntity(
            serialNo = serialNo ?: "unknown_serial",
            udid = udid,
            name = deviceName,
        )
        
        Log.d(TAG, "getLocalDevice: 设备信息创建完成，序列号 = ${device.serialNo}, UDID = ${device.udid}, 名称 = ${device.name}")
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
    
    /**
     * 获取设备名称
     * 优先使用蓝牙名称，如果没有则使用设备型号
     */
    private fun getDeviceName(): String {
        return try {
            // 尝试获取蓝牙名称
            if (context != null) {
                val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter != null) {
                    val name = bluetoothAdapter.name
                    if (!name.isNullOrEmpty()) {
                        Log.d(TAG, "使用蓝牙名称作为设备名称: $name")
                        return name
                    }
                }
                
                // 如果没有蓝牙名称，使用设备型号
                val model = "${Build.MANUFACTURER} ${Build.MODEL}"
                Log.d(TAG, "使用设备型号作为设备名称: $model")
                return model
            } else {
                "Unknown Device"
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取设备名称时出错: ${e.message}", e)
            "Unknown Device"
        }
    }

    @RequiresPermission("android.permission.READ_PHONE_STATE")
    private fun getSerialNo(): String? {
        return try {
            // 1. 优先使用系统属性获取序列号 (不需要特殊权限)
            val systemSerial = getSerialFromSystemProperty()
            if (!systemSerial.isNullOrEmpty()) {
                Log.d(TAG, "通过系统属性获取设备序列号: $systemSerial")
                return systemSerial
            }
            
            // 2. 尝试使用 Build.getSerial() (需要 READ_PHONE_STATE 权限)
            try {
                val serial = Build.getSerial()
                if (serial != null && serial != "unknown") {
                    Log.d(TAG, "使用 Build.getSerial() 获取设备序列号: $serial")
                    return serial
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Build.getSerial() 需要READ_PHONE_STATE权限: ${e.message}")
            } catch (e: Exception) {
                Log.w(TAG, "Build.getSerial() 失败: ${e.message}")
            }
            
            // 3. 尝试使用 ANDROID_ID 作为序列号
            if (context != null) {
                try {
                    val androidId = android.provider.Settings.Secure.getString(
                        context.contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    )
                    if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
                        Log.d(TAG, "使用 ANDROID_ID 作为设备序列号: $androidId")
                        return androidId
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "获取 ANDROID_ID 失败: ${e.message}")
                }
                
                // 4. 如果以上都失败，使用组合UDID作为序列号
                val udid = getDeviceUDID(context)
                Log.d(TAG, "使用组合UDID作为设备序列号: $udid")
                return udid
            }
            
            // 5. 最后的备选方案
            "unknown_device_id"
        } catch (e: Exception) {
            Log.e(TAG, "获取设备序列号时出错: ${e.message}", e)
            if (context != null) {
                getDeviceUDID(context)
            } else {
                "unknown_device_id"
            }
        }
    }

    val device: DeviceEntity
        @RequiresPermission("android.permission.READ_PHONE_STATE")
        get() = getLocalDevice()

    
    /**
     * 向服务器注册设备
     * @return 注册后的设备实体信息
     */
    @RequiresPermission("android.permission.READ_PHONE_STATE")
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
        Log.d("DeviceManager","deviceRepository=${deviceRepository}")
        val updatedDevice = deviceRepository!!.registerDevice(localDevice)
            ?: throw Exception("设备注册失败，无法获取注册后的设备信息")
            
        Log.d(TAG, "registerLocalDeviceWithServer: 设备注册完成，设备信息已更新")
        return updatedDevice
    }


    /**
     * 请求服务器检查设备调试权限、安装支持的应用等情况
     * @return 服务器检查后的设备实体信息
     */
    @RequiresPermission("android.permission.READ_PHONE_STATE")
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