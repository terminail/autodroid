package com.autodroid.guardiansdk.ui.wards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.autodroid.guardiansdk.data.database.GuardianDatabase

class WardViewModelFactory(private val database: GuardianDatabase) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WardViewModel::class.java)) {
            return WardViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}