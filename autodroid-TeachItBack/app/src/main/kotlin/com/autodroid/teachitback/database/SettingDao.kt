package com.autodroid.teachitback.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.autodroid.teachitback.model.SettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings ORDER BY lastUpdated DESC")
    fun getAllSettings(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM settings ORDER BY lastUpdated DESC")
    suspend fun getAllSettingsSync(): List<SettingEntity>

    @Query("SELECT * FROM settings WHERE key = :key")
    fun getSettingByKey(key: String): Flow<SettingEntity?>

    @Query("SELECT * FROM settings WHERE key = :key")
    suspend fun getSettingByKeySync(key: String): SettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)

    @Update
    suspend fun updateSetting(setting: SettingEntity)

    @Delete
    suspend fun deleteSetting(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteSettingByKey(key: String)

    @Query("DELETE FROM settings")
    suspend fun deleteAllSettings()
}
