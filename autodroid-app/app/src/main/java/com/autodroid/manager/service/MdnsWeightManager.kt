package com.autodroid.manager.service

import android.content.Context
import android.util.Log

/**
 * Manager class for handling mDNS implementation weights and failure tracking
 */
class MdnsWeightManager(private val context: Context) {
    
    private val TAG = "MdnsWeightManager"
    private val database = MdnsWeightDatabase(context)
    
    companion object {
        // Weight constants
        const val WEIGHT_DISABLED = -1
        const val WEIGHT_MIN = 0
        const val WEIGHT_MAX = 10
        
        // Default weights for implementations
        const val WEIGHT_LEGACY_NSD = 10
        const val WEIGHT_JMDNS = 9
        const val WEIGHT_WEUPNP = 8
        const val WEIGHT_STANDARD_NSD = 7
    }
    
    /**
     * Get the current weight for an implementation
     */
    fun getWeight(implementation: MdnsImplementation): Int {
        val implementationName = implementation.javaClass.simpleName
        return database.getWeight(implementationName)
    }
    
    /**
     * Handle implementation failure - reduce weight but don't disable completely
     */
    fun onImplementationFailed(implementation: MdnsImplementation) {
        val implementationName = implementation.javaClass.simpleName
        
        // Get current weight from database
        val currentWeight = database.getWeight(implementationName)
        
        Log.d(TAG, "Implementation $implementationName failed. Current weight: $currentWeight")
        
        // Instead of disabling completely, reduce weight by 1
        // This allows implementations to recover after temporary failures
        if (currentWeight > WEIGHT_MIN) {
            val newWeight = maxOf(WEIGHT_MIN, currentWeight - 1)
            database.updateWeight(implementationName, newWeight)
            Log.d(TAG, "Implementation $implementationName weight reduced to: $newWeight")
        } else if (currentWeight == WEIGHT_DISABLED) {
            // If already disabled, try to restore with minimal weight
            database.updateWeight(implementationName, WEIGHT_MIN)
            Log.d(TAG, "Implementation $implementationName restored to minimal weight: $WEIGHT_MIN")
        }
    }
    
    /**
     * Handle implementation success - restore weight to default value
     */
    fun onImplementationSuccess(implementation: MdnsImplementation) {
        val implementationName = implementation.javaClass.simpleName
        
        // Get current weight from database
        val currentWeight = database.getWeight(implementationName)
        
        Log.d(TAG, "Implementation $implementationName succeeded. Current weight: $currentWeight")
        
        // If implementation was disabled, restore it to default weight
        if (currentWeight == WEIGHT_DISABLED) {
            val defaultWeight = getDefaultWeight(implementationName)
            database.updateWeight(implementationName, defaultWeight)
            Log.d(TAG, "Implementation $implementationName restored to default weight: $defaultWeight")
        }
    }
    
    /**
     * Check if an implementation is available (weight >= 0)
     */
    fun isAvailable(implementation: MdnsImplementation): Boolean {
        val weight = getWeight(implementation)
        return weight >= WEIGHT_MIN
    }
    
    /**
     * Get the availability status for an implementation
     */
    fun getAvailabilityStatus(implementation: MdnsImplementation): String {
        val weight = getWeight(implementation)
        return when {
            weight == WEIGHT_DISABLED -> "DISABLED"
            weight > WEIGHT_MIN -> "AVAILABLE"
            else -> "UNKNOWN"
        }
    }
    
    /**
     * Get all implementation weights
     */
    fun getAllWeights(): Map<String, Int> {
        return database.getAllWeights()
    }
    
    /**
     * Reset all implementation weights to default values
     */
    fun resetAllWeights() {
        database.resetAllWeights()
        Log.d(TAG, "All implementation weights reset to default values")
    }
    
    /**
     * Get the default weight for an implementation
     */
    private fun getDefaultWeight(implementationName: String): Int {
        return when (implementationName) {
            "LegacyNsdImplementation" -> WEIGHT_LEGACY_NSD
            "JmDNSImplementation" -> WEIGHT_JMDNS
            "WeUPnPImplementation" -> WEIGHT_WEUPNP
            "StandardNsdImplementation" -> WEIGHT_STANDARD_NSD
            else -> WEIGHT_STANDARD_NSD
        }
    }
    
    /**
     * Get sorted list of implementations by weight (highest first)
     */
    fun getSortedImplementations(implementations: List<MdnsImplementation>): List<MdnsImplementation> {
        return implementations.sortedByDescending { getWeight(it) }
    }
    
    /**
     * Get available implementations (weight >= 0)
     */
    fun getAvailableImplementations(implementations: List<MdnsImplementation>): List<MdnsImplementation> {
        return implementations.filter { isAvailable(it) }
    }
    
    /**
     * Get disabled implementations (weight = -1)
     */
    fun getDisabledImplementations(implementations: List<MdnsImplementation>): List<MdnsImplementation> {
        return implementations.filter { getWeight(it) == WEIGHT_DISABLED }
    }
    
    /**
     * Get implementation statistics
     */
    fun getStatistics(): ImplementationStatistics {
        val allWeights = getAllWeights()
        val total = allWeights.size
        val available = allWeights.count { it.value >= WEIGHT_MIN }
        val disabled = allWeights.count { it.value == WEIGHT_DISABLED }
        
        return ImplementationStatistics(total, available, disabled)
    }
    
    /**
     * Data class for implementation statistics
     */
    data class ImplementationStatistics(
        val totalImplementations: Int,
        val availableImplementations: Int,
        val disabledImplementations: Int
    )
}