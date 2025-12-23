package com.autodroid.trader.ui.tradeplans

sealed class TradePlansItem(val type: Int) {
    companion object {
        const val TYPE_TRADE_PLAN = 0
        const val TYPE_SUMMARY = 1
    }
    
    data class ItemTradePlans(
        val status: String = "Loading trade plans...",
        val executionStatus: String = "IDLE",
        val pendingCount: Int = 0,
        val approvedCount: Int = 0,
        val rejectedCount: Int = 0,
        val executedSuccessCount: Int = 0,
        val executedFailedCount: Int = 0
    ) : TradePlansItem(TYPE_TRADE_PLAN)

    data class ItemTradePlansSummary(
        val status: String = "Loading trade plan summary...",
        val pendingCount: Int = 0,
        val approvedCount: Int = 0,
        val rejectedCount: Int = 0,
        val executedSuccessCount: Int = 0,
        val executedFailedCount: Int = 0
    ) : TradePlansItem(TYPE_SUMMARY)
}
