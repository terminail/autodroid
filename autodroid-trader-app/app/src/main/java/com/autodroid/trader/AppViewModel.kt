package com.autodroid.trader

import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.autodroid.trader.data.repository.ServerRepository
import com.autodroid.trader.data.repository.DeviceRepository
import com.autodroid.trader.data.dao.ServerEntity
import com.autodroid.trader.data.dao.DeviceEntity
import com.autodroid.trader.data.database.AppDatabase
import com.autodroid.trader.managers.DeviceManager
import com.autodroid.trader.model.User
import com.autodroid.trader.model.Network
import com.autodroid.trader.model.Wifi
import com.autodroid.trader.model.TradePlan
import com.autodroid.trader.auth.viewmodel.AuthViewModel
import com.autodroid.trader.utils.NetworkUtils

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
    private var deviceRepository: DeviceRepository? = null
    
    // Managers
    val deviceManager = DeviceManager(application, this)
    
    // Authentication ViewModel
    private val authViewModel = AuthViewModel()
    
    // User authentication state (global shared state)
    val user = MutableLiveData<User>()
    val errorMessage = MutableLiveData<String?>()
    
    // Server connection state (global shared state)
    val server = MediatorLiveData<ServerEntity?>()
    
    // Device information (global shared state)
    val device = MediatorLiveData<DeviceEntity?>()
    
    // WiFi information (global shared state)
    val wifi = MutableLiveData<Wifi>()
    
    // Network information (global shared state)
    val network = MutableLiveData<Network>()
    
    // Test Plans information
    val availableTradePlans = MutableLiveData<MutableList<TradePlan>>()

    /**
     * Initialize() with application context
     */
    fun initialize(context: Context) {
        android.util.Log.d("AppViewModel", "initialize: 开始初始化AppViewModel")
        
        serverRepository = ServerRepository.getInstance(context.applicationContext as Application)
        android.util.Log.d("AppViewModel", "initialize: ServerRepository已初始化")
        
        // Initialize DeviceRepository and set it to DeviceManager
        deviceRepository = DeviceRepository.getInstance(context.applicationContext as Application)
        android.util.Log.d("AppViewModel", "initialize: DeviceRepository已初始化")
        
        deviceManager.setDeviceRepository(deviceRepository!!)
        android.util.Log.d("AppViewModel", "initialize: DeviceRepository已设置到DeviceManager")
        
        // 自动初始化本地设备信息
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("AppViewModel", "initialize: 开始自动初始化本地设备信息")
                val localDevice = deviceManager.device
                android.util.Log.d("AppViewModel", "initialize: 获取到本地设备信息，ID: ${localDevice.id}, 名称: ${localDevice.name}")
                
                // 保存设备信息到数据库
                deviceRepository?.insertOrUpdateDevice(localDevice)
                android.util.Log.d("AppViewModel", "initialize: 本地设备信息已保存到数据库")
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "initialize: 自动初始化设备信息失败", e)
            }
        }
        
        // 直接监控 Room 数据库中最后更新的服务器
        serverRepository?.getCurrentServer()?.let { liveData: LiveData<ServerEntity?> ->
            android.util.Log.d("AppViewModel", "initialize: 开始监控服务器数据变化")
            // 将数据库中的服务器数据映射到 ViewModel 的 server LiveData
            server.addSource(liveData) { serverEntity: ServerEntity? ->
                android.util.Log.d("AppViewModel", "initialize: 服务器数据更新，服务器: ${serverEntity?.ip}:${serverEntity?.port}")
                server.value = serverEntity
            }
        }
        
        // 直接监控 Room 数据库中最后更新的设备
        deviceRepository?.getCurrentDevice()?.let { liveData: LiveData<DeviceEntity?> ->
            android.util.Log.d("AppViewModel", "initialize: 开始监控设备数据变化")
            // 将数据库中的设备数据映射到 ViewModel 的 device LiveData
            device.addSource(liveData) { deviceEntity: DeviceEntity? ->
                android.util.Log.d("AppViewModel", "initialize: 设备数据更新，设备ID: ${deviceEntity?.id}, 设备名称: ${deviceEntity?.name}")
                device.value = deviceEntity
            }
        }
        
        android.util.Log.d("AppViewModel", "initialize: AppViewModel初始化完成")
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
    fun initializeDevice(id: String, ip: String? = null, name: String? = null) {
        val deviceEntity = DeviceEntity.empty().copy(id = id, ip = ip, name = name)
        CoroutineScope(Dispatchers.IO).launch {
            deviceRepository?.insertOrUpdateDevice(deviceEntity)
        }
    }
    
    fun initializeWifi(ssid: String? = null, ipAddress: String? = null) {
        setWifi(Wifi.empty().copy(ssid = ssid, ipAddress = ipAddress))
    }
    
    fun initializeNetwork(connectionType: Network.ConnectionType = Network.ConnectionType.NONE) {
        setNetwork(Network.empty().copy(connectionType = connectionType))
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