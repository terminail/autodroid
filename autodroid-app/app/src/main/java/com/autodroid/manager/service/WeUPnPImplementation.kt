package com.autodroid.manager.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.autodroid.manager.model.Server
import org.bitlet.weupnp.GatewayDevice
import org.bitlet.weupnp.GatewayDiscover
import java.net.InetAddress

/**
 * Third-party mDNS implementation using WeUPnP library
 */
class WeUPnPImplementation(
    private val context: Context
) : MdnsImplementation, DefaultLifecycleObserver {

    private val TAG = "WeUPnPImplementation"
    private var gatewayDevice: GatewayDevice? = null
    private var isDiscovering = false
    
    override fun isAvailable(): Boolean {
        return try {
            // Check if WeUPnP classes are available
            Class.forName("org.bitlet.weupnp.GatewayDevice")
            Class.forName("org.bitlet.weupnp.GatewayDiscover")
            
            // Try to create a GatewayDiscover instance to verify functionality
            val discover = GatewayDiscover()
            true
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "WeUPnP library not available: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "WeUPnP not functional: ${e.message}")
            false
        }
    }

    override fun startDiscovery(callback: MdnsImplementation.Callback) {
        if (isDiscovering) {
            Log.d(TAG, "Discovery already in progress")
            return
        }

        isDiscovering = true
        Log.d(TAG, "Starting UPnP gateway discovery")

        // WeUPnPImplementation uses UPnP protocol which is fundamentally different
        // from mDNS and cannot discover Autodroid services. We'll immediately fail
        // to avoid misleading users with fake service information.
        
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                Log.w(TAG, "WeUPnPImplementation cannot discover Autodroid services - UPnP protocol mismatch")
                
                // Don't even attempt UPnP discovery since it's irrelevant for Autodroid
                // This prevents misleading users with fake service information
                Log.w(TAG, "Skipping UPnP discovery to avoid false positives")
                
                // Immediately fail with clear error message
                Log.w(TAG, "WeUPnP discovery failed - use mDNS implementations instead")
                callback.onDiscoveryFailed()
            } catch (e: Exception) {
                Log.e(TAG, "UPnP discovery error: ${e.message}")
                callback.onDiscoveryFailed()
            } finally {
                isDiscovering = false
            }
        }, 100) // Minimal delay to avoid blocking
    }

    override fun stopDiscovery() {
        if (!isDiscovering) {
            return
        }
        
        Log.d(TAG, "Stopping UPnP discovery")
        isDiscovering = false
        
        // Clean up resources
        try {
            gatewayDevice = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery: ${e.message}")
        }
    }

    // Lifecycle management
    override fun onDestroy(owner: LifecycleOwner) {
        stopDiscovery()
    }
}