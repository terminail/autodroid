package com.autodroid.trader.aas.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
    
    @TypeConverter
    fun fromJsonString(value: String?): Map<String, String>? {
        return value?.let { 
            Gson().fromJson(it, object : TypeToken<Map<String, String>>() {}.type)
        }
    }
    
    @TypeConverter
    fun mapToJsonString(map: Map<String, String>?): String? {
        return map?.let { Gson().toJson(it) }
    }
}