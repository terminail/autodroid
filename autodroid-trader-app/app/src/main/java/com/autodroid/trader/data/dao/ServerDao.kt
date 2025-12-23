package com.autodroid.trader.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy

/**
 * 服务器数据访问对象
 * 提供对服务器信息的增删改查操作
 */
@Dao
interface ServerDao {
    
    /**
     * 获取所有服务器列表
     */
    @Query("SELECT * FROM servers ORDER BY updatedAt DESC")
    fun getAllServers(): LiveData<List<ServerEntity>>

    /**
     * 获取所有服务器列表（同步方式，用于协程中）
     */
    @Query("SELECT * FROM servers ORDER BY updatedAt DESC")
    suspend fun getAllServersSync(): List<ServerEntity>

    /**
     * 获取已连接的服务器，如果没有连接的服务器则返回最近连接过的（时间戳最新的）
     */
    @Query("SELECT * FROM servers WHERE isConnected = 1 ORDER BY lastConnectedTime DESC LIMIT 1")
    fun getConnectedServer(): LiveData<ServerEntity?>

    /**
     * 获取最后更新的服务器
     */
    @Query("SELECT * FROM servers ORDER BY updatedAt DESC LIMIT 1")
    fun getLastUpdatedServer(): LiveData<ServerEntity?>

    /**
     * 根据服务器主键获取服务器
     */
    @Query("SELECT * FROM servers WHERE ip = :ip AND port = :port")
    fun getServerByKey(ip: String, port: Int): ServerEntity?

    /**
     * 插入新服务器
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertServer(server: ServerEntity): Long

    /**
     * 更新服务器信息
     */
    @Update
    fun updateServer(server: ServerEntity)

    /**
     * 删除服务器
     */
    @Delete
    fun deleteServer(server: ServerEntity)

    /**
     * 根据服务器主键删除服务器
     */
    @Query("DELETE FROM servers WHERE ip = :ip AND port = :port")
    fun deleteServerByKey(ip: String, port: Int)

    /**
     * 更新服务器连接状态
     */
    @Query("UPDATE servers SET isConnected = :isConnected, lastConnectedTime = :lastConnectedTime, updatedAt = :updatedAt WHERE ip = :ip AND port = :port")
    fun updateConnectionStatus(ip: String, port: Int, isConnected: Boolean, lastConnectedTime: Long, updatedAt: Long)

    /**
     * 断开所有服务器的连接
     */
    @Query("UPDATE servers SET isConnected = 0, updatedAt = :updatedAt")
    fun disconnectAllServers(updatedAt: Long)

    /**
     * 获取服务器数量
     */
    @Query("SELECT COUNT(*) FROM servers")
    fun getServerCount(): Int
}