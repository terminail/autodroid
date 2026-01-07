package com.autodroid.trader.ime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AccessibilityDumpReceiver extends BroadcastReceiver {
    private static final String TAG = "AccessibilityDumpReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("com.autodroid.trader.ACCESSIBILITY_DUMP_REQUEST".equals(action)) {
            Log.d(TAG, "Received accessibility dump request");
            // 直接调用 TraderAccessibilityService 的静态方法来触发 dump
            TraderImeService.requestAccessibilityDump(context);
            Log.d(TAG, "Called TraderAccessibilityService.requestAccessibilityDump");
        }
    }
}