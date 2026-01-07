package com.autodroid.trader.aas.database

import androidx.room.*
import com.autodroid.trader.aas.database.ElementFeature

@Dao
interface ElementFeatureDao {
    
    @Insert
    suspend fun insert(feature: ElementFeature)
    
    @Update
    suspend fun update(feature: ElementFeature)
    
    @Query("SELECT * FROM element_features WHERE element_signature = :signature")
    suspend fun getBySignature(signature: String): ElementFeature?
    
    @Query("SELECT * FROM element_features WHERE package_name = :packageName")
    suspend fun getByPackage(packageName: String): List<ElementFeature>
    
    @Query("SELECT * FROM element_features WHERE package_name = :packageName ORDER BY usage_count DESC")
    suspend fun getElementsByPackage(packageName: String): List<ElementFeature>
    
    @Query("SELECT * FROM element_features WHERE package_name = :packageName AND element_type = :elementType")
    suspend fun getByPackageAndType(packageName: String, elementType: String): List<ElementFeature>
    
    @Query("SELECT * FROM element_features WHERE auto_fill_enabled = 1 AND package_name = :packageName")
    suspend fun getAutoFillElements(packageName: String): List<ElementFeature>
    
    @Query("UPDATE element_features SET auto_fill_value = :value WHERE id = :id")
    suspend fun updateAutoFillValue(id: Int, value: String)
    
    @Query("UPDATE element_features SET auto_fill_enabled = :enabled WHERE id = :id")
    suspend fun updateAutoFillEnabled(id: Int, enabled: Boolean)
    
    @Query("DELETE FROM element_features WHERE last_used_time < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
}