// MyFragment.java
package com.autodroid.proxy.ui.my

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.autodroid.proxy.R
import com.autodroid.proxy.ui.BaseFragment
import com.autodroid.proxy.viewmodel.AppViewModel

class MyFragment : BaseFragment() {
    private var myTitleTextView: TextView? = null
    private var logoutButton: Button? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel =
            ViewModelProvider(requireActivity()).get<AppViewModel>(AppViewModel::class.java)
    }

    override val layoutId: Int
        get() = R.layout.fragment_my

    override fun initViews(view: View?) {
        myTitleTextView = view?.findViewById<TextView?>(R.id.my_title)
        logoutButton = view?.findViewById<Button>(R.id.logout_button)

        logoutButton!!.setOnClickListener(View.OnClickListener { v: View? ->
            // Handle logout
            if (listener != null) {
                val data = Bundle()
                data.putString("action", "logout")
                listener!!.onFragmentInteraction(data)
            }
        })
    }

    override fun setupObservers() {
        // Setup observers for my data
    }
}