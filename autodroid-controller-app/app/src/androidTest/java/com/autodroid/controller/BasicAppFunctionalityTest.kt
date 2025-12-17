package com.autodroid.controller

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.autodroid.controller.service.AutoDroidControllerService
import com.autodroid.controller.webdriver.WebDriverClient
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 应用基本功能测试
 * 验证应用是否正常安装和运行
 */
@RunWith(AndroidJUnit4::class)
class BasicAppFunctionalityTest {

    /**
     * 测试应用基本功能
     * 验证应用是否正常安装和运行
     */
    @Test
    fun testBasicAppFunctionality() {
        try {
            println("=== 应用基本功能测试 ===")
            
            // 验证应用包名
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val packageName = context.packageName
            
            assert(packageName == "com.autodroid.controller") {
                "应用包名不匹配: $packageName"
            }
            
            println("✅ 应用包名验证通过: $packageName")
            
            // 验证应用版本
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = packageInfo.versionCode
            
            println("✅ 应用版本信息:")
            println("   版本名称: $versionName")
            println("   版本代码: $versionCode")
            
            // 验证WebDriverClient初始化
            val webDriverClient = WebDriverClient()
            assert(webDriverClient != null) {
                "WebDriverClient初始化失败"
            }
            
            println("✅ WebDriverClient初始化成功")
            
            // 验证AutoDroidControllerService初始化
            val controllerService = AutoDroidControllerService()
            assert(controllerService != null) {
                "AutoDroidControllerService初始化失败"
            }
            
            println("✅ AutoDroidControllerService初始化成功")
            
            println("✅ 应用基本功能测试全部通过")
            
        } catch (e: Exception) {
            println("❌ 应用基本功能测试失败: ${e.message}")
            throw e
        }
    }
}