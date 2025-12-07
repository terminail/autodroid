package com.autodroid.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.autodroid.data.database.ServerProvider
import com.autodroid.data.dao.ServerEntity
import com.autodroid.manager.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 服务器仓库类
 * 提供对服务器数据的统一访问接口，协调数据源和业务逻辑
 */
class ServerRepository private constructor(application: Application) {
    
    private val serverProvider = ServerProvider.getInstance(application)
    private val context = application.applicationContext
    
    // 服务器状态LiveData
    private val _serverConnectionStatus = MutableLiveData<Boolean>()
    val serverConnectionStatus: LiveData<Boolean> = _serverConnectionStatus
    
    // 当前连接的服务器
    private val _currentServer = MutableLiveData<Server?>()
    val currentServer: LiveData<Server?> = _currentServer
    
    /**
     * 获取所有服务器列表
     */
    fun getAllServers(): LiveData<List<ServerEntity>> {
        return serverProvider.getAllServers()
    }
    
    /**
     * 获取已连接的服务器
     */
    fun getConnectedServer(): LiveData<ServerEntity?> {
        return serverProvider.getConnectedServer()
    }
    
    /**
     * 添加新发现的服务器
     */
    suspend fun addDiscoveredServer(server: Server): String {
        return withContext(Dispatchers.IO) {
            // 使用apiEndpoint作为主键，必须非空
            val apiEndpoint = server.apiEndpoint ?: throw IllegalArgumentException("服务器API端点不能为空")
            
            // 直接使用Server的信息构建ServerEntity，不需要再次调用API
            val serverEntity = ServerEntity(
                apiEndpoint = apiEndpoint,
                name = server.serviceName ?: throw IllegalArgumentException("服务器名称不能为空"),
                
                // 使用Server中的信息
                hostname = server.hostname ?: throw IllegalArgumentException("服务器主机名不能为空"),
                platform = server.platform ?: throw IllegalArgumentException("服务器平台不能为空"),
                version = "1.0.0", // 版本号可以使用默认值
                deviceCount = 0, // 设备数量可以使用默认值
                
                discoveryType = "manual" // 手动输入方式
            )
            
            val apiEndpointResult = serverProvider.insertOrUpdateServer(serverEntity)
            
            // 对于手动添加的服务器，自动设置为已连接
            serverProvider.updateConnectionStatus(apiEndpointResult, true)
            _serverConnectionStatus.postValue(true)
            
            apiEndpointResult
        }
    }
    
    /**
     * 连接到指定服务器
     */
    suspend fun connectToServer(apiEndpoint: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 先断开所有服务器连接
                serverProvider.disconnectAllServers()
                
                // 连接到指定服务器
                serverProvider.updateConnectionStatus(apiEndpoint, true)
                
                // 更新UI状态
                val serverEntity = serverProvider.getServerByKey(apiEndpoint)
                if (serverEntity != null) {
                    val server = Server(
                        serviceName = serverEntity.name,
                        name = serverEntity.name,
                        hostname = serverEntity.hostname,
                        platform = serverEntity.platform,
                        apiEndpoint = serverEntity.apiEndpoint,
                        connected = true
                    )
                    _currentServer.postValue(server)
                    _serverConnectionStatus.postValue(true)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    /**
     * 断开服务器连接
     */
    suspend fun disconnectServer() {
        withContext(Dispatchers.IO) {
            serverProvider.disconnectAllServers()
            _serverConnectionStatus.postValue(false)
            _currentServer.postValue(null)
        }
    }
    

    
    /**
     * 更新服务器信息
     */
    suspend fun updateServerInfo(apiEndpoint: String, version: String? = null) {
        withContext(Dispatchers.IO) {
            val serverEntity = serverProvider.getServerByKey(apiEndpoint)
            if (serverEntity != null) {
                val updatedServer = serverEntity.copy(
                    version = version ?: serverEntity.version,
                    isConnected = true,
                    lastConnectedTime = System.currentTimeMillis()
                )
                serverProvider.updateServer(updatedServer)
            }
        }
    }
    
    /**
     * 删除服务器
     */
    suspend fun deleteServer(apiEndpoint: String) {
        withContext(Dispatchers.IO) {
            // 如果删除的是当前连接的服务器，断开连接
            val currentConnectedServer = _currentServer.value
            if (currentConnectedServer != null) {
                val serverEntity = serverProvider.getServerByKey(apiEndpoint)
                if (serverEntity != null && serverEntity.apiEndpoint == apiEndpoint) {
                    disconnectServer()
                }
            }
            
            serverProvider.deleteServerByKey(apiEndpoint)
        }
    }
    
    /**
     * 初始化服务器状态
     */
    suspend fun initializeServerStatus() {
        withContext(Dispatchers.IO) {
            val connectedServerEntity = serverProvider.getConnectedServer().value
            if (connectedServerEntity != null) {
                val server = Server(
                    serviceName = connectedServerEntity.name,
                    name = connectedServerEntity.name,
                    hostname = connectedServerEntity.hostname,
                    platform = connectedServerEntity.platform,
                    apiEndpoint = connectedServerEntity.apiEndpoint,
                    connected = true
                )
                _currentServer.postValue(server)
                _serverConnectionStatus.postValue(true)
            } else {
                _serverConnectionStatus.postValue(false)
                _currentServer.postValue(null)
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