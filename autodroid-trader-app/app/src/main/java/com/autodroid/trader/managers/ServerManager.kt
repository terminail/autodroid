package com.autodroid.trader.managers

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.autodroid.trader.data.repository.ServerRepository
import com.autodroid.trader.data.dao.ServerEntity
import com.autodroid.trader.network.ApiClient
import com.autodroid.trader.network.ServerInfoResponse
import com.autodroid.trader.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 扫描状态枚举
 */
enum class ScanState {
    IDLE,       // 空闲状态
    SCANNING,   // 扫描中
    PAUSED,     // 已暂停
    COMPLETED   // 扫描完成
}

/**
 * 服务器管理器
 * 负责自动扫描局域网内的服务器，并将扫描结果更新到数据库
 */
/**
 * 服务器扫描结果，包含服务器信息和IP地址、端口
 */
data class ServerScanResult(
    val serverEntity: ServerEntity,
    val ip: String,
    val port: Int
)

class ServerManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ServerManager"

        // 默认扫描端口范围
        private const val DEFAULT_PORT_START = 8000
        private const val DEFAULT_PORT_END = 8080

        // 扫描超时时间（毫秒）
        private const val SCAN_TIMEOUT_MS = 3000

        @Volatile
        private var INSTANCE: ServerManager? = null

        fun getInstance(context: Context): ServerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ServerManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // 服务器仓库
    private val serverRepository =
        ServerRepository.getInstance(context.applicationContext as android.app.Application)

    // API客户端
    private val apiClient = ApiClient.getInstance()

    // 扫描状态
    private val _scanStatus = MutableLiveData<String>()
    val scanStatus: MutableLiveData<String> = _scanStatus

    // 扫描状态枚举
    private val _scanState = MutableLiveData<ScanState>()
    val scanState: MutableLiveData<ScanState> = _scanState

    // 扫描进度
    private val _scanProgress = MutableLiveData<String>()
    val scanProgress: MutableLiveData<String> = _scanProgress

    // 发现的服务器
    private val _discoveredServer = MutableLiveData<ServerScanResult?>()
    val discoveredServer: MutableLiveData<ServerScanResult?> = _discoveredServer

    // 扫描任务
    private var scanJob: Job? = null

    // 是否正在扫描
    private var isScanning = false

    // 是否暂停扫描
    private var isPaused = false

    init {
        // 初始化扫描状态为空闲
        _scanState.postValue(ScanState.IDLE)
    }

    /**
     * 获取扫描端口范围
     */
    private fun getPortRange(): Pair<Int, Int> {
        val prefs = context.getSharedPreferences("server_scan_settings", 0)
        val portStart = prefs.getInt("port_start", DEFAULT_PORT_START)
        val portEnd = prefs.getInt("port_end", DEFAULT_PORT_END)
        return Pair(portStart, portEnd)
    }

    /**
     * 获取扫描IP范围
     */
    private fun getIpRange(): Pair<String, String> {
        // 获取当前WiFi的IP前缀
        val localIp = getLocalIpAddress()
        if (localIp == null) {
            Log.e(TAG, "无法获取本地IP地址，使用默认IP前缀")
            return Pair("192.168.1.1", "192.168.1.255")
        }
        
        // 提取IP前缀（前三个部分）
        val ipParts = localIp.split(".")
        if (ipParts.size != 4) {
            Log.e(TAG, "本地IP地址格式不正确: $localIp，使用默认IP前缀")
            return Pair("192.168.1.1", "192.168.1.255")
        }
        
        val ipPrefix = "${ipParts[0]}.${ipParts[1]}.${ipParts[2]}"
        
        // 从SharedPreferences获取IP范围末尾值
        val prefs = context.getSharedPreferences("server_scan_settings", 0)
        val ipStartLast = prefs.getInt("ip_start_last", 20)
        val ipEndLast = prefs.getInt("ip_end_last", 159)
        
        // 验证IP范围末尾值
        val startLast = if (ipStartLast in 0..255) ipStartLast else 20
        val endLast = if (ipEndLast in 0..255) ipEndLast else 159
        
        // 确保起始值不大于结束值
        val actualStart = minOf(startLast, endLast)
        val actualEnd = maxOf(startLast, endLast)
        
        return Pair("$ipPrefix.$actualStart", "$ipPrefix.$actualEnd")
    }

    /**
     * 生成IP范围内的所有IP地址
     */
    private fun generateIpRange(ipStart: String, ipEnd: String): List<String> {
        val result = mutableListOf<String>()
        
        try {
            val startParts = ipStart.split(".").map { it.toInt() }
            val endParts = ipEnd.split(".").map { it.toInt() }
            
            // 验证IP地址格式
            if (startParts.size != 4 || endParts.size != 4) {
                Log.e(TAG, "IP地址格式不正确: $ipStart, $ipEnd")
                return emptyList()
            }
            
            // 生成IP范围
            for (i in startParts[0]..endParts[0]) {
                val jStart = if (i == startParts[0]) startParts[1] else 0
                val jEnd = if (i == endParts[0]) endParts[1] else 255
                for (j in jStart..jEnd) {
                    val kStart = if (i == startParts[0] && j == startParts[1]) startParts[2] else 0
                    val kEnd = if (i == endParts[0] && j == endParts[1]) endParts[2] else 255
                    for (k in kStart..kEnd) {
                        val lStart = if (i == startParts[0] && j == startParts[1] && k == startParts[2]) startParts[3] else 0
                        val lEnd = if (i == endParts[0] && j == endParts[1] && k == endParts[2]) endParts[3] else 255
                        for (l in lStart..lEnd) {
                            val ip = "$i.$j.$k.$l"
                            if (isValidIpAddress(ip)) {
                                result.add(ip)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "生成IP范围时发生错误: ${e.message}")
        }
        
        return result
    }

    /**
     * 开始或恢复扫描局域网内的服务器
     */
    fun startServerScan() {
        if (isScanning && !isPaused) {
            Log.d(TAG, "服务器扫描已在进行中")
            return
        }

        // 检查WiFi是否开启
        if (!NetworkUtils.isWifiEnabled(context)) {
            _scanStatus.postValue("WiFi未开启，正在打开WiFi设置...")
            try {
                // 尝试打开WiFi设置界面
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "无法打开WiFi设置: ${e.message}")
                _scanStatus.postValue("WiFi未开启，请手动前往设置开启WiFi功能")
            }
            return
        }

        // 如果是恢复扫描，直接设置状态并返回
        if (isPaused) {
            isPaused = false
            _scanState.postValue(ScanState.SCANNING)
            _scanStatus.postValue("正在恢复扫描...")
            return
        }

        // 启动扫描任务
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            isScanning = true
            _scanState.postValue(ScanState.SCANNING)

            try {
                // 首先检查数据库中的服务器
                _scanStatus.postValue("正在检查已保存的服务器...")
                if (scanServerWithDatabase()) return@launch

                // 如果没有找到可用的服务器，开始局域网扫描
                _scanStatus.postValue("正在扫描局域网内的服务器...")
                if (scanServerWithLocalNetwork()) return@launch

            } catch (e: Exception) {
                Log.e(TAG, "扫描过程中发生错误: ${e.message}", e)
                _scanStatus.postValue("扫描失败: ${e.message}")
            } finally {
                isScanning = false
                isPaused = false
                _scanState.postValue(ScanState.IDLE)
            }
        }
    }

    private suspend fun scanServerWithLocalNetwork(): Boolean {
        // 获取当前WiFi网络信息
        val wifiInfo = NetworkUtils.getCurrentWiFiInfo(context)
        if (wifiInfo == null || !wifiInfo.isWifiConnected()) {
            _scanStatus.postValue("无法获取WiFi网络信息")
            return true
        }

        _scanProgress.postValue("已连接到WiFi: ${wifiInfo.ssid}")

        // 获取本地IP地址
        val localIp = getLocalIpAddress()
        if (localIp == null) {
            _scanStatus.postValue("无法获取本地IP地址")
            return true
        }

        _scanProgress.postValue("本地IP地址: $localIp")

        // 获取用户设置的IP范围
        val (ipStart, ipEnd) = getIpRange()
        _scanProgress.postValue("正在扫描IP范围: $ipStart ~ $ipEnd")

        // 生成IP范围内的所有IP地址
        val ipRange = generateIpRange(ipStart, ipEnd)
        if (ipRange.isEmpty()) {
            _scanStatus.postValue("IP范围无效或为空")
            return true
        }

        _scanProgress.postValue("IP范围内共有 ${ipRange.size} 个地址，开始扫描...")

        // 扫描IP范围内的地址
        val reachableIps = scanIpRangeWithPing(ipRange)

        if (reachableIps.isEmpty()) {
            _scanStatus.postValue("未发现任何可用的设备")
            return true
        }

        _scanProgress.postValue("发现 ${reachableIps.size} 个设备，正在检查服务器...")

        // 检查每个IP地址是否是服务器
        val serverScanResult = checkForServers(reachableIps)

        if (serverScanResult != null) {
            _scanStatus.postValue("成功发现服务器: ${serverScanResult.serverEntity.name}")
            _discoveredServer.postValue(serverScanResult)

        } else {
            _scanStatus.postValue("未发现任何服务器")
        }
        return false
    }

    private suspend fun scanServerWithDatabase(): Boolean {
        val existingServers = serverRepository.getAllServersSync()
        if (existingServers.isNotEmpty()) {
            _scanProgress.postValue("找到 ${existingServers.size} 个已保存的服务器，正在检查连接状态...")

            for (server in existingServers) {
                // 检查是否已暂停
                while (isPaused) {
                    delay(1000)
                }

                // 检查扫描是否已被停止
                if (!isScanning) {
                    break
                }

                _scanProgress.postValue("正在检查服务器: ${server.name} (${server.apiEndpoint()})")
                Log.d(TAG, "正在检查服务器: ${server.name} (${server.apiEndpoint()})")

                // 检查服务器状态
                val serverInfo = checkServer(server.ip, server.port)
                if (serverInfo != null) {
                    // 找到可用服务器，更新数据库
                    _scanStatus.postValue("已连接到服务器: ${serverInfo.name}")
                    _scanProgress.postValue("服务器信息: ${serverInfo.name} (${serverInfo.apiEndpoint()})")
                    Log.i(TAG, "已连接到服务器: ${serverInfo.name} (${serverInfo.apiEndpoint()})")
                    
                    // 创建ServerScanResult并设置发现的服务器
                    val serverScanResult = ServerScanResult(
                        serverEntity = serverInfo,
                        ip = serverInfo.ip,
                        port = serverInfo.port
                    )
                    _discoveredServer.postValue(serverScanResult)

                    // 停止扫描
                    stopServerScan()
                    return true
                }
            }

            _scanProgress.postValue("所有已保存的服务器都不可用，开始扫描局域网...")
        } else {
            _scanProgress.postValue("数据库中没有保存的服务器，开始扫描局域网...")
        }
        return false
    }

    /**
     * 暂停扫描局域网内的服务器
     */
    fun pauseServerScan() {
        if (!isScanning || isPaused) {
            Log.d(TAG, "服务器扫描未在进行中或已暂停")
            return
        }

        isPaused = true
        _scanState.postValue(ScanState.PAUSED)
        _scanStatus.postValue("扫描已暂停")
        _scanStatus.postValue("扫描已暂停，点击继续扫描")
        Log.d(TAG, "服务器扫描已暂停")
    }

    /**
     * 停止服务器扫描
     */
    fun stopServerScan() {
        scanJob?.cancel()
        isScanning = false
        isPaused = false
        _scanState.postValue(ScanState.IDLE)
        _scanStatus.postValue("扫描已停止")
    }

    /**
     * 获取扫描状态
     */
    fun getScanStatus(): String {
        return when {
            !isScanning -> "IDLE"
            isPaused -> "PAUSED"
            else -> "SCANNING"
        }
    }

    /**
     * 是否正在扫描
     */
    fun isScanning(): Boolean = isScanning

    /**
     * 是否已暂停扫描
     */
    fun isPaused(): Boolean = isPaused

    /**
     * 获取本地IP地址
     */
    private fun getLocalIpAddress(): String? {
        return NetworkUtils.getLocalIpAddress()
    }

    /**
     * 扫描IP范围内的地址
     */
    private suspend fun scanIpRangeWithPing(ipRange: List<String>): List<String> =
        withContext(Dispatchers.IO) {
            val reachableIps = mutableListOf<String>()
            val jobs = mutableListOf<Job>()

            _scanProgress.postValue("开始ping扫描IP范围，共 ${ipRange.size} 个地址")

            for (ip in ipRange) {
                // 验证IP地址是否有效
                if (!isValidIpAddress(ip)) {
                    Log.w(TAG, "跳过无效的IP地址: $ip")
                    continue
                }
                
                val job = launch {
                    _scanProgress.postValue("正在ping: $ip")
                    if (NetworkUtils.isReachable(ip, SCAN_TIMEOUT_MS)) {
                        synchronized(reachableIps) {
                            reachableIps.add(ip)
                        }
                        _scanProgress.postValue("ping发现设备: $ip (已响应ping)")
                    } else {
                        _scanProgress.postValue("ping无响应: $ip")
                    }
                }
                jobs.add(job)

                // 限制并发数量，避免过多连接
                if (jobs.size >= 20) {
                    jobs.joinAll()
                    jobs.clear()
                }
            }

            // 等待剩余任务完成
            jobs.joinAll()

            _scanProgress.postValue("IP范围扫描完成，发现 ${reachableIps.size} 个设备")

            return@withContext reachableIps
        }

    /**
     * 扫描网络段内的IP地址（已弃用，改用scanIpRangeWithPing）
     */
    private suspend fun scanNetworkWithPing(networkPrefix: String): List<String> {
        // 获取用户设置的IP范围
        val (ipStart, ipEnd) = getIpRange()
        
        // 生成IP范围内的所有IP地址
        val ipRange = generateIpRange(ipStart, ipEnd)
        
        // 使用新的IP范围扫描方法
        return scanIpRangeWithPing(ipRange)
    }

    /**
     * 检查IP地址是否是服务器
     */
    private suspend fun checkForServers(ipAddresses: List<String>): ServerScanResult? =
        withContext(Dispatchers.IO) {
            val jobs = mutableListOf<Job>()
            var foundServerResult: ServerScanResult? = null
            val (portStart, portEnd) = getPortRange()

            _scanProgress.postValue("开始检查 ${ipAddresses.size} 个ping可达的设备是否为服务器 (端口范围: $portStart-$portEnd)")

            // 过滤掉无效的IP地址
            val validIpAddresses = ipAddresses.filter { ip ->
                if (isValidIpAddress(ip)) {
                    true
                } else {
                    Log.w(TAG, "跳过无效的IP地址: $ip")
                    false
                }
            }
            
            _scanProgress.postValue("过滤后有效IP地址数量: ${validIpAddresses.size}")

            for (ip in validIpAddresses) {
                // 检查是否已暂停
                while (isPaused) {
                    delay(1000) // 暂停检查间隔
                }

                // 检查扫描是否已被停止
                if (!isScanning) {
                    break
                }

                // 如果已经找到服务器，提前退出
                if (foundServerResult != null) {
                    _scanProgress.postValue("已找到服务器，停止检查其他设备")
                    break
                }

                val job = launch {
                    _scanProgress.postValue("正在检查ping可达的设备: $ip")
                    // 检查端口范围
                    for (port in portStart..portEnd) {
                        // 再次检查是否已暂停
                        while (isPaused) {
                            delay(1000) // 暂停检查间隔
                        }

                        // 再次检查扫描是否已被停止
                        if (!isScanning) {
                            break
                        }

                        // 如果已经找到服务器，停止检查
                        if (foundServerResult != null) {
                            _scanProgress.postValue("已找到服务器，停止检查 $ip 的其他端口")
                            break
                        }

                        val se = checkServer(ip, port)
                        if (se != null) {
                            foundServerResult = ServerScanResult(se, ip, port)
                            _scanProgress.postValue("找到服务器: ${se.name} ($ip:$port)，停止扫描")
                            break
                        }
                    }
                }
                jobs.add(job)

                // 限制并发数量
                if (jobs.size >= 10) {
                    jobs.joinAll()
                    jobs.clear()

                    // 如果已经找到服务器，提前退出
                    if (foundServerResult != null) {
                        _scanProgress.postValue("已找到服务器，停止创建新的检查任务")
                        break
                    }
                }
            }

            // 等待剩余任务完成
            jobs.joinAll()

            val result = foundServerResult
            if (result != null) {
                _scanProgress.postValue("服务器检查完成，已找到服务器: ${result.serverEntity.name}")
            } else {
                _scanProgress.postValue("服务器检查完成，未找到任何服务器")
            }

            return@withContext result
        }

    /**
     * 检查指定IP和端口是否是服务器
     */
    private suspend fun checkServer(ip: String, port: Int): ServerEntity? =
        withContext(Dispatchers.IO) {
            try {
                // 验证IP和端口的有效性
                if (!isValidIpAddress(ip) || !isValidPort(port)) {
                    Log.e(TAG, "无效的IP地址或端口: $ip:$port")
                    _scanProgress.postValue("检查服务器: $ip:$port - 无效的IP地址或端口")
                    return@withContext null
                }
                
                val se = verifyAndSyncServer(ip,port)

                if (se != null) {
                    _scanProgress.postValue("检查服务器: $ip:$port - /api/server 响应正常，正在解析服务器信息")

                    _scanProgress.postValue("成功发现服务器: ${se.name} ($ip:$port)")
                    return@withContext se
                } else {
                    _scanProgress.postValue("检查服务器: $ip:$port - /api/server 无响应")
                }
            } catch (e: Exception) {
                _scanProgress.postValue("检查服务器: $ip:$port - 连接错误: ${e.message}")
            }

            return@withContext null
        }


    /**
     * 给定服务器ip和port，检查是否是有效服务器，如是则保存到数据库
     *
     * @param ip 服务器IP地址
     * @param port 服务器端口号
     *
     * @return 服务器实体对象，如果检查失败则返回null
     */
    suspend fun verifyAndSyncServer(ip: String, port: Int): ServerEntity? {
        return try {
            Log.d(TAG, "开始验证服务器: $ip:$port")

            // 验证IP和端口的有效性
            if (!isValidIpAddress(ip) || !isValidPort(port)) {
                Log.e(TAG, "无效的IP地址或端口: $ip:$port")
                return null
            }

            // 检查服务器是否有效
            val apiClient = ApiClient.getInstance().setApiEndpoint("http://$ip:$port/api")
            val serverInfo = apiClient.getServerInfo()

            if (serverInfo == null) {
                Log.e(TAG, "无法获取服务器信息: $ip:$port")
                return null
            }

            // 验证服务器信息是否有效
            if (!isValidServerInfo(serverInfo)) {
                Log.e(TAG, "服务器信息无效: $ip:$port")
                return null
            }

            // 验证返回的IP和端口是否与请求的一致
            if (serverInfo.ip != ip || serverInfo.port != port) {
                Log.w(
                    TAG,
                    "服务器返回的IP或端口与请求不匹配: 请求($ip:$port), 返回(${serverInfo.ip}:${serverInfo.port})"
                )
            }
            
            // 检查数据库中是否已存在该服务器
            val existingServer = serverRepository.getServerByKey(ip, port)
            
            val se = if (existingServer != null) {
                // 更新现有服务器，保留原有的discoveryType和createdAt
                existingServer.copy(
                    name = serverInfo.name,
                    platform = serverInfo.platform,
                    services = serverInfo.services,
                    capabilities = serverInfo.capabilities,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                // 创建新服务器
                ServerEntity(
                    ip = serverInfo.ip,
                    port = serverInfo.port,
                    name = serverInfo.name,
                    platform = serverInfo.platform,
                    services = serverInfo.services,
                    capabilities = serverInfo.capabilities,
                    isConnected = false,
                    discoveryType = "manual",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            // 保存服务器到数据库
            try {
                // 使用协程作用域调用suspend函数
                serverRepository.insertOrUpdateServer(se)
                Log.d(TAG, "服务器验证成功并保存到数据库: ${se.name} (${se.ip}:${se.port})")
            } catch (e: Exception) {
                Log.e(TAG, "保存服务器到数据库失败: ${e.message}", e)
            }
            
            // 即使保存失败，也返回服务器信息，因为服务器验证成功
            return se
        } catch (e: Exception) {
            Log.e(TAG, "验证服务器时发生异常: $ip:$port - ${e.message}", e)
            null
        }
    }

    /**
     * 验证IP地址是否有效
     */
    private fun isValidIpAddress(ip: String): Boolean {
        return ip.isNotBlank() &&
                ip.matches(Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"))
    }

    /**
     * 验证端口是否有效
     */
    private fun isValidPort(port: Int): Boolean {
        return port in 1..65535
    }

    /**
     * 验证服务器信息是否有效
     */
    private fun isValidServerInfo(serverInfo: ServerInfoResponse): Boolean {
        return serverInfo.name.isNotBlank() &&
                serverInfo.platform.isNotBlank()
    }

    /**
     * 验证网络前缀是否有效
     */
    private fun isValidNetworkPrefix(networkPrefix: String): Boolean {
        // 检查网络前缀格式，应该是xxx.xxx.xxx的形式
        return networkPrefix.isNotBlank() &&
                networkPrefix.matches(Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){2}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"))
    }
}