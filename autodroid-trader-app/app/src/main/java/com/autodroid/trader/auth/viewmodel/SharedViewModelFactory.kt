// SharedViewModelFactory.kt
package com.autodroid.trader.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

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