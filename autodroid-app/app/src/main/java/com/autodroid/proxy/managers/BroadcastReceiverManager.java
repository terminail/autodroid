// BroadcastReceiverManager.java
package com.autodroid.proxy.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.autodroid.proxy.viewmodel.AppViewModel;
import com.google.gson.Gson;

public class BroadcastReceiverManager {
    private static final String TAG = "BroadcastReceiverManager";
    private final Context context;
    private final AppViewModel viewModel;
    private final Gson gson;
    private final WorkflowManager workflowManager;

    private BroadcastReceiver serverInfoReceiver;
    private BroadcastReceiver workflowsReceiver;
    private BroadcastReceiver reportsReceiver;
    private BroadcastReceiver executionReceiver;
    private BroadcastReceiver matchedWorkflowsReceiver;

    public interface BroadcastListener {
        void onServerInfoReceived(String serverInfoJson);
        void onWorkflowsReceived(String workflowsJson);
        void onReportsReceived(String reportsJson);
        void onExecutionReceived(String executionJson);
        void onMatchedWorkflowsReceived(String matchedWorkflowsJson);
    }

    private BroadcastListener listener;

    public BroadcastReceiverManager(Context context, AppViewModel viewModel, WorkflowManager workflowManager) {
        this.context = context;
        this.viewModel = viewModel;
        this.workflowManager = workflowManager;
        this.gson = new Gson();
    }

    public void setListener(BroadcastListener listener) {
        this.listener = listener;
    }

    public void registerReceivers() {
        serverInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String serverInfoJson = intent.getStringExtra("server_info");
                if (listener != null && serverInfoJson != null) {
                    listener.onServerInfoReceived(serverInfoJson);
                }
            }
        };

        workflowsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String workflowsJson = intent.getStringExtra("workflows");
                if (listener != null && workflowsJson != null) {
                    listener.onWorkflowsReceived(workflowsJson);
                }
            }
        };

        // Register other receivers similarly...

        context.registerReceiver(serverInfoReceiver, new IntentFilter("com.autodroid.proxy.SERVER_INFO_UPDATE"));
        context.registerReceiver(workflowsReceiver, new IntentFilter("com.autodroid.proxy.WORKFLOWS_UPDATE"));
        // Register other intent filters...
    }

    public void unregisterReceivers() {
        if (serverInfoReceiver != null) {
            context.unregisterReceiver(serverInfoReceiver);
        }
        if (workflowsReceiver != null) {
            context.unregisterReceiver(workflowsReceiver);
        }
        // Unregister other receivers...
    }

    public void handleServerInfo(String serverInfoJson) {
        try {
            // Handle server info parsing and update ViewModel
            // This can be moved from MainActivity
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle server info: " + e.getMessage());
        }
    }
}