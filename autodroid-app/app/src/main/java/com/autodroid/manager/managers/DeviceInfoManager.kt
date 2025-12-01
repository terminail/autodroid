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
                "Device: %s\nAndroid: %s\nIP: %s",
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
                val wifiInterface =
                    NetworkInterface.getByName("wlan0")
                if (wifiInterface != null) {
                    val addresses =
                        wifiInterface.getInetAddresses()
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress() && address is Inet4Address) {
                            return address.getHostAddress()
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