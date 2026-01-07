package com.autodroid.trader.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy

/**
 * 交易计划数据访问对象
 * 提供对交易计划信息的增删改查操作
 */
@Dao
interface TradePlanDao {
    
    /**
     * 获取所有交易计划
     */
    @Query("SELECT * FROM trade_plans ORDER BY updatedAt DESC")
    fun getAllTradePlans(): LiveData<List<TradePlanEntity>>

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
     * 获取交易计划数量
     */
    @Query("SELECT COUNT(*) FROM trade_plans")
    fun getTradePlanCount(): Int

    /**
     * 更新交易计划状态（待批准/已批准）
     */
    @Query("UPDATE trade_plans SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    fun updateTradePlanStatus(id: String, status: String, updatedAt: String)

    /**
     * 根据状态获取交易计划
     * 当 status = "ALL" 时，返回所有交易计划
     */
    @Query("SELECT * FROM trade_plans WHERE (:status = 'ALL' OR status = :status) ORDER BY createdAt DESC")
    fun getTradePlansByStatus(status: String): LiveData<List<TradePlanEntity>>
}