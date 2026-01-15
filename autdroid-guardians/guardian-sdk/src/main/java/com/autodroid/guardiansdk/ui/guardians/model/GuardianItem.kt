package com.autodroid.guardiansdk.ui.guardians.model

sealed class GuardianItem {
    data class GuardianCard(
        val id: Int,
        val name: String,
        val avatar: String = "",
        val lastContactTime: String,
        val lastLocation: String,
        val lastAlarmMessage: String
    ) : GuardianItem()

    data class GuardianListItem(
        val id: Int,
        val name: String,
        val avatar: String = "",
        val lastContactTime: String,
        val lastLocation: String,
        val lastAlarmMessage: String
    ) : GuardianItem()

    data class AddGuardianButton(
        val index: Int
    ) : GuardianItem()

    data class EmptyState(
        val message: String = "还没有添加监护人"
    ) : GuardianItem()
}
