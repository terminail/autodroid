package com.autodroid.guardiansdk.ui.wards.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.autodroid.guardiansdk.data.dao.MessageDao
import com.autodroid.guardiansdk.data.dao.WardDao

class WardDetailViewModelFactory(
    private val wardDao: WardDao,
    private val messageDao: MessageDao
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WardDetailViewModel::class.java)) {
            return WardDetailViewModel(wardDao, messageDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}