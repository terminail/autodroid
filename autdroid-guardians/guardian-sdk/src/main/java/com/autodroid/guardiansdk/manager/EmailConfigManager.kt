package com.autodroid.guardiansdk.manager

import android.content.Context
import android.content.SharedPreferences
import com.autodroid.guardiansdk.util.EncryptionUtils

/**
 * 邮箱配置管理器
 * 负责邮箱账号密码的加密存储和读取
 */
class EmailConfigManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "email_config"
        private const val KEY_EMAIL_ENABLED = "email_enabled"
        private const val KEY_EMAIL_ADDRESS = "email_address"
        private const val KEY_EMAIL_PASSWORD = "email_password"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_SMTP_TLS = "smtp_tls"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 检查是否启用了邮件发送功能
     */
    fun isEmailEnabled(): Boolean {
        return prefs.getBoolean(KEY_EMAIL_ENABLED, false)
    }

    /**
     * 设置邮件发送功能启用状态
     */
    fun setEmailEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EMAIL_ENABLED, enabled).apply()
    }

    /**
     * 保存邮箱配置（自动加密密码）
     */
    fun saveEmailConfig(
        emailAddress: String,
        password: String,
        smtpHost: String,
        smtpPort: String,
        useTLS: Boolean
    ) {
        prefs.edit()
            .putString(KEY_EMAIL_ADDRESS, emailAddress)
            .putString(KEY_EMAIL_PASSWORD, EncryptionUtils.encryptString(password))
            .putString(KEY_SMTP_HOST, smtpHost)
            .putString(KEY_SMTP_PORT, smtpPort)
            .putBoolean(KEY_SMTP_TLS, useTLS)
            .apply()

        // 自动启用邮件功能
        setEmailEnabled(true)
    }

    /**
     * 获取邮箱配置（自动解密密码）
     */
    fun getEmailConfig(): EmailConfig? {
        val emailAddress = prefs.getString(KEY_EMAIL_ADDRESS, null)
        val encryptedPassword = prefs.getString(KEY_EMAIL_PASSWORD, null)
        val smtpHost = prefs.getString(KEY_SMTP_HOST, null)
        val smtpPort = prefs.getString(KEY_SMTP_PORT, null)
        val useTLS = prefs.getBoolean(KEY_SMTP_TLS, true)

        if (emailAddress == null || encryptedPassword == null || smtpHost == null || smtpPort == null) {
            return null
        }

        try {
            val password = EncryptionUtils.decryptString(encryptedPassword)
            return EmailConfig(
                emailAddress = emailAddress,
                password = password,
                smtpHost = smtpHost,
                smtpPort = smtpPort,
                useTLS = useTLS
            )
        } catch (e: Exception) {
            android.util.Log.e("EmailConfigManager", "解密邮箱密码失败", e)
            return null
        }
    }

    /**
     * 清除邮箱配置
     */
    fun clearEmailConfig() {
        prefs.edit().clear().apply()
        setEmailEnabled(false)
    }

    /**
     * 邮箱配置数据类
     */
    data class EmailConfig(
        val emailAddress: String,
        val password: String,
        val smtpHost: String,
        val smtpPort: String,
        val useTLS: Boolean = true
    )
}
