package com.autodroid.manager.service

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * Database for storing mDNS implementation weights and failure records
 */
class MdnsWeightDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    companion object {
        private const val DATABASE_NAME = "mdns_weights.db"
        private const val DATABASE_VERSION = 1
        
        // Table name
        private const val TABLE_WEIGHTS = "implementation_weights"
        
        // Column names
        private const val COLUMN_ID = "id"
        private const val COLUMN_IMPLEMENTATION_NAME = "implementation_name"
        private const val COLUMN_WEIGHT = "weight"
        private const val COLUMN_LAST_FAILED = "last_failed"
        private const val COLUMN_FAILURE_COUNT = "failure_count"
        private const val COLUMN_LAST_SUCCESS = "last_success"
        
        // Default weights for implementations
        const val WEIGHT_LEGACY_NSD = 10
        const val WEIGHT_JMDNS = 9
        const val WEIGHT_WEUPNP = 8
        const val WEIGHT_STANDARD_NSD = 7
        const val WEIGHT_DISABLED = -1
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_WEIGHTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_IMPLEMENTATION_NAME TEXT UNIQUE NOT NULL,
                $COLUMN_WEIGHT INTEGER NOT NULL,
                $COLUMN_LAST_FAILED INTEGER,
                $COLUMN_FAILURE_COUNT INTEGER DEFAULT 0,
                $COLUMN_LAST_SUCCESS INTEGER
            )
        """.trimIndent()
        
        db.execSQL(createTable)
        
        // Initialize with default weights
        initializeDefaultWeights(db)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WEIGHTS")
        onCreate(db)
    }
    
    private fun initializeDefaultWeights(db: SQLiteDatabase) {
        val implementations = listOf(
            Pair("LegacyNsdImplementation", WEIGHT_LEGACY_NSD),
            Pair("JmDNSImplementation", WEIGHT_JMDNS),
            Pair("WeUPnPImplementation", WEIGHT_WEUPNP),
            Pair("StandardNsdImplementation", WEIGHT_STANDARD_NSD)
        )
        
        implementations.forEach { (name, weight) ->
            val insertSQL = """
                INSERT INTO $TABLE_WEIGHTS 
                ($COLUMN_IMPLEMENTATION_NAME, $COLUMN_WEIGHT) 
                VALUES (?, ?)
            """.trimIndent()
            
            db.execSQL(insertSQL, arrayOf(name, weight))
        }
        
        Log.d("MdnsWeightDatabase", "Initialized default weights for ${implementations.size} implementations")
    }
    
    /**
     * Get weight for an implementation
     */
    fun getWeight(implementationName: String): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_WEIGHTS,
            arrayOf(COLUMN_WEIGHT),
            "$COLUMN_IMPLEMENTATION_NAME = ?",
            arrayOf(implementationName),
            null, null, null
        )
        
        return if (cursor.moveToFirst()) {
            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT))
        } else {
            // If no record exists, create a new record with default weight
            val defaultWeight = when (implementationName) {
                "LegacyNsdImplementation" -> WEIGHT_LEGACY_NSD
                "JmDNSImplementation" -> WEIGHT_JMDNS
                "WeUPnPImplementation" -> WEIGHT_WEUPNP
                "StandardNsdImplementation" -> WEIGHT_STANDARD_NSD
                else -> WEIGHT_STANDARD_NSD
            }
            updateWeight(implementationName, defaultWeight)
            defaultWeight
        }.also {
            cursor.close()
        }
    }
    
    /**
     * Update weight for an implementation
     */
    fun updateWeight(implementationName: String, weight: Int) {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put(COLUMN_WEIGHT, weight)
            if (weight == WEIGHT_DISABLED) {
                put(COLUMN_LAST_FAILED, System.currentTimeMillis())
                put(COLUMN_FAILURE_COUNT, getFailureCount(implementationName) + 1)
            } else if (weight > WEIGHT_DISABLED) {
                put(COLUMN_LAST_SUCCESS, System.currentTimeMillis())
            }
        }
        
        val rowsAffected = db.update(
            TABLE_WEIGHTS,
            values,
            "$COLUMN_IMPLEMENTATION_NAME = ?",
            arrayOf(implementationName)
        )
        
        // If no rows were affected, insert a new record
        if (rowsAffected == 0) {
            values.put(COLUMN_IMPLEMENTATION_NAME, implementationName)
            db.insert(TABLE_WEIGHTS, null, values)
        }
        
        Log.d("MdnsWeightDatabase", "Updated weight for $implementationName to $weight")
    }
    
    /**
     * Get failure count for an implementation
     */
    fun getFailureCount(implementationName: String): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_WEIGHTS,
            arrayOf(COLUMN_FAILURE_COUNT),
            "$COLUMN_IMPLEMENTATION_NAME = ?",
            arrayOf(implementationName),
            null, null, null
        )
        
        return if (cursor.moveToFirst()) {
            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAILURE_COUNT))
        } else {
            0
        }
    }
    
    /**
     * Reset all weights to default values
     */
    fun resetAllWeights() {
        val db = writableDatabase
        val implementations = listOf(
            "LegacyNsdImplementation",
            "JmDNSImplementation", 
            "WeUPnPImplementation",
            "StandardNsdImplementation"
        )
        
        implementations.forEach { name ->
            val defaultWeight = when (name) {
                "LegacyNsdImplementation" -> WEIGHT_LEGACY_NSD
                "JmDNSImplementation" -> WEIGHT_JMDNS
                "WeUPnPImplementation" -> WEIGHT_WEUPNP
                "StandardNsdImplementation" -> WEIGHT_STANDARD_NSD
                else -> WEIGHT_STANDARD_NSD
            }
            
            updateWeight(name, defaultWeight)
        }
        
        Log.d("MdnsWeightDatabase", "Reset all weights to default values")
    }
    
    /**
     * Get all implementation weights
     */
    fun getAllWeights(): Map<String, Int> {
        val db = readableDatabase
        val weights = mutableMapOf<String, Int>()
        
        val cursor = db.query(
            TABLE_WEIGHTS,
            arrayOf(COLUMN_IMPLEMENTATION_NAME, COLUMN_WEIGHT),
            null, null, null, null, null
        )
        
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMPLEMENTATION_NAME))
            val weight = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT))
            weights[name] = weight
        }
        
        cursor.close()
        return weights
    }
}