package com.autodroid.controller.model

sealed class ControlItem {
    data class HeaderItem(
        val title: String,
        var isExpanded: Boolean = true
    ) : ControlItem()
    
    data class ServiceControlItem(
        val id: String,
        val title: String,
        val description: String,
        val startAction: String,
        val stopAction: String,
        val status: ServiceStatus = ServiceStatus.STOPPED,
        val lastCheckTime: String? = null
    ) : ControlItem()
    
    data class AppiumControlItem(
        val id: String,
        val title: String,
        val description: String,
        val startAction: String,
        val stopAction: String,
        val status: AppiumStatus = AppiumStatus.STOPPED,
        val lastCheckTime: String? = null
    ) : ControlItem()
    
    data class TestControlItem(
        val id: String,
        val title: String,
        val description: String,
        val action: String,
        val testType: TestType = TestType.BASIC
    ) : ControlItem()
    
    data class SettingsItem(
        val id: String,
        val title: String,
        val description: String,
        val action: String,
        val settingsType: SettingsType = SettingsType.ACCESSIBILITY
    ) : ControlItem()
    
    data class UIA2StatusItem(
        val id: String,
        val title: String,
        val description: String,
        val status: String = "未知",
        val version: String = "",
        val port: String = "",
        val sessionId: String = "",
        val message: String = "",
        val lastCheckTime: String? = null
    ) : ControlItem()
    
    data class AppiumStatusItem(
        val id: String,
        val title: String,
        val description: String,
        val status: String = "未知",
        val processStatus: String = "未检查",
        val portStatus: String = "未检查",
        val httpStatus: String = "未检查",
        val version: String = "",
        val port: String = "",
        val sessionId: String = "",
        val message: String = "",
        val lastCheckTime: String? = null
    ) : ControlItem()
}

enum class ButtonStyle {
    PRIMARY, OUTLINED
}

enum class AppiumStatus {
    RUNNING, STOPPED, STARTING, STOPPING
}

enum class ServiceStatus {
    RUNNING, STOPPED, STARTING, STOPPING
}

enum class TestType {
    BASIC, ADVANCED, CUSTOM
}

enum class SettingsType {
    ACCESSIBILITY, APP_SETTINGS, SYSTEM
}