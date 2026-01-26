package com.autodroid.teachitback.database

import androidx.room.TypeConverter
import com.autodroid.teachitback.service.AIAbility
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * TopicConverters - Room数据库类型转换器
 * 用于将复杂数据类型（List、Set、Map）转换为Room可存储的简单类型
 * 必须为object单例，这样Room才能识别
 */
object TopicConverters {

    private val gson = Gson()

    // List<String> 转换
    @JvmStatic
    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @JvmStatic
    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson<List<String>>(json, type) ?: emptyList()
    }

    // Set<AIAbility> 转换
    @JvmStatic
    @TypeConverter
    fun fromAIAbilitySet(set: Set<AIAbility>?): String {
        if (set == null) return "[]"
        return gson.toJson(set.map { it.name })
    }

    @JvmStatic
    @TypeConverter
    fun toAIAbilitySet(json: String?): Set<AIAbility> {
        if (json.isNullOrBlank()) return emptySet()
        val names = gson.fromJson(json, Array<String>::class.java) ?: arrayOf()
        return names.map { AIAbility.valueOf(it) }.toSet()
    }

    // Map<String, Double> 转换
    @JvmStatic
    @TypeConverter
    fun fromServicePreferencesMap(map: Map<String, Double>?): String {
        return gson.toJson(map ?: emptyMap<String, Double>())
    }

    @JvmStatic
    @TypeConverter
    fun toServicePreferencesMap(json: String?): Map<String, Double> {
        if (json.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, Double>>() {}.type
        return gson.fromJson<Map<String, Double>>(json, type) ?: emptyMap()
    }
}
