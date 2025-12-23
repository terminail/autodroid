// TradePlanManager.kt
package com.autodroid.trader.managers

import android.app.Application
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import com.autodroid.trader.R
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.model.TradePlan
import com.autodroid.trader.model.TradePlanStatus
import com.autodroid.trader.data.repository.TradePlanRepository
import com.autodroid.trader.data.dao.TradePlanEntity
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            val application = it.applicationContext as? Application
            application?.let { app ->
                this.tradePlanRepository = TradePlanRepository.getInstance(app)
                Log.d(TAG, "TradePlanRepository initialized successfully")
            }
        }
    }
    
    /**
     * 设置交易计划仓库
     */
    fun setTradePlanRepository(repository: TradePlanRepository) {
        this.tradePlanRepository = repository
    }

    fun handleTradePlans(tradeplansJson: String?) {
        try {
            val tradeplansElement =
                gson.fromJson<JsonElement>(tradeplansJson, JsonElement::class.java)
            val tradeplansLists: MutableList<TradePlan> =
                ArrayList<TradePlan>()

            if (tradeplansElement.isJsonObject()) {
                tradeplansLists.add(parseTradePlanObject(tradeplansElement.getAsJsonObject()))
            } else if (tradeplansElement.isJsonArray()) {
                val tradeplansArray = tradeplansElement.getAsJsonArray()
                for (tradeplanElement in tradeplansArray) {
                    if (tradeplanElement.isJsonObject()) {
                        tradeplansLists.add(parseTradePlanObject(tradeplanElement.getAsJsonObject()))
                    }
                }
            }

            appViewModel.setAvailableTradePlans(tradeplansLists)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse tradeplans: " + e.message)
        }
    }

    private fun parseTradePlanObject(tradeplan: JsonObject): TradePlan {
        return TradePlan(
            id = if (tradeplan.has("id")) tradeplan.get("id").getAsString() else null,
            name = if (tradeplan.has("name")) tradeplan.get("name").getAsString() else null,
            title = if (tradeplan.has("title")) tradeplan.get("title").getAsString() else null,
            subtitle = if (tradeplan.has("subtitle")) tradeplan.get("subtitle").getAsString() else null,
            description = if (tradeplan.has("description")) tradeplan.get("description").getAsString() else null,
            status = if (tradeplan.has("status")) tradeplan.get("status").getAsString() else null
        )
    }

    fun updateTradePlansUI(
        tradeplans: MutableList<TradePlan>?,
        container: LinearLayout,
        titleView: TextView
    ) {
        container.removeAllViews()

        if (tradeplans == null || tradeplans.isEmpty()) {
            titleView.setText("No trade plans available")
        } else {
            titleView.setText("Available Trade Plans")

            for (tradeplan in tradeplans) {
                val tradeplanItem = inflater.inflate(R.layout.item_trade_plan, null)

                val tradeplanName = tradeplanItem.findViewById<TextView>(R.id.trade_plan_name)
                val tradeplanInfoLine1 = tradeplanItem.findViewById<TextView>(R.id.trade_plan_info_line1)
                val tradeplanInfoLine2 = tradeplanItem.findViewById<TextView>(R.id.trade_plan_info_line2)
                val tradeplanStatus = tradeplanItem.findViewById<TextView>(R.id.trade_plan_status)

                // Use title if available, otherwise use name
                val displayName = tradeplan.title ?: tradeplan.name ?: "Unknown Trade Plan"
                tradeplanName.text = displayName
                
                // Format info line 1: stock code | name | closing price
                val infoLine1 = tradeplan.getDisplayInfoLine1()
                tradeplanInfoLine1.text = infoLine1
                
                // Format info line 2: price change | volume
                val infoLine2 = tradeplan.getDisplayInfoLine2()
                tradeplanInfoLine2.text = infoLine2
                
                // Set status
                val statusText = tradeplan.status ?: "UNKNOWN"
                tradeplanStatus.text = statusText

                container.addView(tradeplanItem)
            }
        }
    }
    
    /**
     * 更新交易计划状态（待批准/已批准）
     * @param id 交易计划ID
     * @param status 新状态（TradePlanStatus.PENDING/TradePlanStatus.APPROVED）
     * @return 更新结果消息
     */
    suspend fun updateTradePlanStatus(id: String, status: String): String {
        return try {
            if (tradePlanRepository == null) {
                throw Exception("交易计划仓库未初始化")
            }
            
            Log.d(TAG, "updateTradePlanStatus: 开始更新交易计划状态 - ID: $id, 状态: $status")
            
            // 调用Repository更新状态并同步到服务器
            val result = tradePlanRepository!!.updateTradePlanStatus(id, status)
            
            Log.d(TAG, "updateTradePlanStatus: 交易计划状态更新完成 - $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "updateTradePlanStatus: 更新交易计划状态失败 - ${e.message}", e)
            throw e
        }
    }
    
    /**
     * 执行交易计划
     * @param id 交易计划ID
     * @return 执行结果消息
     */
    suspend fun executeTradePlan(id: String): String {
        return try {
            if (tradePlanRepository == null) {
                throw Exception("交易计划仓库未初始化")
            }
            
            Log.d(TAG, "executeTradePlan: 开始执行交易计划 - ID: $id")
            
            // 调用Repository执行交易计划
            val result = tradePlanRepository!!.executeTradePlan(id)
            
            Log.d(TAG, "executeTradePlan: 交易计划执行请求已发送 - $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "executeTradePlan: 执行交易计划失败 - ${e.message}", e)
            throw e
        }
    }
    
    /**
     * 获取待批准的交易计划
     */
    fun getPendingTradePlans(): List<TradePlanEntity> {
        return try {
            if (tradePlanRepository == null) {
                Log.e(TAG, "getPendingTradePlans: 交易计划仓库未初始化")
                emptyList()
            } else {
                // 使用协程获取数据
                var result = emptyList<TradePlanEntity>()
                CoroutineScope(Dispatchers.IO).launch {
                    val liveData = tradePlanRepository!!.getPendingTradePlans()
                    // 这里需要观察LiveData并获取当前值
                    // 在实际应用中，可能需要使用LiveData.observe或者直接从数据库获取
                }
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "getPendingTradePlans: 获取待批准交易计划失败 - ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 获取已批准的交易计划
     */
    fun getApprovedTradePlans(): List<TradePlanEntity> {
        return try {
            if (tradePlanRepository == null) {
                Log.e(TAG, "getApprovedTradePlans: 交易计划仓库未初始化")
                emptyList()
            } else {
                // 使用协程获取数据
                var result = emptyList<TradePlanEntity>()
                CoroutineScope(Dispatchers.IO).launch {
                    val liveData = tradePlanRepository!!.getApprovedTradePlans()
                    // 这里需要观察LiveData并获取当前值
                    // 在实际应用中，可能需要使用LiveData.observe或者直接从数据库获取
                }
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "getApprovedTradePlans: 获取已批准交易计划失败 - ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 获取所有交易计划（从服务器）
     * @return 交易计划列表
     */
    suspend fun getAllTradePlans(): List<TradePlan> {
        return try {
            if (tradePlanRepository == null) {
                throw Exception("交易计划仓库未初始化")
            }
            
            Log.d(TAG, "getAllTradePlans: 开始获取所有交易计划")
            
            // 调用Repository获取所有交易计划
            val result = tradePlanRepository!!.getAllTradePlansFromServer()
            
            Log.d(TAG, "getAllTradePlans: 获取到 ${result.size} 个交易计划")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "getAllTradePlans: 获取所有交易计划失败 - ${e.message}", e)
            throw e
        }
    }
    
    /**
     * 获取所有交易计划（LiveData）
     * @return 交易计划列表的LiveData
     */
    fun getAllTradePlansLiveData(): androidx.lifecycle.LiveData<List<TradePlanEntity>> {
        return try {
            if (tradePlanRepository == null) {
                Log.e(TAG, "getAllTradePlansLiveData: 交易计划仓库未初始化")
                androidx.lifecycle.MutableLiveData(emptyList())
            } else {
                tradePlanRepository!!.getAllTradePlans()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAllTradePlansLiveData: 获取所有交易计划失败 - ${e.message}", e)
            androidx.lifecycle.MutableLiveData(emptyList())
        }
    }
    
    /**
     * 获取待批准的交易计划（LiveData）
     * @return 待批准交易计划列表的LiveData
     */
    fun getPendingTradePlansLiveData(): androidx.lifecycle.LiveData<List<TradePlanEntity>> {
        return try {
            if (tradePlanRepository == null) {
                Log.e(TAG, "getPendingTradePlansLiveData: 交易计划仓库未初始化")
                androidx.lifecycle.MutableLiveData(emptyList())
            } else {
                tradePlanRepository!!.getPendingTradePlans()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getPendingTradePlansLiveData: 获取待批准交易计划失败 - ${e.message}", e)
            androidx.lifecycle.MutableLiveData(emptyList())
        }
    }
    
    /**
     * 获取已批准的交易计划（LiveData）
     * @return 已批准交易计划列表的LiveData
     */
    fun getApprovedTradePlansLiveData(): androidx.lifecycle.LiveData<List<TradePlanEntity>> {
        return try {
            if (tradePlanRepository == null) {
                Log.e(TAG, "getApprovedTradePlansLiveData: 交易计划仓库未初始化")
                androidx.lifecycle.MutableLiveData(emptyList())
            } else {
                tradePlanRepository!!.getApprovedTradePlans()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getApprovedTradePlansLiveData: 获取已批准交易计划失败 - ${e.message}", e)
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
            
            // 调用Repository执行所有已批准的交易计划
            val result = tradePlanRepository!!.executeApprovedPlans()
            
            Log.d(TAG, "executeApprovedPlans: 已批准的交易计划执行请求已发送 - $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "executeApprovedPlans: 执行已批准的交易计划失败 - ${e.message}", e)
            throw e
        }
    }
}