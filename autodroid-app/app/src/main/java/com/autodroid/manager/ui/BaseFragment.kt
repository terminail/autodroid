// BaseFragment.java
package com.autodroid.manager.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.autodroid.manager.R
import com.autodroid.manager.viewmodel.AppViewModel

abstract class BaseFragment : Fragment() {
    protected lateinit var viewModel: AppViewModel
    @JvmField
    protected var listener: FragmentListener? = null

    interface FragmentListener {
        fun onFragmentInteraction(data: Bundle?)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentListener) {
            listener = context as FragmentListener
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize ViewModel here if needed
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(this.layoutId, container, false)
        initViews(view)
        setupObservers()
        return view
    }

    protected abstract val layoutId: Int
    protected abstract fun initViews(view: View?)
    protected open fun setupObservers() {
        // 默认空实现，子类可以选择性重写
    }
}