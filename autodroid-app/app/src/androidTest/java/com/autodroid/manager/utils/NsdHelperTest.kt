package com.autodroid.manager.utils

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class NsdHelperTest {
    private lateinit var context: Context
    private lateinit var nsdHelper: NsdHelper
    private lateinit var nsdManager: NsdManager
    private val serviceName = "TestServer"
    private val serviceType = "_autodroid._tcp.local."
    private val host = "192.168.1.100"
    private val port = 8000
    private lateinit var countDownLatch: CountDownLatch
    private var discoveredHost: String? = null
    private var discoveredPort: Int? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        countDownLatch = CountDownLatch(1)
        discoveredHost = null
        discoveredPort = null

        // Initialize NsdHelper with a callback that captures discovered server info
        nsdHelper = NsdHelper(context, object : NsdHelper.ServiceDiscoveryCallback {
            override fun onServiceFound(serviceName: String, host: String, port: Int) {
                println("✓ Service found: $serviceName at $host:$port")
                discoveredHost = host
                discoveredPort = port
                countDownLatch.countDown()
            }

            override fun onServiceLost(serviceName: String) {
                println("⚠ Service lost: $serviceName")
            }

            override fun onDiscoveryStarted() {
                println("🔍 Discovery started")
            }

            override fun onDiscoveryFailed() {
                println("❌ Discovery failed")
                // Don't count down the latch on failure - let it timeout
            }
        })

        nsdHelper.initialize()
    }

    @After
    fun tearDown() {
        nsdHelper.tearDown()
    }

    @Test
    fun testServiceDiscovery() {
        // This test verifies that the mDNS discovery mechanism is correctly implemented
        // It should fail if discovery doesn't work, not just timeout
        
        // First, verify that the test environment has proper network connectivity
        if (!isNetworkAvailable()) {
            throw AssertionError("Network not available - cannot test mDNS discovery")
        }
        
        // Start discovery - real server should be publishing via mDNS
        nsdHelper.discoverServices()

        // Wait for discovery to complete (timeout after 15 seconds)
        val success = countDownLatch.await(15, TimeUnit.SECONDS)

        // Verify that discovery actually worked
        assert(success) { "mDNS service discovery timed out - no server found via mDNS" }
        assert(discoveredHost != null) { "No host discovered - mDNS discovery failed" }
        assert(discoveredPort != null) { "No port discovered - mDNS discovery failed" }
        
        // The port should be whatever the server publishes, not hardcoded to 8001
        assert(discoveredPort!! > 0) { "Invalid port discovered: $discoveredPort" }
        
        println("✓ mDNS service discovery succeeded: $discoveredHost:$discoveredPort")
    }

    @Test
    fun testHealthCheckAfterDiscovery() {
        // This test verifies that after discovery, we can communicate with the server
        
        // First, verify that the test environment has proper network connectivity
        if (!isNetworkAvailable()) {
            throw AssertionError("Network not available - cannot test mDNS discovery")
        }
        
        // Start discovery - real server should be publishing via mDNS
        nsdHelper.discoverServices()

        // Wait for discovery to complete (timeout after 15 seconds)
        val discoverySuccess = countDownLatch.await(15, TimeUnit.SECONDS)

        // Verify that discovery actually worked
        assert(discoverySuccess) { "mDNS service discovery timed out - no server found via mDNS" }
        assert(discoveredHost != null) { "No host discovered - mDNS discovery failed" }
        assert(discoveredPort != null) { "No port discovered - mDNS discovery failed" }
        
        // The port should be whatever the server publishes, not hardcoded to 8001
        assert(discoveredPort!! > 0) { "Invalid port discovered: $discoveredPort" }

        // Perform health check on the discovered server
        val healthCheckSuccess = performHealthCheck(discoveredHost!!, discoveredPort!!)
        assert(healthCheckSuccess) { "Health check failed - server at $discoveredHost:$discoveredPort is not responding" }
        println("✓ Health check passed successfully")
    }

    private fun performHealthCheck(host: String, port: Int): Boolean {
        return try {
            val url = URL("http://$host:$port/api/health")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            connection.disconnect()

            // Return true if response code is 200 (OK)
            responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun registerTestService() {
        val serviceInfo = NsdServiceInfo()
        // Use setter methods instead of direct property assignment for compatibility with Android 8.0.0
        serviceInfo.setServiceName(serviceName)
        serviceInfo.setServiceType(serviceType)
        serviceInfo.setPort(port)

        val registerListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                // Service registered successfully
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Registration failed, but we'll continue with the test
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                // Service unregistered, but we'll continue with the test
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Unregistration failed, but we'll continue with the test
            }
        }

        // Register the service on the main thread
        Handler(Looper.getMainLooper()).post {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registerListener)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            // Try DNS resolution first
            val address = java.net.InetAddress.getByName("google.com")
            address.hostAddress != null
        } catch (e: Exception) {
            // If DNS fails, try direct socket connection
            try {
                val socket = java.net.Socket()
                val socketAddress = java.net.InetSocketAddress("8.8.8.8", 53)
                socket.connect(socketAddress, 3000)
                socket.close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    private fun getLocalNetworkInfo(): String {
        return try {
            val networkInterfaces = java.net.NetworkInterface.getNetworkInterfaces()
            val sb = StringBuilder()
            sb.append("Network Interfaces:\n")
            
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                if (networkInterface.isUp && !networkInterface.isLoopback) {
                    sb.append("Interface: ${networkInterface.displayName}\n")
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        sb.append("  Address: ${address.hostAddress}\n")
                    }
                }
            }
            sb.toString()
        } catch (e: Exception) {
            "Unable to get network info: ${e.message}"
        }
    }
}
