package com.autodroid.teachitback.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.autodroid.teachitback.model.MessageEntity
import com.autodroid.teachitback.model.TopicEntity

@Database(
    entities = [TopicEntity::class, MessageEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun topicDao(): TopicDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teachitback_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
    }
}
