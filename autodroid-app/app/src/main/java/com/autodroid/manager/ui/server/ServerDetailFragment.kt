package com.autodroid.manager.ui.server

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.autodroid.manager.R
import com.autodroid.manager.ui.BaseFragment
import com.autodroid.manager.viewmodel.AppViewModel

class ServerDetailFragment : BaseFragment() {
    // UI Components
    private var serverNameTextView: TextView? = null
    private var serverIpTextView: TextView? = null
    private var serverPortTextView: TextView? = null
    private var connectButton: Button? = null
    private var disconnectButton: Button? = null

    private var serverIp: String? = null
    private var serverPort: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AppViewModel::class.java]

        // Get server info from arguments
        arguments?.let { args ->
            serverIp = args.getString("serverIp")
            serverPort = args.getInt("serverPort")
        }
    }

    override val layoutId: Int
        get() = R.layout.fragment_server_detail

    override fun initViews(view: View?) {
        serverNameTextView = view?.findViewById(R.id.server_name_text_view)
        serverIpTextView = view?.findViewById(R.id.server_ip_text_view)
        serverPortTextView = view?.findViewById(R.id.server_port_text_view)
        connectButton = view?.findViewById(R.id.connect_button)
        disconnectButton = view?.findViewById(R.id.disconnect_button)

        // Update UI with server info
        updateServerInfoUI()

        // Set up click listeners
        connectButton?.setOnClickListener {
            connectToServer()
        }

        disconnectButton?.setOnClickListener {
            disconnectFromServer()
        }
    }

    override fun setupObservers() {
        viewModel?.let { vm ->
            // Observe the unified serverInfo object to ensure consistent state
            vm.serverInfo.observe(viewLifecycleOwner) { serverInfo ->
                if (serverInfo != null) {
                    val connected = serverInfo["connected"] as? Boolean ?: false
                    val currentIp = serverInfo["ip"]?.toString()
                    val currentPort = serverInfo["port"] as? Int ?: 0
                    
                    // Update connection buttons based on whether this server is the connected one
                    val isThisServerConnected = connected && currentIp == serverIp && currentPort == serverPort
                    updateConnectionButtons(isThisServerConnected)
                } else {
                    updateConnectionButtons(false)
                }
            }
        }
    }

    private fun updateServerInfoUI() {
        serverNameTextView?.text = "Autodroid Server"
        serverIpTextView?.text = "IP: $serverIp"
        serverPortTextView?.text = "Port: $serverPort"
    }

    private fun connectToServer() {
        if (serverIp.isNullOrEmpty() || serverPort <= 0) {
            Toast.makeText(context, "Invalid server information", Toast.LENGTH_SHORT).show()
            return
        }

        // Set this server as the current connected server using unified serverInfo
        val serverInfo = mutableMapOf<String?, Any?>()
        serverInfo["name"] = "Autodroid Server"
        serverInfo["ip"] = serverIp!!
        serverInfo["port"] = serverPort
        serverInfo["connected"] = true
        
        viewModel?.setServerInfo(serverInfo)

        Toast.makeText(context, "Connected to server $serverIp:$serverPort", Toast.LENGTH_SHORT).show()
    }

    private fun disconnectFromServer() {
        // Disconnect from current server by clearing serverInfo
        viewModel?.setServerInfo(null)
        Toast.makeText(context, "Disconnected from server", Toast.LENGTH_SHORT).show()
    }

    private fun updateConnectionButtons(isConnected: Boolean) {
        connectButton?.isEnabled = !isConnected
        disconnectButton?.isEnabled = isConnected
    }
}