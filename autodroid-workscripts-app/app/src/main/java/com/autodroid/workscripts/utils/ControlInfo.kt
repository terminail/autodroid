package com.autodroid.workscripts.utils

import android.graphics.Rect

data class ControlInfo(
    val id: String,
    val text: String?,
    val className: String,
    val xpath: String,
    val resourceId: String?,
    val bounds: Rect,
    val clickable: Boolean,
    val visible: Boolean,
    val type: String
) {
    /**
     * 获取显示文本
     */
    fun getDisplayText(): String {
        if (!text.isNullOrEmpty()) {
            return if (text.length > 20) text.substring(0, 20) + "..." else text
        } else if (!resourceId.isNullOrEmpty()) {
            val parts = resourceId.split("/")
            if (parts.size > 1) {
                return parts[1]
            }
        } else if (className.isNotEmpty()) {
            val simpleName = className.substring(className.lastIndexOf('.') + 1)
            return simpleName
        }
        return "未知控件"
    }
}