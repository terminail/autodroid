package com.autodroid.trader.data.database

import android.content.Context
import androidx.lifecycle.LiveData
import com.autodroid.trader.data.dao.TradePlanEntity
import com.autodroid.trader.model.TradePlanStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 交易计划数据提供者
 * 提供对交易计划数据的数据库操作封装，协调DAO层和业务逻辑层
 */
class TradePlanProvider private constructor(context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val tradePlanDao = database.tradePlanDao()
    
    /**
     * 获取所有交易计划列表
     */
    fun getAllTradePlans(): LiveData<List<TradePlanEntity>> {
        return tradePlanDao.getAllTradePlans()
    }
    
    /**
     * 获取激活的交易计划列表
     */
    fun getActiveTradePlans(): LiveData<List<TradePlanEntity>> {
        return tradePlanDao.getActiveTradePlans()
    }

    /**
     * 获取最后更新的交易计划
     */
    fun getLastUpdatedTradePlan(): LiveData<TradePlanEntity?> {
        return tradePlanDao.getLastUpdatedTradePlan()
    }
    
    /**
     * 根据ID获取交易计划
     */
    suspend fun getTradePlanById(id: String): TradePlanEntity? {
        return withContext(Dispatchers.IO) {
            tradePlanDao.getTradePlanById(id)
        }
    }
    
    /**
     * 根据服务器获取交易计划列表
     */
    fun getTradePlansByServer(serverIp: String, serverPort: Int): LiveData<List<TradePlanEntity>> {
        return tradePlanDao.getTradePlansByServer(serverIp, serverPort)
    }
    
    /**
     * 插入或更新交易计划
     */
    suspend fun insertOrUpdateTradePlan(tradePlan: TradePlanEntity): String {
        return withContext(Dispatchers.IO) {
            // 检查是否已存在相同ID的交易计划
            val existingTradePlan = tradePlanDao.getTradePlanById(tradePlan.id)
            
            val result: String = if (existingTradePlan != null) {
                // 更新现有交易计划
                val updatedTradePlan = tradePlan.copy(updatedAt = System.currentTimeMillis())
                tradePlanDao.updateTradePlan(updatedTradePlan)
                tradePlan.id
            } else {
                // 插入新交易计划
                tradePlanDao.insertTradePlan(tradePlan)
                tradePlan.id
            }
            
            return@withContext result
        }
    }
    
    /**
     * 更新交易计划信息
     */
    suspend fun updateTradePlan(tradePlan: TradePlanEntity) {
        withContext(Dispatchers.IO) {
            val updatedTradePlan = tradePlan.copy(updatedAt = System.currentTimeMillis())
            tradePlanDao.updateTradePlan(updatedTradePlan)
        }
    }
    
    /**
     * 删除交易计划
     */
    suspend fun deleteTradePlan(tradePlan: TradePlanEntity) {
        withContext(Dispatchers.IO) {
            tradePlanDao.deleteTradePlan(tradePlan)
        }
    }
    
    /**
     * 根据ID删除交易计划
     */
    suspend fun deleteTradePlanById(id: String) {
        withContext(Dispatchers.IO) {
            tradePlanDao.deleteTradePlanById(id)
        }
    }

    /**
     * 根据服务器删除所有交易计划
     */
    suspend fun deleteTradePlansByServer(serverIp: String, serverPort: Int) {
        withContext(Dispatchers.IO) {
            tradePlanDao.deleteTradePlansByServer(serverIp, serverPort)
        }
    }
    
    /**
     * 更新交易计划激活状态
     */
    suspend fun updateActiveStatus(id: String, isActive: Boolean) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            tradePlanDao.updateActiveStatus(id, isActive, now)
        }
    }
    
    /**
     * 更新交易计划执行信息
     */
    suspend fun updateExecutionInfo(id: String, lastExecutedTime: Long) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            tradePlanDao.updateExecutionInfo(id, lastExecutedTime, now)
        }
    }
    
    /**
     * 停用所有交易计划
     */
    suspend fun deactivateAllTradePlans() {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            tradePlanDao.deactivateAllTradePlans(now)
        }
    }
    
    /**
     * 获取交易计划数量
     */
    suspend fun getTradePlanCount(): Int {
        return withContext(Dispatchers.IO) {
            tradePlanDao.getTradePlanCount()
        }
    }
    
    /**
     * 获取激活的交易计划数量
     */
    suspend fun getActiveTradePlanCount(): Int {
        return withContext(Dispatchers.IO) {
            tradePlanDao.getActiveTradePlanCount()
        }
    }
    
    /**
     * 更新交易计划状态（待批准/已批准）
     */
    suspend fun updateTradePlanStatus(id: String, status: String) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            tradePlanDao.updateTradePlanStatus(id, status, now)
        }
    }
    
    /**
     * 根据状态获取交易计划
     */
    fun getTradePlansByStatus(status: String): LiveData<List<TradePlanEntity>> {
        return tradePlanDao.getTradePlansByStatus(status)
    }
    
    /**
     * 获取所有待批准的交易计划
     */
    fun getPendingTradePlans(): LiveData<List<TradePlanEntity>> {
        return tradePlanDao.getPendingTradePlans(TradePlanStatus.PENDING.value)
    }
    
    /**
     * 获取所有已批准的交易计划
     */
    fun getApprovedTradePlans(): LiveData<List<TradePlanEntity>> {
        return tradePlanDao.getApprovedTradePlans(TradePlanStatus.APPROVED.value)
    }
    
    /**
     * 更新交易计划执行结果
     */
    suspend fun updateExecutionResult(id: String, executionStatus: String, executionResult: String) {
        withContext(Dispatchers.IO) {
            val existingTradePlan = tradePlanDao.getTradePlanById(id)
            if (existingTradePlan != null) {
                val updatedTradePlan = existingTradePlan.copy(
                    executionStatus = executionStatus,
                    lastExecutionResult = executionResult,
                    updatedAt = System.currentTimeMillis()
                )
                tradePlanDao.updateTradePlan(updatedTradePlan)
            }
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: TradePlanProvider? = null
        
        /**
         * 获取交易计划数据提供者实例（单例模式）
         */
        fun getInstance(context: Context): TradePlanProvider {
            return INSTANCE ?: synchronized(this) {
                val instance = TradePlanProvider(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}