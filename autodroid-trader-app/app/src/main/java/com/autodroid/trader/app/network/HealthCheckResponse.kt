package com.autodroid.trader.app.network

/**
 * Response model for health check
 */
data class HealthCheckResponse(
    val status: String,
    val version: String? = null,
    val timestamp: String? = null,
    val services: Map<String, String> = emptyMap()
)