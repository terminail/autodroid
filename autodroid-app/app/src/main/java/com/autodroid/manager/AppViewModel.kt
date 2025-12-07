package com.autodroid.manager

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.autodroid.data.repository.ServerRepository
import com.autodroid.manager.model.User
import com.autodroid.manager.model.DiscoveryStatus
import com.autodroid.manager.model.Device
import com.autodroid.manager.model.Network
import com.autodroid.manager.model.Apk
import com.autodroid.manager.model.Server
import com.autodroid.manager.model.Wifi
import com.autodroid.manager.model.Workflow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AppViewModel - 应用级共享状态管理ViewModel
 * 
 * 职责说明：
 * - 管理全局应用状态和数据，如用户认证信息、设备信息、网络状态等
 * - 提供跨Activity/Fragment共享的LiveData，确保数据一致性
 * - 协调不同模块之间的数据共享和状态同步
 * 
 * 设计原则：
 * 1. 单一职责原则：只管理全局共享状态，不处理具体业务逻辑
 * 2. 避免God ViewModel：每个Activity/Fragment应有自己的ViewModel处理本地状态
 * 3. 数据驱动：通过LiveData提供响应式数据更新
 * 4. 依赖倒置：通过Repository层访问数据，不直接操作数据源
 * 
 * 使用场景：
 * - 用户认证状态（登录/登出）
 * - 设备连接状态
 * - 网络连接状态
 * - 全局配置信息
 * - 跨页面共享的数据
 * 
 * 注意事项：
 * - 每个Activity/Fragment除了使用AppViewModel外，应有自己的ViewModel处理本地UI状态
 * - 具体业务逻辑应由对应的Manager层（如DiscoveryStatusManager）处理
 * - UI组件应直接调用Manager层方法，AppViewModel仅提供状态观察
 */
class AppViewModel : ViewModel() {
    
    companion object {
        @Volatile
        private var instance: AppViewModel? = null
        
        fun getInstance(): AppViewModel {
            return instance ?: synchronized(this) {
                instance ?: AppViewModel().also { instance = it }
            }
        }
    }
    
    // Repository instances
    private var serverRepository: ServerRepository? = null
    
    // Server information (single source of truth)
    val server: LiveData<Server?> get() = serverRepository?.getConnectedServer()?.map { entity ->
        if (entity != null) {
            Server(
                serviceName = entity.name,
                name = entity.name,
                connected = entity.isConnected,
                apiEndpoint = if (entity.apiEndpoint.isNotEmpty()) entity.apiEndpoint else "http://localhost/api",
                discoveryMethod = entity.discoveryType.ifEmpty { "Database" },
                hostname = entity.hostname,
                platform = entity.platform,
                supportsDeviceRegistration = entity.supportsDeviceRegistration,
                supportsApkManagement = entity.supportsApkManagement,
                supportsWorkflowExecution = entity.supportsWorkflowExecution
            )
        } else {
            null
        }
    } ?: MutableLiveData<Server?>().apply { value = null }
    
    // Saved servers list from repository
    val savedServers = MutableLiveData<List<Server>>()

    // User authentication information (global shared state)
    val user = MutableLiveData<User>(User(isAuthenticated = false))
    val errorMessage = MutableLiveData<String?>()

    // Device information (global shared state)
    val device = MutableLiveData<Device>()

    // WiFi information (global shared state)
    val wifi = MutableLiveData<Wifi>()
    
    // Network information (global shared state)
    val network = MutableLiveData<Network>()
    
    // APK information (global shared state)
    val apk = MutableLiveData<Apk>()
    val apkScanStatus = MutableLiveData<String?>()
    val apkList = MutableLiveData<List<com.autodroid.manager.model.Apk>?>()
    val selectedApkIndex = MutableLiveData<Int>()
    
    // Workflows information
    val availableWorkflows = MutableLiveData<MutableList<Workflow>>()

    /**
     * Initialize the ViewModel with application context
     */
    fun initialize(context: Context) {
        serverRepository = ServerRepository.getInstance(context.applicationContext as Application)
        
        // Load saved servers from repository
        loadSavedServers()
    }
    
