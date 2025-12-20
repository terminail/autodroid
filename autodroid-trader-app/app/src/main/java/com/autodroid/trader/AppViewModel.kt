package com.autodroid.trader

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.LiveData
import com.autodroid.trader.data.repository.ServerRepository
import com.autodroid.trader.data.dao.ServerEntity
import com.autodroid.trader.model.User
import com.autodroid.trader.model.Network
import com.autodroid.trader.model.Device
import com.autodroid.trader.model.Wifi
import com.autodroid.trader.model.TradePlan
import com.autodroid.trader.auth.viewmodel.AuthViewModel

class AppViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        @Volatile
        private var instance: AppViewModel? = null
        
        fun getInstance(application: Application): AppViewModel {
            return instance ?: synchronized(this) {
                instance ?: AppViewModel(application).also { instance = it }
            }
        }
    }
    
    // Repositories
    private var serverRepository: ServerRepository? = null
    
    // Authentication ViewModel
    private val authViewModel = AuthViewModel()
    
    // User authentication state (global shared state)
    val user = MutableLiveData<User>()
    val errorMessage = MutableLiveData<String?>()
    
    // Server connection state (global shared state)
    val server = MediatorLiveData<ServerEntity?>()
    
    // Device information (global shared state)
    val device = MutableLiveData<Device>()
    
    // WiFi information (global shared state)
    val wifi = MutableLiveData<Wifi>()
    
    // Network information (global shared state)
    val network = MutableLiveData<Network>()
    
    // Test Plans information
    val availableTradePlans = MutableLiveData<MutableList<TradePlan>>()

    /**
     * Initialize the ViewModel with application context
     */
    fun initialize(context: Context) {
        serverRepository = ServerRepository.getInstance(context.applicationContext as Application)
        
        // 直接监控 Room 数据库中最后更新的服务器
        serverRepository?.getLastUpdatedServer()?.let { liveData: LiveData<ServerEntity?> ->
            // 将数据库中的服务器数据映射到 ViewModel 的 server LiveData
            server.addSource(liveData) { serverEntity: ServerEntity? ->
                server.value = serverEntity
            }
        }
    }
    
    /**
     * Check if the ViewModel is properly initialized
     */
    fun isInitialized(): Boolean {
        return serverRepository != null
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
    
    fun setAvailableTradePlans(tradePlans: MutableList<TradePlan>) {
        availableTradePlans.setValue(tradePlans)
    }
    
    // Authentication methods
    fun login(email: String?, password: String?) {
        authViewModel.login(email, password)
    }
    
    fun register(email: String?, password: String?, confirmPassword: String?) {
        authViewModel.register(email, password, confirmPassword)
    }
    
    fun logout() {
        authViewModel.logout()
    }
    
    fun loginWithBiometrics() {
        authViewModel.loginWithBiometrics()
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
    
    // Convenience methods for common operations
    fun connectDevice(ip: String, name: String? = null) {
        setDevice(Device.connected(ip, name))
    }
    
    fun disconnectDevice() {
        device.value?.let { currentInfo: Device ->
            setDevice(currentInfo.disconnected())
        }
    }
    
    fun setWifiConnected(ssid: String, ipAddress: String, signalStrength: Int? = null) {
        setWifi(Wifi.connected(ssid, ipAddress, signalStrength))
    }
    
    fun disconnectWifi() {
        wifi.value?.let { currentInfo: Wifi ->
            setWifi(currentInfo.disconnected())
        }
    }
}