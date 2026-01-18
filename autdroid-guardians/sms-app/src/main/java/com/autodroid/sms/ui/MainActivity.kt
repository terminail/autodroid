package com.autodroid.sms.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.sms.R
import com.autodroid.sms.SmsApplication
import com.autodroid.sms.data.model.Conversation
import com.autodroid.sms.ui.adapter.ConversationAdapter
import com.autodroid.sms.ui.viewmodel.ConversationViewModel
import com.autodroid.sms.ui.viewmodel.ConversationViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabNewMessage: FloatingActionButton
    private lateinit var conversationAdapter: ConversationAdapter
    
    private val viewModel: ConversationViewModel by viewModels {
        ConversationViewModelFactory(this)
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initAfterPermissions()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    private lateinit var smsObserver: ContentObserver
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        registerSmsObserver()
        
        if (hasRequiredPermissions()) {
            initAfterPermissions()
        } else {
            requestRequiredPermissions()
        }
    }
    
    private fun registerSmsObserver() {
        smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                viewModel.refreshConversations()
            }
        }
        
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            smsObserver
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(smsObserver)
    }
    
    private fun hasRequiredPermissions(): Boolean {
        val requiredPermissions = getRequiredPermissions()
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要通知权限
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_CONTACTS
            )
        } else {
            // 旧版本Android
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_CONTACTS
            )
        }
    }
    
    private fun requestRequiredPermissions() {
        permissionLauncher.launch(getRequiredPermissions())
    }
    
    private fun initAfterPermissions() {
        initViews()
        setupRecyclerView()
        setupObservers()
        
        // 检查是否为默认短信应用
        if (!SmsApplication.instance.isDefaultSmsApp()) {
            showDefaultSmsAppDialog()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                // 打开设置界面
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.menu_default_sms -> {
                // 请求成为默认短信应用
                SmsApplication.instance.requestDefaultSmsApp(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        fabNewMessage = findViewById(R.id.fab_new_message)
        
        fabNewMessage.setOnClickListener {
            // 打开新建消息界面
            val intent = Intent(this, ComposeActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter { conversation ->
            // 点击会话，打开聊天界面
            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra("thread_id", conversation.threadId)
                putExtra("address", conversation.address)
                putExtra("contact_name", conversation.contactName)
            }
            startActivity(intent)
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = conversationAdapter
        }
    }
    
    private fun setupObservers() {
        viewModel.conversations.observe(this) { conversations ->
            conversationAdapter.submitList(conversations)
        }
        
        viewModel.unreadCount.observe(this) { unreadCount ->
            // 更新未读消息数显示
            supportActionBar?.subtitle = if (unreadCount > 0) {
                "$unreadCount 条未读消息"
            } else {
                "短信"
            }
        }
    }
    
    private fun showDefaultSmsAppDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("设为默认短信应用")
            .setMessage("要使用完整的短信功能，请将本应用设为默认短信应用。")
            .setPositiveButton("立即设置") { _, _ ->
                SmsApplication.instance.requestDefaultSmsApp(this)
            }
            .setNegativeButton("稍后设置", null)
            .show()
    }
    
    private fun showPermissionDeniedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("权限被拒绝")
            .setMessage("短信应用需要短信和联系人权限才能正常工作。请前往设置中授予权限。")
            .setPositiveButton("前往设置") { _, _ ->
                // 打开应用设置页面
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        // 刷新会话列表
        viewModel.refreshConversations()
    }
}