package com.autodroid.trader.data.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.autodroid.trader.MyApplication
import com.autodroid.trader.data.database.TradePlanProvider
import com.autodroid.trader.data.dao.TradePlanEntity
import com.autodroid.trader.model.TradePlan
import com.autodroid.trader.model.TradePlanStatus
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
    
    /**
     * 获取ApiClient实例
     * 通过MyApplication获取已配置的ApiClient，不直接创建实例
     */
    private fun getApiClient(): ApiClient? {
        return (context as? MyApplication)?.getApiClient()
    }
    
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
                
                val client = getApiClient()
                if (client == null) {
                    Log.e("TradePlanRepository", "ApiClient未初始化，无法获取交易计划")
                    return@withContext emptyList()
                }
                
                // 调用API获取交易计划列表
                val tradePlans = client.getTradePlans()
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
    
    /**
     * 更新交易计划状态（待批准/已批准）并同步到服务器
     */
    suspend fun updateTradePlanStatus(id: String, status: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // 先更新本地数据库
                tradePlanProvider.updateTradePlanStatus(id, status)
                Log.d("TradePlanRepository", "本地交易计划状态已更新: $id -> $status")
                
                // 获取已连接的服务器
                val serverRepository = ServerRepository.getInstance(context as Application)
                val connectedServer = serverRepository.getAndSyncServer().value
                
                if (connectedServer != null) {
                    try {
                        val client = getApiClient()
                        if (client == null) {
                            Log.e("TradePlanRepository", "ApiClient未初始化，无法同步交易计划状态")
                            return@withContext "本地状态已更新，但ApiClient未初始化"
                        }
                        
                        // 调用API更新服务器上的交易计划状态
                        val response = client.updateTradePlanStatus(id, status)
                        Log.d("TradePlanRepository", "服务器交易计划状态已更新: $response")
                        return@withContext "交易计划状态已更新并同步到服务器: $id -> $status"
                    } catch (e: Exception) {
                        Log.e("TradePlanRepository", "同步交易计划状态到服务器失败: ${e.message}")
                        return@withContext "本地状态已更新，但同步到服务器失败: ${e.message}"
                    }
                } else {
                    Log.w("TradePlanRepository", "未连接到服务器，仅更新本地状态")
                    return@withContext "本地状态已更新，但未连接到服务器"
                }
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "更新交易计划状态失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 执行交易计划
     */
    suspend fun executeTradePlan(id: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // 获取已连接的服务器
                val serverRepository = ServerRepository.getInstance(context as Application)
                val connectedServer = serverRepository.getAndSyncServer().value
                
                if (connectedServer == null) {
                    throw Exception("未连接到服务器，无法执行交易计划")
                }
                
                try {
                    val client = getApiClient()
                    if (client == null) {
                        throw Exception("ApiClient未初始化，无法执行交易计划")
                    }
                    
                    // 调用API执行交易计划
                    val response = client.executeTradePlan(id)
                    Log.d("TradePlanRepository", "交易计划执行请求已发送: $id")
                    
                    // 更新本地执行信息
                    updateExecutionInfo(id)
                    
                    // 更新执行结果
                    tradePlanProvider.updateExecutionResult(id, TradePlanStatus.EXECUTING.value, "交易计划执行请求已发送")
                    
                    return@withContext "交易计划执行请求已发送: $id"
                } catch (e: Exception) {
                    Log.e("TradePlanRepository", "执行交易计划失败: ${e.message}")
                    
                    // 更新执行结果
                    tradePlanProvider.updateExecutionResult(id, TradePlanStatus.FAILED.value, "执行失败: ${e.message}")
                    
                    throw e
                }
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "执行交易计划失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 获取所有交易计划（从服务器）
     */
    suspend fun getAllTradePlansFromServer(): List<TradePlan> {
        return withContext(Dispatchers.IO) {
            try {
                val client = getApiClient()
                if (client == null) {
                    Log.e("TradePlanRepository", "ApiClient未初始化，无法获取交易计划")
                    throw Exception("ApiClient未初始化，无法获取交易计划")
                }
                
                // 调用API获取所有交易计划
                val tradePlans = client.getAllTradePlans()
                Log.d("TradePlanRepository", "从服务器获取到 ${tradePlans.size} 个交易计划")
                
                // 获取已连接的服务器信息
                val serverRepository = ServerRepository.getInstance(context as Application)
                val connectedServer = serverRepository.getAndSyncServer().value
                
                if (connectedServer != null) {
                    // 从API端点中提取IP和端口
                    val urlParts = connectedServer.apiEndpoint().replace("http://", "").replace("https://", "").split("/")
                    val hostPort = urlParts.getOrNull(0) ?: return@withContext tradePlans
                    val hostParts = hostPort.split(":")
                    val serverIp = hostParts.getOrNull(0) ?: return@withContext tradePlans
                    val serverPort = hostParts.getOrNull(1)?.toIntOrNull() ?: return@withContext tradePlans
                    
                    // 将获取的交易计划保存到本地数据库
                    tradePlans.forEach { tradePlan ->
                        val tradePlanEntity = TradePlanEntity.fromTradePlan(tradePlan, serverIp, serverPort)
                        insertOrUpdateTradePlan(tradePlanEntity)
                    }
                }
                
                return@withContext tradePlans
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "获取所有交易计划失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 执行所有已批准的交易计划
     */
    suspend fun executeApprovedPlans(): String {
        return withContext(Dispatchers.IO) {
            try {
                val client = getApiClient()
                if (client == null) {
                    throw Exception("ApiClient未初始化，无法执行交易计划")
                }
                
                // 调用API执行所有已批准的交易计划
                val response = client.executeApprovedPlans()
                Log.d("TradePlanRepository", "已批准的交易计划执行请求已发送")
                
                // 更新所有已批准交易计划的执行信息
                val approvedPlans = getApprovedTradePlans()
                approvedPlans.value?.forEach { tradePlan ->
                    updateExecutionInfo(tradePlan.id)
                    tradePlanProvider.updateExecutionResult(tradePlan.id, TradePlanStatus.EXECUTING.value, "交易计划执行请求已发送")
                }
                
                return@withContext "已批准的交易计划执行请求已发送"
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "执行已批准的交易计划失败: ${e.message}")
                
                // 更新所有已批准交易计划的执行结果
                val approvedPlans = getApprovedTradePlans()
                approvedPlans.value?.forEach { tradePlan ->
                    tradePlanProvider.updateExecutionResult(tradePlan.id, TradePlanStatus.FAILED.value, "执行失败: ${e.message}")
                }
                
                throw e
            }
        }
    }
    
    /**
     * 获取待批准的交易计划
     */
    fun getPendingTradePlans(): LiveData<List<TradePlanEntity>> {
        // 启动异步任务更新交易计划信息
        updateTradePlansFromServers()
        return tradePlanProvider.getPendingTradePlans()
    }
    
    /**
     * 获取已批准的交易计划
     */
    fun getApprovedTradePlans(): LiveData<List<TradePlanEntity>> {
        // 启动异步任务更新交易计划信息
        updateTradePlansFromServers()
        return tradePlanProvider.getApprovedTradePlans()
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