package com.autodroid.trader.aas.database

import androidx.room.*
import com.autodroid.trader.aas.database.UIEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface UIEventDao {
    
    @Insert
    suspend fun insert(event: UIEvent): Long
    
    @Update
    suspend fun update(event: UIEvent)
    
    @Query("SELECT * FROM ui_events ORDER BY event_time DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int = 100): List<UIEvent>
    
    @Query("SELECT * FROM ui_events WHERE package_name = :packageName ORDER BY event_time DESC")
    suspend fun getEventsByPackage(packageName: String): List<UIEvent>
    
    @Query("SELECT * FROM ui_events WHERE event_type = :eventType ORDER BY event_time DESC")
    suspend fun getEventsByType(eventType: String): List<UIEvent>
    
    @Query("SELECT * FROM ui_events WHERE element_id = :elementId ORDER BY event_time DESC")
    suspend fun getEventsByElement(elementId: String): List<UIEvent>
    
    @Query("SELECT * FROM ui_events WHERE id = :id")
    suspend fun getEventById(id: Int): UIEvent?
    
    @Query("SELECT * FROM ui_events WHERE package_name = :packageName AND element_id = :elementId AND event_type = :eventType AND event_time > :sinceTime")
    suspend fun getRecentEventByElementAndType(packageName: String, elementId: String, eventType: String, sinceTime: Long): UIEvent?
    
    @Query("DELETE FROM ui_events WHERE event_time < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    @Query("DELETE FROM ui_events WHERE package_name = :packageName")
    suspend fun deleteByPackage(packageName: String): Int
    
    @Query("SELECT COUNT(*) FROM ui_events WHERE package_name = :packageName")
    suspend fun getEventCount(packageName: String): Int
    
    @Query("""
        SELECT DISTINCT package_name 
        FROM ui_events 
        WHERE event_time > :sinceTime
        ORDER BY package_name
    """)
    suspend fun getRecordedPackages(sinceTime: Long = System.currentTimeMillis() - 86400000): List<String>
    
    @Query("SELECT * FROM ui_events ORDER BY event_time DESC")
    fun getAllEventsFlow(): Flow<List<UIEvent>>
}