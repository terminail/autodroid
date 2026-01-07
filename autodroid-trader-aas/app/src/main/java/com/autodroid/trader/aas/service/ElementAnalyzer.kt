package com.autodroid.trader.aas.service

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.autodroid.trader.aas.service.UIEventProcessor
import com.google.gson.Gson

class ElementAnalyzer {
    
    fun extractElementInfo(node: AccessibilityNodeInfo): UIEventProcessor.ElementInfo {
        return UIEventProcessor.ElementInfo(
            id = node.viewIdResourceName,
            type = getElementType(node),
            text = node.text?.toString(),
            hint = node.hintText?.toString(),
            contentDesc = node.contentDescription?.toString()
        )
    }
    
    fun getElementType(node: AccessibilityNodeInfo): String {
        node.className?.toString()?.let { className ->
            return when {
                className.contains("Button", ignoreCase = true) -> "Button"
                className.contains("EditText", ignoreCase = true) -> "EditText"
                className.contains("TextView", ignoreCase = true) -> "TextView"
                className.contains("CheckBox", ignoreCase = true) -> "CheckBox"
                className.contains("RadioButton", ignoreCase = true) -> "RadioButton"
                className.contains("Spinner", ignoreCase = true) -> "Spinner"
                className.contains("ListView", ignoreCase = true) -> "ListView"
                className.contains("RecyclerView", ignoreCase = true) -> "RecyclerView"
                className.contains("WebView", ignoreCase = true) -> "WebView"
                else -> className.substringAfterLast(".")
            }
        }
        
        return "Unknown"
    }
    
    fun extractParentHierarchy(node: AccessibilityNodeInfo): String {
        val hierarchy = mutableListOf<Map<String, String>>()
        var current = node.parent
        var depth = 0
        
        while (current != null && depth < 5) {
            val info = mapOf(
                "class" to (current.className?.toString() ?: ""),
                "id" to (current.viewIdResourceName ?: ""),
                "text" to (current.text?.toString()?.take(50) ?: ""),
                "childCount" to current.childCount.toString()
            )
            hierarchy.add(info)
            current = current.parent
            depth++
        }
        
        return Gson().toJson(hierarchy)
    }
    
    fun extractSiblingInfo(node: AccessibilityNodeInfo): String? {
        val parent = node.parent ?: return null
        
        val siblings = mutableListOf<Map<String, String>>()
        
        for (i in 0 until parent.childCount) {
            val sibling = parent.getChild(i)
            sibling?.let {
                val info = mapOf(
                    "index" to i.toString(),
                    "class" to (it.className?.toString() ?: ""),
                    "id" to (it.viewIdResourceName ?: ""),
                    "text" to (it.text?.toString()?.take(30) ?: ""),
                    "isTarget" to (it == node).toString()
                )
                siblings.add(info)
            }
        }
        
        return Gson().toJson(siblings)
    }
    
    fun getBoundsString(node: AccessibilityNodeInfo): String {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return "${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}"
    }
    
    fun getSelectedValue(node: AccessibilityNodeInfo): String? {
        // 对于选择器类型的元素，获取选中的值
        if (node.isCheckable) {
            return if (node.isChecked) "true" else "false"
        }
        
        // 尝试从子节点获取选中的文本
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            child?.let {
                if (it.isSelected) {
                    return it.text?.toString()
                }
            }
        }
        
        return node.text?.toString()
    }
    
    fun getClickExtraData(node: AccessibilityNodeInfo): String {
        val extra = mutableMapOf<String, String>()
        
        if (node.isClickable) extra["clickable"] = "true"
        if (node.isLongClickable) extra["longClickable"] = "true"
        if (node.isEnabled) extra["enabled"] = "true"
        if (node.isCheckable) {
            extra["checkable"] = "true"
            extra["checked"] = node.isChecked.toString()
        }
        
        return Gson().toJson(extra)
    }
    
    fun getInputExtraData(beforeText: CharSequence?, afterText: CharSequence?): String {
        val extra = mutableMapOf<String, String>()
        
        extra["beforeLength"] = (beforeText?.length ?: 0).toString()
        extra["afterLength"] = (afterText?.length ?: 0).toString()
        extra["isPassword"] = (afterText?.length ?: 0 > 0 && afterText.toString().all { it == '*' }).toString()
        
        return Gson().toJson(extra)
    }
    
    fun getSelectionExtraData(node: AccessibilityNodeInfo): String {
        val extra = mutableMapOf<String, String>()
        
        if (node.isSelected) extra["selected"] = "true"
        if (node.isCheckable) {
            extra["checkable"] = "true"
            extra["checked"] = node.isChecked.toString()
        }
        
        // 对于下拉列表，获取选项数量
        if (node.className?.toString()?.contains("Spinner") == true) {
            extra["isSpinner"] = "true"
        }
        
        return Gson().toJson(extra)
    }
}