package com.autodroid.guardiansdk.data.dao

import androidx.room.*
import com.autodroid.guardiansdk.data.entity.Ward
import kotlinx.coroutines.flow.Flow

/**
 * 被监护人数据访问对象
 */
@Dao
interface WardDao {

    /**
     * 插入或更新被监护人
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWard(ward: Ward)

    /**
     * 批量插入或更新被监护人
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAllWards(wards: List<Ward>)

    /**
     * 根据手机号获取被监护人
     */
    @Query("SELECT * FROM wards WHERE phoneNumber = :phoneNumber")
    suspend fun getWardByPhoneNumber(phoneNumber: String): Ward?

    /**
     * 根据手机号监听被监护人变化
     */
    @Query("SELECT * FROM wards WHERE phoneNumber = :phoneNumber")
    fun observeWardByPhoneNumber(phoneNumber: String): Flow<Ward?>

    /**
     * 获取所有被监护人
     */
    @Query("SELECT * FROM wards ORDER BY createdAt DESC")
    suspend fun getAllWards(): List<Ward>

    /**
     * 监听所有被监护人变化
     */
    @Query("SELECT * FROM wards ORDER BY createdAt DESC")
    fun observeAllWards(): Flow<List<Ward>>

    /**
     * 获取活跃的被监护人
     */
    @Query("SELECT * FROM wards WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveWards(): List<Ward>

    /**
     * 监听活跃的被监护人变化
     */
    @Query("SELECT * FROM wards WHERE isActive = 1 ORDER BY createdAt DESC")
    fun observeActiveWards(): Flow<List<Ward>>

    /**
     * 获取被监护人数量
     */
    @Query("SELECT COUNT(*) FROM wards")
    suspend fun getWardCount(): Int

    /**
     * 获取活跃的被监护人数量
     */
    @Query("SELECT COUNT(*) FROM wards WHERE isActive = 1")
    suspend fun getActiveWardCount(): Int

    /**
     * 根据手机号删除被监护人
     */
    @Query("DELETE FROM wards WHERE phoneNumber = :phoneNumber")
    suspend fun deleteWardByPhoneNumber(phoneNumber: String)

    /**
     * 批量删除被监护人
     */
    @Query("DELETE FROM wards WHERE phoneNumber IN (:phoneNumbers)")
    suspend fun deleteWardsByPhoneNumbers(phoneNumbers: List<String>)

    /**
     * 清空所有被监护人
     */
    @Query("DELETE FROM wards")
    suspend fun deleteAllWards()

    /**
     * 更新被监护人状态
     */
    @Query("UPDATE wards SET isActive = :isActive, updatedAt = :updatedAt WHERE phoneNumber = :phoneNumber")
    suspend fun updateWardStatus(phoneNumber: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    /**
     * 更新被监护人报警信息
     */
    @Query("UPDATE wards SET lastAlarmTime = :lastAlarmTime, alarmCount = alarmCount + 1, updatedAt = :updatedAt WHERE phoneNumber = :phoneNumber")
    suspend fun updateWardAlarmInfo(phoneNumber: String, lastAlarmTime: Long, updatedAt: Long = System.currentTimeMillis())
}