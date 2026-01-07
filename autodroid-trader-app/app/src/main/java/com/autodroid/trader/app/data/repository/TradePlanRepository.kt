package com.autodroid.trader.app.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.autodroid.trader.app.MyApplication
import com.autodroid.trader.app.data.database.TradePlanProvider
import com.autodroid.trader.app.data.dao.TradePlanEntity
import com.autodroid.trader.app.model.TradeData
import com.autodroid.trader.app.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 交易计划仓库类
 * 提供对交易计划数据的统一访问接口，协调数据源和业务逻辑
 */
class TradePlanRepository private constructor(application: MyApplication) {
    
    private val tradePlanProvider = TradePlanProvider.getInstance(application)
    private val context = application
    private val myApplication = application
    
    /**
     * 根据ID获取交易计划
     */
    suspend fun getTradePlanById(id: String): TradePlanEntity? {
        return withContext(Dispatchers.IO) {
            tradePlanProvider.getTradePlanById(id)
        }
    }
    
    /**
     * 根据状态获取交易计划
     * 只返回本地数据库数据，不主动同步
     */
    fun getTradePlansByStatus(status: String): LiveData<List<TradePlanEntity>> {
        return tradePlanProvider.getTradePlansByStatus(status)
    }
    
    /**
     * 根据状态获取交易计划并同步
     * 根据"本地优先"设计理念，先返回本地缓存数据，然后在后台同步服务器数据
     */
    fun getAndSyncTradePlansByStatus(status: String): LiveData<List<TradePlanEntity>> {
        syncTradePlans()
        return tradePlanProvider.getTradePlansByStatus(status)
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
     * 更新交易计划信息
     */
    suspend fun update(tradePlanEntity: TradePlanEntity) {
        val updatedTradePlan = tradePlanEntity.copy(updatedAt = java.time.Instant.now().toString())
        tradePlanProvider.updateTradePlan(updatedTradePlan)
    }
    
    /**
     * 插入或更新交易计划
     */
    suspend fun insertOrUpdateTradePlan(tradePlanEntity: TradePlanEntity): String {
        return withContext(Dispatchers.IO) {
            try {
                val existingTradePlan = tradePlanProvider.getTradePlanById(tradePlanEntity.id)
                
                if (existingTradePlan != null) {
                    val updatedTradePlan = tradePlanEntity.copy(updatedAt = java.time.Instant.now().toString())
                    tradePlanProvider.updateTradePlan(updatedTradePlan)
                    Log.d("TradePlanRepository", "交易计划已更新: ${tradePlanEntity.name}")
                    return@withContext "交易计划已更新: ${tradePlanEntity.name}"
                } else {
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
     * 更新交易计划状态并同步（先更新本地，再同步到服务器）
     * 用于需要立即反映本地状态的场景
     * @return 更新后的TradePlanEntity，如果更新失败返回null
     */
    suspend fun updateTradePlanStatusAndSync(id: String, status: String): TradePlanEntity? {
        return withContext(Dispatchers.IO) {
            try {
                val client = getApiClient()
                
                val response = client.updateTradePlanStatus(id, status)
                Log.d("TradePlanRepository", "服务器交易计划状态已更新: $response")
                
                if (response.tradePlanResponse!=null) {
                    val tradePlanEntity = TradePlanEntity.fromTradePlan(response.tradePlanResponse)
                    insertOrUpdateTradePlan(tradePlanEntity)
                    
                    return@withContext tradePlanEntity
                } else {
                    Log.e("TradePlanRepository", "服务器返回失败: ${response.message}")
                    return@withContext null
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
    suspend fun startTradePlan(id: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val client = getApiClient()
                
                // 调用API执行交易计划
                client.startTradePlan(id)
                Log.d("TradePlanRepository", "交易计划执行请求已发送: $id")
                
                return@withContext "交易计划执行请求已发送: $id"
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "执行交易计划失败: ${e.message}")
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
                client.executeApprovedPlans()
                Log.d("TradePlanRepository", "已批准的交易计划执行请求已发送")
                return@withContext "已批准的交易计划执行请求已发送"
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "执行已批准的交易计划失败: ${e.message}")
                throw e
            }
        }
    }
    
    suspend fun stopApprovedPlans(): String {
        return withContext(Dispatchers.IO) {
            try {
                val client = getApiClient()
                client.stopApprovedPlans()
                Log.d("TradePlanRepository", "停止已批准的交易计划请求已发送")
                return@withContext "停止已批准的交易计划请求已发送"
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "停止已批准的交易计划失败: ${e.message}")
                throw e
            }
        }
    }
    
    /**
     * 获取ApiClient实例
     * 通过MyApplication获取已配置的ApiClient，不直接创建实例
     */
    private fun getApiClient(): ApiClient {
        return myApplication.getApiClient() ?: throw Exception("API客户端未初始化")
    }
    
    /**
     * 从服务器同步交易计划信息
     * 根据"本地优先"设计理念，主动检查服务器上的交易计划并更新本地数据库
     */
    private fun syncTradePlans() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("TradePlanRepository", "从服务器获取交易计划")
                
                val client = getApiClient()
                
                Log.d("TradePlanRepository", "开始调用API获取交易计划列表")
                val tradePlans = client.getTradePlans()
                Log.d("TradePlanRepository", "从服务器获取到 ${tradePlans.size} 个交易计划")
                
                if (tradePlans.isEmpty()) {
                    Log.w("TradePlanRepository", "交易计划列表为空，跳过数据库插入")
                    return@launch
                }
                
                Log.d("TradePlanRepository", "开始将 ${tradePlans.size} 个交易计划保存到本地数据库")
                var successCount = 0
                var failureCount = 0
                
                for ((index, tradePlan) in tradePlans.withIndex()) {
                    try {
                        Log.d("TradePlanRepository", "处理第 ${index + 1}/${tradePlans.size} 个交易计划: ${tradePlan.id}")
                        val tradePlanEntity = TradePlanEntity.fromTradePlan(tradePlan)
                        Log.d("TradePlanRepository", "创建TradePlanEntity: id=${tradePlanEntity.id}, name=${tradePlanEntity.name}")
                        
                        val result = insertOrUpdateTradePlan(tradePlanEntity)
                        Log.d("TradePlanRepository", "交易计划保存结果: $result")
                        successCount++
                    } catch (e: Exception) {
                        Log.e("TradePlanRepository", "保存第 ${index + 1} 个交易计划失败: ${e.message}", e)
                        failureCount++
                    }
                }
                
                Log.d("TradePlanRepository", "交易计划保存完成: 成功 $successCount 个, 失败 $failureCount 个")
                Log.d("TradePlanRepository", "交易计划同步成功")
            } catch (e: Exception) {
                Log.e("TradePlanRepository", "同步交易计划失败: ${e.message}")
            }
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: TradePlanRepository? = null
        
        /**
         * 获取交易计划仓库实例（单例模式）
         */
        fun getInstance(application: MyApplication): TradePlanRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TradePlanRepository(application)
                INSTANCE = instance
                instance
            }
        }
    }
}
