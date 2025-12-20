package com.autodroid.trader.data.database

import androidx.room.TypeConverter
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
}