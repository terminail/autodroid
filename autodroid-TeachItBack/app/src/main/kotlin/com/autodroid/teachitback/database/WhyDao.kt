package com.autodroid.teachitback.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.autodroid.teachitback.model.WhyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhyDao {
    @Query("SELECT * FROM why_content ORDER BY orderIndex ASC")
    fun getAllWhyContent(): Flow<List<WhyEntity>>

    @Query("SELECT * FROM why_content ORDER BY orderIndex ASC")
    suspend fun getAllWhyContentSync(): List<WhyEntity>

    @Query("SELECT * FROM why_content WHERE id = :id")
    fun getWhyContentById(id: String): Flow<WhyEntity?>

    @Query("SELECT * FROM why_content WHERE id = :id")
    suspend fun getWhyContentByIdSync(id: String): WhyEntity?

    @Query("SELECT * FROM why_content WHERE type = :type ORDER BY orderIndex ASC")
    fun getWhyContentByType(type: String): Flow<List<WhyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhyContent(content: WhyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWhyContent(contents: List<WhyEntity>)

    @Update
    suspend fun updateWhyContent(content: WhyEntity)

    @Delete
    suspend fun deleteWhyContent(content: WhyEntity)

    @Query("DELETE FROM why_content WHERE id = :id")
    suspend fun deleteWhyContentById(id: String)

    @Query("DELETE FROM why_content")
    suspend fun deleteAllWhyContent()
}
