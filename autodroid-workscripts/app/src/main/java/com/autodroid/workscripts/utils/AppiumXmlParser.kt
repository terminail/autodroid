package com.autodroid.workscripts.utils

import android.graphics.Rect
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.InputStream
import java.util.*

class AppiumXmlParser {
    private val TAG = "AppiumXmlParser"
    
    /**
     * 解析Appium XML文件，提取控件信息
     */
    fun parseXml(inputStream: InputStream): List<ControlInfo> {
        val allControls = mutableListOf<ControlInfo>()
        
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            
            val nodes = mutableListOf<NodeInfo>()
            
            Log.d(TAG, "Starting XML parsing")
            
            // 使用迭代方式解析XML，避免递归导致的栈溢出
            var eventType = parser.eventType
            val depthStack = mutableListOf<Int>()
            val xpathStack = mutableListOf<String>()
            var currentDepth = 0
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val node = NodeInfo()
                        node.depth = currentDepth
                        
                        // 解析属性
                        for (i in 0 until parser.attributeCount) {
                            val attrName = parser.getAttributeName(i)
                            val attrValue = parser.getAttributeValue(i)
                            
                            when (attrName) {
                                "class" -> node.className = attrValue
                                "resource-id" -> node.resourceId = attrValue
                                "text" -> node.text = attrValue
                                "clickable" -> node.clickable = "true" == attrValue
                                "displayed" -> node.visible = "true" == attrValue
                                "bounds" -> node.bounds = parseBounds(attrValue)
                                "index" -> node.index = attrValue.toIntOrNull() ?: 0
                                "enabled" -> node.enabled = "true" == attrValue
                                "focused" -> node.focused = "true" == attrValue
                                "long-clickable" -> node.longClickable = "true" == attrValue
                                "password" -> node.password = "true" == attrValue
                                "scrollable" -> node.scrollable = "true" == attrValue
                                "selected" -> node.selected = "true" == attrValue
                            }
                        }
                        
                        // 构建xpath
                        val className = node.className
                        if (!className.isNullOrEmpty()) {
                            val parentPath = xpathStack.lastOrNull() ?: ""
                            val index = getChildIndex(nodes, className, currentDepth)
                            node.xpath = if (parentPath.isEmpty()) 
                                "/${className}[$index]" 
                            else 
                                "$parentPath/${className}[$index]"
                            
                            nodes.add(node)
                            depthStack.add(currentDepth)
                            xpathStack.add(node.xpath ?: "")
                            
                            Log.d(TAG, "Found node: class=$className, clickable=${node.clickable}, text=${node.text}, resourceId=${node.resourceId}, displayed=${node.visible}")
                        }
                        
                        currentDepth++
                    }
                    XmlPullParser.END_TAG -> {
                        if (depthStack.isNotEmpty() && currentDepth <= depthStack.last()) {
                            depthStack.removeAt(depthStack.lastIndex)
                            xpathStack.removeAt(xpathStack.lastIndex)
                        }
                        currentDepth--
                    }
                }
                
                eventType = parser.next()
            }
            
            // 过滤和筛选控件
            allControls.addAll(processNodes(nodes))
            
            Log.d(TAG, "Total nodes parsed: ${nodes.size}")
            Log.d(TAG, "Filtered controls: ${allControls.size}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XML: ${e.message}", e)
        }
        
        // 按位置排序（从上到下）
        allControls.sortWith(compareBy<ControlInfo> { it.bounds.top }.thenBy { it.bounds.left })
        
        return allControls
    }
    
    private fun processNodes(nodes: List<NodeInfo>): List<ControlInfo> {
        val controls = mutableListOf<ControlInfo>()
        
        for (i in nodes.indices) {
            val node = nodes[i]
            
            // 跳过不可见的控件
            if (!node.visible) continue
            
            // 检查是否为可交互的控件
            val className = node.className ?: continue
            
            // 简化过滤条件：只要可点击或有文本或有resource-id就显示
            if (node.clickable || !node.text.isNullOrEmpty() || !node.resourceId.isNullOrEmpty()) {
                // 检查是否有可点击的子控件
                var hasClickableChild = false
                for (j in i + 1 until nodes.size) {
                    val child = nodes[j]
                    if (child.depth <= node.depth) break // 同层级或父层级，跳出
                    if (child.depth == node.depth + 1 && child.clickable) {
                        hasClickableChild = true
                        break
                    }
                }
                
                // 如果本身可点击但有可点击子控件，跳过
                if (node.clickable && hasClickableChild) {
                    continue
                }
                
                // 检查bounds是否有效：宽度和高度必须大于0
                val bounds = node.bounds ?: Rect(0, 0, 0, 0)
                val width = bounds.width()
                val height = bounds.height()
                
                if (width <= 0 || height <= 0) {
                    Log.d(TAG, "Skipping control with invalid bounds: ${className}, bounds=[${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}], width=$width, height=$height")
                    continue
                }
                
                // 创建ControlInfo
                val type = getControlType(className)
                val control = ControlInfo(
                    id = "control_${controls.size}",
                    text = node.text,
                    className = className,
                    xpath = node.xpath ?: "",
                    resourceId = node.resourceId,
                    bounds = bounds,
                    clickable = node.clickable,
                    visible = node.visible,
                    type = type
                )
                
                controls.add(control)
                Log.d(TAG, "Added control: ${className}, text=${node.text}, clickable=${node.clickable}, resourceId=${node.resourceId}, bounds=[${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}]")
            }
        }
        
        return controls
    }
    
    private fun getControlType(className: String): String {
        val classNameLower = className.lowercase()
        return when {
            classNameLower.contains("image") -> {
                if (classNameLower.contains("button")) "ImageButton" else "ImageView"
            }
            classNameLower.contains("button") -> "Button"
            classNameLower.contains("textview") -> "TextView"
            classNameLower.contains("edittext") -> "EditText"
            else -> "View"
        }
    }
    
    private fun parseBounds(boundsStr: String?): Rect {
        if (boundsStr.isNullOrEmpty()) return Rect(0, 0, 0, 0)
        
        return try {
            val cleanStr = boundsStr.replace("[", "").replace("]", "")
            val coords = cleanStr.split(",")
            
            val left = coords[0].trim().toInt()
            val top = coords[1].trim().toInt()
            val right = coords[2].trim().toInt()
            val bottom = coords[3].trim().toInt()
            
            Rect(left, top, right, bottom)
        } catch (e: Exception) {
            Rect(0, 0, 0, 0)
        }
    }
    
    private fun getChildIndex(nodes: List<NodeInfo>, className: String, depth: Int): Int {
        var count = 1
        for (node in nodes) {
            if (node.depth == depth && className == node.className) {
                count++
            }
        }
        return count
    }
    
    // 内部节点信息类
    private class NodeInfo {
        var className: String? = null
        var resourceId: String? = null
        var text: String? = null
        var xpath: String? = null
        var bounds: Rect? = null
        var clickable: Boolean = false
        var visible: Boolean = false
        var index: Int = 0
        var depth: Int = 0
        var enabled: Boolean = false
        var focused: Boolean = false
        var longClickable: Boolean = false
        var password: Boolean = false
        var scrollable: Boolean = false
        var selected: Boolean = false
    }
}