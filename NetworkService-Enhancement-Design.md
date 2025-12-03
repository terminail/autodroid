# NetworkService Enhancement Design Document

## Overview

This document outlines the requirements and design for enhancing the NetworkService in the Autodroid Android app to improve mDNS discovery, service lifecycle management, and UI updates.

## Current Issues

1. **mDNS Discovery Failures**: The logs show repeated `FAILURE_INTERNAL_ERROR` when trying to discover `_autodroid._tcp.local.` services
2. **No Auto-Start**: NetworkService is not automatically started when the app launches
3. **No Auto-Stop**: NetworkService continues running even after multiple discovery failures
4. **UI Update Issues**: The "connection_status" TextView doesn't update properly during discovery
5. **No Fallback**: No automatic fallback to QR code scanning when mDNS fails

## Requirements

### 1. Auto-Start NetworkService
- NetworkService should automatically start when the app launches
- This ensures mDNS discovery begins immediately without user interaction

### 2. Discovery Lifecycle Management
- Implement auto-stop mechanism after multiple mDNS discovery failures
- Prevent unnecessary battery drain from continuous failed discovery attempts
- Define a reasonable retry limit (current implementation has 5 retries)

### 3. UI Updates During Discovery
- Ensure the "connection_status" TextView updates properly during all phases of discovery
- Show appropriate status messages for:
  - Initial discovery start
  - Retry attempts with count
  - Discovery failure
  - Service found

### 4. QR Code Fallback
- Automatically prompt user to scan QR code when mDNS discovery fails
- Make QR code scanning more prominent in the UI when mDNS is not working

## Implementation Plan

### 1. Auto-Start NetworkService

**Location**: `MyApplication.kt`

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize the DiscoveryStatusManager with application context
        DiscoveryStatusManager.initialize(this)
        
        // Auto-start NetworkService for mDNS discovery
        DiscoveryStatusManager.startNetworkService()
    }
}
```

### 2. Discovery Lifecycle Management

**Location**: `NetworkService.kt`

Add a new field to track consecutive failures:
```kotlin
private var consecutiveFailures = 0
private val maxConsecutiveFailures = 3 // After 3 full cycles of failures, stop the service
```

Modify the `onDiscoveryFailed()` callback:
```kotlin
override fun onDiscoveryFailed() {
    Log.e(TAG, "mDNS discovery failed")
    isDiscoveryInProgress = false
    
    // Increment retry count
    currentRetryCount++
    
    // Check if we should retry
    if (currentRetryCount < maxRetries) {
        // Update DiscoveryStatusManager about retry
        DiscoveryStatusManager.updateDiscoveryStatus(true, currentRetryCount, maxRetries)
        
        // Retry discovery with exponential backoff
        executorService!!.submit(Runnable {
            try {
                val delayMs = 1000 * (1 shl minOf(currentRetryCount - 1, 3)) // 1s, 2s, 4s, 8s, 8s
                Log.d(TAG, "Retrying discovery in ${delayMs}ms (attempt ${currentRetryCount + 1}/$maxRetries)")
                Thread.sleep(delayMs)
                
                // Check if service is still running before retrying
                if (DiscoveryStatusManager.isServiceRunning.value == true) {
                    nsdHelper!!.discoverServices()
                }
            } catch (e: InterruptedException) {
                Log.e(TAG, "Discovery retry interrupted", e)
            }
        })
    } else {
        // All retries failed, increment consecutive failures
        consecutiveFailures++
        DiscoveryStatusManager.updateDiscoveryFailed(true)
        
        // Check if we should stop the service
        if (consecutiveFailures >= maxConsecutiveFailures) {
            Log.w(TAG, "Stopping NetworkService after $maxConsecutiveFailures consecutive failure cycles")
            DiscoveryStatusManager.stopNetworkService()
            
            // Reset counter for future manual starts
            consecutiveFailures = 0
        }
    }
}
```

Reset the counter on successful discovery:
```kotlin
override fun onServiceFound(serviceName: String?, host: String?, port: Int) {
    Log.d(TAG, "Service found: " + serviceName + " at " + host + ":" + port)
    if (serviceName != null && host != null) {
        discoveredServer = DiscoveredServer(serviceName, host, port)
        isDiscoveryInProgress = false
        
        // Reset consecutive failures on success
        consecutiveFailures = 0
        
        // Update DiscoveryStatusManager
        DiscoveryStatusManager.updateServerInfo(discoveredServer)
        DiscoveryStatusManager.updateDiscoveryStatus(false, currentRetryCount, maxRetries)
        DiscoveryStatusManager.updateDiscoveryFailed(false)

        // Publish device information to the discovered server
        publishDeviceInfo(host, port)
    }
}
```

### 3. UI Updates During Discovery

**Location**: `DashboardFragment.kt`

The current implementation already has observers for discovery status, but we need to ensure they're properly updating the UI:

```kotlin
// In setupObservers() method

