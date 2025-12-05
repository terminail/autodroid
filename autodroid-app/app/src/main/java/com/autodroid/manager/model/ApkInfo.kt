package com.autodroid.manager.model

/**
 * APK信息封装类
 * 封装APK相关的属性和状态
 */
data class ApkInfo(
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val appName: String? = null,
    val filePath: String? = null,
    val fileSize: Long? = null,
    val installTime: Long? = null,
    val updateTime: Long? = null,
    val isSystemApp: Boolean = false,
    val isEnabled: Boolean = true,
    val minSdkVersion: Int? = null,
    val targetSdkVersion: Int? = null
) {
    companion object {
        /**
         * 创建空APK信息
         */
        fun empty(): ApkInfo = ApkInfo()
        
        /**
         * 创建基本APK信息
         */
        fun basic(
            packageName: String,
            versionName: String,
            appName: String
        ): ApkInfo = ApkInfo(
            packageName = packageName,
            versionName = versionName,
            appName = appName
        )
        
        /**
         * 创建详细APK信息
         */
        fun detailed(
            packageName: String,
            versionName: String,
            versionCode: Long,
            appName: String,
            filePath: String,
            fileSize: Long,
            installTime: Long,
            updateTime: Long,
            isSystemApp: Boolean,
            minSdkVersion: Int,
            targetSdkVersion: Int
        ): ApkInfo = ApkInfo(
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            appName = appName,
            filePath = filePath,
            fileSize = fileSize,
            installTime = installTime,
            updateTime = updateTime,
            isSystemApp = isSystemApp,
            minSdkVersion = minSdkVersion,
            targetSdkVersion = targetSdkVersion
        )
    }
    
    /**
         * 检查APK信息是否完整
         */
    fun isComplete(): Boolean = packageName != null && versionName != null && appName != null
    
    /**
     * 获取版本信息
     */
    fun getVersionInfo(): String = "${versionName ?: "未知"} (${versionCode ?: "未知"})"
    
    /**
     * 获取文件大小描述
     */
    fun getFileSizeDescription(): String {
        return when (val size = fileSize) {
            null -> "未知"
            in 0..1023 -> "${size}B"
            in 1024..1048575 -> "${size / 1024}KB"
            in 1048576..1073741823 -> "${size / 1048576}MB"
            else -> "${size / 1073741824}GB"
        }
    }
    
    /**
     * 禁用APK
     */
    fun disabled(): ApkInfo = this.copy(
        isEnabled = false
    )
    
    /**
     * 启用APK
     */
    fun enabled(): ApkInfo = this.copy(
        isEnabled = true
    )
    
    /**
     * 更新版本信息
     */
    fun updateVersion(
        versionName: String,
        versionCode: Long
    ): ApkInfo = this.copy(
        versionName = versionName,
        versionCode = versionCode,
        updateTime = System.currentTimeMillis()
    )
}