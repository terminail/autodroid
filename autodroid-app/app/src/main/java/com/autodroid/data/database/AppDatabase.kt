package com.autodroid.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.autodroid.data.dao.ServerDao
import com.autodroid.data.dao.ServerEntity

/**
 * Room数据库主类
 * 管理应用程序的数据库实例和数据访问对象
 */
@Database(
    entities = [ServerEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * 获取服务器数据访问对象
     */
    abstract fun serverDao(): ServerDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 获取数据库实例（单例模式）
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autodroid_database"
                )
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
    }
}