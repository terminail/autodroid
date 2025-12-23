package com.autodroid.trader.ui.tradeorders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.autodroid.trader.model.TradeOrder

class TradeOrderViewModel : ViewModel() {
    // private val _reportData = MutableLiveData<Report>()
    // val reportData: LiveData<Report> = _reportData

    private val _orderData = MutableLiveData<TradeOrder>()
    val orderData: LiveData<TradeOrder> = _orderData

    // fun setReportData(data: Report) {
    //     _reportData.value = data
    // }

    fun setOrderData(data: TradeOrder) {
        _orderData.value = data
    }
}