package com.autodroid.manager.model

/**
 * APK信息封装类
 * 封装APK相关的属性和状态
 */
data class Apk(
    val packageName: String,
    val appName: String,
    val version: String,
    val versionCode: Int,
    val installedTime: Long,
    val isSystem: Boolean,
    val iconPath: String
) {
    companion object {
        fun empty(): Apk = Apk(
            packageName = "",
            appName = "",
            version = "",
            versionCode = 0,
            installedTime = 0L,
            isSystem = false,
            iconPath = ""
        )
    }
}