package com.autodroid.guardiansdk.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodroid.guardiansdk.data.dao.ContactDao
import com.autodroid.guardiansdk.data.database.GuardianDatabase
import com.autodroid.guardiansdk.data.entity.Contact
import com.autodroid.guardiansdk.data.entity.ContactType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 联系人列表页面的ViewModel
 * 负责管理所有联系人数据（被监护人和监护人）
 */
class ContactViewModel(private val database: GuardianDatabase) : ViewModel() {
    
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val contactDao: ContactDao = database.contactDao()
    
    companion object {
        // 被监护人没有数量限制
    }
    
    init {
        loadAllContacts()
    }
    
    /**
     * 加载所有联系人（被监护人和监护人）
     */
    fun loadAllContacts() {
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activeGuardians = contactDao.getActiveContactsByType(ContactType.GUARDIAN)
                val activeWards = contactDao.getActiveContactsByType(ContactType.WARD)

                // 合并所有联系人，监护人显示在被监护人之前
                val allContacts = activeGuardians + activeWards
                _contacts.value = allContacts

                android.util.Log.d("ContactViewModel", "=== 加载联系人完成，监护人: ${activeGuardians.size}, 被监护人: ${activeWards.size}, 总数: ${allContacts.size} ===")
                
                // 记录每个联系人的详细信息
                allContacts.forEach { contact ->
                    android.util.Log.d("ContactViewModel", "=== 联系人: ${contact.name} (${contact.phoneNumber}) - 类型: ${contact.type} ===")
                }
                
                _errorMessage.value = null
            } catch (e: Exception) {
                android.util.Log.e("ContactViewModel", "=== 加载联系人失败 ===", e)
                _errorMessage.value = "加载联系人失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * 添加新被监护人
     */
    fun addWard(name: String, phone: String, relationship: String) {
        if (name.isEmpty() || phone.isEmpty()) {
            _errorMessage.value = "姓名和手机号不能为空"
            return
        }
        
        if (!isValidPhoneNumber(phone)) {
            _errorMessage.value = "请输入有效的手机号"
            return
        }
        
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 检查手机号是否已存在
                val existingContact = contactDao.getContactByPhoneNumber(phone)
                if (existingContact != null && existingContact.isActive && existingContact.type == ContactType.WARD) {
                    _errorMessage.value = "该手机号已存在"
                    _loading.value = false
                    return@launch
                }
                
                // 短信发送在Fragment中处理，ViewModel只负责数据操作
                
                // 创建新被监护人
                val newWard = Contact(
                    phoneNumber = phone,
                    name = name,
                    type = ContactType.WARD,
                    relationship = relationship,
                    passwordBook = "",
                    alarmCount = 0,
                    isActive = true
                )
                
                // 保存到数据库
                contactDao.insertOrUpdate(newWard)
                
                android.util.Log.d("ContactViewModel", "=== 添加被监护人成功: ${name} (${phone}) ===")
                
                // 重新加载数据
                loadAllContacts()
                
            } catch (e: Exception) {
                android.util.Log.e("ContactViewModel", "=== 添加被监护人失败 ===", e)
                _errorMessage.value = "添加被监护人失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * 更新被监护人信息
     */
    fun updateWard(ward: Contact, name: String, phone: String, relationship: String) {
        if (name.isEmpty() || phone.isEmpty()) {
            _errorMessage.value = "姓名和手机号不能为空"
            return
        }
        
        if (!isValidPhoneNumber(phone)) {
            _errorMessage.value = "请输入有效的手机号"
            return
        }
        
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 检查手机号是否与其他被监护人重复（排除当前被监护人）
                val existingContact = contactDao.getContactByPhoneNumber(phone)
                if (existingContact != null && existingContact.isActive && existingContact.phoneNumber != ward.phoneNumber) {
                    _errorMessage.value = "该手机号已存在"
                    _loading.value = false
                    return@launch
                }
                
                // 更新被监护人信息
                val updatedContact = ward.copy(
                    phoneNumber = phone,
                    name = name,
                    relationship = relationship
                )
                
                // 保存到数据库
                contactDao.insertOrUpdate(updatedContact)
                
                android.util.Log.d("ContactViewModel", "=== 更新被监护人成功: ${name} (${phone}) ===")
                
                // 重新加载数据
                loadAllContacts()
                
            } catch (e: Exception) {
                android.util.Log.e("ContactViewModel", "=== 更新被监护人失败 ===", e)
                _errorMessage.value = "更新被监护人失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * 删除被监护人（软删除）
     */
    fun deleteWard(ward: Contact) {
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 软删除：将isActive设置为false
                val deletedContact = ward.copy(isActive = false)
                contactDao.insertOrUpdate(deletedContact)
                
                android.util.Log.d("ContactViewModel", "=== 删除被监护人成功: ${ward.name} (${ward.phoneNumber}) ===")
                
                // 重新加载数据
                loadAllContacts()
                
            } catch (e: Exception) {
                android.util.Log.e("ContactViewModel", "=== 删除被监护人失败 ===", e)
                _errorMessage.value = "删除被监护人失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * 验证手机号格式
     */
    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("^1[3-9]\\d{9}$"))
    }
    
    /**
     * 获取设备名称
     */
    private fun getDeviceName(): String {
        // 从设置中获取设备名称，如果没有则使用默认名称
        return "我的设备" // 可以扩展为从SharedPreferences或数据库获取
    }
    
    /**
     * 获取设备手机号
     */
    private fun getDevicePhoneNumber(): String? {
        // 获取设备手机号（需要权限）
        // 这里返回null，实际应用中可以申请权限获取
        return null
    }
}
