package com.autodroid.trader.app.network

import com.google.gson.annotations.SerializedName

data class TradePlanStatusUpdateResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("trade_plan_response")
    val tradePlanResponse: TradePlanResponse? = null
)
