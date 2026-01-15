package com.autodroid.guardiansdk.data.dao

import androidx.room.*
import com.autodroid.guardiansdk.data.entity.Setting
import kotlinx.coroutines.flow.Flow

/**
 * 设置项数据访问对象
 */
@Dao
interface SettingDao {

    /**
     * 插入或更新设置项
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(setting: Setting)

    /**
     * 批量插入或更新设置项
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(settings: List<Setting>)

    /**
     * 根据键名获取设置项
     */
    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun getSetting(key: String): Setting?

    /**
     * 根据键名监听设置项变化
     */
    @Query("SELECT * FROM settings WHERE `key` = :key")
    fun observeSetting(key: String): Flow<Setting?>

    /**
     * 获取所有设置项
     */
    @Query("SELECT * FROM settings ORDER BY category, `key`")
    suspend fun getAllSettings(): List<Setting>

    /**
     * 监听所有设置项变化
     */
    @Query("SELECT * FROM settings ORDER BY category, `key`")
    fun observeAllSettings(): Flow<List<Setting>>

    /**
     * 根据分类获取设置项
     */
    @Query("SELECT * FROM settings WHERE category = :category ORDER BY `key`")
    suspend fun getSettingsByCategory(category: String): List<Setting>

    /**
     * 监听分类设置项变化
     */
    @Query("SELECT * FROM settings WHERE category = :category ORDER BY `key`")
    fun observeSettingsByCategory(category: String): Flow<List<Setting>>

    /**
     * 根据键名列表获取设置项
     */
    @Query("SELECT * FROM settings WHERE `key` IN (:keys)")
    suspend fun getSettingsByKeys(keys: List<String>): List<Setting>

    /**
     * 删除指定设置项
     */
    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)

    /**
     * 批量删除设置项
     */
    @Query("DELETE FROM settings WHERE `key` IN (:keys)")
    suspend fun deleteSettings(keys: List<String>)

    /**
     * 删除分类下的所有设置项
     */
    @Query("DELETE FROM settings WHERE category = :category")
    suspend fun deleteSettingsByCategory(category: String)

    /**
     * 清空所有设置项
     */
    @Query("DELETE FROM settings")
    suspend fun deleteAllSettings()

    /**
     * 检查设置项是否存在
     */
    @Query("SELECT COUNT(*) FROM settings WHERE `key` = :key")
    suspend fun exists(key: String): Int

    /**
     * 获取设置项数量
     */
    @Query("SELECT COUNT(*) FROM settings")
    suspend fun getCount(): Int

    /**
     * 获取分类数量
     */
    @Query("SELECT COUNT(DISTINCT category) FROM settings")
    suspend fun getCategoryCount(): Int
}