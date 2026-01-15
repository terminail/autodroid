package com.autodroid.guardiansdk.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.autodroid.guardiansdk.data.dao.SettingDao
import com.autodroid.guardiansdk.data.entity.Setting
import com.autodroid.guardiansdk.data.entity.SettingTypeConverter

/**
 * 报警功能SDK数据库
 * 包含所有数据实体和DAO
 */
@Database(
    entities = [Setting::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(SettingTypeConverter::class)
abstract class GuardianDatabase : RoomDatabase() {
    
    /**
     * 获取设置项DAO
     */
    abstract fun settingDao(): SettingDao
    
    companion object {
        @Volatile
        private var INSTANCE: GuardianDatabase? = null
        
        /**
         * 获取数据库实例
         */
        fun getDatabase(context: Context): GuardianDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GuardianDatabase::class.java,
                    "guardian_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}