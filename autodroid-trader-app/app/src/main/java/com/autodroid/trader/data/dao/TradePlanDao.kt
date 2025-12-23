package com.autodroid.trader.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import com.autodroid.trader.model.TradePlanStatus

/**
 * 交易计划数据访问对象
 * 提供对交易计划信息的增删改查操作
 */
@Dao
interface TradePlanDao {
    
    /**
     * 获取所有交易计划列表
     */
    @Query("SELECT * FROM trade_plans ORDER BY updatedAt DESC")
    fun getAllTradePlans(): LiveData<List<TradePlanEntity>>

    /**
     * 获取激活的交易计划列表
     */
    @Query("SELECT * FROM trade_plans WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getActiveTradePlans(): LiveData<List<TradePlanEntity>>

    /**
     * 获取最后更新的交易计划
     */
    @Query("SELECT * FROM trade_plans ORDER BY updatedAt DESC LIMIT 1")
    fun getLastUpdatedTradePlan(): LiveData<TradePlanEntity?>

    /**
     * 根据ID获取交易计划
     */
    @Query("SELECT * FROM trade_plans WHERE id = :id")
    fun getTradePlanById(id: String): TradePlanEntity?

    /**
     * 根据服务器获取交易计划列表
     */
    @Query("SELECT * FROM trade_plans WHERE sourceServerIp = :serverIp AND sourceServerPort = :serverPort ORDER BY updatedAt DESC")
    fun getTradePlansByServer(serverIp: String, serverPort: Int): LiveData<List<TradePlanEntity>>

    /**
     * 插入新交易计划
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTradePlan(tradePlan: TradePlanEntity): Long

    /**
     * 更新交易计划信息
     */
    @Update
    fun updateTradePlan(tradePlan: TradePlanEntity)

    /**
     * 删除交易计划
     */
    @Delete
    fun deleteTradePlan(tradePlan: TradePlanEntity)

    /**
     * 根据ID删除交易计划
     */
    @Query("DELETE FROM trade_plans WHERE id = :id")
    fun deleteTradePlanById(id: String)

    /**
     * 根据服务器删除所有交易计划
     */
    @Query("DELETE FROM trade_plans WHERE sourceServerIp = :serverIp AND sourceServerPort = :serverPort")
    fun deleteTradePlansByServer(serverIp: String, serverPort: Int)

    /**
     * 更新交易计划激活状态
     */
    @Query("UPDATE trade_plans SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    fun updateActiveStatus(id: String, isActive: Boolean, updatedAt: Long)

    /**
     * 更新交易计划执行信息
     */
    @Query("UPDATE trade_plans SET lastExecutedTime = :lastExecutedTime, executionCount = executionCount + 1, updatedAt = :updatedAt WHERE id = :id")
    fun updateExecutionInfo(id: String, lastExecutedTime: Long, updatedAt: Long)

    /**
     * 停用所有交易计划
     */
    @Query("UPDATE trade_plans SET isActive = 0, updatedAt = :updatedAt")
    fun deactivateAllTradePlans(updatedAt: Long)

    /**
     * 获取交易计划数量
     */
    @Query("SELECT COUNT(*) FROM trade_plans")
    fun getTradePlanCount(): Int

    /**
     * 获取激活的交易计划数量
     */
    @Query("SELECT COUNT(*) FROM trade_plans WHERE isActive = 1")
    fun getActiveTradePlanCount(): Int

    /**
     * 更新交易计划状态（待批准/已批准）
     */
    @Query("UPDATE trade_plans SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    fun updateTradePlanStatus(id: String, status: String, updatedAt: Long)

    /**
     * 根据状态获取交易计划
     */
    @Query("SELECT * FROM trade_plans WHERE status = :status ORDER BY updatedAt DESC")
    fun getTradePlansByStatus(status: String): LiveData<List<TradePlanEntity>>

    /**
     * 获取所有待批准的交易计划
     */
    @Query("SELECT * FROM trade_plans WHERE status = :status ORDER BY updatedAt DESC")
    fun getPendingTradePlans(status: String): LiveData<List<TradePlanEntity>>

    /**
     * 获取所有已批准的交易计划
     */
    @Query("SELECT * FROM trade_plans WHERE status = :status ORDER BY updatedAt DESC")
    fun getApprovedTradePlans(status: String): LiveData<List<TradePlanEntity>>
}