// DetailViewModel.kt
package com.autodroid.manager.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.autodroid.manager.model.WorkScriptData
import com.autodroid.manager.model.Report
import com.autodroid.manager.model.OrderData

class DetailViewModel : ViewModel() {
    private val _workScriptData = MutableLiveData<WorkScriptData>()
    val workScriptData: LiveData<WorkScriptData> = _workScriptData

    private val _reportData = MutableLiveData<Report>()
    val reportData: LiveData<Report> = _reportData

    private val _orderData = MutableLiveData<OrderData>()
    val orderData: LiveData<OrderData> = _orderData

    fun setWorkScriptData(data: WorkScriptData) {
        _workScriptData.value = data
    }

    fun setReportData(data: Report) {
        _reportData.value = data
    }

    fun setOrderData(data: OrderData) {
        _orderData.value = data
    }
}