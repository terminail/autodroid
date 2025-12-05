// DetailViewModel.kt
package com.autodroid.manager.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.autodroid.manager.model.WorkflowData
import com.autodroid.manager.model.Report
import com.autodroid.manager.model.OrderData

class DetailViewModel : ViewModel() {
    private val _workflowData = MutableLiveData<WorkflowData>()
    val workflowData: LiveData<WorkflowData> = _workflowData

    private val _reportData = MutableLiveData<Report>()
    val reportData: LiveData<Report> = _reportData

    private val _orderData = MutableLiveData<OrderData>()
    val orderData: LiveData<OrderData> = _orderData

    fun setWorkflowData(data: WorkflowData) {
        _workflowData.value = data
    }

    fun setReportData(data: Report) {
        _reportData.value = data
    }

    fun setOrderData(data: OrderData) {
        _orderData.value = data
    }
}