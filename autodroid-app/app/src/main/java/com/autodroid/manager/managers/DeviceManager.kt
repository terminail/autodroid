// DeviceManager.kt
package com.autodroid.manager.managers

import android.content.Context
import android.os.Build
import android.util.Log
import com.autodroid.manager.AppViewModel
import com.autodroid.manager.model.Device
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*

class DeviceManager(private val context: Context?, private val appViewModel: AppViewModel) {
    val device: Device
        get() {
            val deviceName = Build.MODEL
            val androidVersion = Build.VERSION.RELEASE
            val localIp = this.localIpAddress

            return Device.detailed(
                ip = localIp ?: "Not Available",
                name = deviceName,
                model = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                androidVersion = androidVersion,
                platform = "Android",
                brand = Build.BRAND,
                device = Build.DEVICE,
                product = Build.PRODUCT
            )
        }

    fun updateDevice() {
        val device = this.device
        appViewModel.setDevice(device)
        // You can return this or use a callback to update UI
    }

    private val localIpAddress: String?
        get() {
            try {
                // Try to get the first non-loopback IPv4 address from any network interface
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            // Return the first non-loopback IPv4 address (including wlan0)
                            return address.hostAddress
                        }
                    }
                }
                return "Not Available"
            } catch (e: Exception) {
                return "Not Available"
            }
        }

    companion object {
        private const val TAG = "DeviceManager"
    }
}