package com.autodroid.guardiansdk

import android.app.Application
import com.autodroid.guardiansdk.data.model.GuardianInfo

/**
 * Application class for Guardian SDK
 * Used for initializing any global components
 */
class GuardianApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize mock data for testing
        initializeMockData()
    }

    private fun initializeMockData() {
        // Pre-populate with some mock guardian data for testing
        MockDataManager.guardians = listOf(
            GuardianInfo(
                id = 1,
                name = "爸爸",
                avatar = "",
                lastContactTime = "2分钟前",
                lastLocation = "北京市朝阳区xx街道",
                lastAlarmMessage = "收到您的位置信息"
            ),
            GuardianInfo(
                id = 2,
                name = "妈妈",
                avatar = "",
                lastContactTime = "1小时前",
                lastLocation = "上海市浦东新区xx路",
                lastAlarmMessage = "安全到达目的地"
            ),
            GuardianInfo(
                id = 3,
                name = "哥哥",
                avatar = "",
                lastContactTime = "昨天",
                lastLocation = "广州市天河区xx大厦",
                lastAlarmMessage = "正在开会，暂时无法接听"
            ),
            GuardianInfo(
                id = 4,
                name = "姐姐",
                avatar = "",
                lastContactTime = "3天前",
                lastLocation = "深圳市南山区xx科技园",
                lastAlarmMessage = "一切安好"
            ),
            GuardianInfo(
                id = 5,
                name = "爷爷",
                avatar = "",
                lastContactTime = "上周",
                lastLocation = "成都市锦江区xx小区",
                lastAlarmMessage = "身体健康，不用担心"
            ),
            GuardianInfo(
                id = 6,
                name = "奶奶",
                avatar = "",
                lastContactTime = "2周前",
                lastLocation = "西安市雁塔区xx社区",
                lastAlarmMessage = "家中平安"
            )
        )
    }
}

/**
 * Simple data manager for mock data during testing
 */
object MockDataManager {
    var guardians: List<GuardianInfo> = emptyList()
}