package com.autodroid.trader.data.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.autodroid.trader.data.database.TradePlanProvider
import com.autodroid.trader.data.dao.TradePlanEntity
import com.autodroid.trader.model.TradePlan
import com.autodroid.trader.model.TradeData
import com.autodroid.trader.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 交易计划仓库类
 * 提供对交易计划数据的统一访问接口，协调数据源和业务逻辑
 */
class TradePlanRepository private constructor(application: Application) {
    
    private val tradePlanProvider = TradePlanProvider.getInstance(application)
    private val context = application.applicationContext
    private val apiClient = ApiClient.getInstance()
    
    /**
     * 更新交易计划信息
     */
    suspend fun update(tradePlanEntity: TradePlanEntity) {
        tradePlanProvider.updateTradePlan(tradePlanEntity)
    }
    
    /**
     * 从所有已连接的服务器更新交易计划信息
     * 根据"本地优先"设计理念，主动检查所有服务器上的交易计划并更新本地数据库
     */
    private fun updateTradePlansFromServers() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 获取所有已连接的服务器
                val serverRepository = ServerRepository.getInstance(context as Application)
                val connectedServer = serverRepository.getAndSyncServer().value
                
                if (connectedServer != null) {
                    try {
                        // 从已连接的服务器获取交易计划
                        fetchTradePlansFromServer(connectedServer.apiEndpoint())
                        Log.d("TradePlanRepository", "交易计划同步成功: ${connectedServer.name}")
                    } catch (e: Exception) {
                        Log.e("TradeRepository", "从服务器${connectedServer.name}同步交易计划失败: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "同步交易计划时出错: ${e.message}")
            }
        }
    }

    /**
     * 获取所有交易计划
     * 根据"本地优先"设计理念，主动检查交易计划状态并更新本地数据库
     */
    fun getAllTradePlans(): LiveData<List<TradePlanEntity>> {
        // 启动异步任务更新交易计划信息
        updateTradePlansFromServers()
        return tradePlanProvider.getAllTradePlans()
    }
    
    /**
     * 获取激活的交易计划
     * 根据"本地优先"设计理念，主动检查交易计划状态并更新本地数据库
     */
    fun getActiveTradePlans(): LiveData<List<TradePlanEntity>> {
        // 启动异步任务更新交易计划信息
        updateTradePlansFromServers()
        return tradePlanProvider.getActiveTradePlans()
    }

    /**
     * 获取当前交易计划
     * 根据"本地优先"设计理念，主动检查交易计划状态并更新本地数据库
     */
    fun getCurrentTradePlan(): LiveData<TradePlanEntity?> {
        // 启动异步任务更新交易计划信息
        updateTradePlansFromServers()
        return tradePlanProvider.getLastUpdatedTradePlan()
    }
    
    /**
     * 根据ID获取交易计划
     */
    suspend fun getTradePlanById(id: String): TradePlanEntity? {
        return withContext(Dispatchers.IO) {
            tradePlanProvider.getTradePlanById(id)
        }
    }
    
    /**
     * 根据服务器获取交易计划列表
     */
    fun getTradePlansByServer(apiEndpoint: String): LiveData<List<TradePlanEntity>> {
        // 从API端点中提取IP和端口
        val urlParts = apiEndpoint.replace("http://", "").replace("https://", "").split("/")
        val hostPort = urlParts.getOrNull(0) ?: return tradePlanProvider.getAllTradePlans()
        val hostParts = hostPort.split(":")
        val serverIp = hostParts.getOrNull(0) ?: return tradePlanProvider.getAllTradePlans()
        val serverPort = hostParts.getOrNull(1)?.toIntOrNull() ?: return tradePlanProvider.getAllTradePlans()
        
        return tradePlanProvider.getTradePlansByServer(serverIp, serverPort)
    }
    
