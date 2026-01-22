package com.autodroid.teachitback.model

import org.junit.Assert.*
import org.junit.Test

class TencentCloudSettingsTest {

    @Test
    fun `TencentCloudSettings should have all required constants`() {
        // 验证所有配置常量都存在
        assertNotNull("API_KEY constant should exist", TencentCloudSettings.API_KEY)
        assertNotNull("SECRET_ID constant should exist", TencentCloudSettings.SECRET_ID)
        assertNotNull("ENABLED constant should exist", TencentCloudSettings.ENABLED)
        assertNotNull("TEST_MODE constant should exist", TencentCloudSettings.TEST_MODE)
        assertNotNull("REGION constant should exist", TencentCloudSettings.REGION)
        assertNotNull("DEFAULT_REGION constant should exist", TencentCloudSettings.DEFAULT_REGION)
    }

    @Test
    fun `TencentCloudSettings constants should have correct values`() {
        assertEquals("tencentcloud_api_key", TencentCloudSettings.API_KEY)
        assertEquals("tencentcloud_secret_id", TencentCloudSettings.SECRET_ID)
        assertEquals("tencentcloud_enabled", TencentCloudSettings.ENABLED)
        assertEquals("tencentcloud_test_mode", TencentCloudSettings.TEST_MODE)
        assertEquals("tencentcloud_region", TencentCloudSettings.REGION)
        assertEquals("ap-guangzhou", TencentCloudSettings.DEFAULT_REGION)
    }
}
