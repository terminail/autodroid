package com.autodroid.trader.config

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.IOException

/**
 * 应用配置管理类
 * 用于读取和解析YAML配置文件
 */
data class AppConfig(
    val server: ServerConfig = ServerConfig(),
    val network: NetworkConfig = NetworkConfig(),
    val device: DeviceConfig = DeviceConfig(),
    val driverDevelopment: DriverDevelopmentConfig = DriverDevelopmentConfig(),
    val ui: UIConfig = UIConfig(),
    val security: SecurityConfig = SecurityConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val development: DevelopmentConfig = DevelopmentConfig()
)

data class ServerConfig(
    val defaultApiEndpoint: String = "http://192.168.1.59:8004/api",
    val connectionTimeout: Int = 10,
    val maxRetries: Int = 3,
    val healthCheckInterval: Int = 30
)

data class NetworkConfig(
    val wifi: WiFiConfig = WiFiConfig()
)

data class WiFiConfig(
    val scanInterval: Int = 60,
    val autoConnect: Boolean = true
)

data class DeviceConfig(
    val info: DeviceInfoConfig = DeviceInfoConfig(),
    val udid: UDIDConfig = UDIDConfig()
)

data class DeviceInfoConfig(
    val autoRefresh: Boolean = true,
    val refreshInterval: Int = 60
)

data class UDIDConfig(
    val generationMethod: String = "android_id",
    val cacheEnabled: Boolean = true
)

data class DriverDevelopmentConfig(
    val debugMode: Boolean = false,
    val logLevel: String = "INFO",
    val performanceMonitoring: PerformanceMonitoringConfig = PerformanceMonitoringConfig(),
    val testing: TestingConfig = TestingConfig()
)

data class PerformanceMonitoringConfig(
    val enabled: Boolean = true,
    val metricsInterval: Int = 60
)

data class TestingConfig(
    val mockServerEnabled: Boolean = false,
    val testTimeout: Int = 30
)

data class UIConfig(
    val theme: ThemeConfig = ThemeConfig(),
    val navigation: NavigationConfig = NavigationConfig(),
    val refresh: RefreshConfig = RefreshConfig()
)

data class ThemeConfig(
    val primaryColor: String = "#2196F3",
    val accentColor: String = "#FF4081",
    val darkMode: Boolean = false
)

data class NavigationConfig(
    val animationEnabled: Boolean = true,
    val bottomNavigationEnabled: Boolean = true
)

data class RefreshConfig(
    val pullToRefresh: Boolean = true,
    val autoRefreshInterval: Int = 300
)

data class SecurityConfig(
    val authentication: AuthenticationConfig = AuthenticationConfig(),
    val encryption: EncryptionConfig = EncryptionConfig()
)

data class AuthenticationConfig(
    val tokenExpiry: Int = 86400,
    val autoLogin: Boolean = true
)

data class EncryptionConfig(
    val enabled: Boolean = true,
    val algorithm: String = "AES"
)

data class DatabaseConfig(
    val name: String = "autodroid_database",
    val room: RoomConfig = RoomConfig(),
    val cache: CacheConfig = CacheConfig()
)

data class RoomConfig(
    val schemaLocation: String = "schemas",
    val exportSchema: Boolean = true
)

data class CacheConfig(
    val enabled: Boolean = true,
    val maxSizeMb: Int = 50
)

data class DevelopmentConfig(
    val tools: DevelopmentToolsConfig = DevelopmentToolsConfig(),
    val hotReload: HotReloadConfig = HotReloadConfig()
)

data class DevelopmentToolsConfig(
    val adbEnabled: Boolean = true,
    val logcatEnabled: Boolean = true
)

data class HotReloadConfig(
    val enabled: Boolean = false,
    val port: Int = 8080
)

/**
 * 配置管理器单例类
 */
object ConfigManager {
    private var config: AppConfig? = null
    
    /**
     * 加载配置文件
     */
    fun loadConfig(context: Context): AppConfig {
        if (config != null) {
            return config!!
        }
        
        try {
            val inputStream = context.assets.open("config.yaml")
            val mapper = ObjectMapper(YAMLFactory())
            config = mapper.readValue(inputStream, AppConfig::class.java)
            inputStream.close()
            return config!!
        } catch (e: IOException) {
            // 如果配置文件不存在，返回默认配置
            println("Config file not found, using default configuration: ${e.message}")
            config = AppConfig()
            return config!!
        }
    }
    
    /**
     * 获取当前配置
     */
    fun getConfig(context: Context? = null): AppConfig {
        if (config == null && context != null) {
            // If config is not loaded and context is provided, load it now
            return loadConfig(context)
        } else if (config == null) {
            // If config is not loaded and no context is provided, throw exception
            throw IllegalStateException("Config not loaded. Call loadConfig() first or provide a context.")
        }
        return config!!
    }
    
    /**
     * 重新加载配置
     */
    fun reloadConfig(context: Context): AppConfig {
        config = null
        return loadConfig(context)
    }
}