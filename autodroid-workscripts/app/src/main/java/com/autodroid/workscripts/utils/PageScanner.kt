package com.autodroid.workscripts.utils

import android.content.Context
import com.autodroid.workscripts.model.NavigationItem

/**
 * 页面扫描器 - 负责扫描页面级别的XML文件
 */
object PageScanner {
    
    /**
     * 扫描指定路径下的所有XML页面文件
     */
    fun scanPages(context: Context, folderPath: String): List<NavigationItem.PageItem> {
        val pages = mutableListOf<NavigationItem.PageItem>()
        
        try {
            println("Scanning pages in folder: $folderPath")
            
            // 获取文件夹下的所有文件
            val fileNames = context.assets.list(folderPath) ?: emptyArray()
            
            println("Found files: ${fileNames.joinToString(", ")}")
            
            // 过滤XML文件并创建PageItem
            fileNames.filter { it.endsWith(".xml") }.forEach { fileName ->
                try {
                    val pageItem = createPageItem(fileName, folderPath)
                    pages.add(pageItem)
                    println("Added page: ${pageItem.name}")
                } catch (e: Exception) {
                    println("Error processing file $fileName: ${e.message}")
                }
            }
            
            // 按文件名排序
            pages.sortBy { it.name }
            
        } catch (e: Exception) {
            println("Error scanning pages in $folderPath: ${e.message}")
        }
        
        return pages
    }
    
    /**
     * 根据XML文件名创建PageItem
     */
    private fun createPageItem(fileName: String, folderPath: String): NavigationItem.PageItem {
        // 生成页面名称（美化文件名）
        val pageName = fileName.removeSuffix(".xml")
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
        
        // 生成Android资源名（替换 - 为 _）
        val resourceName = fileName.removeSuffix(".xml").replace("-", "_")
        
        // 生成完整路径
        val fullPath = "$folderPath/${fileName.removeSuffix(".xml")}"
        
        return NavigationItem.PageItem(
            name = pageName,
            layoutResourceName = resourceName,
            fullPath = fullPath
        )
    }
}