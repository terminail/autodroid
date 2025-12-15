package com.autodroid.workscripts.utils

import android.content.Context
import com.autodroid.workscripts.model.NavigationItem
import org.yaml.snakeyaml.Yaml

/**
 * 流程扫描器 - 负责扫描流程级别的配置和页面信息
 */
object FlowScanner {
    
    /**
     * 扫描指定流程文件夹的配置和页面信息
     */
    fun scanFlow(context: Context, flowPath: String): NavigationItem.FlowItem? {
        try {
            println("Scanning flow: $flowPath")
            
            // 读取流程配置文件
            val configContent = context.assets.open("pages/$flowPath/config.yaml").bufferedReader().use { it.readText() }
            val yaml = Yaml()
            val configMap = yaml.load<Map<String, Any>>(configContent)
            
            println("Flow config map: $configMap")
            
            // 解析流程名称和描述
            val flowName = configMap["name"] as? String ?: flowPath.substringAfterLast("/")
            val flowDescription = configMap["description"] as? String ?: ""
            
            // 解析步骤配置
            val pages = parseStepsFromConfig(configMap, flowPath)
            
            // 创建FlowItem
            return NavigationItem.FlowItem(
                name = flowName,
                description = flowDescription,
                pages = pages
            )
            
        } catch (e: Exception) {
            println("Error scanning flow $flowPath: ${e.message}")
            return null
        }
    }
    
    /**
     * 从配置中解析步骤信息
     */
    private fun parseStepsFromConfig(configMap: Map<String, Any>, flowPath: String): List<NavigationItem.StepItem> {
        val pages = mutableListOf<NavigationItem.StepItem>()
        
        val stepsConfig = configMap["steps"] as? List<Map<String, Any>>
        if (stepsConfig != null) {
            println("Found ${stepsConfig.size} steps in flow config")
            
            stepsConfig.forEach { stepMap ->
                try {
                    val stepName = stepMap["name"] as? String ?: ""
                    val stepFile = stepMap["layout"] as? String ?: ""  // Changed from "file" to "layout" to match YAML
                    val stepDescription = stepMap["description"] as? String ?: ""
                    val stepNumber = stepMap["step"] as? Int ?: 0
                    val screenshots = stepMap["screenshots"] as? List<String> ?: emptyList()
                    val actions = parseActions(stepMap["actions"])
                    
                    println("Processing step: $stepName -> $stepFile")
                    
                    // 创建StepItem
                    val stepItem = NavigationItem.StepItem(
                        name = stepName,
                        layoutResourceName = stepFile.removeSuffix(".xml").replace("-", "_"),
                        fullPath = "$flowPath/${stepFile.removeSuffix(".xml")}",
                        description = stepDescription,
                        step = stepNumber,
                        screenshots = screenshots,
                        actions = actions
                    )
                    
                    pages.add(stepItem)
                    println("Added step: ${stepItem.name}")
                    
                } catch (e: Exception) {
                    println("Error parsing step: ${e.message}")
                }
            }
        } else {
            println("No steps configuration found in flow config.yaml")
        }
        
        return pages
    }
    
    /**
     * 解析动作配置
     */
    private fun parseActions(actionsObj: Any?): List<NavigationItem.Action> {
        val actions = mutableListOf<NavigationItem.Action>()
        
        val actionsList = actionsObj as? List<Map<String, Any>>
        if (actionsList != null) {
            actionsList.forEach { actionMap ->
                try {
                    val click = actionMap["click"] as? String ?: ""
                    val description = actionMap["description"] as? String ?: ""
                    
                    val action = NavigationItem.Action(
                        click = click,
                        description = description
                    )
                    
                    actions.add(action)
                } catch (e: Exception) {
                    println("Error parsing action: ${e.message}")
                }
            }
        }
        
        return actions
    }
}