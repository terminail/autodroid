package com.autodroid.trader.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.autodroid.trader.data.dao.ServerDao
import com.autodroid.trader.data.dao.ServerEntity
import com.autodroid.trader.data.dao.DeviceDao
import com.autodroid.trader.data.dao.DeviceEntity
import com.autodroid.trader.data.dao.TradePlanDao
import com.autodroid.trader.data.dao.TradePlanEntity
import com.autodroid.trader.config.ConfigManager

/**
 * Room数据库主类
 * 管理应用程序的数据库实例和数据访问对象
 */
@Database(
    entities = [ServerEntity::class, DeviceEntity::class, TradePlanEntity::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * 获取服务器数据访问对象
     */
    abstract fun serverDao(): ServerDao
    
    /**
     * 获取设备数据访问对象
     */
    abstract fun deviceDao(): DeviceDao
    
    /**
     * 获取交易计划数据访问对象
     */
    abstract fun tradePlanDao(): TradePlanDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 获取数据库实例（单例模式）
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // 从配置文件中获取数据库名称
                val config = ConfigManager.getConfig(context)
                val databaseName = config.database.name
                
                android.util.Log.d("AppDatabase", "创建数据库实例: $databaseName")
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    databaseName
                )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onCreate(db)
                            android.util.Log.d("AppDatabase", "数据库已创建")
                        }
                        
                        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onOpen(db)
                            android.util.Log.d("AppDatabase", "数据库已打开")
                        }
                    })
                .build()
                INSTANCE = instance
                android.util.Log.d("AppDatabase", "数据库实例创建完成")
                instance
            }
        }
    }
}