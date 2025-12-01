// BaseDetailFragment.kt
package com.autodroid.proxy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.autodroid.proxy.R
import com.autodroid.proxy.viewmodel.AppViewModel

abstract class BaseDetailFragment : Fragment() {
    protected lateinit var viewModel: AppViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AppViewModel::class.java]
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