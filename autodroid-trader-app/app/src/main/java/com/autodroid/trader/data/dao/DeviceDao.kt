package com.autodroid.trader.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy

/**
 * 设备数据访问对象
 * 提供对设备信息的增删改查操作
 */
@Dao
interface DeviceDao {
    
    /**
     * 获取所有设备列表
     */
    @Query("SELECT * FROM devices ORDER BY updatedAt DESC")
    fun getAllDevices(): LiveData<List<DeviceEntity>>

    /**
     * 获取当前设备信息
     */
    @Query("SELECT * FROM devices WHERE isOnline = 1 LIMIT 1")
    fun getCurrentDevice(): LiveData<DeviceEntity?>

    /**
     * 根据设备序列号获取设备
     */
    @Query("SELECT * FROM devices WHERE serialNo = :serialNo")
    suspend fun getDeviceById(serialNo: String): DeviceEntity?
    
    /**
     * 根据设备序列号获取设备（LiveData）
     */
    @Query("SELECT * FROM devices WHERE serialNo = :serialNo")
    fun getDeviceByIdLiveData(serialNo: String): LiveData<DeviceEntity?>

    /**
     * 插入新设备
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity): Long

    /**
     * 更新设备信息
     */
    @Update
    suspend fun updateDevice(device: DeviceEntity)

    /**
     * 删除设备
     */
    @Delete
    suspend fun deleteDevice(device: DeviceEntity)

    /**
     * 根据设备序列号删除设备
     */
    @Query("DELETE FROM devices WHERE serialNo = :serialNo")
    suspend fun deleteDeviceById(serialNo: String): DeviceEntity?

    /**
     * 更新设备连接状态
     */
    @Query("UPDATE devices SET isOnline = :isOnline, updatedAt = :updatedAt WHERE serialNo = :serialNo")
    suspend fun updateConnectionStatus(serialNo: String, isOnline: Boolean, updatedAt: Long)

    /**
     * 断开所有设备的连接
     */
    @Query("UPDATE devices SET isOnline = 0, updatedAt = :updatedAt")
    suspend fun disconnectAllDevices(updatedAt: Long)

    /**
     * 获取设备数量
     */
    @Query("SELECT COUNT(*) FROM devices")
    suspend fun getDeviceCount(): Int

    /**
     * 插入或更新设备信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: DeviceEntity)
}