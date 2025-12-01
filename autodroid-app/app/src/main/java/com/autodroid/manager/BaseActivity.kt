// BaseActivity.kt
package com.autodroid.manager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.net.Inet4Address
import java.net.NetworkInterface

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Common initialization can go here
    }

    protected fun checkPermissions() {
        // Move permission checking logic here
    }

    protected val localIpAddress: String?
        get() {
            // Move IP address logic here
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
        protected const val TAG: String = "BaseActivity"
    }
}