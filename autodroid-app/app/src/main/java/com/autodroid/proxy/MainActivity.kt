// MainActivity.java
package com.autodroid.proxy

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.autodroid.proxy.auth.activity.LoginActivity
import com.autodroid.proxy.auth.viewmodel.AuthViewModel
import com.autodroid.proxy.model.DiscoveredServer
import com.autodroid.proxy.service.NetworkService
import com.autodroid.proxy.ui.BaseFragment
import com.autodroid.proxy.ui.dashboard.DashboardFragment
import com.autodroid.proxy.ui.my.MyFragment
import com.autodroid.proxy.ui.orders.OrdersFragment
import com.autodroid.proxy.ui.reports.ReportsFragment
import com.autodroid.proxy.ui.workflows.WorkflowsFragment
import com.autodroid.proxy.viewmodel.AppViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity(), BaseFragment.FragmentListener {
    // ViewModels
    private var viewModel: AppViewModel? = null
    private var authViewModel: AuthViewModel? = null

    // Network service
    private var networkService: NetworkService? = null
    private var isNetworkServiceBound = false

    // UI Components
    private var fragmentContainer: FrameLayout? = null
    private var bottomNavigation: BottomNavigationView? = null

    // Service connection for NetworkService
    private val networkServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder: NetworkService.LocalBinder = service as NetworkService.LocalBinder
            networkService = binder.service
            isNetworkServiceBound = true


            // Set up server discovery callback
            networkService?.setServerDiscoveryCallback(object :
                NetworkService.ServerDiscoveryCallback {
                override fun onServerDiscovered(server: DiscoveredServer?) {
                    runOnUiThread(Runnable {
                        if (viewModel != null) {
                            viewModel!!.setServerIp(server?.host)
                            viewModel!!.setServerConnected(true)
                            Toast.makeText(
                                this@MainActivity,
                                "Server discovered: " + server?.host,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                }

                override fun onServerLost(server: DiscoveredServer?) {
                    runOnUiThread(Runnable {
                        if (viewModel != null) {
                            viewModel!!.setServerConnected(false)
                            Toast.makeText(
                                this@MainActivity,
                                "Server lost: " + server?.host,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                }
            })
        }

        override fun onServiceDisconnected(arg0: ComponentName?) {
            isNetworkServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViewModels()
        checkAuthentication()
        initializeUI()
        startNetworkService()


        // Load default fragment
        loadFragment(WorkflowsFragment())
    }

    private fun initializeViewModels() {
        viewModel = ViewModelProvider(this).get<AppViewModel>(AppViewModel::class.java)
        authViewModel = ViewModelProvider(this).get<AuthViewModel>(AuthViewModel::class.java)
    }

    private fun checkAuthentication() {
        // Check if we have valid authentication data from LoginActivity
        val isAuthenticatedFromIntent = getIntent().getBooleanExtra("isAuthenticated", false)

        if (isAuthenticatedFromIntent) {
            // We came from LoginActivity with authentication - don't redirect back!
            val userId = getIntent().getStringExtra("userId")
            val email = getIntent().getStringExtra("email")
            val token = getIntent().getStringExtra("token")


            // Update the ViewModel with the authentication data
            if (userId != null && email != null && token != null) {
                authViewModel?.setIsAuthenticated(true)
                authViewModel?.setUserId(userId)
                authViewModel?.setEmail(email)
                authViewModel?.setToken(token)
            }
            return  // Stay in MainActivity
        }


        // Only redirect if not authenticated AND we didn't come from LoginActivity
        authViewModel?.isAuthenticated()?.let {
            if (!it) {
                Log.d(TAG, "Not authenticated, redirecting to LoginActivity")
                val intent: Intent = Intent(this, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun initializeUI() {
        fragmentContainer = findViewById<FrameLayout?>(R.id.fragment_container)
        bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigation?.setOnItemSelectedListener(NavigationBarView.OnItemSelectedListener setOnItemSelectedListener@{ item: MenuItem? ->
            var fragment: Fragment? = null
            when (item!!.getItemId()) {
                R.id.nav_workflows -> fragment = WorkflowsFragment()
                R.id.nav_reports -> fragment = ReportsFragment()
                R.id.nav_dashboard -> fragment = DashboardFragment()
                R.id.nav_orders -> fragment = OrdersFragment()
                R.id.nav_my -> fragment = MyFragment()
            }

            if (fragment != null) {
                loadFragment(fragment)
                return@setOnItemSelectedListener true
            }
            false
        })
    }

    private fun loadFragment(fragment: Fragment) {
        val fragmentManager = getSupportFragmentManager()
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

    private fun startNetworkService() {
        val intent: Intent = Intent(this, NetworkService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, networkServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onFragmentInteraction(data: Bundle?) {
        if (data != null) {
            val action: String? = data.getString("action")
            if ("logout" == action) {
                handleLogout()
            }
        }
    }

    private fun handleLogout() {
        // Clear authentication state
        authViewModel.clearAuthentication()


        // Stop network service
        if (isNetworkServiceBound) {
            unbindService(networkServiceConnection)
            isNetworkServiceBound = false
        }


        // Stop the network service
        val intent: Intent = Intent(this, NetworkService::class.java)
        stopService(intent)


        // Navigate to login screen
        val loginIntent: Intent = Intent(this, LoginActivity::class.java)
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(loginIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isNetworkServiceBound) {
            unbindService(networkServiceConnection)
            isNetworkServiceBound = false
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

private fun AuthViewModel?.clearAuthentication() {
    TODO("Not yet implemented")
}
