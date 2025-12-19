package com.autodroid.workscripts.model

import java.io.Serializable

/**
 * 导航项目基类 - Represents a navigation item in the app
 */
sealed class NavigationItem : Serializable {
    
    /**
     * 应用程序项 - Represents an application in the navigation
     */
    data class AppItem(
        val name: String,
        val packageName: String,
        val iconResourceName: String? = null,
        var flows: List<FlowItem>? = null,
        var isExpanded: Boolean = false
    ) : NavigationItem(), Serializable {
        fun hasFlows(): Boolean = flows?.isNotEmpty() == true
    }
    
    /**
     * 流程项 - Represents a flow (workflow) in the navigation
     */
    data class FlowItem(
        val name: String,
        val description: String = "",
        var pages: List<StepItem>? = null,
        var isExpanded: Boolean = false
    ) : NavigationItem(), Serializable {
        fun hasPages(): Boolean = pages?.isNotEmpty() == true
    }
    
    /**
     * 步骤项 - Represents a step in the navigation
     */
    data class StepItem(
        val name: String,
        val layoutResourceName: String,
        val fullPath: String,
        val description: String = "",
        val step: Int = 0,
        val screenshots: List<String> = emptyList(),
        val actions: List<Action> = emptyList()
    ) : NavigationItem(), Serializable
    
    /**
     * 动作项 - Represents an action that can be performed on a page
     */
    data class Action(
        val click: String = "",
        val description: String = ""
    ) : Serializable
}