package com.autodroid.guardiansdk.ui.wards.model

sealed class WardItem {
    data class WardCard(
        val phoneNumber: String,
        val name: String,
        val avatar: String = "",
        val lastContactTime: String,
        val lastLocation: String,
        val lastAlarmMessage: String
    ) : WardItem()

    data class WardListItem(
        val phoneNumber: String,
        val name: String,
        val avatar: String = "",
        val lastContactTime: String,
        val lastLocation: String,
        val lastAlarmMessage: String
    ) : WardItem()

    data class AddWardButton(
        val index: Int
    ) : WardItem()

    data class EmptyState(
        val message: String = "还没有添加监护人"
    ) : WardItem()
}
