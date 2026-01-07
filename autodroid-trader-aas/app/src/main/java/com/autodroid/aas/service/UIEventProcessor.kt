package com.autodroid.aas.service

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import com.autodroid.aas.database.*
import com.autodroid.aas.database.*
import com.autodroid.aas.service.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.*

class UIEventProcessor(private val context: Context) {
    
    companion object {
        const val TAG = "UIEventProcessor"
    }
    
    private lateinit var elementFeatureDao: ElementFeatureDao
    
    fun init(database: UIRecorderDatabase) {
        elementFeatureDao = database.elementFeatureDao()
    }
    
    fun processClickEvent(
        source: AccessibilityNodeInfo?,
        packageName: String,
        activityName: String?
    ): UIEvent? {
        source ?: return null
        
        val elementInfo = extractElementInfo(source)
        
        // 更新元素特征
        updateElementFeature(source, packageName, activityName, null)
        
        // 生成事件
        return UIEvent(
            packageName = packageName,
            activityName = activityName,
            eventType = "CLICK",
            elementId = elementInfo.id,
            elementType = elementInfo.type,
            elementText = elementInfo.text,
            elementHint = elementInfo.hint,
            elementContentDesc = elementInfo.contentDesc,
            elementClass = source.className?.toString(),
            elementBounds = getBoundsString(source),
            inputValue = null,
            selectedValue = null,
            parentHierarchy = extractParentHierarchy(source),
            siblingInfo = extractSiblingInfo(source),
            extraData = getClickExtraData(source),
            screenshotPath = null
        )
    }
    
    fun processInputEvent(
        source: AccessibilityNodeInfo?,
        packageName: String,
        activityName: String?,
        beforeText: CharSequence?,
        afterText: CharSequence?
    ): UIEvent? {
        source ?: return null
        
        val elementInfo = extractElementInfo(source)
        val inputValue = afterText?.toString()
        
        // 更新元素特征
        updateElementFeature(source, packageName, activityName, inputValue)
        
        return UIEvent(
            packageName = packageName,
            activityName = activityName,
            eventType = "INPUT",
            elementId = elementInfo.id,
            elementType = elementInfo.type,
            elementText = elementInfo.text,
            elementHint = elementInfo.hint,
            elementContentDesc = elementInfo.contentDesc,
            elementClass = source.className?.toString(),
            elementBounds = getBoundsString(source),
            inputValue = inputValue,
            selectedValue = null,
            parentHierarchy = extractParentHierarchy(source),
            siblingInfo = extractSiblingInfo(source),
            extraData = getInputExtraData(beforeText, afterText),
            screenshotPath = null
        )
    }
    
    fun processSelectionEvent(
        source: AccessibilityNodeInfo?,
        packageName: String,
        activityName: String?
    ): UIEvent? {
        source ?: return null
        
        val elementInfo = extractElementInfo(source)
        val selectedValue = getSelectedValue(source)
        
        // 更新元素特征
        updateElementFeature(source, packageName, activityName, selectedValue)
        
        return UIEvent(
            packageName = packageName,
            activityName = activityName,
            eventType = "SELECT",
            elementId = elementInfo.id,
            elementType = elementInfo.type,
            elementText = elementInfo.text,
            elementHint = elementInfo.hint,
            elementContentDesc = elementInfo.contentDesc,
            elementClass = source.className?.toString(),
            elementBounds = getBoundsString(source),
            inputValue = null,
            selectedValue = selectedValue,
            parentHierarchy = extractParentHierarchy(source),
            siblingInfo = extractSiblingInfo(source),
            extraData = getSelectionExtraData(source),
            screenshotPath = null
        )
    }
    
