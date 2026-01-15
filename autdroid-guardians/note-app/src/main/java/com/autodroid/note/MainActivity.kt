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
            .setPositiveButton("去设置") { _, _ ->
                openOverlayPermissionSettings()
            }
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show()
    }

    private fun showAccessibilityServiceDialog() {
        accessibilityServiceDialog?.dismiss()
        accessibilityServiceDialog = AlertDialog.Builder(this)
            .setTitle("需要无障碍服务")
            .setMessage("Guardian SDK 需要无障碍服务权限才能监听按键和屏幕事件。请前往设置开启无障碍服务。")
            .setPositiveButton("去设置") { _, _ ->
                openAccessibilitySettings()
            }
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show()
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
        GuardianSdk.getInstance().openAccessibilitySettings()
        accessibilitySettingsLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
        
        // 检查悬浮窗权限，如果有权限但服务未启动，则启动服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                // 检查无障碍服务
                if (GuardianSdk.getInstance().isAccessibilityServiceEnabled()) {
                    GuardianSdk.getInstance().startFloatingWindowService()
                }
            }
        } else {
            // Android 6.0 以下不需要权限，检查无障碍服务
            if (GuardianSdk.getInstance().isAccessibilityServiceEnabled()) {
                GuardianSdk.getInstance().startFloatingWindowService()
            }
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