    /**
     * 插入或更新交易计划
     */
    suspend fun insertOrUpdateTradePlan(tradePlanEntity: TradePlanEntity): String {
        return withContext(Dispatchers.IO) {
            try {
                // 检查交易计划是否已存在
                val existingTradePlan = tradePlanProvider.getTradePlanById(tradePlanEntity.id)
                
                if (existingTradePlan != null) {
                    // 更新现有交易计划
                    tradePlanProvider.updateTradePlan(tradePlanEntity)
                    Log.d("TradePlanRepository", "交易计划已更新: ${tradePlanEntity.name}")
                    return@withContext "交易计划已更新: ${tradePlanEntity.name}"
                } else {
                    // 插入新交易计划
                    tradePlanProvider.insertOrUpdateTradePlan(tradePlanEntity)
                    Log.d("TradePlanRepository", "新交易计划已添加: ${tradePlanEntity.name}")
                    return@withContext "新交易计划已添加: ${tradePlanEntity.name}"
                }
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "插入或更新交易计划失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 从TradePlan模型创建并插入交易计划
     */
    suspend fun insertOrUpdateTradePlan(tradePlan: TradePlan, 
                                       sourceServerIp: String? = null, 
                                       sourceServerPort: Int? = null): String {
        return withContext(Dispatchers.IO) {
            try {
                val tradePlanEntity = TradePlanEntity.fromTradePlan(tradePlan, sourceServerIp, sourceServerPort)
                insertOrUpdateTradePlan(tradePlanEntity)
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "从TradePlan模型创建交易计划失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 删除交易计划
     */
    suspend fun deleteTradePlan(id: String) {
        withContext(Dispatchers.IO) {
            try {
                tradePlanProvider.deleteTradePlanById(id)
                Log.d("TradePlanRepository", "交易计划已删除: $id")
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "删除交易计划失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 根据服务器删除所有交易计划
     */
    suspend fun deleteTradePlansByServer(apiEndpoint: String) {
        withContext(Dispatchers.IO) {
            try {
                // 从API端点中提取IP和端口
                val urlParts = apiEndpoint.replace("http://", "").replace("https://", "").split("/")
                val hostPort = urlParts.getOrNull(0) ?: return@withContext
                val hostParts = hostPort.split(":")
                val serverIp = hostParts.getOrNull(0) ?: return@withContext
                val serverPort = hostParts.getOrNull(1)?.toIntOrNull() ?: return@withContext
                
                tradePlanProvider.deleteTradePlansByServer(serverIp, serverPort)
                Log.d("TradePlanRepository", "服务器 $apiEndpoint 的所有交易计划已删除")
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "删除服务器交易计划失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 更新交易计划激活状态
     */
    suspend fun updateActiveStatus(id: String, isActive: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                tradePlanProvider.updateActiveStatus(id, isActive)
                Log.d("TradePlanRepository", "交易计划 $id 激活状态已更新为: $isActive")
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "更新交易计划激活状态失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 更新交易计划执行信息
     */
    suspend fun updateExecutionInfo(id: String) {
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                tradePlanProvider.updateExecutionInfo(id, now)
                Log.d("TradePlanRepository", "交易计划 $id 执行信息已更新")
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "更新交易计划执行信息失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 停用所有交易计划
     */
    suspend fun deactivateAllTradePlans() {
        withContext(Dispatchers.IO) {
            try {
                tradePlanProvider.deactivateAllTradePlans()
                Log.d("TradePlanRepository", "所有交易计划已停用")
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "停用所有交易计划失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 从服务器获取交易计划列表
     */
    suspend fun fetchTradePlansFromServer(apiEndpoint: String): List<TradePlan> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("TradePlanRepository", "从服务器获取交易计划: $apiEndpoint")
                
                // 设置API端点
                apiClient.setApiEndpoint(apiEndpoint)
                
                // 调用API获取交易计划列表
                val tradePlans = apiClient.getTradePlans()
                Log.d("TradePlanRepository", "从服务器获取到 ${tradePlans.size} 个交易计划")
                
                // 从API端点中提取IP和端口
                val urlParts = apiEndpoint.replace("http://", "").replace("https://", "").split("/")
                val hostPort = urlParts.getOrNull(0) ?: return@withContext emptyList()
                val hostParts = hostPort.split(":")
                val serverIp = hostParts.getOrNull(0) ?: return@withContext emptyList()
                val serverPort = hostParts.getOrNull(1)?.toIntOrNull() ?: return@withContext emptyList()
                
                // 将获取的交易计划保存到本地数据库
                tradePlans.forEach { tradePlan ->
                    val tradePlanEntity = TradePlanEntity.fromTradePlan(tradePlan, serverIp, serverPort)
                    insertOrUpdateTradePlan(tradePlanEntity)
                }
                
                return@withContext tradePlans
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "从服务器获取交易计划失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 获取最新交易数据
     */
    suspend fun getLatestTradeData(): TradeData {
        return withContext(Dispatchers.IO) {
            try {
                // 模拟获取最新交易数据
                // 在实际应用中，这里应该调用API获取实时数据
                val tradeData = TradeData(
                    price = "123.45",
                    changePercent = "+2.34",
                    volume = "123456"
                )
                // Log.d("TradePlanRepository", "获取到最新交易数据: $tradeData")
                return@withContext tradeData
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "获取最新交易数据失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: TradePlanRepository? = null
        
        /**
         * 获取交易计划仓库实例（单例模式）
         */
        fun getInstance(application: Application): TradePlanRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TradePlanRepository(application)
                INSTANCE = instance
                instance
            }
        }
    }
}