// APKScannerManager.java
package com.autodroid.proxy.managers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.autodroid.proxy.service.ZenohService;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APKScannerManager {
    private static final String TAG = "APKScannerManager";
    private final Context context;
    private final Gson gson;

    public APKScannerManager(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public void scanInstalledApks() {
        new Thread(() -> {
            try {
                List<Map<String, Object>> installedApks = new ArrayList<>();
                simulateInstalledApks(installedApks);
                sendApkInfoToServer(installedApks);
            } catch (Exception e) {
                Log.e(TAG, "Failed to scan installed APKs: " + e.getMessage());
            }
        }).start();
    }

    private void simulateInstalledApks(List<Map<String, Object>> installedApks) {
        Map<String, Object> apk1 = new HashMap<>();
        apk1.put("app_name", "Sample App");
        apk1.put("package_name", "com.sample.app");
        apk1.put("version_name", "1.0.0");
        installedApks.add(apk1);

        Map<String, Object> apk2 = new HashMap<>();
        apk2.put("app_name", "Another App");
        apk2.put("package_name", "com.another.app");
        apk2.put("version_name", "2.1.0");
        installedApks.add(apk2);
    }

    private void sendApkInfoToServer(List<Map<String, Object>> installedApks) {
        String apkInfoJson = gson.toJson(installedApks);
        Intent intent = new Intent(context, ZenohService.class);
        intent.setAction("MATCH_WORKFLOWS");
        intent.putExtra("apk_info_list", apkInfoJson);
        context.startService(intent);
    }
}