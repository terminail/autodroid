package com.autodroid.teachitback

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.viewmodel.AppViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: AppViewModel
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var navController: NavController

    companion object {
        private const val REQUEST_STORAGE_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 检查存储权限 - 如果没有权限，显示对话框并阻止继续使用
        if (!hasStoragePermission()) {
            showPermissionRequiredDialog()
            return  // 不继续初始化
        }
        
        // 有权限，正常初始化
        initializeApp()
    }
    
    private fun initializeApp() {
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[AppViewModel::class.java]
        bottomNav = findViewById(R.id.bottom_navigation)
        
        // Setup toolbar
        setupToolbar()

        // AI服务已经在Application中初始化，这里只检查状态
        checkAIServiceStatus()

        // Setup Navigation Component
        setupNavigation()
        
        // 处理测试Intent
        handleTestIntent()
    }
    
    private fun handleTestIntent() {
        if (intent?.action == "com.autodroid.teachitback.TEST_MESSAGE") {
            val message = intent?.getStringExtra("message") ?: "解释一下抛物线"
            val topicId = intent?.getStringExtra("topicId") ?: "default_topic"
            
            android.util.Log.d("MainActivity", "收到测试Intent: message='$message', topicId='$topicId'")
            
            // 导航到ChatFragment并发送消息
            navigateToChatAndSendMessage(message, topicId)
        }
    }
    
    private fun navigateToChatAndSendMessage(message: String, topicId: String) {
        // 使用Bundle传递参数 - 查找高中数学topic
        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val topicDao = database.topicDao()
                
                // 如果是测试topic，查找高中数学的id
                val actualTopicId = if (topicId.startsWith("test_") || topicId == "default_topic") {
                    // 查找高中数学topic
                    val allTopics = topicDao.getAllTopics().first()
                    val mathTopic = allTopics.find { it.title == "高中数学" }
                    if (mathTopic != null) {
                        android.util.Log.d("MainActivity", "找到高中数学topic: id=${mathTopic.id}")
                        mathTopic.id
                    } else {
                        android.util.Log.e("MainActivity", "未找到高中数学topic")
                        topicId // 回退到原始id
                    }
                } else {
                    topicId
                }
                
                val bundle = Bundle().apply {
                    putString("topicId", actualTopicId)
                    putString("topicTitle", "高中数学")
                }
                
                // 导航到ChatFragment
                navController.navigate(R.id.action_nav_topics_to_chat, bundle)
                
                // 延迟发送消息，等待Fragment加载完成
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    sendMessageToChat(message, actualTopicId)
                }, 1000) // 延迟1秒等待Fragment加载
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "获取topic失败: ${e.message}", e)
            }
        }
    }
    
    private fun sendMessageToChat(message: String, topicId: String) {
        // 查找当前Fragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
        
        if (currentFragment is com.autodroid.teachitback.ui.ChatFragment) {
            android.util.Log.d("MainActivity", "向ChatFragment发送消息: '$message'")
            currentFragment.sendMessageDirectly(message)
        } else {
            android.util.Log.e("MainActivity", "当前Fragment不是ChatFragment: ${currentFragment?.javaClass?.simpleName}")
        }
    }
    
    private fun hasStoragePermission(): Boolean {
        val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED
    }
    
    private fun showPermissionRequiredDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("需要存储权限")
            .setMessage("应用需要存储权限来访问本地AI模型文件。没有权限将无法使用应用。\n\n请点击\"确定\"授予存储权限。")
            .setCancelable(false)  // 不允许取消
            .setPositiveButton("确定") { _, _ ->
                requestStoragePermission()
            }
            .setNegativeButton("退出应用") { _, _ ->
                finish()  // 退出应用
            }
            .show()
    }
    
    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            REQUEST_STORAGE_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // 权限授予成功，初始化应用
                android.widget.Toast.makeText(
                    this,
                    "存储权限已授予，正在初始化应用...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
                // 重新创建Activity以初始化应用
                recreate()
            } else {
                // 权限被拒绝，显示错误并退出
                android.app.AlertDialog.Builder(this)
                    .setTitle("权限被拒绝")
                    .setMessage("没有存储权限，应用无法继续使用。请授予存储权限后重新打开应用。")
                    .setCancelable(false)
                    .setPositiveButton("退出") { _, _ ->
                        finish()
                    }
                    .show()
            }
        }
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun checkAIServiceStatus() {
        // AI服务已经在Application中初始化，这里只检查状态
        val status = (application as? TeachItBackApplication)?.getAIServiceStatus() ?: "未知状态"
        android.util.Log.d("MainActivity", "AI服务状态: $status")
        
        // 初始化默认AI服务（从偏好设置）
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val apiKey = sharedPreferences.getString("ai_api_key", "") ?: ""
        val model = sharedPreferences.getString("ai_model", "tinybert_local") ?: "tinybert_local"

        if (apiKey.isNotBlank() || model == "tinybert_local" || model == "chatglm_local") {
            viewModel.initializeAI(apiKey, model)
        }
    }

    private fun setupNavigation() {
        // Find NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup bottom navigation with NavController
        bottomNav.setupWithNavController(navController)
        
        // Configure AppBar with NavController
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_topics, R.id.nav_why, R.id.nav_settings)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Setup back button behavior
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Only show bottom navigation for main fragments: topics, why, settings
            when (destination.id) {
                R.id.nav_topics, R.id.nav_why, R.id.nav_settings -> {
                    bottomNav.visibility = android.view.View.VISIBLE
                    // Hide back button for main fragments
                    showBackButton(false)
                }
                else -> {
                    bottomNav.visibility = android.view.View.GONE
                    // Show back button for other fragments
                    showBackButton(true)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    fun showBackButton(show: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(show)
        if (show) {
            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }
}