    private fun updateElementFeature(
        node: AccessibilityNodeInfo,
        packageName: String,
        activityName: String?,
        inputValue: String?
    ) {
        val signature = generateElementSignature(node)
        
        GlobalScope.launch(Dispatchers.IO) {
            try {
                var feature = elementFeatureDao.getBySignature(signature)
                
                if (feature == null) {
                    // 创建新特征
                    feature = ElementFeature(
                        packageName = packageName,
                        activityName = activityName,
                        elementSignature = signature,
                        elementId = node.viewIdResourceName,
                        elementType = getElementType(node),
                        elementText = node.text?.toString(),
                        elementHint = node.hintText?.toString(),
                        elementContentDesc = node.contentDescription?.toString(),
                        elementClass = node.className?.toString(),
                        parentHierarchy = extractParentHierarchy(node),
                        siblingInfo = extractSiblingInfo(node),
                        commonValues = if (inputValue != null) "[\"$inputValue\"]" else null,
                        lastUsedTime = System.currentTimeMillis(),
                        usageCount = 1,
                        autoFillValue = null
                    )
                    elementFeatureDao.insert(feature)
                } else {
                    // 更新现有特征
                    val values = updateCommonValues(feature.commonValues, inputValue)
                    feature.copy(
                        elementId = node.viewIdResourceName ?: feature.elementId,
                        elementText = node.text?.toString() ?: feature.elementText,
                        elementHint = node.hintText?.toString() ?: feature.elementHint,
                        elementContentDesc = node.contentDescription?.toString() ?: feature.elementContentDesc,
                        commonValues = values,
                        lastUsedTime = System.currentTimeMillis(),
                        usageCount = feature.usageCount + 1
                    ).let { updatedFeature ->
                        elementFeatureDao.update(updatedFeature)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update element feature", e)
            }
        }
    }
    
    // 添加一个公共方法供UIRecorderAccessibilityService调用
    fun updateElementFeatureFromService(
        node: AccessibilityNodeInfo,
        packageName: String,
        activityName: String?,
        inputValue: String?
    ) {
        updateElementFeature(node, packageName, activityName, inputValue)
    }
    
    private fun updateCommonValues(
        existingValues: String?,
        newValue: String?
    ): String? {
        newValue ?: return existingValues
        
        val values = try {
            val listType = object : TypeToken<List<String>>() {}.type
            Gson().fromJson<List<String>>(existingValues, listType) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf<String>()
        }.toMutableList()
        
        // 添加新值（如果不存在）
        if (!values.contains(newValue)) {
            values.add(newValue)
            
            // 只保留最近的10个值
            if (values.size > 10) {
                values.removeAt(0)
            }
        }
        
        return Gson().toJson(values)
    }
    
    private fun generateElementSignature(node: AccessibilityNodeInfo): String {
        // 使用多个特征生成唯一签名
        val builder = StringBuilder()
        
        // 1. 类名
        node.className?.toString()?.let { builder.append(it) }
        
        // 2. 资源ID
        node.viewIdResourceName?.let { builder.append("|id:$it") }
        
        // 3. 文本（前20个字符）
        node.text?.toString()?.take(20)?.let { builder.append("|text:$it") }
        
        // 4. 提示文本
        node.hintText?.toString()?.let { builder.append("|hint:$it") }
        
        // 5. 内容描述
        node.contentDescription?.toString()?.let { builder.append("|desc:$it") }
        
        // 6. 父级结构哈希
        val parentHash = calculateParentHash(node)
        builder.append("|parent:$parentHash")
        
        return builder.toString().hashCode().toString()
    }
    
    private fun calculateParentHash(node: AccessibilityNodeInfo): Int {
        var hash = 0
        var current = node.parent
        var depth = 0
        
        while (current != null && depth < 3) {
            val className = current.className?.toString() ?: ""
            val childCount = current.childCount
            hash = 31 * hash + (className.hashCode() + childCount)
            current = current.parent
            depth++
        }
        
        return hash
    }
    
    data class ElementInfo(
        val id: String?,
        val type: String?,
        val text: String?,
        val hint: String?,
        val contentDesc: String?
    )
    
    private fun extractElementInfo(node: AccessibilityNodeInfo): ElementInfo {
        return ElementInfo(
            id = node.viewIdResourceName,
            type = getElementType(node),
            text = node.text?.toString(),
            hint = node.hintText?.toString(),
            contentDesc = node.contentDescription?.toString()
        )
    }
    
    private fun getElementType(node: AccessibilityNodeInfo): String {
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
    
    private fun extractParentHierarchy(node: AccessibilityNodeInfo): String {
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
    
    private fun extractSiblingInfo(node: AccessibilityNodeInfo): String? {
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
    
    private fun getBoundsString(node: AccessibilityNodeInfo): String {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return "${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}"
    }
    
    private fun getSelectedValue(node: AccessibilityNodeInfo): String? {
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
    
    private fun getClickExtraData(node: AccessibilityNodeInfo): String {
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
    
    private fun getInputExtraData(beforeText: CharSequence?, afterText: CharSequence?): String {
        val extra = mutableMapOf<String, String>()
        
        extra["beforeLength"] = (beforeText?.length ?: 0).toString()
        extra["afterLength"] = (afterText?.length ?: 0).toString()
        extra["isPassword"] = (afterText?.length ?: 0 > 0 && afterText.toString().all { it == '*' }).toString()
        
        return Gson().toJson(extra)
    }
    
    private fun getSelectionExtraData(node: AccessibilityNodeInfo): String {
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