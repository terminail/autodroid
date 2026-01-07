package com.autodroid.trader.aas.rule

import com.autodroid.trader.aas.database.UIEvent

/**
 * Rule engine for handling sensitive content detection and filtering
 */
class RuleEngine {
    
    /**
     * Determines if an event should be recorded based on various rules
     */
    fun shouldRecordEvent(
        event: UIEvent,
        rules: List<RecordingRule>
    ): Boolean {
        return rules.all { rule ->
            when (rule.condition) {
                RuleCondition.PACKAGE_INCLUDE -> 
                    rule.packages.contains(event.packageName)
                RuleCondition.PACKAGE_EXCLUDE -> 
                    !rule.packages.contains(event.packageName)
                RuleCondition.EVENT_TYPE_INCLUDE -> 
                    rule.eventTypes.contains(event.eventType)
                RuleCondition.CONTAINS_SENSITIVE_TEXT -> 
                    !containsSensitiveInfo(event.elementText)
                RuleCondition.ELEMENT_TYPE_FILTER -> 
                    rule.elementTypes.contains(event.elementType)
                else -> true
            }
        }
    }
    
    /**
     * Checks if text contains sensitive information
     */
    fun containsSensitiveInfo(text: String?): Boolean {
        text ?: return false
        
        val sensitivePatterns = listOf(
            "password", "passwd", "pwd", "credit", "card",
            "ssn", "social", "security", "身份证", "密码",
            "手机号", "电话", "email", "邮箱", "phone",
            "account", "pin", "cvv", "cvc", "cv2",
            "iban", "swift", "routing", "account", "debit"
        )
        
        return sensitivePatterns.any { pattern ->
            text.contains(pattern, ignoreCase = true)
        }
    }
    
    /**
     * Redacts sensitive information from text
     */
    fun redactSensitiveInfo(text: String?): String? {
        if (text.isNullOrEmpty()) return text
        
        var redactedText = text
        
        // Redact email addresses
        redactedText = redactPattern(redactedText, Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"))
        
        // Redact phone numbers (various formats)
        redactedText = redactPattern(redactedText, Regex("\\b(\\+?\\d{1,3}[-.]?)?\\(?\\d{3}\\)?[-.]?\\d{3}[-.]?\\d{4}\\b"))
        
        // Redact credit card numbers (with spaces or dashes)
        redactedText = redactPattern(redactedText, Regex("\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b"))
        
        // Redact SSN pattern (XXX-XX-XXXX)
        redactedText = redactPattern(redactedText, Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b"))
        
        return redactedText
    }
    
    private fun redactPattern(text: String, pattern: Regex): String {
        return pattern.replace(text) { match ->
            val matchedText = match.value
            if (matchedText.length > 4) {
                "${matchedText.substring(0, 2)}${"*".repeat(matchedText.length - 4)}${matchedText.substring(matchedText.length - 2)}"
            } else {
                "*".repeat(matchedText.length)
            }
        }
    }
}

/**
 * Enum for different rule conditions
 */
enum class RuleCondition {
    PACKAGE_INCLUDE,
    PACKAGE_EXCLUDE,
    EVENT_TYPE_INCLUDE,
    CONTAINS_SENSITIVE_TEXT,
    ELEMENT_TYPE_FILTER
}

/**
 * Data class for recording rules
 */
data class RecordingRule(
    val condition: RuleCondition,
    val packages: List<String> = emptyList(),
    val eventTypes: List<String> = emptyList(),
    val elementTypes: List<String> = emptyList()
)