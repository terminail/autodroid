package com.autodroid.guardiansdk.ui.why

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class WhyViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WhyViewModel::class.java)) {
            return WhyViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}