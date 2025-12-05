package com.autodroid.manager

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.autodroid.manager.model.UserInfo
import com.autodroid.manager.model.DiscoveryStatus
import com.autodroid.manager.model.DeviceInfo
import com.autodroid.manager.model.WifiInfo
import com.autodroid.manager.model.NetworkInfo
import com.autodroid.manager.model.ApkInfo

class AppViewModel : ViewModel() {
    // Getters for LiveData
    // Server information - unified object from mDNS discovery
    val serverInfo = MutableLiveData<MutableMap<String?, Any?>?>()
    
    // Discovery status information (encapsulated)
    val discoveryStatus = MutableLiveData<DiscoveryStatus>()

    // User authentication information (global shared state)
    val userInfo = MutableLiveData<UserInfo>()
    val errorMessage = MutableLiveData<String?>()

    // Device information (global shared state)
    val deviceInfo = MutableLiveData<DeviceInfo>()

    // WiFi information (global shared state)
    val wifiInfo = MutableLiveData<WifiInfo>()
    
    // Network information (global shared state)
    val networkInfo = MutableLiveData<NetworkInfo>()
    
    // APK information (global shared state)
    val apkInfo = MutableLiveData<ApkInfo>()
    val apkScanStatus = MutableLiveData<String?>()
    val apkList = MutableLiveData<List<com.autodroid.manager.model.DashboardItem.ApkInfo>?>()
    val selectedApkIndex = MutableLiveData<Int>()
    
    // Workflows information
    val availableWorkflows = MutableLiveData<MutableList<MutableMap<String?, Any?>?>>()

    // Setters
    fun setServerInfo(info: MutableMap<String?, Any?>?) {
        serverInfo.setValue(info)
    }
    
    // User authentication setters
    fun setUserInfo(userInfo: UserInfo) {
        this.userInfo.setValue(userInfo)
    }
    
    fun setErrorMessage(message: String?) {
        errorMessage.setValue(message)
    }
    
    fun setWifiInfo(info: WifiInfo) {
        wifiInfo.setValue(info)
    }
    
    fun setNetworkInfo(info: NetworkInfo) {
        networkInfo.setValue(info)
    }
    
