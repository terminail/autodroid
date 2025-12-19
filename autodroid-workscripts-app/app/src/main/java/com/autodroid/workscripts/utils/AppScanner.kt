package com.autodroid.workscripts.utils

import android.content.Context
import com.autodroid.workscripts.model.NavigationItem
import org.yaml.snakeyaml.Yaml

/**
 * 应用扫描器 - 负责扫描应用级别的配置和数据
 */
object AppScanner {
    
    /**
     * 扫描所有应用并返回AppItem列表
     */
    fun scanApps(context: Context): List<NavigationItem.AppItem> {
        val appItems = mutableListOf<NavigationItem.AppItem>()
        
        try {
            // 获取assets/pages目录下的所有应用文件夹
            val assetManager = context.assets
            val appFolders = assetManager.list("pages") ?: emptyArray()
            
            println("Found app folders: ${appFolders.joinToString(", ")}")
            
            // 扫描每个应用文件夹
            appFolders.forEach { appFolder ->
                try {
                    val appItem = scanApp(context, appFolder)
                    if (appItem.flows?.isNotEmpty() == true) {
                        appItems.add(appItem)
                    }
                } catch (e: Exception) {
                    println("Error scanning app $appFolder: ${e.message}")
                }
            }
            
            // Sort by name as we don't have displayOrder in AppItem anymore
            appItems.sortBy { it.name }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return appItems
    }
    
    /**
     * 扫描单个应用文件夹
     */
    private fun scanApp(context: Context, appFolder: String): NavigationItem.AppItem {
        // 读取应用配置文件
        val configContent = context.assets.open("pages/$appFolder/config.yaml").bufferedReader().use { it.readText() }
        val yaml = Yaml()
        val configMap = yaml.load<Map<String, Any>>(configContent)
        
        println("Scanning app: $appFolder")
        println("Config map: $configMap")
        
        // 自动扫描流程文件夹，而不是从配置中读取
        val flows = scanFlowsFromFolders(context, appFolder)
        
        // 创建AppItem
        return NavigationItem.AppItem(
            name = configMap["name"] as? String ?: appFolder,
            packageName = configMap["package"] as? String ?: "",
            flows = flows
        )
    }
    
    /**
     * 自动扫描应用文件夹下的流程文件夹
     */
    private fun scanFlowsFromFolders(context: Context, appFolder: String): List<NavigationItem.FlowItem> {
        val flows = mutableListOf<NavigationItem.FlowItem>()
        
        try {
            // 获取应用文件夹下的所有子文件夹（流程文件夹）
            val assetManager = context.assets
            val flowFolders = assetManager.list("pages/$appFolder") ?: emptyArray()
            
            println("Found ${flowFolders.size} potential flow folders in $appFolder")
            
            flowFolders.forEach { flowFolder ->
                try {
                    // 跳过非文件夹项（如config.yaml文件）
                    if (flowFolder.endsWith(".yaml") || flowFolder.endsWith(".yml")) {
                        return@forEach
                    }
                    
                    println("Scanning potential flow folder: $flowFolder")
                    
                    // 使用FlowScanner扫描流程配置和页面
                    val flowPath = "$appFolder/$flowFolder"
                    println("Scanning flow with FlowScanner: $flowPath")
                    val flowItem = FlowScanner.scanFlow(context, flowPath)
                    
                    if (flowItem != null) {
                        flows.add(flowItem)
                        println("Added flow: ${flowItem.name} with ${flowItem.pages?.size ?: 0} pages")
                    } else {
                        println("Failed to scan flow: $flowPath")
                    }
                    
                } catch (e: Exception) {
                    println("Error scanning flow folder $flowFolder: ${e.message}")
                }
            }
            
            // Sort by name as we don't have displayOrder in FlowItem anymore
            flows.sortBy { it.name }
            
        } catch (e: Exception) {
            println("Error scanning flow folders for $appFolder: ${e.message}")
        }
        
        return flows
    }
}