package com.autodroid.note

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.GuardianSdk
import com.autodroid.note.adapter.NoteAdapter
import com.autodroid.note.data.database.NoteDatabase
import com.autodroid.note.data.repository.NoteRepository
import com.autodroid.note.model.Note
import com.autodroid.note.ui.editor.NoteEditorActivity
import android.widget.Button
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var btnOpenSettings: Button
    private lateinit var noteRepository: NoteRepository

    private var overlayPermissionDialog: AlertDialog? = null
    private var accessibilityServiceDialog: AlertDialog? = null

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                // 用户已授权，检查无障碍服务
                checkAndRequestAccessibilityService()
            } else {
                // 用户未授权，再次提示
                showOverlayPermissionDialog()
            }
        }
    }

    private val accessibilitySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // 从无障碍设置返回，检查是否已启用
        if (GuardianSdk.getInstance().isAccessibilityServiceEnabled()) {
            // 无障碍服务已启用，启动浮动窗口服务
            GuardianSdk.getInstance().startFloatingWindowService()
        } else {
            // 用户未启用，再次提示
            showAccessibilityServiceDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 Guardian SDK
        GuardianSdk.initialize(this)

        // 检查并请求悬浮窗权限
        checkAndRequestOverlayPermission()

        // Initialize database and repository
        val database = NoteDatabase.getDatabase(this)
        noteRepository = NoteRepository(database.noteDao())

        setupViews()
        loadNotes()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewNotes)
        fabAddNote = findViewById(R.id.fabAddNote)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)

        noteAdapter = NoteAdapter(
            onNoteClick = { note ->
                // Open note for editing
                val intent = Intent(this, NoteEditorActivity::class.java).apply {
                    putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { note ->
                showDeleteConfirmationDialog(note)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = noteAdapter
        }

        fabAddNote.setOnClickListener {
            // Open editor to create a new note
            val intent = Intent(this, NoteEditorActivity::class.java)
            startActivity(intent)
        }

        btnOpenSettings.setOnClickListener {
            // Open Guardian SDK settings
            GuardianSdk.getInstance().startSettingActivity()
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showOverlayPermissionDialog()
            } else {
                // 已有权限，检查无障碍服务
                checkAndRequestAccessibilityService()
            }
        } else {
            // Android 6.0 以下不需要权限，检查无障碍服务
            checkAndRequestAccessibilityService()
        }
    }

    private fun checkAndRequestAccessibilityService() {
        if (!GuardianSdk.getInstance().isAccessibilityServiceEnabled()) {
            showAccessibilityServiceDialog()
        } else {
            // 无障碍服务已启用，启动浮动窗口服务
            GuardianSdk.getInstance().startFloatingWindowService()
        }
    }

    private fun showOverlayPermissionDialog() {
        overlayPermissionDialog?.dismiss()
        overlayPermissionDialog = AlertDialog.Builder(this)
            .setTitle("需要悬浮窗权限")
            .setMessage("Guardian SDK 需要悬浮窗权限才能显示浮动报警按钮。请前往设置开启权限。")
            .setPositiveButton("去设置") { dialog, _ ->
                dialog.dismiss()
                openOverlayPermissionSettings()
            }
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show()
    }

    private fun showAccessibilityServiceDialog() {
        accessibilityServiceDialog?.dismiss()
        
        // 构建详细的白名单引导信息
        val message = buildString {
            append("Guardian SDK 需要无障碍服务权限才能监听按键和屏幕事件。\n\n")
            append("💡 为了防止服务被系统关闭，建议：\n")
            append("• 开启无障碍服务后，将应用加入系统白名单（自启动/后台运行）\n")
            append("• 关闭电池优化（设置 → 应用 → 特殊应用权限 → 电池优化）\n")
            append("• 允许应用在后台运行\n\n")
            append("请前往设置开启无障碍服务。")
        }
        
        accessibilityServiceDialog = AlertDialog.Builder(this)
            .setTitle("需要无障碍服务")
            .setMessage(message)
            .setPositiveButton("无障碍设置") { dialog, _ ->
                dialog.dismiss()
                openAccessibilitySettings()
            }
            .setNeutralButton("更多设置") { dialog, _ ->
                dialog.dismiss()
                showAdditionalSettingsDialog()
            }
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show()
    }
    
    private fun showAdditionalSettingsDialog() {
        val items = arrayOf("系统白名单（自启动/后台运行）", "电池优化", "应用详情")
        
        AlertDialog.Builder(this)
            .setTitle("其他系统设置")
            .setItems(items) { dialog, which ->
                when (which) {
                    0 -> openSystemWhitelistSettings()
                    1 -> openBatteryOptimizationSettings()
                    2 -> openAppDetailsSettings()
                }
                dialog.dismiss()
            }
            .setNegativeButton("返回", null)
            .show()
    }
    
    private fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            // 如果无法打开电池优化设置，显示提示并引导到应用详情
            AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("无法直接打开电池优化设置，请前往'应用详情' → '电池优化'进行设置。")
                .setPositiveButton("应用详情") { dialog, _ ->
                    dialog.dismiss()
                    openAppDetailsSettings()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
    
    private fun openAppDetailsSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            // 如果无法打开应用详情设置，显示提示
            AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("无法打开应用详情页面，请在系统设置中手动找到本应用进行设置。")
                .setPositiveButton("确定", null)
                .show()
        }
    }
    
    private fun openSystemWhitelistSettings() {
        try {
            // 尝试不同的系统白名单设置路径
            val intent = Intent()
            
            // 方法1：尝试打开自启动管理（华为、小米等）
            try {
                intent.setClassName("com.huawei.systemmanager", 
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                startActivity(intent)
                return
            } catch (e: Exception) {
                // 继续尝试其他方法
            }
            
            // 方法2：尝试小米自启动管理
            try {
                intent.setClassName("com.miui.securitycenter", 
                    "com.miui.permcenter.autostart.AutoStartManagementActivity")
                startActivity(intent)
                return
            } catch (e: Exception) {
                // 继续尝试其他方法
            }
            
            // 方法3：尝试OPPO自启动管理
            try {
                intent.setClassName("com.coloros.safecenter", 
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                startActivity(intent)
                return
            } catch (e: Exception) {
                // 继续尝试其他方法
            }
            
            // 方法4：尝试VIVO自启动管理
            try {
                intent.setClassName("com.vivo.abe", 
                    "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity")
                startActivity(intent)
                return
            } catch (e: Exception) {
                // 继续尝试其他方法
            }
            
            // 方法5：通用方法，打开应用详情页面
            openAppDetailsSettings()
            
        } catch (e: Exception) {
            // 如果所有方法都失败，显示提示并引导到应用详情
            AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("无法直接打开系统白名单设置，请前往'应用详情' → '自启动管理'或'后台运行权限'进行设置。")
                .setPositiveButton("应用详情") { dialog, _ ->
                    dialog.dismiss()
                    openAppDetailsSettings()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun openOverlayPermissionSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        }
        overlayPermissionLauncher.launch(intent)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        accessibilitySettingsLauncher.launch(intent)
    }

    private fun loadNotes() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notes = noteRepository.getAllNotes()
                launch(Dispatchers.Main) {
                    noteAdapter.submitList(notes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showDeleteConfirmationDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("删除笔记")
            .setMessage("确定要删除这条笔记吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteNote(note)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteNote(note: Note) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                noteRepository.deleteNote(note)
                launch(Dispatchers.Main) {
                    loadNotes() // Refresh the list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotes() // Reload notes when returning to main activity
        
        // 重新检查权限和服务状态
        checkPermissionsAndServices()
        
        // 如果无障碍服务已开启，重置提示计数
        if (GuardianSdk.getInstance().isAccessibilityServiceEnabled()) {
            (application as? NoteApplication)?.resetAccessibilityPromptCount()
        }
    }
    
    private fun checkPermissionsAndServices() {
        // 检查悬浮窗权限
        val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true // Android 6.0以下默认有权限
        }
        
        if (!hasOverlayPermission) {
            showOverlayPermissionDialog()
        }
        
        // 检查无障碍服务
        if (!GuardianSdk.getInstance().isAccessibilityServiceEnabled()) {
            showAccessibilityServiceDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                GuardianSdk.getInstance().startSettingActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
