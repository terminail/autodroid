// DetailViewModel.kt
package com.autodroid.proxy.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DetailViewModel : ViewModel() {
    private val _workflowData = MutableLiveData<Map<String, Any>>()
    val workflowData: LiveData<Map<String, Any>> = _workflowData

    private val _reportData = MutableLiveData<Map<String, Any>>()
    val reportData: LiveData<Map<String, Any>> = _reportData

    private val _orderData = MutableLiveData<Map<String, Any>>()
    val orderData: LiveData<Map<String, Any>> = _orderData

    fun setWorkflowData(data: Map<String, Any>) {
        _workflowData.value = data
    }

    fun setReportData(data: Map<String, Any>) {
        _reportData.value = data
    }

    fun setOrderData(data: Map<String, Any>) {
        _orderData.value = data
    }
}