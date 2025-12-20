package com.autodroid.trader.data.database

import android.content.Context
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
     * 获取当前设备
     */
    fun getCurrentDevice(): LiveData<DeviceEntity?> {
        return deviceDao.getCurrentDevice()
    }
    
    /**
     * 根据设备ID获取设备
     */
    suspend fun getDeviceById(id: String): DeviceEntity? {
        return withContext(Dispatchers.IO) {
            deviceDao.getDeviceById(id)
        }
    }
    
    /**
     * 插入或更新设备
     */
    suspend fun insertOrUpdateDevice(device: DeviceEntity): String {
        return withContext(Dispatchers.IO) {
            // 检查是否已存在相同ID的设备
            val existingDevice = deviceDao.getDeviceById(device.id)
            
            val result: String = if (existingDevice != null) {
                // 更新现有设备
                val updatedDevice = device.copy(updatedAt = System.currentTimeMillis())
                deviceDao.updateDevice(updatedDevice)
                device.id
            } else {
                // 插入新设备
                deviceDao.insertOrUpdateDevice(device)
                device.id
            }
            
            return@withContext result
        }
    }
    
    /**
     * 更新设备信息
     */
    suspend fun updateDevice(device: DeviceEntity) {
        withContext(Dispatchers.IO) {
            val updatedDevice = device.copy(updatedAt = System.currentTimeMillis())
            deviceDao.updateDevice(updatedDevice)
        }
    }
    
    /**
     * 删除设备
     */
    suspend fun deleteDevice(device: DeviceEntity) {
        withContext(Dispatchers.IO) {
            deviceDao.deleteDevice(device)
        }
    }
    
    /**
     * 根据设备ID删除设备
     */
    suspend fun deleteDeviceById(id: String) {
        withContext(Dispatchers.IO) {
            deviceDao.deleteDeviceById(id)
        }
    }

    /**
     * 更新设备连接状态
     */
    suspend fun updateConnectionStatus(id: String, isConnected: Boolean, lastSeen: Long) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            deviceDao.updateConnectionStatus(id, isConnected, lastSeen, now)
        }
    }
    
    /**
     * 断开所有设备的连接
     */
    suspend fun disconnectAllDevices() {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            deviceDao.disconnectAllDevices(now)
        }
    }
    
    /**
     * 获取设备数量
     */
    suspend fun getDeviceCount(): Int {
        return withContext(Dispatchers.IO) {
            deviceDao.getDeviceCount()
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