    /**
     * Load saved servers from repository
     */
    private fun loadSavedServers() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serversLiveData = serverRepository?.getAllServers()
                serversLiveData?.observeForever { servers ->
                    savedServers.value = (servers?.map { entity ->
                        Server(
                            serviceName = entity.name,
                            name = entity.name,
                            connected = entity.lastConnectedTime > 0,
                            apiEndpoint = "http://localhost/api"
                        )
                    } ?: emptyList()) as List<Server>?
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error loading saved servers: ${e.message}", e)
            }
        }
    }

    // Setters
    
    // User authentication setters
    fun setUser(userInfo: User) {
        this.user.setValue(userInfo)
    }
    
    fun setErrorMessage(message: String?) {
        errorMessage.setValue(message)
    }
    
    fun setWifi(info: Wifi) {
        wifi.setValue(info)
    }
    
    fun setNetwork(info: Network) {
        network.setValue(info)
    }
    
    fun setDevice(info: Device) {
        device.setValue(info)
    }
    
    fun setApk(info: Apk) {
        apk.setValue(info)
    }
    
    fun setApkScanStatus(status: String?) {
        apkScanStatus.setValue(status)
    }
    
    fun setApkList(apkList: List<com.autodroid.manager.model.Apk>?) {
        this.apkList.setValue(apkList)
    }
    
    fun setSelectedApkIndex(index: Int) {
        selectedApkIndex.setValue(index)
    }
    
    // Device information helper methods
    fun getDeviceIp(): String? {
        return device.value?.ip
    }
    
    fun isDeviceConnected(): Boolean {
        return device.value?.isAvailable() ?: false
    }
    
    fun getDeviceName(): String? {
        return device.value?.name
    }
    
    // WiFi information helper methods
    fun isWifiConnected(): Boolean {
        return wifi.value?.isWifiConnected() ?: false
    }
    
    fun getWifiSsid(): String? {
        return wifi.value?.ssid
    }
    
    fun getWifiIpAddress(): String? {
        return wifi.value?.ipAddress
    }
    
    // Network information helper methods
    fun isNetworkAvailable(): Boolean {
        return network.value?.isNetworkAvailable() ?: false
    }
    
    fun getNetworkConnectionType(): Network.ConnectionType {
        return network.value?.connectionType ?: Network.ConnectionType.NONE
    }
    
    fun getNetworkIpAddress(): String? {
        return network.value?.ipAddress
    }
    
    // APK information helper methods
    fun getApkPackageName(): String? {
        return apk.value?.packageName
    }
    
    fun getApkVersion(): String? {
        return apk.value?.let { "${it.version} (${it.versionCode})" }
    }
    
    fun isApkComplete(): Boolean {
        return apk.value?.let { it.packageName.isNotEmpty() && it.appName.isNotEmpty() } ?: false
    }
    
    // User authentication helper methods
    fun isAuthenticated(): Boolean {
        return user.value?.isAuthenticated ?: false
    }
    
    fun getUserId(): String? {
        return user.value?.userId
    }
    
    fun getEmail(): String? {
        return user.value?.email
    }
    
    fun getToken(): String? {
        return user.value?.token
    }
    
    fun getErrorMessage(): String? {
        return errorMessage.value
    }
    
    // Clear authentication data (for logout)
    fun clearAuthentication() {
        setUser(User.empty())
        setErrorMessage(null)
    }
    
    // Authentication state management
    // Note: setServerInfo method is already defined at line 41
    
    // Server connection status observer for authentication state coordination
    // This is the only authentication-related state that should be in AppViewModel
    // as it's needed for server change detection
    private var previousServer: Server? = null

    // Server change detection callback - can be used by AuthViewModel
    var onServerChanged: ((oldServer: Server?, newServer: Server?) -> Unit)? = null
    
    // Convenience methods for initializing encapsulated states
    fun initializeDevice(ip: String? = null, name: String? = null) {
        setDevice(Device.empty().copy(ip = ip, name = name))
    }
    
    fun initializeWifi(ssid: String? = null, ipAddress: String? = null) {
        setWifi(Wifi.empty().copy(ssid = ssid, ipAddress = ipAddress))
    }
    
    fun initializeNetwork(connectionType: Network.ConnectionType = Network.ConnectionType.NONE) {
        setNetwork(Network.empty().copy(connectionType = connectionType))
    }
    
    fun initializeApk(packageName: String? = null, appName: String? = null) {
        setApk(Apk.empty().copy(packageName = packageName ?: "", appName = appName ?: ""))
    }
    
    // Convenience methods for common operations
    fun connectDevice(ip: String, name: String? = null) {
        setDevice(Device.connected(ip, name))
    }
    
    fun disconnectDevice() {
        device.value?.let { currentInfo ->
            setDevice(currentInfo.disconnected())
        }
    }
    
    fun setWifiConnected(ssid: String, ipAddress: String, signalStrength: Int? = null) {
        setWifi(Wifi.connected(ssid, ipAddress, signalStrength))
    }
    
    fun disconnectWifi() {
        wifi.value?.let { currentInfo ->
            setWifi(currentInfo.disconnected())
        }
    }
    
    fun updateApkVersion(packageName: String, versionName: String, versionCode: Int) {
        apk.value?.let { currentInfo ->
            if (currentInfo.packageName == packageName) {
                setApk(currentInfo.copy(version = versionName, versionCode = versionCode))
            }
        }
    }
    
    // Missing methods to fix compilation errors
    fun register(email: String, password: String, confirmPassword: String) {
        // Create a new User object with the provided credentials
        val userInfo = User(
            userId = null,
            email = email,
            token = null,
            isAuthenticated = false
        )
        setUser(userInfo)
    }
    
    fun setDeviceIp(ip: String?) {
        device.value?.let { currentInfo ->
            setDevice(currentInfo.copy(ip = ip))
        }
    }
    
    fun setAvailableWorkflows(workflows: MutableList<Workflow>) {
        availableWorkflows.setValue(workflows)
    }
    
    /**
     * Connect to a saved server by key
     */
    fun connectToSavedServer(serverKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = serverRepository?.connectToServer(serverKey) ?: false
                if (success) {
                    Log.d("AppViewModel", "Connected to server with key: $serverKey")
                } else {
                    Log.w("AppViewModel", "Failed to connect to server with key: $serverKey")
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error connecting to saved server: ${e.message}", e)
            }
        }
    }
    
    /**
     * Disconnect from current server
     */
    fun disconnectFromServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverRepository?.disconnectServer()
                Log.d("AppViewModel", "Disconnected from server")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error disconnecting from server: ${e.message}", e)
            }
        }
    }
    

    
    /**
     * Delete a saved server
     */
    fun deleteSavedServer(serverKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverRepository?.deleteServer(serverKey)
                // Refresh saved servers list
                loadSavedServers()
                Log.d("AppViewModel", "Deleted server with key: $serverKey")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error deleting server: ${e.message}", e)
            }
        }
    }
    
    /**
     * Refresh saved servers list
     */
    fun refreshSavedServers() {
        loadSavedServers()
    }
    
    // Server connection state helper methods
    fun isServerConnected(): Boolean {
        return server.value?.connected ?: false
    }
    
    fun getServerHost(): String? {
        return server.value?.hostname
    }
}