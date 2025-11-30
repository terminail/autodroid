// BaseActivity.java
package com.autodroid.proxy;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    protected static final String TAG = "BaseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Common initialization can go here
    }

    protected void checkPermissions() {
        // Move permission checking logic here
    }

    protected String getLocalIpAddress() {
        // Move IP address logic here
        try {
            java.net.NetworkInterface wifiInterface = java.net.NetworkInterface.getByName("wlan0");
            if (wifiInterface != null) {
                java.util.Enumeration<java.net.InetAddress> addresses = wifiInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
            return "Not Available";
        } catch (Exception e) {
            return "Not Available";
        }
    }
}