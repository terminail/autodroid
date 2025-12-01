// SharedViewModelFactory.kt
package com.autodroid.manager.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.autodroid.manager.auth.viewmodel.AuthViewModel

class SharedViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            if (sharedViewModel == null) {
                sharedViewModel = AuthViewModel()
            }
            return sharedViewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    companion object {
        private var sharedViewModel: AuthViewModel? = null
    }
}