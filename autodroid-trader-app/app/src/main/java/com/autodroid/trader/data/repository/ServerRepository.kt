package com.autodroid.trader.data.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.autodroid.trader.data.database.ServerProvider
import com.autodroid.trader.data.dao.ServerEntity
import com.autodroid.trader.model.Server
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
     * 更新服务器信息
     */
    suspend fun update(server: Server) {
        // 将Server对象转换为ServerEntity
        val serverEntity = ServerEntity(
            apiEndpoint = server.apiEndpoint,
            name = server.name,
            isConnected = server.connected,
            lastConnectedTime = if (server.connected) System.currentTimeMillis() else 0,
            hostname = server.hostname ?: "",
            platform = server.platform ?: "",
            version = "",
            deviceCount = 0,
            supportsDeviceRegistration = server.supportsDeviceRegistration,
            supportsApkManagement = server.supportsApkManagement,
            supportsTradePlanExecution = server.supportsTradePlanExecution,
            supportsTradeScheduling = server.supportsTradeScheduling(),
            supportsEventTriggering = false, // Server类中没有这个属性，设为默认值
            discoveryType = server.discoveryMethod ?: "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        serverProvider.updateServer(serverEntity)
    }
    
    /**
     * 获取所有服务器
     */
    fun getAllServers(): LiveData<List<ServerEntity>> {
        return serverProvider.getAllServers()
    }
    
    /**
     * 获取已连接的服务器
     * 根据"本地优先"设计理念，主动检查服务器状态并更新本地数据库
     */
    fun getConnectedServer(): LiveData<ServerEntity?> {
        // 启动异步任务更新所有服务器信息
        updateAllServers()
        return serverProvider.getConnectedServer()
    }
    
    /**
     * 插入或更新服务器
     */
    suspend fun insertOrUpdateServer(server: Server): String {
        return withContext(Dispatchers.IO) {
            // 使用apiEndpoint作为主键，必须非空
            val apiEndpoint = server.apiEndpoint ?: throw IllegalArgumentException("服务器API端点不能为空")
            
            // 直接使用Server的信息构建ServerEntity，不需要再次调用API
            val serverEntity = ServerEntity(
                apiEndpoint = apiEndpoint,
                name = server.serviceName,
                
                // 使用Server中的信息
                hostname = server.hostname ?: throw IllegalArgumentException("服务器主机名不能为空"),
                platform = server.platform ?: throw IllegalArgumentException("服务器平台不能为空"),
                version = "1.0.0", // 版本号可以使用默认值
                deviceCount = 0, // 设备数量可以使用默认值
                
                discoveryType = "manual" // 手动输入方式
            )
            
            val apiEndpointResult = serverProvider.insertOrUpdateServer(serverEntity)
            
            // 对于手动添加的服务器，自动设置为已连接
            serverProvider.updateConnectionStatus(apiEndpointResult!!, true)
            
            return@withContext apiEndpointResult
        }
    }
    
    /**
     * 删除服务器
     */
    suspend fun deleteServer(apiEndpoint: String) {
        withContext(Dispatchers.IO) {
            serverProvider.deleteServerByKey(apiEndpoint)
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
                        // 设置API端点并获取服务器信息
                        apiClient.setApiEndpoint(server.apiEndpoint)
                        
                        // 调用getServerInfo()获取完整的服务器信息
                        val serverInfo = apiClient.getServerInfo()
                        
                        // 根据服务器信息更新本地数据库
                        if (serverInfo != null) {
                            // 服务器可用，更新服务器信息
                            updateServer(server.apiEndpoint, serverInfo)
                            Log.d("ServerRepository", "服务器信息同步成功: ${server.apiEndpoint}")
                        } else {
                            // 服务器不可用，断开连接
                            serverProvider.updateConnectionStatus(
                                server.apiEndpoint, 
                                false
                            )
                            Log.w("ServerRepository", "服务器信息获取失败: ${server.apiEndpoint}")
                        }
                    } catch (e: Exception) {
                        Log.e("ServerRepository", "检查服务器${server.apiEndpoint}状态时出错: ${e.message}")
                        // 检查失败时断开连接
                        serverProvider.updateConnectionStatus(server.apiEndpoint, false)
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
    private suspend fun updateServer(apiEndpoint: String, serverInfo: ServerInfoResponse) {
        withContext(Dispatchers.IO) {
            try {
                val serverEntity = serverProvider.getServerByKey(apiEndpoint)
                if (serverEntity != null) {
                    // 使用服务器返回的完整信息更新本地数据库
                    val updatedServer = serverEntity.copy(
                        name = serverInfo.name ?: serverEntity.name,
                        hostname = serverInfo.hostname ?: serverEntity.hostname,
                        platform = serverInfo.platform ?: serverEntity.platform,
                        version = "1.0.0", // 可以从serverInfo中获取版本信息
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