    fun setDeviceInfo(info: DeviceInfo) {
        deviceInfo.setValue(info)
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
    
    fun setApkInfo(info: ApkInfo) {
        apkInfo.setValue(info)
    }
    
    fun setApkScanStatus(status: String?) {
        apkScanStatus.setValue(status)
    }
    
    fun setApkList(apkList: List<com.autodroid.manager.model.DashboardItem.ApkInfo>?) {
        this.apkList.setValue(apkList)
    }
    
    fun setSelectedApkIndex(index: Int) {
        selectedApkIndex.setValue(index)
    }
    
    // Server connection status helper methods
    fun isServerConnected(): Boolean {
        return serverInfo.value?.get("connected") as? Boolean ?: false
    }
    
    fun getServerHost(): String? {
        return serverInfo.value?.get("ip") as? String
    }
    
    fun getServerPort(): Int? {
        return serverInfo.value?.get("port") as? Int
    }
    
    fun getServerConnectionStatus(): Boolean {
        return isServerConnected()
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
        return deviceInfo.value?.ip
    }
    
    fun isDeviceConnected(): Boolean {
        return deviceInfo.value?.isAvailable() ?: false
    }
    
    fun getDeviceName(): String? {
        return deviceInfo.value?.name
    }
    
    // WiFi information helper methods
    fun isWifiConnected(): Boolean {
        return wifiInfo.value?.isWifiConnected() ?: false
    }
    
    fun getWifiSsid(): String? {
        return wifiInfo.value?.ssid
    }
    
    fun getWifiIpAddress(): String? {
        return wifiInfo.value?.ipAddress
    }
    
    // Network information helper methods
    fun isNetworkAvailable(): Boolean {
        return networkInfo.value?.isNetworkAvailable() ?: false
    }
    
    fun getNetworkConnectionType(): NetworkInfo.ConnectionType {
        return networkInfo.value?.connectionType ?: NetworkInfo.ConnectionType.NONE
    }
    
    fun getNetworkIpAddress(): String? {
        return networkInfo.value?.ipAddress
    }
    
    // APK information helper methods
    fun getApkPackageName(): String? {
        return apkInfo.value?.packageName
    }
    
    fun getApkVersion(): String? {
        return apkInfo.value?.getVersionInfo()
    }
    
    fun isApkComplete(): Boolean {
        return apkInfo.value?.isComplete() ?: false
    }
    
    // User authentication helper methods
    fun isAuthenticated(): Boolean {
        return userInfo.value?.isAuthenticated ?: false
    }
    
    fun getUserId(): String? {
        return userInfo.value?.userId
    }
    
    fun getEmail(): String? {
        return userInfo.value?.email
    }
    
    fun getToken(): String? {
        return userInfo.value?.token
    }
    
    fun getErrorMessage(): String? {
        return errorMessage.value
    }
    
    // Clear authentication data (for logout)
    fun clearAuthentication() {
        setUserInfo(UserInfo.empty())
        setErrorMessage(null)
    }
    
    // Authentication state management
    // Note: setServerInfo method is already defined at line 41
    
    // Server connection status observer for authentication state coordination
    // This is the only authentication-related state that should be in AppViewModel
    // as it's needed for server change detection
    private var previousServerInfo: MutableMap<String?, Any?>? = null
    
    // Server change detection callback - can be used by AuthViewModel
    var onServerChanged: ((oldServer: MutableMap<String?, Any?>?, newServer: MutableMap<String?, Any?>?) -> Unit)? = null
    
    // Convenience methods for initializing encapsulated states
    fun initializeDeviceInfo(ip: String? = null, name: String? = null) {
        setDeviceInfo(DeviceInfo.empty().copy(ip = ip, name = name))
    }
    
    fun initializeWifiInfo(ssid: String? = null, ipAddress: String? = null) {
        setWifiInfo(WifiInfo.empty().copy(ssid = ssid, ipAddress = ipAddress))
    }
    
    fun initializeNetworkInfo(connectionType: NetworkInfo.ConnectionType = NetworkInfo.ConnectionType.NONE) {
        setNetworkInfo(NetworkInfo.empty().copy(connectionType = connectionType))
    }
    
    fun initializeApkInfo(packageName: String? = null, appName: String? = null) {
        setApkInfo(ApkInfo.empty().copy(packageName = packageName, appName = appName))
    }
    
    // Convenience methods for common operations
    fun connectDevice(ip: String, name: String? = null) {
        setDeviceInfo(DeviceInfo.connected(ip, name))
    }
    
    fun disconnectDevice() {
        deviceInfo.value?.let { currentInfo ->
            setDeviceInfo(currentInfo.disconnected())
        }
    }
    
    fun connectToWifi(ssid: String, ipAddress: String, signalStrength: Int? = null) {
        setWifiInfo(WifiInfo.connected(ssid, ipAddress, signalStrength))
    }
    
    fun disconnectWifi() {
        wifiInfo.value?.let { currentInfo ->
            setWifiInfo(currentInfo.disconnected())
        }
    }
    
    fun updateApkVersion(packageName: String, versionName: String, versionCode: Long) {
        apkInfo.value?.let { currentInfo ->
            if (currentInfo.packageName == packageName) {
                setApkInfo(currentInfo.updateVersion(versionName, versionCode))
            }
        }
    }
    
    // Missing methods to fix compilation errors
    fun register(email: String, password: String, confirmPassword: String) {
        // Create a new UserInfo object with the provided credentials
        val userInfo = UserInfo(
            userId = null,
            email = email,
            token = null,
            isAuthenticated = false
        )
        setUserInfo(userInfo)
    }
    
    fun setDeviceIp(ip: String?) {
        deviceInfo.value?.let { currentInfo ->
            setDeviceInfo(currentInfo.copy(ip = ip))
        }
    }
    
    fun setAvailableWorkflows(workflows: MutableList<MutableMap<String?, Any?>?>) {
        availableWorkflows.setValue(workflows)
    }
}