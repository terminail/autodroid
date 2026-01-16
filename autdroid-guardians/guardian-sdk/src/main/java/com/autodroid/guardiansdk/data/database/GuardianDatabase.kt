package com.autodroid.guardiansdk.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.autodroid.guardiansdk.data.dao.SettingDao
import com.autodroid.guardiansdk.data.dao.WardDao
import com.autodroid.guardiansdk.data.dao.MessageDao
import com.autodroid.guardiansdk.data.entity.Setting
import com.autodroid.guardiansdk.data.entity.Ward
import com.autodroid.guardiansdk.data.entity.Message
import com.autodroid.guardiansdk.data.entity.SettingTypeConverter
import com.autodroid.guardiansdk.data.entity.MessageContent
import com.autodroid.guardiansdk.data.entity.MessageContentSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 报警功能SDK数据库
 * 包含所有数据实体和DAO
 */
@Database(
    entities = [Setting::class, Ward::class, Message::class],
    version = 4,  // 升级版本号，强制重建数据库
    exportSchema = false
)
@TypeConverters(SettingTypeConverter::class)
abstract class GuardianDatabase : RoomDatabase() {
    
    /**
     * 获取设置项DAO
     */
    abstract fun settingDao(): SettingDao
    
    /**
     * 获取被监护人DAO
     */
    abstract fun wardDao(): WardDao
    
