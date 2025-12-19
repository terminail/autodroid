package com.autodroid.trader.data.database

import android.content.Context
import androidx.lifecycle.LiveData
import com.autodroid.trader.data.dao.ServerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 服务器数据提供者
 * 提供对服务器数据的数据库操作封装，协调DAO层和业务逻辑层
 */
class ServerProvider private constructor(context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val serverDao = database.serverDao()
    
    /**
     * 获取所有服务器列表
     */
    fun getAllServers(): LiveData<List<ServerEntity>> {
        return serverDao.getAllServers()
    }
    
    /**
     * 获取已连接的服务器
     */
    fun getConnectedServer(): LiveData<ServerEntity?> {
        return serverDao.getConnectedServer()
    }
    
    /**
     * 根据服务器主键获取服务器
     */
    suspend fun getServerByKey(apiEndpoint: String): ServerEntity? {
        return withContext(Dispatchers.IO) {
            serverDao.getServerByKey(apiEndpoint)
        }
    }
    
    /**
     * 根据主机名获取服务器
     */
    suspend fun getServerByHostname(hostname: String): ServerEntity? {
        return withContext(Dispatchers.IO) {
            // 由于ServerEntity没有host和port字段，需要从所有服务器中查找匹配hostname的服务器
            serverDao.getAllServers().value?.find { it.hostname == hostname }
        }
    }
    
    /**
     * 插入或更新服务器
     */
    suspend fun insertOrUpdateServer(server: ServerEntity): String {
        return withContext(Dispatchers.IO) {
            // 检查是否已存在相同主键的服务器
            val existingServer = serverDao.getServerByKey(server.apiEndpoint)
            
            val result: String = if (existingServer != null) {
                // 更新现有服务器
                val updatedServer = server.copy(updatedAt = System.currentTimeMillis())
                serverDao.updateServer(updatedServer)
                existingServer.apiEndpoint!!
            } else {
                // 插入新服务器
                serverDao.insertServer(server)
                server.apiEndpoint
            }
            
            return@withContext result
        }
    }
    
    /**
     * 更新服务器信息
     */
    suspend fun updateServer(server: ServerEntity) {
        withContext(Dispatchers.IO) {
            val updatedServer = server.copy(updatedAt = System.currentTimeMillis())
            serverDao.updateServer(updatedServer)
        }
    }
    
    /**
     * 删除服务器
     */
    suspend fun deleteServer(server: ServerEntity) {
        withContext(Dispatchers.IO) {
            serverDao.deleteServer(server)
        }
    }
    
    /**
     * 根据服务器主键删除服务器
     */
    suspend fun deleteServerByKey(apiEndpoint: String) {
        withContext(Dispatchers.IO) {
            serverDao.deleteServerByKey(apiEndpoint)
        }
    }

    /**
     * 更新服务器连接状态
     */
    suspend fun updateConnectionStatus(apiEndpoint: String, isConnected: Boolean) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            serverDao.updateConnectionStatus(apiEndpoint, isConnected, now, now)
        }
    }
    
    /**
     * 断开所有服务器的连接
     */
    suspend fun disconnectAllServers() {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            serverDao.disconnectAllServers(now)
        }
    }
    
    /**
     * 获取服务器数量
     */
    suspend fun getServerCount(): Int {
        return withContext(Dispatchers.IO) {
            serverDao.getServerCount()
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ServerProvider? = null
        
        /**
         * 获取服务器数据提供者实例（单例模式）
         */
        fun getInstance(context: Context): ServerProvider {
            return INSTANCE ?: synchronized(this) {
                val instance = ServerProvider(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}