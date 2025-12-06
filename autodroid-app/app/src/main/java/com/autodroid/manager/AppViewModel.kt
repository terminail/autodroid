package com.autodroid.manager

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.autodroid.manager.model.User
import com.autodroid.manager.model.DiscoveryStatus
import com.autodroid.manager.model.Device
import com.autodroid.manager.model.Network
import com.autodroid.manager.model.Apk
import com.autodroid.manager.model.Server
import com.autodroid.manager.model.Wifi
import com.autodroid.manager.model.Workflow

class AppViewModel : ViewModel() {
    // Getters for LiveData
    // Server information - unified object from mDNS discovery
    val server = MutableLiveData<Server?>()
    
    // Discovery status information (encapsulated)
    val discoveryStatus = MutableLiveData<DiscoveryStatus>()

    // User authentication information (global shared state)
    val user = MutableLiveData<User>()
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

    // Setters
    fun setServer(info: Server?) {
        server.setValue(info)
    }
    
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
    
    fun setDiscoveryStatus(status: DiscoveryStatus) {
        discoveryStatus.setValue(status)
    }
    
    // Convenience methods for common discovery state changes
    fun startDiscovery(maxRetries: Int = 3) {
        setDiscoveryStatus(DiscoveryStatus.discovering(maxRetries))
    }
    
    fun stopDiscovery(failed: Boolean = false) {
        val currentStatus = discoveryStatus.value ?: DiscoveryStatus.initial()
        setDiscoveryStatus(
            if (failed) {
                DiscoveryStatus.failed(currentStatus.maxRetries, currentStatus.retryCount)
            } else {
                currentStatus.copy(inProgress = false)
            }
        )
    }
    
    fun incrementDiscoveryRetry() {
        val currentStatus = discoveryStatus.value ?: DiscoveryStatus.initial()
        setDiscoveryStatus(currentStatus.withIncrementedRetry())
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
    
    // Server connection status helper methods
    fun isServerConnected(): Boolean {
        return server.value != null
    }
    
    fun getServerHost(): String? {
        return server.value?.ip
    }
    
    fun getServerPort(): Int? {
        // Extract port from api_endpoint URL
        return server.value?.api_endpoint?.let { endpoint ->
            try {
                val url = java.net.URL(endpoint)
                url.port
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun getServerConnectionStatus(): Boolean {
        return isServerConnected()
    }
    
    // Enhanced server connection state management
    fun getServerConnectionState(): ServerConnectionState {
        return when {
            isServerConnected() -> ServerConnectionState.CONNECTED
            isDiscoveryInProgress() -> ServerConnectionState.DISCOVERING
            isDiscoveryFailed() -> ServerConnectionState.FAILED
            else -> ServerConnectionState.DISCONNECTED
        }
    }
    
    // Server connection state enum
    enum class ServerConnectionState {
        DISCONNECTED,    // Server not connected and not discovering
        DISCOVERING,     // Actively searching for server
        CONNECTED,       // Server connected successfully
        FAILED           // Server discovery failed
    }
    
    // Discovery status helper methods
    fun isDiscoveryInProgress(): Boolean {
        return discoveryStatus.value?.isDiscovering() ?: false
    }
    
    fun isDiscoveryFailed(): Boolean {
        return discoveryStatus.value?.isDiscoveryFailed() ?: false
    }
    
    fun getDiscoveryRetryCount(): Int {
        return discoveryStatus.value?.retryCount ?: 0
    }
    
    fun getDiscoveryMaxRetries(): Int {
        return discoveryStatus.value?.maxRetries ?: 3
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
}