    /**
     * 获取消息DAO
     */
    abstract fun messageDao(): MessageDao
    
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
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            android.util.Log.d("GuardianDatabase", "=== onCreate called, inserting test data ===")
                            try {
                                insertTestDataWithSQL(db)
                                android.util.Log.d("GuardianDatabase", "=== Test data inserted successfully ===")
                            } catch (e: Exception) {
                                android.util.Log.e("GuardianDatabase", "=== Error inserting test data ===", e)
                                throw e  // 重新抛出异常，确保能看到错误
                            }
                        }
                        
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            android.util.Log.d("GuardianDatabase", "=== Database opened ===")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * 使用SQL语句插入测试数据（在onCreate回调中使用）
         */
        private fun insertTestDataWithSQL(db: SupportSQLiteDatabase) {
            try {
                val currentTime = System.currentTimeMillis()
                
                // 插入被监护人数据
                db.execSQL("INSERT INTO wards (phoneNumber, name, relationship, passwordBook, lastAlarmTime, alarmCount, isActive, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", arrayOf(
                    "13812345678", "张三", "社区居民", "default_password_book", 
                    (currentTime - 86400000).toString(), "3", "1", currentTime.toString(), currentTime.toString()
                ))
                
                db.execSQL("INSERT INTO wards (phoneNumber, name, relationship, passwordBook, lastAlarmTime, alarmCount, isActive, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", arrayOf(
                    "13987654321", "李四", "学生", "default_password_book",
                    (currentTime - 172800000).toString(), "1", "1", currentTime.toString(), currentTime.toString()
                ))
                
                db.execSQL("INSERT INTO wards (phoneNumber, name, relationship, passwordBook, lastAlarmTime, alarmCount, isActive, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", arrayOf(
                    "13711112222", "王五", "社区居民", "default_password_book",
                    (currentTime - 259200000).toString(), "5", "1", currentTime.toString(), currentTime.toString()
                ))
                
                // 插入消息数据 - 张三的消息
                val zhangsanAlarmContent = MessageContentSerializer.serialize(
                    MessageContent.AlarmMessage(
                        location = "北京市朝阳区XX街道",
                        level = "emergency",
                        message = "紧急报警：发现可疑人员"
                    )
                )
                db.execSQL("INSERT INTO messages (fromPhoneNumber, toPhoneNumber, content, timestamp) VALUES (?, ?, ?, ?)", arrayOf(
                    "13812345678", "13800000000", zhangsanAlarmContent, (currentTime - 86400000).toString()
                ))
                
                val policeReply1Content = MessageContentSerializer.serialize(
                    MessageContent.TextMessage("收到报警，已派警员前往")
                )
                db.execSQL("INSERT INTO messages (fromPhoneNumber, toPhoneNumber, content, timestamp) VALUES (?, ?, ?, ?)", arrayOf(
                    "13800000000", "13812345678", policeReply1Content, (currentTime - 86390000).toString()
                ))
                
                val zhangsanSafeContent = MessageContentSerializer.serialize(
                    MessageContent.TextMessage("感谢，已经安全")
                )
                db.execSQL("INSERT INTO messages (fromPhoneNumber, toPhoneNumber, content, timestamp) VALUES (?, ?, ?, ?)", arrayOf(
                    "13812345678", "13800000000", zhangsanSafeContent, (currentTime - 86380000).toString()
                ))
                
                // 李四的消息
                val lisiQueryContent = MessageContentSerializer.serialize(
                    MessageContent.QueryMessage("查询今天的社区活动安排")
                )
                db.execSQL("INSERT INTO messages (fromPhoneNumber, toPhoneNumber, content, timestamp) VALUES (?, ?, ?, ?)", arrayOf(
                    "13987654321", "13800000000", lisiQueryContent, (currentTime - 172800000).toString()
                ))
                
                val policeReply2Content = MessageContentSerializer.serialize(
                    MessageContent.TextMessage("今天有社区安全知识讲座，下午2点开始")
                )
                db.execSQL("INSERT INTO messages (fromPhoneNumber, toPhoneNumber, content, timestamp) VALUES (?, ?, ?, ?)", arrayOf(
                    "13800000000", "13987654321", policeReply2Content, (currentTime - 172790000).toString()
                ))
                
                // 王五的消息
                val wangwuAlarmContent = MessageContentSerializer.serialize(
                    MessageContent.AlarmMessage(
                        location = "上海市浦东新区XX路",
                        level = "normal",
                        message = "日常报警：门窗未锁"
                    )
                )
                db.execSQL("INSERT INTO messages (fromPhoneNumber, toPhoneNumber, content, timestamp) VALUES (?, ?, ?, ?)", arrayOf(
                    "13711112222", "13800000000", wangwuAlarmContent, (currentTime - 259200000).toString()
                ))
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        /**
         * 使用DAO插入测试数据（备用方法）
         */
        private suspend fun insertTestData(database: GuardianDatabase) {
            try {
                // 测试用的被监护人数据
                val testWards = listOf(
                    Ward(
                        phoneNumber = "13812345678",
                        name = "张三",
                        relationship = "社区居民",
                        passwordBook = "default_password_book",
                        lastAlarmTime = System.currentTimeMillis() - 86400000, // 1天前
                        alarmCount = 3,
                        isActive = true
                    ),
                    Ward(
                        phoneNumber = "13987654321",
                        name = "李四",
                        relationship = "学生",
                        passwordBook = "default_password_book",
                        lastAlarmTime = System.currentTimeMillis() - 172800000, // 2天前
                        alarmCount = 1,
                        isActive = true
                    ),
                    Ward(
                        phoneNumber = "13711112222",
                        name = "王五",
                        relationship = "社区居民",
                        passwordBook = "default_password_book",
                        lastAlarmTime = System.currentTimeMillis() - 259200000, // 3天前
                        alarmCount = 5,
                        isActive = true
                    )
                )
                
                // 插入被监护人
                database.wardDao().insertOrUpdateAllWards(testWards)
                
                // 测试用的消息数据
                val currentTime = System.currentTimeMillis()
                val testMessages = listOf(
                    // 张三的消息
                    Message(
                        fromPhoneNumber = "13812345678",
                        toPhoneNumber = "13800000000",
                        content = MessageContentSerializer.serialize(
                            MessageContent.AlarmMessage(
                                location = "北京市朝阳区XX街道",
                                level = "emergency",
                                message = "紧急报警：发现可疑人员"
                            )
                        ),
                        timestamp = currentTime - 86400000
                    ),
                    Message(
                        fromPhoneNumber = "13800000000",
                        toPhoneNumber = "13812345678",
                        content = MessageContentSerializer.serialize(
                            MessageContent.TextMessage("收到报警，已派警员前往")
                        ),
                        timestamp = currentTime - 86390000
                    ),
                    Message(
                        fromPhoneNumber = "13812345678",
                        toPhoneNumber = "13800000000",
                        content = MessageContentSerializer.serialize(
                            MessageContent.TextMessage("感谢，已经安全")
                        ),
                        timestamp = currentTime - 86380000
                    ),
                    
                    // 李四的消息
                    Message(
                        fromPhoneNumber = "13987654321",
                        toPhoneNumber = "13800000000",
                        content = MessageContentSerializer.serialize(
                            MessageContent.QueryMessage("查询今天的社区活动安排")
                        ),
                        timestamp = currentTime - 172800000
                    ),
                    Message(
                        fromPhoneNumber = "13800000000",
                        toPhoneNumber = "13987654321",
                        content = MessageContentSerializer.serialize(
                            MessageContent.TextMessage("今天有社区安全知识讲座，下午2点开始")
                        ),
                        timestamp = currentTime - 172790000
                    ),
                    
                    // 王五的消息（报警消息）
                    Message(
                        fromPhoneNumber = "13711112222",
                        toPhoneNumber = "13800000000",
                        content = MessageContentSerializer.serialize(
                            MessageContent.AlarmMessage(
                                location = "上海市浦东新区XX路",
                                level = "normal",
                                message = "日常报警：门窗未锁"
                            )
                        ),
                        timestamp = currentTime - 259200000
                    )
                )
                
                // 插入消息
                database.messageDao().insertAll(testMessages)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}