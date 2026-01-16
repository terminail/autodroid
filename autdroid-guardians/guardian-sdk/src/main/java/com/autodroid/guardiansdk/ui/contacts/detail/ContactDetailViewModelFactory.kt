package com.autodroid.guardiansdk.ui.contacts.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.autodroid.guardiansdk.data.dao.MessageDao
import com.autodroid.guardiansdk.data.dao.ContactDao

class ContactDetailViewModelFactory(
    private val contactDao: ContactDao,
    private val messageDao: MessageDao
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactDetailViewModel::class.java)) {
            return ContactDetailViewModel(contactDao, messageDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}