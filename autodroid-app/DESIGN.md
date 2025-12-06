class DashboardFragment : BaseFragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化服务器连接状态观察
        setupServerConnectionObservers()
        
        // 初始化mDNS发现状态观察
        setupDiscoveryObservers()
        
        // 设置UI事件监听
        setupUIListeners()
    }
    
    private fun setupServerConnectionObservers() {
        // 观察服务器连接状态
        DiscoveryStatusManager.serverInfo.observe(viewLifecycleOwner) { serverInfo ->
            updateConnectionStatusUI(serverInfo)
        }
        
        // 观察连接状态
        DiscoveryStatusManager.connected.observe(viewLifecycleOwner) { connected ->
            if (connected == true) {
                // 服务器连接成功，可以显示登录相关选项
                showAuthenticationOptions()
            } else {
                // 服务器未连接，专注于发现流程
                showDiscoveryOptions()
            }
        }
    }
    
    private fun setupDiscoveryObservers() {
        // 观察mDNS发现状态
        DiscoveryStatusManager.discoveryInProgress.observe(viewLifecycleOwner) { inProgress ->
            updateDiscoveryStatusUI(inProgress)
        }
        
        // 观察重试次数
        DiscoveryStatusManager.discoveryRetryCount.observe(viewLifecycleOwner) { retryCount ->
            updateRetryStatusUI(retryCount)
        }
    }
    
    private fun setupUIListeners() {
        // 扫描二维码按钮点击事件
        scanQrButton.setOnClickListener {
            // 启动二维码扫描作为mDNS发现的回退方案
            startQrCodeScan()
        }
        
        // 手动输入服务器地址
        manualInputButton.setOnClickListener {
            // 显示手动输入对话框
            showManualInputDialog()
        }
    }
    
    private fun updateConnectionStatusUI(serverInfo: MutableMap<String?, Any?>?) {
        val isConnected = serverInfo?.get("connected") as? Boolean ?: false
        
        if (isConnected) {
            val host = serverInfo["ip"] as? String
            val port = serverInfo["port"] as? Int
            connectionStatusTextView.text = "已连接服务器: $host:$port"
            connectionStatusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
        } else {
            connectionStatusTextView.text = "正在发现服务器..."
            connectionStatusTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning))
        }
    }
}

class LoginActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置布局
        setContentView(R.layout.activity_login)
        
        // 移除对DiscoveryStatus的直接依赖
        // LoginActivity只负责登录功能，不处理服务器连接状态
        
        setupLoginForm()
        setupNavigation()
    }
    
    private fun setupLoginForm() {
        // 设置登录表单和验证逻辑
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            
            if (validateCredentials(username, password)) {
                performLogin(username, password)
            }
        }
    }
    
    private fun setupNavigation() {
        // 设置返回按钮
        backButton.setOnClickListener {
            // 返回Dashboard
            finish()
        }
        
        // 注册链接
        registerLink.setOnClickListener {
            // 跳转到注册界面
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun validateCredentials(username: String, password: String): Boolean {
        // 简单的客户端验证
        return username.isNotBlank() && password.length >= 6
    }
    
    private fun performLogin(username: String, password: String) {
        // 执行登录API调用
        // 成功后保存认证状态并返回MainActivity
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("is_authenticated", true).apply()
        
        // 返回MainActivity，此时会显示已认证的Dashboard
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}

object DiscoveryStatusManager {
    
    // 服务器连接状态（仅包含连接相关信息）
    val serverInfo: MutableLiveData<MutableMap<String?, Any?>?> = MutableLiveData()
    val connected: MutableLiveData<Boolean?> = MutableLiveData()
    
    // mDNS发现状态
    val discoveryInProgress: MutableLiveData<Boolean?> = MutableLiveData()
    val discoveryRetryCount: MutableLiveData<Int?> = MutableLiveData()
    val discoveryMaxRetries: MutableLiveData<Int?> = MutableLiveData(3)
    
    // 网络服务状态
    val isServiceRunning: MutableLiveData<Boolean?> = MutableLiveData()
    
    fun init(context: Context) {
        // 初始化状态管理器
        // 不自动启动网络服务，由MyApplication统一管理
    }
    
    fun startNetworkService() {
        // 启动网络服务（mDNS发现）
        // 仅由MyApplication调用
    }
    
    fun updateServerInfo(discoveredServer: DiscoveredServer?) {
        // 更新服务器信息
        val newServerInfo = mutableMapOf<String?, Any?>()
        
        discoveredServer?.let { server ->
            newServerInfo["ip"] = server.host
            newServerInfo["port"] = server.port
            newServerInfo["connected"] = true
            connected.postValue(true)
        } ?: run {
            newServerInfo["connected"] = false
            connected.postValue(false)
        }
        
        serverInfo.postValue(newServerInfo)
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置布局
        setContentView(R.layout.activity_main)
        
        // 设置底部导航
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        NavigationUI.setupWithNavController(bottomNavigation, navController)
    }
    
    private fun checkServerConnectionAndNavigation() {
        val serverInfo = DiscoveryStatusManager.serverInfo.value
        val isConnected = serverInfo?.get("connected") as? Boolean ?: false
        
        if (isConnected) {
            // 服务器已连接，保持在Dashboard页面
            // 用户可以在Dashboard自由停留，查看服务器状态
            // 只有当用户尝试访问受保护的页面时，才检查认证状态
            navController.navigate(R.id.dashboardFragment)
            
            // 更新导航项状态：启用受保护的页面
            updateNavigationItemsState(true)
        } else {
            // 服务器未连接，保持在Dashboard进行发现
            navController.navigate(R.id.dashboardFragment)
            
            // 更新导航项状态：禁用受保护的页面
            updateNavigationItemsState(false)
        }
    }
    
    private fun updateNavigationItemsState(isServerConnected: Boolean) {
        val menu = bottomNavigation.menu
        
        // Dashboard始终启用
        menu.findItem(R.id.nav_dashboard).isEnabled = true
        
        // 其他受保护的页面根据服务器连接状态启用/禁用
        menu.findItem(R.id.nav_workflows).isEnabled = isServerConnected
        menu.findItem(R.id.nav_reports).isEnabled = isServerConnected
        menu.findItem(R.id.nav_orders).isEnabled = isServerConnected
        menu.findItem(R.id.nav_my).isEnabled = isServerConnected
        
        // 更新标题显示连接状态
        if (!isServerConnected) {
            menu.findItem(R.id.nav_workflows).title = "Workflows (需要连接)"
            menu.findItem(R.id.nav_reports).title = "Reports (需要连接)"
            menu.findItem(R.id.nav_orders).title = "Orders (需要连接)"
            menu.findItem(R.id.nav_my).title = "My (需要连接)"
        } else {
            menu.findItem(R.id.nav_workflows).title = "Workflows"
            menu.findItem(R.id.nav_reports).title = "Reports"
            menu.findItem(R.id.nav_orders).title = "Orders"
            menu.findItem(R.id.nav_my).title = "My"
        }
    }
    
    private fun setupNavigationInterception() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Dashboard始终允许导航
                    NavigationUI.onNavDestinationSelected(item, navController)
                    true
                }
                else -> {
                    // 受保护的页面：检查服务器连接和认证状态
                    val serverInfo = DiscoveryStatusManager.serverInfo.value
                    val isConnected = serverInfo?.get("connected") as? Boolean ?: false
                    
                    if (!isConnected) {
                        // 服务器未连接，显示提示并阻止导航
                        Toast.makeText(this, "请先连接服务器", Toast.LENGTH_SHORT).show()
                        false
                    } else if (!checkAuthenticationStatus()) {
                        // 服务器已连接但未认证，跳转到登录页面
                        navigateToLoginActivity()
                        false
                    } else {
                        // 已连接且已认证，允许导航
                        NavigationUI.onNavDestinationSelected(item, navController)
                        true
                    }
                }
            }
        }
    }
    
    private fun checkAuthenticationStatus(): Boolean {
        // 检查用户是否已认证
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_authenticated", false)
    }
}

class DiscoveredServer {
    private String name;
    private String host;
    private int port;
    private long discoveredTime;
    
    public DiscoveredServer(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.discoveredTime = System.currentTimeMillis();
    }
    
    // Getters and setters
    public String getName() { return name; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public long getDiscoveredTime() { return discoveredTime; }
}
