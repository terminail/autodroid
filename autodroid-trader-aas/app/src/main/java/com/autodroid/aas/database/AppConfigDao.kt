package com.autodroid.aas.database

import androidx.room.*
import com.autodroid.aas.database.AppConfig

@Dao
interface AppConfigDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: AppConfig)
    
    @Update
    suspend fun update(config: AppConfig)
    
    @Query("SELECT * FROM app_configs WHERE package_name = :packageName")
    suspend fun getConfig(packageName: String): AppConfig?
    
    @Query("SELECT * FROM app_configs ORDER BY app_name")
    suspend fun getAllConfigs(): List<AppConfig>
    
    @Query("DELETE FROM app_configs WHERE package_name = :packageName")
    suspend fun delete(packageName: String)
    
    @Query("UPDATE app_configs SET recording_enabled = :enabled WHERE package_name = :packageName")
    suspend fun setRecordingEnabled(packageName: String, enabled: Boolean)
    
    @Query("UPDATE app_configs SET record_clicks = :enabled WHERE package_name = :packageName")
    suspend fun setRecordClicks(packageName: String, enabled: Boolean)
    
    @Query("UPDATE app_configs SET record_inputs = :enabled WHERE package_name = :packageName")
    suspend fun setRecordInputs(packageName: String, enabled: Boolean)
}