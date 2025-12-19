package com.autodroid.trader.ui.floatwindow

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.autodroid.trader.R

class FloatWindowService : Service() {
    
    companion object {
        private const val TAG = "FloatWindowService"
        const val ACTION_SHOW = "com.autodroid.trader.action.SHOW_FLOAT_WINDOW"
        const val ACTION_HIDE = "com.autodroid.trader.action.HIDE_FLOAT_WINDOW"
    }
    
    private lateinit var windowManager: WindowManager
    private lateinit var floatView: View
    private lateinit var floatWindowViewModel: FloatWindowViewModel
    
    private var layoutParams: WindowManager.LayoutParams? = null
    private var originalWidth = 0
    private var originalHeight = 0
    private var originalX = 0
    private var originalY = 0
    
    // 触摸事件相关变量
    private var x = 0
    private var y = 0
    private var initialX = 0
    private var initialY = 0
    private var isMoving = false
    private var lastClickTime = 0L
    private var isCollapsed = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatWindowService created")
        
        // 初始化ViewModel
        floatWindowViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(FloatWindowViewModel::class.java)
        
        // 创建悬浮窗
        createFloatWindow()
        
        // 设置数据观察者
        setupDataObserver()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_SHOW -> showFloatWindow()
                ACTION_HIDE -> try {
                    windowManager.removeView(floatView)
                    Log.d(TAG, "Float window hidden")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to hide float window: ${e.message}")
                }
            }
        }
        
        // Android 8.0+ 需要设置前台服务
        startForeground(1, android.app.Notification())

        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        removeFloatWindow()
        Log.d(TAG, "FloatWindowService destroyed")
    }
    
    private fun createFloatWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 加载悬浮窗布局
        floatView = LayoutInflater.from(this).inflate(R.layout.float_window_normal, null)
        
        // 保存原始高度
        originalHeight = 120
        
        // 设置窗口参数
        layoutParams = WindowManager.LayoutParams().apply {
            // 设置窗口类型
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // 设置窗口标志
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            
            // 设置窗口格式
            format = PixelFormat.TRANSLUCENT
            
            // 设置窗口大小
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            
            // 设置窗口位置
            gravity = android.view.Gravity.START or android.view.Gravity.TOP
            x = 100
            y = 100
        }
        
        // 设置触摸事件
        setupTouchListener()
    }
    
    /**
     * 设置触摸事件监听器
     */
    private fun setupTouchListener() {
        floatView.setOnTouchListener { _, event ->
            val MOVEMENT_THRESHOLD = 10
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = layoutParams?.x ?: 0
                    y = layoutParams?.y ?: 0
                    initialX = event.rawX.toInt()
                    initialY = event.rawY.toInt()
                    isMoving = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX.toInt() - initialX
                    val dy = event.rawY.toInt() - initialY
                    layoutParams?.x = x + dx
                    layoutParams?.y = y + dy
                    windowManager.updateViewLayout(floatView, layoutParams)
                    // 只有当移动距离超过阈值时才标记为正在移动
                    isMoving = Math.abs(dx) > MOVEMENT_THRESHOLD || Math.abs(dy) > MOVEMENT_THRESHOLD
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // 如果移动距离未超过阈值，触发点击事件
                    if (!isMoving) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < 300) {
                            // 双击事件
                            if (isCollapsed) {
                                // 展开悬浮窗
                                expandFloatWindow()
                            } else {
                                // 收缩悬浮窗到右侧中间
                                collapseFloatWindow()
                            }
                        } else {
                            // 单击事件
                            if (isCollapsed) {
                                // 展开悬浮窗
                                expandFloatWindow()
                            } else {
                                // 点击悬浮窗返回主应用
                                val intent = packageManager.getLaunchIntentForPackage(packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }
                        }
                        lastClickTime = currentTime
                    }
                    true
                }
                else -> false
            }
        }
    }
    

    
    private fun showFloatWindow() {
        try {
            if (floatView.parent == null) {
                windowManager.addView(floatView, layoutParams)
                Log.d(TAG, "Float window shown")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show float window: ${e.message}")
        }
    }
    

    
    private fun removeFloatWindow() {
        try {
            windowManager.removeView(floatView)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove float window: ${e.message}")
        }
    }
    
    /**
     * 收缩悬浮窗到右侧中间位置
     */
    private fun collapseFloatWindow() {
        // 保存当前位置
        originalX = layoutParams?.x ?: 0
        originalY = layoutParams?.y ?: 0
        
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // 计算右侧中间位置
        val centerY = screenHeight / 2 - (originalHeight / 2)
        
        // 移除当前视图
        windowManager.removeView(floatView)
        
        // 加载收缩布局
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatView = inflater.inflate(R.layout.float_window_collapse, null)
        
        // 更新窗口参数
        layoutParams?.width = 48
        layoutParams?.height = 300
        layoutParams?.x = screenWidth - 48
        layoutParams?.y = centerY
        
        // 重新设置触摸事件，支持点击展开
        setupTouchListener()
        
        // 添加新视图
        windowManager.addView(floatView, layoutParams)
        
        isCollapsed = true
        Log.d(TAG, "Float window collapsed to right middle as vertical strip")
    }
    
    /**
     * 展开悬浮窗
     */
    private fun expandFloatWindow() {
        // 移除当前视图
        windowManager.removeView(floatView)
        
        // 加载正常布局
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatView = inflater.inflate(R.layout.float_window_normal, null)
        
        // 恢复原始大小
        layoutParams?.width = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams?.x = originalX
        layoutParams?.y = originalY
        
        // 添加新视图
        windowManager.addView(floatView, layoutParams)
        
        // 重新设置触摸事件
        setupTouchListener()
        
        // 重新设置数据观察
        setupDataObserver()
        
        isCollapsed = false
        Log.d(TAG, "Float window expanded")
    }
    
    private fun setupDataObserver() {
        // 观察实时数据更新
        floatWindowViewModel.latestPrice.observeForever { price ->
            floatView.findViewById<TextView>(R.id.tv_price)?.text = price
        }
        
        floatWindowViewModel.priceChange.observeForever { change ->
            floatView.findViewById<TextView>(R.id.tv_change)?.text = change
        }
        
        floatWindowViewModel.tradeVolume.observeForever { volume ->
            floatView.findViewById<TextView>(R.id.tv_volume)?.text = volume
        }
    }
}