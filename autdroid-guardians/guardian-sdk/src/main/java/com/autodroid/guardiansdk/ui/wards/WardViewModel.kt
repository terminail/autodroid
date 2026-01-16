package com.autodroid.guardiansdk.ui.wards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodroid.guardiansdk.data.dao.WardDao
import com.autodroid.guardiansdk.data.database.GuardianDatabase
import com.autodroid.guardiansdk.data.entity.Ward
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 被监护人列表页面的ViewModel
 * 负责管理被监护人数据的增删改查
 */
class WardViewModel(private val database: GuardianDatabase) : ViewModel() {
    
    private val _wards = MutableStateFlow<List<Ward>>(emptyList())
    val wards: StateFlow<List<Ward>> = _wards.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val wardDao: WardDao = database.wardDao()
    
    companion object {
        private const val MAX_GUARDIANS = 5 // 最多5个监护人
    }
    
    init {
        loadWards()
    }
    
    /**
     * 加载所有监护人
     */
    fun loadWards() {
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activeWards = wardDao.getActiveWards()
                _wards.value = activeWards
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "加载监护人失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * 添加新监护人
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
                // 检查是否已达到最大监护人数量
                val currentCount = wardDao.getActiveWards().size
                if (currentCount >= MAX_GUARDIANS) {
                    _errorMessage.value = "已达到最大监护人数量限制(${MAX_GUARDIANS}个)"
                    _loading.value = false
                    return@launch
                }
                
                // 检查手机号是否已存在
                val existingWard = wardDao.getWardByPhoneNumber(phone)
                if (existingWard != null && existingWard.isActive) {
                    _errorMessage.value = "该手机号已存在"
                    _loading.value = false
                    return@launch
                }
                
                // 短信发送在Fragment中处理，ViewModel只负责数据操作
                
                // 创建新监护人
                val newWard = Ward(
                    phoneNumber = phone,
                    name = name,
                    relationship = relationship,
                    passwordBook = "",
                    lastAlarmTime = 0,
                    alarmCount = 0,
                    isActive = true
                )
                
                // 保存到数据库
                wardDao.insertOrUpdateWard(newWard)
                
                // 重新加载数据
                loadWards()
                
            } catch (e: Exception) {
                _errorMessage.value = "添加监护人失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * 更新监护人信息
     */
    fun updateWard(ward: Ward, name: String, phone: String, relationship: String) {
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
                // 检查手机号是否与其他监护人重复（排除当前监护人）
                val existingWard = wardDao.getWardByPhoneNumber(phone)
                if (existingWard != null && existingWard.isActive && existingWard.phoneNumber != ward.phoneNumber) {
                    _errorMessage.value = "该手机号已存在"
                    _loading.value = false
                    return@launch
                }
                
                // 更新监护人信息
                val updatedWard = ward.copy(
                    phoneNumber = phone,
                    name = name,
                    relationship = relationship
                )
                
                // 保存到数据库
                wardDao.insertOrUpdateWard(updatedWard)
                
                // 重新加载数据
                loadWards()
                
            } catch (e: Exception) {
                _errorMessage.value = "更新监护人失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * 删除监护人（软删除）
     */
    fun deleteWard(ward: Ward) {
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 软删除：将isActive设置为false
                val deletedWard = ward.copy(isActive = false)
                wardDao.insertOrUpdateWard(deletedWard)
                
                // 重新加载数据
                loadWards()
                
            } catch (e: Exception) {
                _errorMessage.value = "删除监护人失败: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * 获取监护人统计信息
     */
    fun getWardStats(): WardStats {
        val currentWards = _wards.value
        val totalAlarms = currentWards.sumOf { it.alarmCount }
        val lastAlarmTime = currentWards.maxOfOrNull { it.lastAlarmTime } ?: 0L
        
        return WardStats(
            totalCount = currentWards.size,
            totalAlarms = totalAlarms,
            lastAlarmTime = lastAlarmTime
        )
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

/**
 * 监护人统计信息
 */
data class WardStats(
    val totalCount: Int,
    val totalAlarms: Int,
    val lastAlarmTime: Long
)