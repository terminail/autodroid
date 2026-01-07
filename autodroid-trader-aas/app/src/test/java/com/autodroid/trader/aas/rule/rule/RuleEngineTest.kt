package com.autodroid.trader.aas.rule

import org.junit.Test
import org.junit.Assert.*
import com.autodroid.trader.aas.database.UIEvent

class RuleEngineTest {
    
    private val ruleEngine = RuleEngine()
    
    @Test
    fun testContainsSensitiveInfo() {
        // Test cases for sensitive information detection
        assertTrue(ruleEngine.containsSensitiveInfo("password"))
        assertTrue(ruleEngine.containsSensitiveInfo("Password"))
        assertTrue(ruleEngine.containsSensitiveInfo("user@email.com"))
        assertTrue(ruleEngine.containsSensitiveInfo("credit card"))
        assertTrue(ruleEngine.containsSensitiveInfo("身份证"))
        assertTrue(ruleEngine.containsSensitiveInfo("手机号"))
        assertTrue(ruleEngine.containsSensitiveInfo("social security"))
        
        // Test cases for non-sensitive information
        assertFalse(ruleEngine.containsSensitiveInfo("username"))
        assertFalse(ruleEngine.containsSensitiveInfo("button"))
        assertFalse(ruleEngine.containsSensitiveInfo("submit"))
        assertFalse(ruleEngine.containsSensitiveInfo("home"))
    }
    
    @Test
    fun testRedactSensitiveInfo() {
        // Test email redaction
        val emailText = "Please contact admin@example.com for support"
        val redactedEmail = ruleEngine.redactSensitiveInfo(emailText)
        assertNotNull(redactedEmail)
        assertTrue(redactedEmail!!.contains("***@***.com"))
        
        // Test phone number redaction
        val phoneText = "Call me at 123-456-7890"
        val redactedPhone = ruleEngine.redactSensitiveInfo(phoneText)
        assertNotNull(redactedPhone)
        assertTrue(redactedPhone!!.contains("***"))
        
        // Test credit card redaction
        val ccText = "Card number is 4111-1111-1111-1111"
        val redactedCC = ruleEngine.redactSensitiveInfo(ccText)
        assertNotNull(redactedCC)
        assertTrue(redactedCC!!.contains("***"))
        
        // Test normal text (should remain unchanged)
        val normalText = "This is a normal text"
        val redactedNormal = ruleEngine.redactSensitiveInfo(normalText)
        assertEquals(normalText, redactedNormal)
    }
    
    @Test
    fun testShouldRecordEvent() {
        // Create a mock UIEvent with sensitive content
        val sensitiveEvent = com.autodroid.aas.database.UIEvent(
            packageName = "com.example.app",
            activityName = "MainActivity",
            eventType = "INPUT",
            elementText = "password123",
            elementHint = "Enter your password",
            elementContentDesc = null,
            elementId = "password_field",
            elementType = "EditText",
            elementClass = "android.widget.EditText",
            elementBounds = "0,0,100,50",
            inputValue = "mySecretPassword",
            selectedValue = null,
            parentHierarchy = null,
            siblingInfo = null,
            extraData = null,
            screenshotPath = null
        )
        
        // Create a mock UIEvent without sensitive content
        val normalEvent = com.autodroid.aas.database.UIEvent(
            packageName = "com.example.app",
            activityName = "MainActivity",
            eventType = "CLICK",
            elementText = "Submit Button",
            elementHint = "Click to submit",
            elementContentDesc = null,
            elementId = "submit_button",
            elementType = "Button",
            elementClass = "android.widget.Button",
            elementBounds = "0,0,100,50",
            inputValue = null,
            selectedValue = null,
            parentHierarchy = null,
            siblingInfo = null,
            extraData = null,
            screenshotPath = null
        )
        
        // Create rules for testing
        val rules = listOf(
            RecordingRule(
                condition = RuleCondition.CONTAINS_SENSITIVE_TEXT
            ),
            RecordingRule(
                condition = RuleCondition.PACKAGE_INCLUDE,
                packages = listOf("com.example.app")
            )
        )
        
        // The sensitive event should not be recorded
        assertFalse(ruleEngine.shouldRecordEvent(sensitiveEvent, rules))
        
        // The normal event should be recorded
        assertTrue(ruleEngine.shouldRecordEvent(normalEvent, rules))
    }
}