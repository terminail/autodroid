package com.autodroid.guardiansdk.data.model

data class GuardianInfo(
    val id: Int,
    val name: String,
    val avatar: String = "",
    val lastContactTime: String = "",
    val lastLocation: String = "",
    val lastAlarmMessage: String = ""
)