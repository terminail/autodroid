package com.autodroid.aas.ui

import com.autodroid.aas.R

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import android.view.View
import com.autodroid.aas.ui.SettingsActivity

class FloatingControlManager(private val context: Context) {
    
    companion object {
        const val TAG = "FloatingControl"
    }
    
    private var floatingView: View? = null
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    
    fun showFloatingControl() {
        if (floatingView != null) return
        
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // 创建浮动窗口参数
        layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.END
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            x = 0
            y = 100
        }
        
        // 创建浮动视图
        floatingView = LayoutInflater.from(context).inflate(R.layout.floating_control, null).apply {
            // 设置点击事件
            findViewById<Button>(R.id.btn_record).setOnClickListener {
                toggleRecording()
            }
            
            findViewById<Button>(R.id.btn_settings).setOnClickListener {
                openSettings()
            }
            
            findViewById<Button>(R.id.btn_close).setOnClickListener {
                hideFloatingControl()
            }
            
            // 拖动功能
            setOnTouchListener(createTouchListener())
        }
        
        windowManager?.addView(floatingView, layoutParams)
    }
    
    fun hideFloatingControl() {
        floatingView?.let {
            windowManager?.removeView(it)
            floatingView = null
        }
    }
    
    private fun toggleRecording() {
        // 实现录制开关逻辑
        val intent = Intent("com.autodroid.aas.TOGGLE_RECORDING")
        context.sendBroadcast(intent)
        Toast.makeText(context, "Recording toggled", Toast.LENGTH_SHORT).show()
    }
    
    private fun openSettings() {
        val intent = Intent(context, SettingsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    private fun createTouchListener(): View.OnTouchListener {
        return object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams?.x ?: 0
                        initialY = layoutParams?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, layoutParams)
                        return true
                    }
                }
                return false
            }
        }
    }
}