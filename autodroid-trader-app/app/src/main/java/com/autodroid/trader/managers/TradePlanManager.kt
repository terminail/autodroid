// TradePlanManager.kt
package com.autodroid.trader.managers

import android.app.Application
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.MyApplication
import com.autodroid.trader.data.repository.TradePlanRepository
import com.autodroid.trader.data.dao.TradePlanEntity
import com.google.gson.Gson

class TradePlanManager private constructor(private val context: Context?, private val appViewModel: AppViewModel) {
    private val gson: Gson
    private val inflater: LayoutInflater
    private var tradePlanRepository: TradePlanRepository? = null
    
    companion object {
        private const val TAG = "TradePlanManager"
        @Volatile
        private var INSTANCE: TradePlanManager? = null
        
        fun getInstance(context: Context?, appViewModel: AppViewModel): TradePlanManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TradePlanManager(context, appViewModel).also { INSTANCE = it }
            }
        }
        
        fun getInstance(context: Context): TradePlanManager {
            return INSTANCE ?: synchronized(this) {
                // 创建一个默认的AppViewModel实例
                val appViewModel = AppViewModel.getInstance(context.applicationContext as Application)
                INSTANCE ?: TradePlanManager(context, appViewModel).also { INSTANCE = it }
            }
        }
    }

    init {
        this.gson = Gson()
        this.inflater = LayoutInflater.from(context)
        
        context?.let {
            val application = it.applicationContext as? MyApplication
            application?.let { app ->
                this.tradePlanRepository = TradePlanRepository.getInstance(app)
                Log.d(TAG, "TradePlanRepository initialized successfully")
            }
        }
    }

    /**
     * 更新交易计划状态并同步（先更新本地，再同步到服务器）
     * 用于需要立即反映本地状态的场景
     * @param id 交易计划ID
     * @param status 新状态
     * @return 更新后的TradePlanEntity，如果更新失败返回null
     */
    suspend fun updateAndSyncTradePlan(id: String, status: String): TradePlanEntity? {
        return try {
            if (tradePlanRepository == null) {
                throw Exception("交易计划仓库未初始化")
            }
            
            Log.d(TAG, "updateAndSyncTradePlan: 开始更新交易计划状态 - ID: $id, 状态: $status")
            
            val result = tradePlanRepository!!.updateTradePlanStatusAndSync(id, status)
            
            if (result != null) {
                Log.d(TAG, "updateAndSyncTradePlan: 交易计划状态更新成功 - ID: ${result.id}, 状态: ${result.status}")
            } else {
                Log.e(TAG, "updateAndSyncTradePlan: 交易计划状态更新失败")
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "updateAndSyncTradePlan: 更新交易计划状态失败 - ${e.message}", e)
            throw e
        }
    }

    /**
     * 根据状态获取交易计划（从本地数据库）
     * UI层只从本地数据库获取数据，Repository层负责在后台同步服务器数据
     * @param status 交易计划状态
     * @return 交易计划列表
     */
    suspend fun getTradePlansByStatus(status: String): List<TradePlanEntity> {
        return try {
            if (tradePlanRepository == null) {
                throw Exception("交易计划仓库未初始化")
            }
            
            Log.d(TAG, "getTradePlansByStatus: 开始获取交易计划 - 状态: $status")
            
            // 从本地数据库获取数据
            val liveData = tradePlanRepository!!.getTradePlansByStatus(status)
            val result = mutableListOf<TradePlanEntity>()
            
            // 获取LiveData的当前值
            liveData.value?.let { result.addAll(it) }
            
            Log.d(TAG, "getTradePlansByStatus: 获取到 ${result.size} 个交易计划")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "getTradePlansByStatus: 获取交易计划失败 - ${e.message}", e)
            throw e
        }
    }

    /**
     * 根据状态获取交易计划并同步（LiveData，本地优先）
     * 根据"本地优先"设计理念，先返回本地缓存数据，然后在后台同步服务器数据
     * @param status 交易计划状态
     * @return 交易计划列表的LiveData
     */
    fun getTradePlansByStatusAndSync(status: String): androidx.lifecycle.LiveData<List<TradePlanEntity>> {
        return try {
            if (tradePlanRepository == null) {
                Log.e(TAG, "getTradePlansByStatusAndSync: 交易计划仓库未初始化")
                androidx.lifecycle.MutableLiveData(emptyList())
            } else {
                tradePlanRepository!!.getAndSyncTradePlansByStatus(status)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTradePlansByStatusAndSync: 获取交易计划失败 - ${e.message}", e)
            androidx.lifecycle.MutableLiveData(emptyList())
        }
    }
    
    /**
     * 执行所有已批准的交易计划
     * @return 执行结果消息
     */
    suspend fun executeApprovedPlans(): String {
        return try {
            if (tradePlanRepository == null) {
                throw Exception("交易计划仓库未初始化")
            }
            
            Log.d(TAG, "executeApprovedPlans: 开始执行所有已批准的交易计划")
            val result = tradePlanRepository!!.executeApprovedPlans()
            Log.d(TAG, "executeApprovedPlans: 已批准的交易计划执行请求已发送 - $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "executeApprovedPlans: 执行已批准的交易计划失败 - ${e.message}", e)
            throw e
        }
    }
    
    suspend fun stopApprovedPlans(): String {
        return try {
            if (tradePlanRepository == null) {
                throw Exception("交易计划仓库未初始化")
            }
            
            Log.d(TAG, "stopApprovedPlans: 停止所有正在执行的交易计划")
            val result = tradePlanRepository!!.stopApprovedPlans()
            Log.d(TAG, "stopApprovedPlans: 停止交易计划请求已发送 - $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "stopApprovedPlans: 停止交易计划失败 - ${e.message}", e)
            throw e
        }
    }
}