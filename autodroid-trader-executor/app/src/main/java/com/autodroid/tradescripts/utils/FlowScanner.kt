package com.autodroid.tradescripts.utils

import android.content.Context
import com.autodroid.tradescripts.model.NavigationItem
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
            
            val configContent = context.assets.open("apks/$flowPath/config.yaml").bufferedReader().use { it.readText() }
            val yaml = Yaml()
            @Suppress("UNCHECKED_CAST")
            val configMap = yaml.load<Map<String, Any>>(configContent)
            
            println("Flow config map: $configMap")
            
            val flowName = configMap["name"] as? String ?: flowPath.substringAfterLast("/")
            val flowDescription = configMap["description"] as? String ?: ""
            
            val steps = parseStepsFromConfig(configMap, flowPath)
            
            return NavigationItem.FlowItem(
                name = flowName,
                description = flowDescription,
                steps = steps
            )
            
        } catch (e: Exception) {
            println("Error scanning flow $flowPath: ${e.message}")
            return null
        }
    }
    
    /**
     * 从配置中解析步骤信息
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseStepsFromConfig(configMap: Map<String, Any>, flowPath: String): List<NavigationItem.StepItem> {
        val steps = mutableListOf<NavigationItem.StepItem>()
        
        val stepsConfig = configMap["steps"] as? List<*>
        if (stepsConfig != null) {
            println("Found ${stepsConfig.size} steps in flow config")
            
            stepsConfig.forEach { stepMap ->
                try {
                    val stepData = stepMap as? Map<String, Any> ?: return@forEach
                    val stepName = stepData["name"] as? String ?: ""
                    val stepFile = stepData["layout"] as? String ?: ""
                    val stepDescription = stepData["description"] as? String ?: ""
                    val stepNumber = stepData["step"] as? Int ?: 0
                    val screenshotsData = stepData["screenshots"] as? List<*>
                    val screenshots = screenshotsData?.filterIsInstance<String>() ?: emptyList()
                    val actions = parseActions(stepData["actions"])
                    
                    println("Processing step: $stepName -> $stepFile")
                    
                    val stepItem = NavigationItem.StepItem(
                        name = stepName,
                        layoutResourceName = stepFile.removeSuffix(".xml").replace("-", "_"),
                        fullPath = "$flowPath/${stepFile.removeSuffix(".xml")}",
                        description = stepDescription,
                        step = stepNumber,
                        screenshots = screenshots,
                        actions = actions
                    )
                    
                    steps.add(stepItem)
                    println("Added step: ${stepItem.name}")
                    
                } catch (e: Exception) {
                    println("Error parsing step: ${e.message}")
                }
            }
        } else {
            println("No steps configuration found in flow config.yaml")
        }
        
        return steps
    }
    
    /**
     * 解析动作配置
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseActions(actionsObj: Any?): List<NavigationItem.Action> {
        val actions = mutableListOf<NavigationItem.Action>()
        
        val actionsList = actionsObj as? List<*>
        actionsList?.forEach { actionMap ->
            try {
                val actionData = actionMap as? Map<String, Any> ?: return@forEach
                val click = actionData["click"] as? String ?: ""
                val description = actionData["description"] as? String ?: ""

                val action = NavigationItem.Action(
                    click = click,
                    description = description
                )

                actions.add(action)
            } catch (e: Exception) {
                println("Error parsing action: ${e.message}")
            }
        }
        
        return actions
    }
}