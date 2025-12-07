// BaseFragment.kt
package com.autodroid.manager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.autodroid.manager.AppViewModel
import com.autodroid.manager.MyApplication

/**
 * 统一的Fragment基类，提供标准化的生命周期管理和ViewModel初始化
 */
abstract class BaseFragment : Fragment() {
    protected lateinit var appViewModel: AppViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViewModel()
    }
    
    /**
     * 初始化全局的AppViewModel
     * 子类可以重写此方法来自定义ViewModel初始化逻辑
     */
    protected open fun initializeViewModel() {
        appViewModel = (requireActivity().application as MyApplication).getAppViewModel()
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(getLayoutId(), container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupObservers()
    }
    
    protected abstract fun getLayoutId(): Int
    protected abstract fun initViews(view: View)
    protected abstract fun setupObservers()
}