package com.autodroid.trader.app.data.database

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.autodroid.trader.app.data.dao.TradePlanEntity
import com.autodroid.trader.app.network.TradePlanStatus
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
     * 获取所有交易计划
     */
    fun getAllTradePlans(): LiveData<List<TradePlanEntity>> {
        return tradePlanDao.getAllTradePlans()
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
     * 插入或更新交易计划
     */
    suspend fun insertOrUpdateTradePlan(tradePlan: TradePlanEntity): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("TradePlanProvider", "insertOrUpdateTradePlan: 开始处理交易计划 id=${tradePlan.id}")
                val existingTradePlan = tradePlanDao.getTradePlanById(tradePlan.id)
                Log.d("TradePlanProvider", "insertOrUpdateTradePlan: existingTradePlan=${existingTradePlan != null}")
                
                val result: String = if (existingTradePlan != null) {
                    val updatedTradePlan = tradePlan.copy(updatedAt = java.time.Instant.now().toString())
                    Log.d("TradePlanProvider", "insertOrUpdateTradePlan: 更新现有交易计划")
                    tradePlanDao.updateTradePlan(updatedTradePlan)
                    tradePlan.id
                } else {
                    val newTradePlan = tradePlan.copy(updatedAt = java.time.Instant.now().toString())
                    Log.d("TradePlanProvider", "insertOrUpdateTradePlan: 插入新交易计划")
                    val rowId = tradePlanDao.insertTradePlan(newTradePlan)
                    Log.d("TradePlanProvider", "insertOrUpdateTradePlan: 插入结果 rowId=$rowId")
                    tradePlan.id
                }
                
                Log.d("TradePlanProvider", "insertOrUpdateTradePlan: 完成 result=$result")
                return@withContext result
            } catch (e: Exception) {
                Log.e("TradePlanProvider", "insertOrUpdateTradePlan: 失败 ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 更新交易计划信息
     */
    suspend fun updateTradePlan(tradePlan: TradePlanEntity) {
        withContext(Dispatchers.IO) {
            val updatedTradePlan = tradePlan.copy(updatedAt = java.time.Instant.now().toString())
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
     * 获取交易计划数量
     */
    suspend fun getTradePlanCount(): Int {
        return withContext(Dispatchers.IO) {
            val count = tradePlanDao.getTradePlanCount()
            Log.d("TradePlanProvider", "getTradePlanCount: 数据库中当前交易计划数量 = $count")
            count
        }
    }
    
    /**
     * 获取所有交易计划（同步方法，用于调试）
     */
    suspend fun getAllTradePlansSync(): List<TradePlanEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val db = database.openHelper.writableDatabase
                val cursor = db.query("SELECT * FROM trade_plans")
                val count = cursor.count
                Log.d("TradePlanProvider", "getAllTradePlansSync: 直接查询数据库，找到 $count 条记录")
                
                if (count > 0) {
                    cursor.moveToFirst()
                    val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    val status = cursor.getString(cursor.getColumnIndexOrThrow("status"))
                    Log.d("TradePlanProvider", "第一条记录: id=$id, name=$name, status=$status")
                }
                
                cursor.close()
                
                val entities = tradePlanDao.getTradePlansByStatus(TradePlanStatus.ALL.value).value ?: emptyList()
                Log.d("TradePlanProvider", "getAllTradePlansSync: LiveData value 返回 ${entities.size} 个实体")
                entities
            } catch (e: Exception) {
                Log.e("TradePlanProvider", "getAllTradePlansSync: 查询失败 ${e.message}", e)
                emptyList()
            }
        }
    }
    
    /**
     * 更新交易计划状态（待批准/已批准）
     */
    suspend fun updateTradePlanStatus(id: String, status: String) {
        withContext(Dispatchers.IO) {
            val now = java.time.Instant.now().toString()
            tradePlanDao.updateTradePlanStatus(id, status, now)
        }
    }
    
    /**
     * 根据状态获取交易计划
     */
    fun getTradePlansByStatus(status: String): LiveData<List<TradePlanEntity>> {
        return tradePlanDao.getTradePlansByStatus(status)
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