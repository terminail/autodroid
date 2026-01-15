package com.autodroid.guardiansdk.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest

/**
 * 密码本工具类 - 轻量级实现，避免QRCode依赖
 */
object PasswordBookUtils {
    
    private const val PREFS_NAME = "guardian_password_book"
    private const val KEY_PASSWORD_MAP = "password_map"
    private const val KEY_LAST_SYNC = "last_sync"
    
    /**
     * 生成简单的文本格式密码本
     * 格式：数字=密码,数字=密码,...
     * 示例：0=苹果,1=香蕉,2=橙子,...
     */
    fun generatePasswordBookText(passwordMap: Map<String, String>): String {
        return passwordMap.entries.joinToString(",") { "${it.key}=${it.value}" }
    }
    
    /**
     * 解析文本格式密码本
     */
    fun parsePasswordBookText(text: String): Map<String, String> {
        return text.split(",").associate { 
            val parts = it.split("=")
            if (parts.size == 2) parts[0] to parts[1] else "" to ""
        }.filter { it.key.isNotEmpty() }
    }
    
    /**
     * 保存密码本到SharedPreferences
     */
    fun savePasswordBook(context: Context, passwordMap: Map<String, String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val passwordText = generatePasswordBookText(passwordMap)
        
        prefs.edit()
            .putString(KEY_PASSWORD_MAP, passwordText)
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * 从SharedPreferences加载密码本
     */
    fun loadPasswordBook(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val passwordText = prefs.getString(KEY_PASSWORD_MAP, "") ?: ""
        
        return if (passwordText.isNotEmpty()) {
            parsePasswordBookText(passwordText)
        } else {
            // 返回默认密码本
            getDefaultPasswordBook()
        }
    }
    
    /**
     * 获取默认密码本
     */
    private fun getDefaultPasswordBook(): Map<String, String> {
        return mapOf(
            "0" to "苹果", "1" to "香蕉", "2" to "橙子", "3" to "葡萄",
            "4" to "西瓜", "5" to "菠萝", "6" to "芒果", "7" to "樱桃",
            "8" to "草莓", "9" to "桃子", "." to "了", "-" to "和"
        )
    }
    
    /**
     * 生成密码本分享文本
     * 使用Base64编码，避免特殊字符问题
     */
    fun generateShareablePasswordBook(passwordMap: Map<String, String>): String {
        val passwordText = generatePasswordBookText(passwordMap)
        return Base64.encodeToString(passwordText.toByteArray(), Base64.NO_WRAP)
    }
    
    /**
     * 解析分享的密码本
     */
    fun parseShareablePasswordBook(encodedText: String): Map<String, String> {
        return try {
            val decodedText = String(Base64.decode(encodedText, Base64.NO_WRAP))
            parsePasswordBookText(decodedText)
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * 使用系统分享功能分享密码本
     * 生成分享文本，包含Base64编码的密码本和校验和
     */
    fun generateShareText(passwordMap: Map<String, String>): String {
        val passwordText = generatePasswordBookText(passwordMap)
        val encodedText = Base64.encodeToString(passwordText.toByteArray(), Base64.NO_WRAP)
        val checksum = generateChecksum(passwordMap)
        
        return "密码本同步数据：\n$encodedText\n\n校验码：$checksum\n\n请将此文本通过微信等应用分享给对方"
    }
    
    /**
     * 从分享文本解析密码本
     */
    fun parseShareText(shareText: String): Map<String, String>? {
        return try {
            val lines = shareText.lines()
            val encodedLine = lines.find { it.startsWith("密码本同步数据：") }?.substringAfter("密码本同步数据：") 
                ?: lines.find { it.isNotBlank() && !it.contains("校验码") && !it.contains("请将此文本") }
            
            if (encodedLine != null) {
                parseShareablePasswordBook(encodedLine.trim())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 生成简单的校验和
     */
    fun generateChecksum(passwordMap: Map<String, String>): String {
        val text = generatePasswordBookText(passwordMap)
        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest(text.toByteArray())
        return Base64.encodeToString(hash, Base64.NO_WRAP).substring(0, 8)
    }
    
    /**
     * 验证密码本完整性
     */
    fun verifyPasswordBook(passwordMap: Map<String, String>, checksum: String): Boolean {
        return generateChecksum(passwordMap) == checksum
    }
}