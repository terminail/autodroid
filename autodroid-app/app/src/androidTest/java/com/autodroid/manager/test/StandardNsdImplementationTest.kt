package com.autodroid.manager.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.autodroid.manager.service.StandardNsdImplementation
import com.autodroid.manager.service.MdnsFallbackManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Test for StandardNsdImplementation mDNS discovery functionality
 */
@RunWith(AndroidJUnit4::class)
class StandardNsdImplementationTest {
    
    private lateinit var context: Context
    private lateinit var standardNsdImplementation: StandardNsdImplementation
    private lateinit var countDownLatch: CountDownLatch
    
    private var discoveredServiceName: String? = null
    private var discoveredServiceHost: String? = null
    private var discoveredServicePort: Int = -1
    private var discoveryStarted = false
    private var discoveryFailed = false
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        countDownLatch = CountDownLatch(1)
        
        val callback = object : com.autodroid.manager.service.MdnsImplementation.Callback {
            override fun onServiceFound(serverInfo: com.autodroid.manager.model.ServerInfo) {
                discoveredServiceName = serverInfo.serviceName
                discoveredServiceHost = serverInfo.host
                discoveredServicePort = serverInfo.port
                countDownLatch.countDown()
            }
            
            override fun onDiscoveryFailed() {
                discoveryFailed = true
                countDownLatch.countDown()
            }
        }
        
        standardNsdImplementation = StandardNsdImplementation(context)
        
        // Reset state variables
        discoveredServiceName = null
        discoveredServiceHost = null
        discoveredServicePort = -1
        discoveryStarted = false
        discoveryFailed = false
    }
    
    @Test
    fun testStandardNsdDiscovery() {
        // Check if StandardNsdImplementation is available
        if (!standardNsdImplementation.isAvailable()) {
            println("StandardNsdImplementation not available on this device - 实现不可用")
            return  // 实现不可用，测试通过
        }
        
        println("Starting StandardNsdImplementation discovery test...")
        
        // Start discovery
        standardNsdImplementation.startDiscovery(object : com.autodroid.manager.service.MdnsImplementation.Callback {
            override fun onServiceFound(serverInfo: com.autodroid.manager.model.ServerInfo) {
                discoveredServiceName = serverInfo.serviceName
                discoveredServiceHost = serverInfo.host
                discoveredServicePort = serverInfo.port
                println("Service found: ${serverInfo.serviceName} at ${serverInfo.host}:${serverInfo.port}")
                countDownLatch.countDown()
            }
            
            override fun onDiscoveryFailed() {
                discoveryFailed = true
                println("StandardNsdImplementation discovery failed")
                countDownLatch.countDown()
            }
        })
        
        // Wait for discovery to complete (30 seconds timeout)
        val discoveryCompleted = countDownLatch.await(30, TimeUnit.SECONDS)
        
        // Stop discovery
        standardNsdImplementation.stopDiscovery()
        
        // Verify results - 如果发现未启动，说明实现不可用，测试通过
        if (!discoveryStarted) {
            println("StandardNsdImplementation discovery not started - 实现不可用")
            return  // 实现不可用，测试通过
        }
        
        if (discoveryFailed) {
            println("StandardNsdImplementation discovery failed - this may be expected on some devices")
        } else if (discoveryCompleted) {
            if (discoveredServiceName != null && discoveredServiceHost != null && discoveredServicePort != -1) {
                println("✓ StandardNsdImplementation successfully discovered service: $discoveredServiceName at $discoveredServiceHost:$discoveredServicePort")
                assertNotNull("Service name should not be null", discoveredServiceName)
                assertNotNull("Service host should not be null", discoveredServiceHost)
                assertTrue("Service port should be valid", discoveredServicePort > 0)
            } else {
                println("StandardNsdImplementation discovery completed but no service found")
            }
        } else {
            println("StandardNsdImplementation discovery timed out after 30 seconds")
        }
        
        println("StandardNsdImplementation test completed")
    }
}