package com.autodroid.workscripts.model

import java.io.Serializable

/**
 * recyclerview数据模型 - hybrid结构
 */
sealed class NavigationItem {
    data class AppItem(
        val name: String,
        val description: String,
        val category: String,
        val version: String,
        val author: String,
        val displayOrder: Int,
        var isExpanded: Boolean = false,
        var flows: List<FlowItem>? = null
    ) : NavigationItem() {
        fun hasFlows(): Boolean = flows?.isNotEmpty() == true
    }
    
    data class FlowItem(
        val folder: String,
        val name: String,
        val description: String,
        val displayOrder: Int,
        var isExpanded: Boolean = false,
        var pages: List<PageItem>? = null
    ): NavigationItem(){
        fun hasPages(): Boolean = pages?.isNotEmpty() == true
    }
    
    data class PageItem(
        val name: String,
        val layoutResourceName: String,
        val fullPath: String,
        val description: String = ""
    ) : NavigationItem(), Serializable
}