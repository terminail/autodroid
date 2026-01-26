package com.autodroid.teachitback.ui.adapter

import org.junit.Assert.*
import org.junit.Test

class TencentCloudSettingsItemTest {

    @Test
    fun `TencentCloudApiKeyItem should be defined`() {
        val item = SettingsItem.TencentCloudAIServiceItem(
            apiKey = "test-api-key",
            secretId = "test-secret-id",
            testMode = true,
            enabled = true,
            region = "ap-guangzhou",
            onApiKeyChanged = {},
            onSecretIdChanged = {},
            onTestModeChanged = {},
            onEnabledChanged = {},
            onRegionChanged = {}
        )

        assertNotNull("TencentCloudAIServiceItem should be created", item)
        assertEquals("test-api-key", item.apiKey)
        assertEquals("test-secret-id", item.secretId)
        assertTrue(item.testMode)
        assertTrue(item.enabled)
        assertEquals("ap-guangzhou", item.region)
    }

    @Test
    fun `TencentCloudApiKeyItem should have correct type`() {
        val item = SettingsItem.TencentCloudAIServiceItem(
            apiKey = "",
            secretId = "",
            testMode = false,
            enabled = false,
            region = "",
            onApiKeyChanged = {},
            onSecretIdChanged = {},
            onTestModeChanged = {},
            onEnabledChanged = {},
            onRegionChanged = {}
        )

        assertEquals(SettingsItem.TYPE_TENCENTCLOUD_AI_SERVICE_ITEM, item.getType())
    }
}

