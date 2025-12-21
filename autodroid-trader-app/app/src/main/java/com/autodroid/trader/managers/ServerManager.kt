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
import com.google.gson.Gson
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
    val serverInfo: ServerInfoResponse,
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
    private val serverRepository = ServerRepository.getInstance(context.applicationContext as android.app.Application)
    
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
            _scanStatus.postValue("正在检查已保存的服务器...")
            
            try {
                // 首先检查数据库中的服务器
                val existingServers = serverRepository.getAllServers().value ?: emptyList()
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
                            val apiEndpoint = server.apiEndpoint()
                            serverRepository.updateServer(apiEndpoint, serverInfo)
                            _scanStatus.postValue("已连接到服务器: ${server.name}")
                            _scanProgress.postValue("服务器信息: ${server.name} (${server.apiEndpoint()})")
                            Log.i(TAG, "已连接到服务器: ${server.name} (${server.apiEndpoint()})")
                            
                            // 停止扫描
                            stopServerScan()
                            return@launch
                        }
                    }
                    
                    _scanProgress.postValue("所有已保存的服务器都不可用，开始扫描局域网...")
                } else {
                    _scanProgress.postValue("数据库中没有保存的服务器，开始扫描局域网...")
                }
                
                // 如果没有找到可用的服务器，开始局域网扫描
                _scanStatus.postValue("正在扫描局域网内的服务器...")
                
                // 获取当前WiFi网络信息
                val wifiInfo = NetworkUtils.getCurrentWiFiInfo(context)
                if (wifiInfo == null || !wifiInfo.isWifiConnected()) {
                    _scanStatus.postValue("无法获取WiFi网络信息")
                    return@launch
                }
                
                _scanProgress.postValue("已连接到WiFi: ${wifiInfo.ssid}")
                
                // 获取本地IP地址和网络前缀
                val localIp = getLocalIpAddress()
                if (localIp == null) {
                    _scanStatus.postValue("无法获取本地IP地址")
                    return@launch
                }
                
                _scanProgress.postValue("本地IP地址: $localIp")
                
                // 计算网络前缀
                val networkPrefix = NetworkUtils.getSubnet(localIp) ?: "192.168.1"
                _scanProgress.postValue("正在扫描网络段: $networkPrefix.x")
                
                // 扫描局域网内的IP地址
                val reachableIps = scanNetwork(networkPrefix)
                
                if (reachableIps.isEmpty()) {
                    _scanStatus.postValue("未发现任何可用的设备")
                    return@launch
                }
                
                _scanProgress.postValue("发现 ${reachableIps.size} 个设备，正在检查服务器...")
                
                // 检查每个IP地址是否是服务器
                val serverScanResult = checkForServers(reachableIps)
                
                if (serverScanResult != null) {
                    _scanStatus.postValue("成功发现服务器: ${serverScanResult.serverInfo.name}")
                    _discoveredServer.postValue(serverScanResult)
                    
                    // 保存服务器到数据库
                    saveServerToDatabase(serverScanResult.serverInfo, serverScanResult.ip, serverScanResult.port)
                } else {
                    _scanStatus.postValue("未发现任何服务器")
                }
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
     * 扫描网络段内的IP地址
     */
    private suspend fun scanNetwork(networkPrefix: String): List<String> = withContext(Dispatchers.IO) {
        val reachableIps = mutableListOf<String>()
        val jobs = mutableListOf<Job>()
        
        _scanProgress.postValue("开始ping扫描网络段: $networkPrefix.x (1-254)")
        
        // 扫描1-254的IP地址
        for (i in 1..254) {
            val ip = "$networkPrefix.$i"
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
        
        _scanProgress.postValue("网络扫描完成，发现 ${reachableIps.size} 个设备")
        
        return@withContext reachableIps
    }
    
    /**
     * 检查IP地址是否是服务器
     */
    private suspend fun checkForServers(ipAddresses: List<String>): ServerScanResult? = withContext(Dispatchers.IO) {
        val jobs = mutableListOf<Job>()
        var foundServerResult: ServerScanResult? = null
        val (portStart, portEnd) = getPortRange()
        
        _scanProgress.postValue("开始检查 ${ipAddresses.size} 个ping可达的设备是否为服务器 (端口范围: $portStart-$portEnd)")
        
        for (ip in ipAddresses) {
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
                    
                    val serverInfo = checkServer(ip, port)
                    if (serverInfo != null) {
                        foundServerResult = ServerScanResult(serverInfo, ip, port)
                        _scanProgress.postValue("找到服务器: ${serverInfo.name} ($ip:$port)，停止扫描")
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
        
        if (foundServerResult != null) {
            _scanProgress.postValue("服务器检查完成，已找到服务器: ${foundServerResult.serverInfo.name}")
        } else {
            _scanProgress.postValue("服务器检查完成，未找到任何服务器")
        }
        
        return@withContext foundServerResult
    }
    
    /**
     * 检查指定IP和端口是否是服务器
     */
    private suspend fun checkServer(ip: String, port: Int): ServerInfoResponse? = withContext(Dispatchers.IO) {
        try {
            val apiEndpoint = "http://$ip:$port/api"
            val apiClient = ApiClient.getInstance().setApiEndpoint(apiEndpoint)
            
            // 更新扫描进度：正在检查服务器
            _scanProgress.postValue("正在检查服务器: $ip:$port - 访问 /api/server")
            
            // 直接检查服务器信息端点
            val serverInfo = apiClient.getServerInfo()
            
            if (serverInfo != null) {
                _scanProgress.postValue("检查服务器: $ip:$port - /api/server 响应正常，正在解析服务器信息")
                
                _scanProgress.postValue("成功发现服务器: ${serverInfo.name} ($ip:$port)")
                return@withContext serverInfo
            } else {
                _scanProgress.postValue("检查服务器: $ip:$port - /api/server 无响应")
            }
        } catch (e: Exception) {
            _scanProgress.postValue("检查服务器: $ip:$port - 连接错误: ${e.message}")
        }
        
        return@withContext null
    }
    
    /**
     * 解析服务器信息
     */
    private fun parseServerInfo(response: String): ServerInfoResponse? {
        return try {
            val gson = Gson()
            gson.fromJson(response, ServerInfoResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "解析服务器信息失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 保存服务器到数据库
     */
    private suspend fun saveServerToDatabase(serverInfo: ServerInfoResponse, ip: String, port: Int) {
        try {
            // 将ServerInfoResponse转换为ServerEntity并保存到数据库
            val serverEntity = com.autodroid.trader.data.dao.ServerEntity(
                ip = ip,
                port = port,
                name = serverInfo.name,
                platform = serverInfo.platform,
                services = serverInfo.services,
                capabilities = serverInfo.capabilities,
                isConnected = true,
                lastConnectedTime = System.currentTimeMillis(),
                discoveryType = "autoscan"
            )
            
            // 使用Repository保存服务器信息
            serverRepository.insertOrUpdateServer(
                serverEntity.apiEndpoint(), 
                serverInfo.name ?: "未知服务器", 
                serverInfo.platform
            )
            
            Log.d(TAG, "服务器已保存到数据库: ${serverInfo.name}")
        } catch (e: Exception) {
            Log.e(TAG, "保存服务器到数据库失败: ${e.message}", e)
        }
    }
    
}