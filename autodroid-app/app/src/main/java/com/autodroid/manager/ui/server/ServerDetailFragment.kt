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
import com.autodroid.manager.service.DiscoveryStatusManager
import com.autodroid.data.repository.ServerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class ServerDetailFragment : BaseFragment() {
    // UI Components
    private var serverNameTextView: TextView? = null
    private var apiEndpointTextView: TextView? = null
    private var connectButton: Button? = null

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

        // Update UI with server info
        updateServerInfoUI()


    }

    override fun setupObservers() {
        // Observe the server from AppViewModel to ensure consistent state
        val viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)
        viewModel.server.observe(viewLifecycleOwner) { server ->
            if (server != null) {
                val connected = server.connected
                val currentApiEndpoint = server.apiEndpoint
                
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


    
    private fun extractApiEndpointKey(apiEndpoint: String): String? {
        return try {
            // Parse API endpoint to extract host and port
            val url = java.net.URL(apiEndpoint)
            val host = url.host
            val port = if (url.port != -1) url.port else url.defaultPort
            "$host:$port"
        } catch (e: Exception) {
            null
        }
    }

    private fun disconnectFromServer() {
        // Disconnect from current server by updating connection status
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverRepository = ServerRepository.getInstance(requireContext().applicationContext as android.app.Application)
                // 断开所有服务器的连接状态
                serverRepository.getAllServers().value?.forEach { serverEntity ->
                    serverRepository.updateServerInfo(serverEntity.apiEndpoint, serverEntity.version)
                }
                Toast.makeText(context, "Disconnected from server", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("ServerDetailFragment", "Error disconnecting from server: ${e.message}", e)
            }
        }
    }

    private fun updateConnectionButtons(isConnected: Boolean) {
        connectButton?.isEnabled = !isConnected
    }
}