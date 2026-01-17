package com.autodroid.guardiansdk.data.dao

import androidx.room.*
import com.autodroid.guardiansdk.data.entity.Contact
import com.autodroid.guardiansdk.data.entity.ContactType
import kotlinx.coroutines.flow.Flow

/**
 * 联系人DAO
 * 统一处理被监护人和监护人的数据访问
 */
@Dao
interface ContactDao {

    /**
     * 插入或更新联系人
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(contact: Contact)

    /**
     * 批量插入或更新联系人
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(contacts: List<Contact>)

    /**
     * 根据手机号获取联系人
     */
    @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber")
    suspend fun getContactByPhoneNumber(phoneNumber: String): Contact?

    /**
     * 监听指定手机号的联系人变化
     */
    @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber")
    fun observeContactByPhoneNumber(phoneNumber: String): Flow<Contact?>

    /**
     * 获取所有指定类型的联系人
     */
    @Query("SELECT * FROM contacts WHERE type = :type ORDER BY createdAt DESC")
    suspend fun getContactsByType(type: ContactType): List<Contact>

    /**
     * 监听所有指定类型的联系人
     */
    @Query("SELECT * FROM contacts WHERE type = :type ORDER BY createdAt DESC")
    fun observeContactsByType(type: ContactType): Flow<List<Contact>>

    /**
     * 获取所有活跃的指定类型联系人
     */
    @Query("SELECT * FROM contacts WHERE type = :type AND isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveContactsByType(type: ContactType): List<Contact>

    /**
     * 监听所有活跃的指定类型联系人
     */
    @Query("SELECT * FROM contacts WHERE type = :type AND isActive = 1 ORDER BY createdAt DESC")
    fun observeActiveContactsByType(type: ContactType): Flow<List<Contact>>

    /**
     * 获取指定类型的联系人数量
     */
    @Query("SELECT COUNT(*) FROM contacts WHERE type = :type")
    suspend fun getContactCountByType(type: ContactType): Int

    /**
     * 获取指定类型的活跃联系人数量
     */
    @Query("SELECT COUNT(*) FROM contacts WHERE type = :type AND isActive = 1")
    suspend fun getActiveContactCountByType(type: ContactType): Int

    /**
     * 根据手机号删除联系人
     */
    @Query("DELETE FROM contacts WHERE phoneNumber = :phoneNumber")
    suspend fun deleteContactByPhoneNumber(phoneNumber: String)

    /**
     * 批量删除联系人
     */
    @Query("DELETE FROM contacts WHERE phoneNumber IN (:phoneNumbers)")
    suspend fun deleteContactsByPhoneNumbers(phoneNumbers: List<String>)

    /**
     * 清空所有联系人
     */
    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()

    /**
     * 更新联系人状态
     */
    @Query("UPDATE contacts SET isActive = :isActive, updatedAt = :updatedAt WHERE phoneNumber = :phoneNumber")
    suspend fun updateContactStatus(phoneNumber: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    /**
     * 更新报警次数（仅对WARD类型有效）
     */
    @Query("UPDATE contacts SET alarmCount = alarmCount + 1, updatedAt = :updatedAt WHERE phoneNumber = :phoneNumber AND type = 'WARD'")
    suspend fun incrementAlarmCount(phoneNumber: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 更新最后消息时间和消息计数
     */
    @Query("UPDATE contacts SET lastMessageTime = :messageTime, messageCount = messageCount + 1, updatedAt = :updatedAt WHERE phoneNumber = :phoneNumber")
    suspend fun updateMessageStats(phoneNumber: String, messageTime: Long, updatedAt: Long = System.currentTimeMillis())

    /**
     * 获取监护人（GUARDIAN类型），按orderIndex排序
     */
    @Query("SELECT * FROM contacts WHERE type = 'GUARDIAN' ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getGuardians(): List<Contact>

    /**
     * 监听监护人变化
     */
    @Query("SELECT * FROM contacts WHERE type = 'GUARDIAN' ORDER BY orderIndex ASC, createdAt ASC")
    fun observeGuardians(): Flow<List<Contact>>

    /**
     * 获取指定索引的监护人
     */
    @Query("SELECT * FROM contacts WHERE type = 'GUARDIAN' AND orderIndex = :orderIndex")
    suspend fun getGuardianByOrderIndex(orderIndex: Int): Contact?

    /**
     * 更新监护人的主要状态
     */
    @Query("UPDATE contacts SET isPrimary = :isPrimary, updatedAt = :updatedAt WHERE phoneNumber = :phoneNumber AND type = 'GUARDIAN'")
    suspend fun updateGuardianPrimaryStatus(phoneNumber: String, isPrimary: Boolean, updatedAt: Long = System.currentTimeMillis())

    /**
     * 获取主要监护人
     */
    @Query("SELECT * FROM contacts WHERE type = 'GUARDIAN' AND isPrimary = 1")
    suspend fun getPrimaryGuardian(): Contact?

    /**
     * 获取被监护人数量
     */
    @Query("SELECT COUNT(*) FROM contacts WHERE type = 'WARD'")
    suspend fun getWardsCount(): Int

    /**
     * 获取被监护人（WARD类型），按orderIndex排序
     */
    @Query("SELECT * FROM contacts WHERE type = 'WARD' ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getWards(): List<Contact>

    /**
     * 监听被监护人变化
     */
    @Query("SELECT * FROM contacts WHERE type = 'WARD' ORDER BY orderIndex ASC, createdAt ASC")
    fun observeWards(): Flow<List<Contact>>

    /**
     * 搜索联系人（模糊搜索姓名和手机号）
     */
    @Query("SELECT * FROM contacts WHERE (name LIKE '%' || :keyword || '%' OR phoneNumber LIKE '%' || :keyword || '%') AND type = :type")
    fun searchContacts(keyword: String, type: ContactType): Flow<List<Contact>>
}