// Observe discovery status from DiscoveryStatusManager
DiscoveryStatusManager.discoveryInProgress.observe(viewLifecycleOwner) { inProgress ->
    if (inProgress == true) {
        connectionStatusTextView?.text = "mDNS Discovering..."
        // Hide QR code button during discovery
        scanQrButton?.visibility = View.GONE
    }
}

// Observe discovery retry count from DiscoveryStatusManager
DiscoveryStatusManager.discoveryRetryCount.observe(viewLifecycleOwner) { retryCount ->
    val maxRetries = DiscoveryStatusManager.discoveryMaxRetries.value ?: 0
    if (retryCount != null && maxRetries > 0) {
        if (retryCount < maxRetries) {
            connectionStatusTextView?.text = "mDNS Retry ${retryCount + 1}/$maxRetries"
            // Hide QR code button during retry
            scanQrButton?.visibility = View.GONE
        }
    }
}

// Observe discovery failure from DiscoveryStatusManager
DiscoveryStatusManager.discoveryFailed.observe(viewLifecycleOwner) { failed ->
    if (failed == true) {
        connectionStatusTextView?.text = "mDNS Failed - Try QR Code"
        serverIpTextView?.text = "Discovery failed"
        serverPortTextView?.text = "-"
        serverStatusTextView?.text = "FAILED"
        apiEndpointTextView?.text = "-"
        // Show QR code button when discovery fails
        scanQrButton?.visibility = View.VISIBLE
        scanQrButton?.text = "Scan QR Code Instead"
    }
}
```

### 4. QR Code Fallback

**Location**: `DashboardFragment.kt`

Add a method to handle automatic QR code prompt:

```kotlin
private fun showQRCodePrompt() {
    // Show a dialog or snackbar suggesting QR code scanning
    val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
    builder.setTitle("mDNS Discovery Failed")
    builder.setMessage("Unable to discover server automatically. Would you like to scan a QR code instead?")
    builder.setPositiveButton("Scan QR Code") { dialog, _ ->
        checkCameraPermissionAndScanQR()
        dialog.dismiss()
    }
    builder.setNegativeButton("Cancel") { dialog, _ ->
        dialog.dismiss()
    }
    builder.show()
}
```

Modify the discovery failure observer to show the prompt:

```kotlin
// Observe discovery failure from DiscoveryStatusManager
DiscoveryStatusManager.discoveryFailed.observe(viewLifecycleOwner) { failed ->
    if (failed == true) {
        connectionStatusTextView?.text = "mDNS Failed - Try QR Code"
        serverIpTextView?.text = "Discovery failed"
        serverPortTextView?.text = "-"
        serverStatusTextView?.text = "FAILED"
        apiEndpointTextView?.text = "-"
        // Show QR code button when discovery fails
        scanQrButton?.visibility = View.VISIBLE
        scanQrButton?.text = "Scan QR Code Instead"
        
        // Show prompt to user
        showQRCodePrompt()
    }
}
```

### 5. Addressing mDNS Discovery Issues

The logs show `FAILURE_INTERNAL_ERROR` when trying to discover `_autodroid._tcp.local.` services. This could be due to:

1. **Network Configuration**: Some networks block mDNS traffic
2. **Android Version**: Some Android versions have stricter mDNS requirements
3. **Service Type**: The service type might not be registered correctly

**Mitigation Strategy**:

1. Add network connectivity checks before starting discovery
2. Implement a fallback mechanism that triggers QR code scanning after multiple failures
3. Add user education about network requirements

## Testing Plan

1. **Auto-Start Test**: Verify NetworkService starts automatically when app launches
2. **Discovery Lifecycle Test**: Verify service stops after multiple failure cycles
3. **UI Update Test**: Verify connection status updates correctly during all discovery phases
4. **QR Code Fallback Test**: Verify QR code prompt appears when mDNS fails
5. **Network Scenarios Test**: Test on different network configurations (home, office, public WiFi)

## Conclusion

These enhancements will significantly improve the user experience by:
- Automatically starting the discovery process
- Preventing unnecessary battery drain
- Providing clear status updates
- Offering a reliable fallback mechanism

The implementation maintains the existing architecture while adding robust error handling and user-friendly features.