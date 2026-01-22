package com.autodroid.teachitback.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.autodroid.teachitback.model.MessageEntity
import com.autodroid.teachitback.model.MindMapEntity
import com.autodroid.teachitback.model.MindMapNode
import com.autodroid.teachitback.model.SettingEntity
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.model.WhyEntity

@Database(
    entities = [TopicEntity::class, MessageEntity::class, MindMapEntity::class, MindMapNode::class, SettingEntity::class, WhyEntity::class],
    version = 6
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun topicDao(): TopicDao
    abstract fun messageDao(): MessageDao
    abstract fun mindMapDao(): MindMapDao
    abstract fun settingDao(): SettingDao
    abstract fun whyDao(): WhyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 数据库迁移从版本2到版本3
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为topics表添加isPreset字段
                database.execSQL("ALTER TABLE topics ADD COLUMN isPreset INTEGER NOT NULL DEFAULT 0")
            }
        }

        // 数据库迁移从版本3到版本4
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建settings表
                database.execSQL("""
                    CREATE TABLE settings (
                        key TEXT PRIMARY KEY NOT NULL,
                        value TEXT NOT NULL,
                        lastUpdated INTEGER NOT NULL,
                        created INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // 创建索引以提高查询性能
                database.execSQL("CREATE INDEX idx_settings_last_updated ON settings(lastUpdated)")
            }
        }

        // 数据库迁移从版本4到版本5
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建why_content表
                database.execSQL("""
                    CREATE TABLE why_content (
                        id TEXT PRIMARY KEY NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        orderIndex INTEGER NOT NULL,
                        lastUpdated INTEGER NOT NULL,
                        created INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // 创建索引以提高查询性能
                database.execSQL("CREATE INDEX idx_why_content_type ON why_content(type)")
                database.execSQL("CREATE INDEX idx_why_content_order ON why_content(orderIndex)")
            }
        }

        // 数据库迁移从版本5到版本6
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为topics表添加presetTopicId字段
                database.execSQL("ALTER TABLE topics ADD COLUMN presetTopicId TEXT")
            }
        }

        // 数据库迁移从版本1到版本2
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建mindmaps表
                database.execSQL("""
                    CREATE TABLE mindmaps (
                        id TEXT PRIMARY KEY NOT NULL,
                        topicId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(topicId) REFERENCES topics(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // 创建mindmap_nodes表
                database.execSQL("""
                    CREATE TABLE mindmap_nodes (
                        id TEXT PRIMARY KEY NOT NULL,
                        mindMapId TEXT NOT NULL,
                        parentId TEXT,
                        title TEXT NOT NULL,
                        description TEXT,
                        progress INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(mindMapId) REFERENCES mindmaps(id) ON DELETE CASCADE,
                        FOREIGN KEY(parentId) REFERENCES mindmap_nodes(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // 创建索引以提高查询性能
                database.execSQL("CREATE INDEX idx_mindmap_nodes_mindmap_id ON mindmap_nodes(mindMapId)")
                database.execSQL("CREATE INDEX idx_mindmap_nodes_parent_id ON mindmap_nodes(parentId)")
                database.execSQL("CREATE INDEX idx_mindmaps_topic_id ON mindmaps(topicId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teachitback_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
    }
}
