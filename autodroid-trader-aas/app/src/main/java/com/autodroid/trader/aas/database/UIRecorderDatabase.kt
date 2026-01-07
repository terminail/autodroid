package com.autodroid.trader.aas.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.autodroid.trader.aas.database.*

@Database(
    entities = [UIEvent::class, ElementFeature::class, AppConfig::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UIRecorderDatabase : RoomDatabase() {
    abstract fun uiEventDao(): UIEventDao
    abstract fun elementFeatureDao(): ElementFeatureDao
    abstract fun appConfigDao(): AppConfigDao
    
    companion object {
        @Volatile
        private var INSTANCE: UIRecorderDatabase? = null
        
        fun getInstance(context: Context): UIRecorderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UIRecorderDatabase::class.java,
                    "ui_recorder.db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // 在数据库创建时添加默认配置
                        val appContext = context.applicationContext
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val database = getInstance(appContext)
                                val config = database.appConfigDao().getConfig("com.tdx.androidCCZQ")
                                if (config == null) {
                                    val defaultAppConfig = AppConfig(
                                        packageName = "com.tdx.androidCCZQ",
                                        appName = "明佣宝",
                                        recordingEnabled = true,
                                        recordClicks = true,
                                        recordInputs = true,
                                        recordSelections = true,
                                        recordScrolls = true,
                                        takeScreenshots = false,
                                        screenshotQuality = 70
                                    )
                                    database.appConfigDao().insert(defaultAppConfig)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("UIRecorderDatabase", "Failed to insert default config", e)
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}