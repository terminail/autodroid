package com.autodroid.guardiansdk.util

import android.content.Context
import android.content.Intent

/**
 * 系统分享工具类
 */
object ShareUtils {
    
    /**
     * 分享密码本到其他应用
     */
    fun sharePasswordBook(context: Context, passwordMap: Map<String, String>) {
        val shareText = PasswordBookUtils.generateShareText(passwordMap)
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "密码本同步数据")
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "分享密码本到"))
    }
    
    /**
     * 处理从其他应用分享过来的密码本数据
     */
    fun handleSharedPasswordBook(context: Context, intent: Intent): Map<String, String>? {
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    sharedText?.let { PasswordBookUtils.parseShareText(it) }
                } else {
                    null
                }
            }
            else -> null
        }
    }
    
    /**
     * 检查Intent是否包含密码本数据
     */
    fun isPasswordBookShareIntent(intent: Intent): Boolean {
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    sharedText?.contains("密码本同步数据") == true ||
                    sharedText?.let { PasswordBookUtils.parseShareText(it) != null } == true
                } else {
                    false
                }
            }
            else -> false
        }
    }
}