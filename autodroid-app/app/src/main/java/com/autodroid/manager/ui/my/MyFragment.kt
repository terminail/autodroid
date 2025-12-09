// MyFragment.java
package com.autodroid.manager.ui.my

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.manager.R
import com.autodroid.manager.ui.BaseFragment
import com.autodroid.manager.AppViewModel
import com.autodroid.manager.ui.adapters.MyAdapter
import com.autodroid.manager.model.MyItem

class MyFragment : BaseFragment() {
    private var myRecyclerView: RecyclerView? = null
    private var myAdapter: MyAdapter? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_my
    }

    override fun initViews(view: View) {
        myRecyclerView = view.findViewById(R.id.my_recycler_view)
        
        // Set up RecyclerView with LinearLayoutManager
        myRecyclerView?.layoutManager = LinearLayoutManager(context)
        
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
            items.add(MyItem.UserQRCodeItem())
            items.add(MyItem.LogoutItem())
        } else {
            items.add(MyItem.LoginItem())
        }
        
        myAdapter?.updateItems(items)
    }
    
    private fun handleLogin() {
        // Start LoginActivity
        val intent = android.content.Intent(activity, com.autodroid.manager.auth.activity.LoginActivity::class.java)
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
}