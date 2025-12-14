package com.autodroid.workscripts.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import java.io.File

object LayoutHelper {
    
    /**
     * 从子文件夹中加载布局文件
     */
    fun inflateLayoutFromSubfolder(context: Context, layoutResourceName: String): View? {
        return try {
            // 尝试直接加载布局资源
            val resourceId = context.resources.getIdentifier(
                layoutResourceName.replace(".xml", ""),
                "layout",
                context.packageName
            )
            
            if (resourceId != 0) {
                LayoutInflater.from(context).inflate(resourceId, null)
            } else {
                // 如果资源ID为0，尝试从文件系统加载
                loadLayoutFromFile(context, layoutResourceName)
            }
        } catch (e: Exception) {
            // Handle exception silently
            null
        }
    }
    
    /**
     * 从文件系统加载布局文件（备用方案）
     */
    private fun loadLayoutFromFile(context: Context, layoutResourceName: String): View? {
        return try {
            // 扫描 pages 目录
            val pagesDir = File("d:\\git\\autodroid\\autodroid-workscripts\\pages")
            
            if (!pagesDir.exists()) {
                return null
            }
            
            // 递归查找布局文件
            val layoutFile = findLayoutFile(pagesDir, "$layoutResourceName.xml")
            
            if (layoutFile != null && layoutFile.exists()) {
                // 这里简化处理，实际项目中可能需要更复杂的XML解析
                // 对于演示目的，我们返回一个占位视图
                createPlaceholderView(context, layoutResourceName, layoutFile)
            } else {
                null
            }
        } catch (e: Exception) {
            // Log error silently
            null
        }
    }
    
    /**
     * 递归查找布局文件
     */
    private fun findLayoutFile(directory: File, fileName: String): File? {
        directory.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> {
                    val found = findLayoutFile(file, fileName)
                    if (found != null) return found
                }
                file.isFile && file.name.equals(fileName, ignoreCase = true) -> {
                    return file
                }
            }
        }
        return null
    }
    
    /**
     * 创建占位视图（用于演示）
     */
    private fun createPlaceholderView(context: Context, layoutName: String, layoutFile: File): View {
        return android.widget.TextView(context).apply {
            text = "布局文件: $layoutName\n文件路径: ${layoutFile.absolutePath}\n\n这是一个演示页面，实际布局内容可以在这里显示。"
            setPadding(32, 32, 32, 32)
            textSize = 16f
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
            setTextColor(android.graphics.Color.BLACK)
        }
    }
}