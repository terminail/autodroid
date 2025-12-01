// DashboardFragment.java
package com.autodroid.manager.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.manager.R
import com.autodroid.manager.managers.APKScannerManager
import com.autodroid.manager.managers.DeviceInfoManager
import com.autodroid.manager.managers.WorkflowManager
import com.autodroid.manager.ui.BaseFragment
import com.autodroid.manager.ui.adapters.BaseItemAdapter
import com.autodroid.manager.viewmodel.AppViewModel

class DashboardFragment : BaseFragment() {
    // UI Components
    private var connectionStatusTextView: TextView? = null
    private var deviceInfoTextView: TextView? = null
    private var serverIpTextView: TextView? = null
    private var serverStatusTextView: TextView? = null
    private var scanApksButton: Button? = null
    private var dashboardItemsRecyclerView: RecyclerView? = null

    // Managers
    private var deviceInfoManager: DeviceInfoManager? = null
    private var workflowManager: WorkflowManager? = null
    private var apkScannerManager: APKScannerManager? = null

    private var adapter: BaseItemAdapter? = null
    private var dashboardItems: MutableList<MutableMap<String?, Any?>?>? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel =
            ViewModelProvider(requireActivity()).get<AppViewModel>(AppViewModel::class.java)


        // Initialize managers
        deviceInfoManager = DeviceInfoManager(requireContext(), viewModel!!)
        workflowManager = WorkflowManager(requireContext(), viewModel!!)
        apkScannerManager = APKScannerManager(requireContext())

        dashboardItems = ArrayList<MutableMap<String?, Any?>?>()
        setupMockData()
    }

    override val layoutId: Int
        get() = R.layout.fragment_dashboard

    override fun initViews(view: View?) {
        connectionStatusTextView = view?.findViewById<TextView?>(R.id.connection_status)
        deviceInfoTextView = view?.findViewById<TextView?>(R.id.device_info)
        serverIpTextView = view?.findViewById<TextView?>(R.id.server_ip)
        serverStatusTextView = view?.findViewById<TextView?>(R.id.server_status)
        scanApksButton = view?.findViewById<Button>(R.id.scan_apks_button)
        dashboardItemsRecyclerView =
            view?.findViewById<RecyclerView>(R.id.dashboard_items_recycler_view)


        // Set up RecyclerView
        dashboardItemsRecyclerView!!.setLayoutManager(LinearLayoutManager(getContext()))
        adapter = BaseItemAdapter(
            dashboardItems,
            BaseItemAdapter.OnItemClickListener { item: MutableMap<String?, Any?>? ->
                // Handle item click
                handleItemClick(item!!)
            },
            R.layout.item_generic
        )
        dashboardItemsRecyclerView!!.setAdapter(adapter)


        // Set up click listeners
        scanApksButton!!.setOnClickListener(View.OnClickListener { v: View? ->
            apkScannerManager!!.scanInstalledApks()
        })


        // Update UI with initial data
        updateUI()
    }

    override fun setupObservers() {
        if (viewModel != null) {
            viewModel!!.serverIp.observe(getViewLifecycleOwner(), Observer { ip: String? ->
                if (serverIpTextView != null) {
                    serverIpTextView!!.setText("Server IP: " + ip)
                }
            })

            viewModel!!.serverConnected
                .observe(getViewLifecycleOwner(), Observer { connected: Boolean? ->
                    if (serverStatusTextView != null) {
                        serverStatusTextView!!.setText("Server Status: " + (if (connected == true) "Connected" else "Disconnected"))
                    }
                })
        }
    }

    private fun setupMockData() {
        // Add mock dashboard items
        val item1: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        item1.put("title", "Active Workflows")
        item1.put("subtitle", "5 workflows currently running")
        item1.put("status", "5")
        dashboardItems!!.add(item1)

        val item2: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        item2.put("title", "Test Reports")
        item2.put("subtitle", "12 reports generated today")
        item2.put("status", "12")
        dashboardItems!!.add(item2)

        val item3: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        item3.put("title", "Test Orders")
        item3.put("subtitle", "3 orders pending execution")
        item3.put("status", "3")
        dashboardItems!!.add(item3)
    }

    private fun handleItemClick(item: MutableMap<String?, Any?>) {
        // Handle dashboard item click
        if (getActivity() != null) {
            Toast.makeText(
                getActivity(),
                "Dashboard item clicked: " + item.get("title"),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateUI() {
        if (deviceInfoTextView != null) {
            deviceInfoTextView!!.setText(deviceInfoManager!!.deviceInfo)
        }
        updateConnectionStatus("Disconnected")
    }

    private fun updateConnectionStatus(status: String?) {
        if (connectionStatusTextView != null) {
            connectionStatusTextView!!.setText("Connection Status: " + status)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}