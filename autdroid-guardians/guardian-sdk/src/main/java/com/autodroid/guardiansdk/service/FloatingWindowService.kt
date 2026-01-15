package com.autodroid.guardiansdk.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import com.autodroid.guardiansdk.R

/**
 * 浮动窗口服务
 * 显示浮动报警按钮，支持长按触发报警
 */
class FloatingWindowService : Service() {
    
    companion object {
        private const val TAG = "FloatingWindowService"
    }
    
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var floatingButton: ImageView? = null
    
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var isLongPress = false
    private var longPressRunnable: Runnable? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "浮动窗口服务已创建")
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        initializeFloatingWindow()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "浮动窗口服务已启动")
        showFloatingWindow()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "浮动窗口服务已销毁")
        hideFloatingWindow()
    }
    
    private fun initializeFloatingWindow() {
        Log.d(TAG, "浮动窗口初始化完成")
    }
    
    private fun showFloatingWindow() {
        if (floatingView != null) {
            Log.d(TAG, "浮动窗口已显示，跳过")
            return
        }
        
        try {
            floatingView = LayoutInflater.from(this).inflate(R.layout.guardian_layout_floating_window, null)
            floatingButton = floatingView?.findViewById(R.id.floatingButton)
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            
            params.gravity = Gravity.TOP or Gravity.START
            params.x = 100
            params.y = 200
            
            windowManager.addView(floatingView, params)
            
            floatingButton?.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isLongPress = false
                        
                        longPressRunnable = Runnable {
                            isLongPress = true
                            Toast.makeText(this, "长按触发报警！", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "长按触发报警")
                        }
                        floatingButton?.postDelayed(longPressRunnable, 1000)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!isLongPress) {
                            val deltaX = event.rawX - initialTouchX
                            val deltaY = event.rawY - initialTouchY
                            params.x = initialX + deltaX.toInt()
                            params.y = initialY + deltaY.toInt()
                            windowManager.updateViewLayout(floatingView, params)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        longPressRunnable?.let { floatingButton?.removeCallbacks(it) }
                        
                        if (!isLongPress) {
                            val deltaX = event.rawX - initialTouchX
                            val deltaY = event.rawY - initialTouchY
                            
                            if (Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {
                                Toast.makeText(this, "点击浮动按钮", Toast.LENGTH_SHORT).show()
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            
            Log.d(TAG, "浮动窗口已显示")
        } catch (e: Exception) {
            Log.e(TAG, "显示浮动窗口失败", e)
        }
    }
    
    private fun hideFloatingWindow() {
        try {
            floatingView?.let {
                windowManager.removeView(it)
                floatingView = null
                floatingButton = null
                Log.d(TAG, "浮动窗口已隐藏")
            }
        } catch (e: Exception) {
            Log.e(TAG, "隐藏浮动窗口失败", e)
        }
    }
}
