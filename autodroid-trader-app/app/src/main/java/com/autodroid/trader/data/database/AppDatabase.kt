package com.autodroid.trader.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    databaseName
                )
                .addMigrations(MIGRATION_6_7)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * 数据库迁移（从版本2到版本3，解决架构哈希不匹配问题）
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 由于使用了fallbackToDestructiveMigration，此迁移主要用于版本管理
                // 实际的数据重建由Room自动处理
            }
        }
        
        /**
         * 数据库迁移（从版本6到版本7，添加设备API级别和屏幕尺寸字段）
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加新字段到devices表
                database.execSQL("ALTER TABLE devices ADD COLUMN apiLevel INTEGER")
                database.execSQL("ALTER TABLE devices ADD COLUMN screenWidth INTEGER")
                database.execSQL("ALTER TABLE devices ADD COLUMN screenHeight INTEGER")
            }
        }
    }
}