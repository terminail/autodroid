// MyFragment.java
package com.autodroid.trader.ui.my

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.R
import com.autodroid.trader.ui.BaseFragment
import com.autodroid.trader.ui.my.MyAdapter
import com.autodroid.trader.ui.my.MyItem
import com.google.android.material.appbar.AppBarLayout

class MyFragment : BaseFragment() {
    private var myRecyclerView: RecyclerView? = null
    private var myAdapter: MyAdapter? = null
    
    // Pull-down detection for fragment header
    private var appBarLayout: AppBarLayout? = null
    private var touchStartY = 0f
    private var isPullingDown = false
    private var touchSlop = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_my
    }

    override fun initViews(view: View) {
        myRecyclerView = view.findViewById(R.id.my_recycler_view)
        
        // Find AppBarLayout
        appBarLayout = view.findViewById(R.id.app_bar_layout)
        
        // Initialize touch slop for pull-down detection
        touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        
        // Set up RecyclerView with LinearLayoutManager
        myRecyclerView?.layoutManager = LinearLayoutManager(context)
        
        // Set up touch listener for pull-down detection
        myRecyclerView?.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }
        
        // Initialize MyAdapter
        myAdapter = MyAdapter()
        myAdapter?.setOnItemClickListener(object : MyAdapter.OnItemClickListener {
            override fun onLoginClick() {
                // Handle login click
                handleLogin()
            }
            
            override fun onLogoutClick() {
                // Handle logout click
                handleLogout()
            }
            
            override fun onUserQRCodeClick() {
                // Handle user QR code click
                handleUserQRCode()
            }
        })
        
        myRecyclerView?.adapter = myAdapter
        
        // Initialize with default items
        updateMyItems()
    }

    override fun setupObservers() {
        // Observe authentication state from AppViewModel
        appViewModel.user.observe(viewLifecycleOwner) { user ->
            updateMyItems()
        }
    }
    
    private fun updateMyItems() {
        val items = mutableListOf<MyItem>()
        
        // Add items based on login status from AppViewModel
        val user = appViewModel.user.value
        val isLoggedIn = user?.isAuthenticated ?: false
        
        if (isLoggedIn) {
            items.add(MyItem.ItemUserQRCode())
            items.add(MyItem.ItemLogout())
        } else {
            items.add(MyItem.ItemLogin())
        }
        
        myAdapter?.updateItems(items)
    }
    
    private fun handleLogin() {
        // Start LoginActivity
        val intent = android.content.Intent(activity, com.autodroid.trader.auth.activity.LoginActivity::class.java)
        startActivity(intent)
    }
    
    private fun handleLogout() {
        // Clear authentication state
        appViewModel.clearAuthentication()
        // UI will be automatically updated by the observer
    }
    
    private fun handleUserQRCode() {
        // TODO: Implement QR code display logic
    }
    
    /**
     * Handle touch events for pull-down detection
     */
    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartY = event.y
                isPullingDown = false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.y - touchStartY
                
                // Check if pulling down at the top of the list
                if (deltaY > touchSlop && !isPullingDown) {
                    val layoutManager = myRecyclerView?.layoutManager as? LinearLayoutManager
                    if (layoutManager?.findFirstVisibleItemPosition() == 0) {
                        // At the top of the list and pulling down
                        isPullingDown = true
                        appBarLayout?.visibility = VISIBLE
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPullingDown = false
            }
        }
        return false
    }
}