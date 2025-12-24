package com.autodroid.trader.data.database

import androidx.room.TypeConverter
import com.autodroid.trader.network.Ohlcv
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room数据库类型转换器
 * 用于处理复杂类型的数据库存储和读取
 */
class Converters {
    private val gson = Gson()
    
    /**
     * 将Map<String, String>转换为JSON字符串存储
     */
    @TypeConverter
    fun fromStringStringMap(map: Map<String, String>): String {
        return gson.toJson(map)
    }
    
    /**
     * 将JSON字符串转换为Map<String, String>
     */
    @TypeConverter
    fun toStringStringMap(json: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(json, type)
    }
    
    /**
     * 将Map<String, Boolean>转换为JSON字符串存储
     */
    @TypeConverter
    fun fromStringBooleanMap(map: Map<String, Boolean>): String {
        return gson.toJson(map)
    }
    
    /**
     * 将JSON字符串转换为Map<String, Boolean>
     */
    @TypeConverter
    fun toStringBooleanMap(json: String): Map<String, Boolean> {
        val type = object : TypeToken<Map<String, Boolean>>() {}.type
        return gson.fromJson(json, type)
    }
    
    /**
     * 将Map<String, Any>转换为JSON字符串存储
     */
    @TypeConverter
    fun fromStringAnyMap(map: Map<String, Any>): String {
        return gson.toJson(map)
    }
    
    /**
     * 将JSON字符串转换为Map<String, Any>
     */
    @TypeConverter
    fun toStringAnyMap(json: String): Map<String, Any> {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(json, type)
    }
    
    /**
     * 将Ohlcv对象转换为JSON字符串存储
     */
    @TypeConverter
    fun fromOhlcv(ohlcv: Ohlcv?): String {
        return gson.toJson(ohlcv)
    }
    
    /**
     * 将JSON字符串转换为Ohlcv对象
     */
    @TypeConverter
    fun toOhlcv(json: String): Ohlcv? {
        return gson.fromJson(json, Ohlcv::class.java)
    }
    
    /**
     * 将JsonObject对象转换为JSON字符串存储
     */
    @TypeConverter
    fun fromJsonObject(jsonObject: com.google.gson.JsonObject?): String {
        return gson.toJson(jsonObject)
    }
    
    /**
     * 将JSON字符串转换为JsonObject对象
     */
    @TypeConverter
    fun toJsonObject(json: String): com.google.gson.JsonObject? {
        return gson.fromJson(json, com.google.gson.JsonObject::class.java)
    }
}