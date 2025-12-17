package com.autodroid.controller.model

enum class ActionType {
    INIT_SESSION,
    FIND_ELEMENT,
    CLICK,
    SEND_KEYS,
    SWIPE,
    TAKE_SCREENSHOT,
    CLOSE_SESSION,
    GET_PAGE_SOURCE
}

data class AutomationTask(
    val taskId: String,
    val deviceId: String,
    val actions: List<Action>
)

data class Action(
    val action: ActionType,
    val params: Map<String, Any> = emptyMap()
)

object SessionManager {
    var currentSessionId: String? = null
    var currentElementId: String? = null
    val context = mutableMapOf<String, Any>()
    
    fun clear() {
        currentSessionId = null
        currentElementId = null
        context.clear()
    }
    
    fun resolveVariable(value: String): String? {
        if (value.startsWith("\${") && value.endsWith("}")) {
            val key = value.removePrefix("\${").removeSuffix("}")
            return context[key]?.toString()
        }
        return value
    }
}