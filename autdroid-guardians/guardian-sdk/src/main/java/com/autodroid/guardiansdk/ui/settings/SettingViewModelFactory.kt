package com.autodroid.guardiansdk.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.autodroid.guardiansdk.data.database.GuardianDatabase

class SettingViewModelFactory(private val database: GuardianDatabase) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingViewModel::class.java)) {
            return SettingViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}