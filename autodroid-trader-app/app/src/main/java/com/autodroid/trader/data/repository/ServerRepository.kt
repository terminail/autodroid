package com.autodroid.trader.data.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.autodroid.trader.data.database.ServerProvider
import com.autodroid.trader.data.dao.ServerEntity
import com.autodroid.trader.network.ApiClient
import com.autodroid.trader.network.ServerInfoResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 服务器仓库类
 * 提供对服务器数据的统一访问接口，协调数据源和业务逻辑
 */
class ServerRepository private constructor(application: Application) {
    
    private val serverProvider = ServerProvider.getInstance(application)
    private val context = application.applicationContext
    private val apiClient = ApiClient.getInstance()
    
    /**
     * 更新服务器连接状态
     */
    suspend fun updateConnectionStatus(apiEndpoint: String, isConnected: Boolean) {
        withContext(Dispatchers.IO) {
            // 从API端点中提取IP和端口
            val urlParts = apiEndpoint.replace("http://", "").replace("https://", "").split("/")
            val hostPort = urlParts.getOrNull(0) ?: return@withContext
            val hostParts = hostPort.split(":")
            val ip = hostParts.getOrNull(0) ?: return@withContext
            val port = hostParts.getOrNull(1)?.toIntOrNull() ?: return@withContext
            
            serverProvider.updateConnectionStatus(ip, port, isConnected)
        }
    }
    
    /**
     * 更新服务器信息
     */
    suspend fun update(serverEntity: ServerEntity) {
        serverProvider.updateServer(serverEntity)
    }
    
    /**
     * 获取所有服务器
     */
    fun getAllServers(): LiveData<List<ServerEntity>> {
        return serverProvider.getAllServers()
    }

    /**
     * 获取所有服务器（同步方式，用于协程中）
     */
    suspend fun getAllServersSync(): List<ServerEntity> {
        return withContext(Dispatchers.IO) {
            try {
                return@withContext serverProvider.getAllServersSync()
            } catch (e: Exception) {
                Log.e("ServerRepository", "获取服务器列表失败: ${e.message}", e)
                return@withContext emptyList()
            }
        }
    }

    /**
     * 获取当前服务器
     * 根据"本地优先"设计理念，主动检查服务器状态并更新本地数据库
     */
    fun getCurrentServer(): LiveData<ServerEntity?> {
        return serverProvider.getCurrentServer()
    }

    /**
     * 获取当前服务器
     * 根据"本地优先"设计理念，主动检查服务器状态并更新本地数据库
     */
    fun getAndSyncServer(): LiveData<ServerEntity?> {
        // 启动异步任务更新所有服务器信息
        updateAllServers()
        return serverProvider.getCurrentServer()
    }
    
    /**
     * 根据IP和端口获取服务器
     */
    suspend fun getServerByKey(ip: String, port: Int): ServerEntity? {
        return withContext(Dispatchers.IO) {
            try {
                return@withContext serverProvider.getServerByKey(ip, port)
            } catch (e: Exception) {
                Log.e("ServerRepository", "获取服务器失败: ${e.message}", e)
                return@withContext null
            }
        }
    }
    
    /**
     * 插入或更新服务器
     */
    suspend fun insertOrUpdateServer(se: ServerEntity): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ServerRepository", "开始插入或更新服务器: ${se.name} (${se.ip}:${se.port})")
                
                // 始终使用 insertOrUpdateServer 方法，确保 updatedAt 字段正确更新
                // 这样可以确保 LiveData 能够正确触发更新
                val result = serverProvider.insertOrUpdateServer(se)
                Log.d("ServerRepository", "服务器已更新: ${se.name}, 结果: $result")
                return@withContext "服务器已更新: ${se.name}"
            } catch (e: Exception) {
                Log.e("ServerRepository", "插入或更新服务器失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 删除服务器
     */
    suspend fun deleteServer(apiEndpoint: String) {
        withContext(Dispatchers.IO) {
            // 从apiEndpoint中提取ip和port
            val urlParts = apiEndpoint.replace("http://", "").replace("https://", "").split(":")
            val ip = urlParts.getOrNull(0) ?: return@withContext
            val port = urlParts.getOrNull(1)?.toIntOrNull() ?: return@withContext
            
            serverProvider.deleteServerByKey(ip, port)
        }
    }
    
    /**
     * 更新所有服务器信息
     * 根据"本地优先"设计理念，主动检查所有服务器状态并更新本地数据库
     * 使用ApiClient获取完整的服务器信息，确保本地数据库与服务器实际状态同步
     */
    private fun updateAllServers() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 获取所有服务器列表，而不是仅获取当前"已连接"的服务器
                val allServers = serverProvider.getAllServers().value ?: emptyList()
                
                // 对每个服务器进行完整信息检查
                allServers.forEach { server ->
                    try {
                        // 构建API端点
                        val apiEndpoint = server.apiEndpoint()
                        
                        // 设置API端点并获取服务器信息
                        apiClient.setApiEndpoint(apiEndpoint)
                        
                        // 调用getServerInfo()获取完整的服务器信息
                        val serverInfo = apiClient.getServerInfo()
                        
                        // 根据服务器信息更新本地数据库
                        if (serverInfo != null) {
                            // 服务器可用，更新服务器信息
                            updateServer(serverInfo)
                            Log.d("ServerRepository", "服务器信息同步成功: ${apiEndpoint}")
                        } else {
                            // 服务器不可用，断开连接
                            serverProvider.updateConnectionStatus(server.ip, server.port, false)
                            Log.w("ServerRepository", "服务器信息获取失败: ${apiEndpoint}")
                        }
                    } catch (e: Exception) {
                        Log.e("ServerRepository", "检查服务器${server.ip}:${server.port}状态时出错: ${e.message}")
                        // 检查失败时断开连接
                        serverProvider.updateConnectionStatus(server.ip, server.port, false)
                    }
                }
            } catch (e: Exception) {
                Log.e("ServerRepository", "检查服务器状态时出错: ${e.message}")
            }
        }
    }
    

    
    /**
     * 更新服务器信息
     */
    suspend fun updateServer(serverInfo: ServerInfoResponse) {
        withContext(Dispatchers.IO) {
            try {

                val serverEntity = serverProvider.getServerByKey(serverInfo.ip, serverInfo.port)
                if (serverEntity != null) {
                    // 使用服务器返回的完整信息更新本地数据库
                    val updatedServer = serverEntity.copy(
                        name = serverInfo.name,
                        platform = serverInfo.platform,
                        isConnected = true,
                        lastConnectedTime = System.currentTimeMillis()
                    )
                    serverProvider.updateServer(updatedServer)
                    Log.d("ServerRepository", "服务器信息更新成功: ${serverInfo.name}")
                }
            } catch (e: Exception) {
                Log.e("ServerRepository", "更新服务器信息失败: ${e.message}")
            }
        }
    }


    companion object {
        @Volatile
        private var INSTANCE: ServerRepository? = null
        
        /**
         * 获取服务器仓库实例（单例模式）
         */
        fun getInstance(application: Application): ServerRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ServerRepository(application)
                INSTANCE = instance
                instance
            }
        }
    }
}