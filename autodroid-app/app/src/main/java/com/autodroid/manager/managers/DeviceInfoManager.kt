// DeviceInfoManager.kt
package com.autodroid.manager.managers

import android.content.Context
import android.os.Build
import android.util.Log
import com.autodroid.manager.viewmodel.AppViewModel
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*

class DeviceInfoManager(private val context: Context?, private val viewModel: AppViewModel) {
    val deviceInfo: String
        get() {
            val deviceName = Build.MODEL
            val androidVersion = Build.VERSION.RELEASE
            val localIp = this.localIpAddress

            return String.format(
                "Device: %s\nAndroid: %s\nNetwork IP: %s",
                deviceName, androidVersion, localIp
            )
        }

    fun updateDeviceInfo() {
        val deviceInfo = this.deviceInfo
        viewModel.setDeviceIp(this.localIpAddress)
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
        private const val TAG = "DeviceInfoManager"
    }
}