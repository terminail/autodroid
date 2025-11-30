// MainActivity.java
package com.autodroid.proxy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.autodroid.proxy.managers.*;
import com.autodroid.proxy.auth.viewmodel.AuthViewModel;
import com.autodroid.proxy.viewmodel.AppViewModel;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends BaseActivity implements BroadcastReceiverManager.BroadcastListener {

    // UI Components
    private TextView connectionStatusTextView;
    private TextView deviceInfoTextView;
    private TextView serverIpTextView;
    private TextView serverStatusTextView;
    private TextView workflowsTitleTextView;
    private LinearLayout workflowsContainer;
    private Button scanApksButton;
    private TextView matchedWorkflowsTitle;
    private LinearLayout matchedWorkflowsContainer;

    // ViewModels
    private AppViewModel viewModel;
    private AuthViewModel authViewModel;

    // Managers
    private DeviceInfoManager deviceInfoManager;
    private WorkflowManager workflowManager;
    private BroadcastReceiverManager broadcastReceiverManager;
    private APKScannerManager apkScannerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUI();
        initializeViewModels();
        initializeManagers();
        checkAuthentication();
        setupObservers();
        setupClickListeners();
        startZenohService();
        updateUI();
    }

    private void initializeUI() {
        connectionStatusTextView = findViewById(R.id.connection_status);
        deviceInfoTextView = findViewById(R.id.device_info);
        serverIpTextView = findViewById(R.id.server_ip);
        serverStatusTextView = findViewById(R.id.server_status);
        workflowsTitleTextView = findViewById(R.id.workflows_title);
        workflowsContainer = findViewById(R.id.workflows_container);
        scanApksButton = findViewById(R.id.scan_apks_button);
        matchedWorkflowsTitle = findViewById(R.id.matched_workflows_title);
        matchedWorkflowsContainer = findViewById(R.id.matched_workflows_container);
    }

    private void initializeViewModels() {
        viewModel = new ViewModelProvider(this).get(AppViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    private void initializeManagers() {
        deviceInfoManager = new DeviceInfoManager(this, viewModel);
        workflowManager = new WorkflowManager(this, viewModel);
        broadcastReceiverManager = new BroadcastReceiverManager(this, viewModel, workflowManager);
        apkScannerManager = new APKScannerManager(this);

        broadcastReceiverManager.setListener(this);
        broadcastReceiverManager.registerReceivers();
    }

    private void checkAuthentication() {
        // Check if we have valid authentication data from LoginActivity
        boolean isAuthenticatedFromIntent = getIntent().getBooleanExtra("isAuthenticated", false);

        if (isAuthenticatedFromIntent) {
            // We came from LoginActivity with authentication - don't redirect back!
            String userId = getIntent().getStringExtra("userId");
            String email = getIntent().getStringExtra("email");
            String token = getIntent().getStringExtra("token");

            // Update the ViewModel with the authentication data
            if (userId != null && email != null && token != null) {
                authViewModel.setIsAuthenticated(true);
                authViewModel.setUserId(userId);
                authViewModel.setEmail(email);
                authViewModel.setToken(token);
            }
            return; // Stay in MainActivity
        }

        // Only redirect if not authenticated AND we didn't come from LoginActivity
        if (!authViewModel.isAuthenticated()) {
            Log.d(TAG, "Not authenticated, redirecting to LoginActivity");
            Intent intent = new Intent(this, com.autodroid.proxy.auth.activity.LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
    private void setupObservers() {
        viewModel.getServerIp().observe(this, ip -> {
            serverIpTextView.setText("Server IP: " + ip);
        });

        viewModel.getServerConnected().observe(this, connected -> {
            serverStatusTextView.setText("Server Status: " + (connected ? "Connected" : "Disconnected"));
        });

        viewModel.getAvailableWorkflows().observe(this, workflows -> {
            workflowManager.updateWorkflowsUI(workflows, workflowsContainer, workflowsTitleTextView);
        });
    }

    private void setupClickListeners() {
        scanApksButton.setOnClickListener(v -> {
            apkScannerManager.scanInstalledApks();
        });
    }

    private void startZenohService() {
        Intent serviceIntent = new Intent(this, com.autodroid.proxy.service.ZenohService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void updateUI() {
        deviceInfoTextView.setText(deviceInfoManager.getDeviceInfo());
        updateConnectionStatus("Disconnected");
    }

    private void updateConnectionStatus(String status) {
        connectionStatusTextView.setText("Connection Status: " + status);
    }

    // BroadcastListener implementation
    @Override
    public void onServerInfoReceived(String serverInfoJson) {
        broadcastReceiverManager.handleServerInfo(serverInfoJson);
        updateConnectionStatus("Connected");
    }

    @Override
    public void onWorkflowsReceived(String workflowsJson) {
        workflowManager.handleWorkflows(workflowsJson);
    }

    @Override
    public void onReportsReceived(String reportsJson) {
        // Handle reports
    }

    @Override
    public void onExecutionReceived(String executionJson) {
        // Handle execution
    }

    @Override
    public void onMatchedWorkflowsReceived(String matchedWorkflowsJson) {
        // Handle matched workflows
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        broadcastReceiverManager.unregisterReceivers();
    }
}