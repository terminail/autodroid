package com.autodroid.manager.ui.server

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.autodroid.manager.R
import com.autodroid.manager.ui.BaseFragment
import com.autodroid.manager.AppViewModel
import com.autodroid.manager.model.Server

class ServerDetailFragment : BaseFragment() {
    // UI Components
    private var serverNameTextView: TextView? = null
    private var apiEndpointTextView: TextView? = null
    private var connectButton: Button? = null
    private var disconnectButton: Button? = null

    private var apiEndpoint: String? = null

   public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get server info from arguments
        arguments?.let { args ->
            apiEndpoint = args.getString("apiEndpoint")
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_server_detail
    }

    override fun initViews(view: View) {
        serverNameTextView = view?.findViewById(R.id.server_name_text_view)
        apiEndpointTextView = view?.findViewById(R.id.api_endpoint_text_view)
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
        // Observe the unified server object to ensure consistent state
        appViewModel.server.observe(viewLifecycleOwner) { server ->
            if (server != null) {
                val connected = server.connected
                val currentApiEndpoint = server.api_endpoint
                
                // Update connection buttons based on whether this server is the connected one
                val isThisServerConnected = connected && currentApiEndpoint == apiEndpoint
                updateConnectionButtons(isThisServerConnected)
            } else {
                updateConnectionButtons(false)
            }
        }
    }

    private fun updateServerInfoUI() {
        serverNameTextView?.text = "Autodroid Server"
        apiEndpointTextView?.text = "API Endpoint: $apiEndpoint"
    }

    private fun connectToServer() {
        if (apiEndpoint.isNullOrEmpty()) {
            Toast.makeText(context, "Invalid server information", Toast.LENGTH_SHORT).show()
            return
        }

        // Set this server as the current connected server using unified serverInfo
        val serverInfo = Server(
            serviceName = "Autodroid Server",
            name = "Autodroid Server",
            api_endpoint = apiEndpoint!!,
            connected = true
        )
        
        appViewModel.setServer(serverInfo)

        Toast.makeText(context, "Connected to server via API Endpoint", Toast.LENGTH_SHORT).show()
    }

    private fun disconnectFromServer() {
        // Disconnect from current server by clearing serverInfo
        appViewModel.setServer(null)
        Toast.makeText(context, "Disconnected from server", Toast.LENGTH_SHORT).show()
    }

    private fun updateConnectionButtons(isConnected: Boolean) {
        connectButton?.isEnabled = !isConnected
        disconnectButton?.isEnabled = isConnected
    }
}