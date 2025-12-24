package com.autodroid.trader.network

import com.google.gson.annotations.SerializedName

/**
 * Data class representing trade plan update request
 * Used to update trade plan status on the server
 */
data class TradePlanStatusUpdateRequest(
    @SerializedName("status")
    val status: String
) {
    companion object {
        /**
         * Create empty trade plan update request
         */
        fun empty(): TradePlanStatusUpdateRequest = TradePlanStatusUpdateRequest("")
    }
}
