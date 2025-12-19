package com.autodroid.trader.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.MyApplication

/**
 * BaseActivity类提供所有Activity的通用功能
 * - ViewModel自动初始化
 * - 错误消息处理
 * - 通用生命周期管理
 */
abstract class BaseActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "BaseActivity"
    }
    
    // 通用的ViewModel
    protected open lateinit var appViewModel: AppViewModel
    
    // 通用的错误显示组件
    protected open lateinit var errorTextView: TextView
    
    /**
     * 获取布局资源ID，子类必须实现
     */
    protected abstract fun getLayoutId(): Int
    
    /**
     * 初始化UI组件，子类必须实现
     */
    protected abstract fun initViews()
    
    /**
     * 设置观察者，子类必须实现
     */
    protected abstract fun setupObservers()
    
    /**
     * 设置点击监听器，子类必须实现
     */
    protected abstract fun setupClickListeners()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置布局
        setContentView(getLayoutId())
        
        // 初始化ViewModel
        initializeViewModel()
        
        // 初始化UI组件
        initViews()
        
        // 设置观察者
        setupObservers()
        
        // 设置点击监听器
        setupClickListeners()
        
        // 调用额外的设置（如果需要）
        onActivitySetupComplete()
        
        Log.d(TAG, "${this::class.java.simpleName} created")
    }
    
    /**
     * 初始化ViewModel
     */
    protected open fun initializeViewModel() {
        appViewModel = (application as MyApplication).getAppViewModel()
    }
    
    protected open fun onActivitySetupComplete() {
        // 可以被子类重写用于基本初始化后的额外设置
    }
    
    protected open fun setupClickListenersImpl() {
        // 这个方法可以被子类重写用于特定的点击监听器设置
    }
    
    /**
     * 显示错误消息
     */
    protected fun showError(message: String) {
        if (::errorTextView.isInitialized) {
            errorTextView.text = message
            errorTextView.visibility = View.VISIBLE
        }
    }
    
    /**
     * 隐藏错误消息
     */
    protected fun hideError() {
        if (::errorTextView.isInitialized) {
            errorTextView.visibility = View.GONE
        }
    }
    
    /**
     * 观察错误消息
     */
    protected fun observeErrorMessages() {
        appViewModel.errorMessage.observe(this, Observer { errorMessage: String? ->
            if (!errorMessage.isNullOrEmpty()) {
                showError(errorMessage)
            } else {
                hideError()
            }
        })
    }
}