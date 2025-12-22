package com.autodroid.trader.data.database

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.autodroid.trader.data.dao.DeviceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 设备数据提供者
 * 提供对设备数据的数据库操作封装，协调DAO层和业务逻辑层
 */
class DeviceProvider private constructor(context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val deviceDao = database.deviceDao()
    
    /**
     * 获取所有设备列表
     */
    fun getAllDevices(): LiveData<List<DeviceEntity>> {
        return deviceDao.getAllDevices()
    }

    /**
     * 根据设备序列号获取设备
     */
    fun getDeviceById(id: String): LiveData<DeviceEntity?> {
        return deviceDao.getDeviceById(id)
    }

    /**
     * 根据设备序列号同步获取设备
     */
    suspend fun getDeviceByIdSync(id: String): DeviceEntity? {
        return withContext(Dispatchers.IO) {
            deviceDao.getDeviceByIdSync(id)
        }
    }


    /**
     * 更新设备信息
     */
    suspend fun updateDevice(device: DeviceEntity) {
        withContext(Dispatchers.IO) {
            val updatedDevice = device.copy(updatedAt = System.currentTimeMillis())
            Log.d("DeviceProvider", "更新设备信息: 序列号=${updatedDevice.serialNo}, 名称=${updatedDevice.name}")
            // 使用insertOrUpdateDevice确保设备存在或更新
            deviceDao.insertOrUpdateDevice(updatedDevice)
            Log.d("DeviceProvider", "设备信息更新完成: 序列号=${updatedDevice.serialNo}")
        }
    }

    
    /**
     * 根据设备序列号删除设备
     */
    suspend fun deleteDeviceById(id: String) {
        withContext(Dispatchers.IO) {
            deviceDao.deleteDeviceById(id)
        }
    }

    /**
     * 更新设备连接状态
     */
    suspend fun updateConnectionStatus(serialNo: String, isOnline: Boolean, updatedAt: Long) {
        withContext(Dispatchers.IO) {
            deviceDao.updateConnectionStatus(serialNo, isOnline, updatedAt)
        }
    }


    companion object {
        @Volatile
        private var INSTANCE: DeviceProvider? = null
        
        /**
         * 获取设备数据提供者实例（单例模式）
         */
        fun getInstance(context: Context): DeviceProvider {
            return INSTANCE ?: synchronized(this) {
                val instance = DeviceProvider(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}