package com.autodroid.guardiansdk.data.repository

object SettingKeys {
    // 统一设置项存储
    const val SETTING_ITEMS = "setting_items"
    
    // 必要的独立设置项
    const val GUARDIAN_CONTACTS = "guardian_contacts"
    const val PASSWORD_BOOK = "password_book"
    const val APP_NAME = "app_name"
    const val APP_VERSION = "app_version"
    
    // 测试模式相关
    const val TEST_MODE_ENABLED = "test_mode_enabled"
    const val TEST_MODE_PRACTICE_COUNT = "test_mode_practice_count"
    const val TEST_MODE_LAST_PRACTICE = "test_mode_last_practice"
}
