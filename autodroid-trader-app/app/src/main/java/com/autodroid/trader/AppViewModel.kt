package com.autodroid.trader

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.autodroid.trader.data.repository.ServerRepository
import com.autodroid.trader.data.repository.DeviceRepository
import com.autodroid.trader.data.dao.ServerEntity
import com.autodroid.trader.data.dao.DeviceEntity
import com.autodroid.trader.managers.DeviceManager
import com.autodroid.trader.model.User
import com.autodroid.trader.model.Network
import com.autodroid.trader.model.Wifi
import com.autodroid.trader.model.TradePlan
import com.autodroid.trader.auth.viewmodel.AuthViewModel
import com.autodroid.trader.network.ApiClient


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
    val deviceManager = DeviceManager.getInstance(application, this)

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
        deviceRepository = DeviceRepository.getInstance(context.applicationContext as MyApplication)
        android.util.Log.d("AppViewModel", "initialize: DeviceRepository已初始化")

        // 确保DeviceManager的设备仓库已设置
        deviceManager.setDeviceRepository(deviceRepository!!)
        android.util.Log.d("AppViewModel", "initialize: DeviceRepository已设置到DeviceManager")

        // 先设置服务器监控，确保API客户端初始化
        // 直接监控 Room 数据库中最后更新的服务器
        serverRepository?.getCurrentServer()?.let { liveData: LiveData<ServerEntity?> ->
            android.util.Log.d("AppViewModel", "initialize: 开始监控服务器数据变化")
            // 将数据库中的服务器数据映射到 ViewModel 的 server LiveData
            server.addSource(liveData) { serverEntity: ServerEntity? ->
                android.util.Log.d(
                    "AppViewModel",
                    "initialize: 服务器数据更新，服务器: ${serverEntity?.ip}:${serverEntity?.port}, 名称: ${serverEntity?.name}"
                )
                server.value = serverEntity
                if (serverEntity != null) {
                    getApplication<MyApplication>().setApiEndpoint(serverEntity.apiEndpoint())

                    // 服务器连接后，初始化设备监控
                    initializeDeviceMonitoring()
                }
            }
        } ?: run {
            android.util.Log.e(
                "AppViewModel",
                "initialize: getCurrentServer() 返回 null，无法设置观察者"
            )
            // 即使没有服务器，也要初始化设备监控（仅本地数据库）
            initializeDeviceMonitoring(false)
        }
        android.util.Log.d("AppViewModel", "initialize: AppViewModel初始化完成")
    }

    /**
     * 初始化设备监控
     * @param syncWithServer 是否与服务器同步设备信息
     */
    private fun initializeDeviceMonitoring(syncWithServer: Boolean = true) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d(
                    "AppViewModel",
                    "initializeDeviceMonitoring: 开始初始化设备监控，syncWithServer=$syncWithServer"
                )
                val localDevice = deviceManager.device
                android.util.Log.d(
                    "AppViewModel",
                    "initializeDeviceMonitoring: 获取到本地设备信息，序列号: ${localDevice.serialNo}, 名称: ${localDevice.name}"
                )

                // 根据syncWithServer参数决定是否进行网络同步
                val deviceLiveData = if (syncWithServer) {
                    deviceRepository?.getAndSyncDevice(localDevice.serialNo)
                } else {
                    deviceRepository?.getDeviceById(localDevice.serialNo)
                }

                // 切换到主线程来设置LiveData观察
                withContext(Dispatchers.Main) {
                    deviceLiveData?.let { liveData: LiveData<DeviceEntity?> ->
                        android.util.Log.d(
                            "AppViewModel",
                            "initializeDeviceMonitoring: 开始监控设备数据变化"
                        )
                        // 将数据库中的设备数据映射到 ViewModel 的 device LiveData
                        device.addSource(liveData) { deviceEntity: DeviceEntity? ->
                            android.util.Log.d(
                                "AppViewModel",
                                "initializeDeviceMonitoring: 设备数据更新，设备序列号: ${deviceEntity?.serialNo}, 设备名称: ${deviceEntity?.name}"
                            )
                            device.value = deviceEntity
                            android.util.Log.d(
                                "AppViewModel",
                                "initializeDeviceMonitoring: device LiveData已更新，设备序列号: ${device.value?.serialNo}, 设备名称: ${device.value?.name}"
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "initializeDeviceMonitoring: 初始化设备监控失败", e)
            }
        }
    }

    /**
     * 手动刷新设备信息
     */
    fun refreshDeviceInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("AppViewModel", "refreshDeviceInfo: 开始刷新设备信息")
                val localDevice = deviceManager.device
                
                // 强制从服务器同步设备信息
                val deviceLiveData = deviceRepository?.getAndSyncDevice(localDevice.serialNo)
                
                // 切换到主线程来更新设备信息
                withContext(Dispatchers.Main) {
                    deviceLiveData?.let { liveData: LiveData<DeviceEntity?> ->
                        android.util.Log.d("AppViewModel", "refreshDeviceInfo: 开始观察设备数据变化")
                        // 观察一次性的数据变化
                        liveData.observeForever { deviceEntity: DeviceEntity? ->
                            android.util.Log.d(
                                "AppViewModel",
                                "refreshDeviceInfo: 设备数据更新，设备序列号: ${deviceEntity?.serialNo}, 设备名称: ${deviceEntity?.name}"
                            )
                            device.value = deviceEntity
                            // 移除观察者，避免重复观察
                            liveData.removeObserver {}
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "refreshDeviceInfo: 刷新设备信息失败", e)
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
        this.user.value = userInfo
    }

    fun setErrorMessage(message: String?) {
        errorMessage.value = message
    }

    fun setWifi(info: Wifi) {
        wifi.value = info
    }


    fun setAvailableTradePlans(tradePlans: MutableList<TradePlan>) {
        availableTradePlans.value = tradePlans
    }

    // Authentication methods
    fun login(email: String?, password: String?) {
        authViewModel.login(email, password)
    }

    fun register(email: String?, password: String?, confirmPassword: String?) {
        authViewModel.register(email, password, confirmPassword)
    }

    fun getErrorMessage(): String? {
        return errorMessage.value
    }

    // Clear authentication data (for logout)
    fun clearAuthentication() {
        setUser(User.empty())
        setErrorMessage(null)
    }


}