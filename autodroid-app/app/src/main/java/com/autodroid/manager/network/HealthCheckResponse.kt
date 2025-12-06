package com.autodroid.manager.network

/**
 * Response model for health check
 */
data class HealthCheckResponse(
    val status: String,
    val version: String? = null,
    val timestamp: String? = null,
    val services: Map<String, String> = emptyMap()
)