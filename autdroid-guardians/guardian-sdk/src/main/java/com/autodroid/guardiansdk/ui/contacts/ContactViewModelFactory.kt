package com.autodroid.guardiansdk.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.autodroid.guardiansdk.data.database.GuardianDatabase

class ContactViewModelFactory(private val database: GuardianDatabase) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactViewModel::class.java)) {
            return ContactViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}