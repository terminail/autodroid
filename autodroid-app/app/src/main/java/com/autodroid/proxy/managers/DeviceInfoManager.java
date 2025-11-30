// DeviceInfoManager.java
package com.autodroid.proxy.managers;

import android.content.Context;
import android.util.Log;
import com.autodroid.proxy.viewmodel.AppViewModel;

public class DeviceInfoManager {
    private static final String TAG = "DeviceInfoManager";
    private final Context context;
    private final AppViewModel viewModel;

    public DeviceInfoManager(Context context, AppViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    public String getDeviceInfo() {
        String deviceName = android.os.Build.MODEL;
        String androidVersion = android.os.Build.VERSION.RELEASE;
        String localIp = getLocalIpAddress();

        return String.format("Device: %s\nAndroid: %s\nIP: %s",
                deviceName, androidVersion, localIp);
    }

    public void updateDeviceInfo() {
        String deviceInfo = getDeviceInfo();
        viewModel.setDeviceIp(getLocalIpAddress());
        // You can return this or use a callback to update UI
    }

    private String getLocalIpAddress() {
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