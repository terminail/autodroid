package com.autodroid.proxy;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.autodroid.proxy.viewmodel.AppViewModel;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Map;

public class ReportDetailActivity extends AppCompatActivity {

    private static final String TAG = "ReportDetailActivity";
    
    private TextView reportIdTextView;
    private TextView workflowNameTextView;
    private TextView statusTextView;
    private TextView startTimeTextView;
    private TextView durationTextView;
    private TextView stepsTextView;
    
    private AppViewModel viewModel;
    private Gson gson;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);
        
        // Initialize Gson
        gson = new Gson();
        
        // Initialize UI components
        reportIdTextView = findViewById(R.id.report_id);
        workflowNameTextView = findViewById(R.id.report_workflow_name);
        statusTextView = findViewById(R.id.report_status);
        startTimeTextView = findViewById(R.id.report_start_time);
        durationTextView = findViewById(R.id.report_duration);
        stepsTextView = findViewById(R.id.report_steps);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(AppViewModel.class);
        
        // Get report ID from intent
        String reportId = getIntent().getStringExtra("report_id");
        
        // Observe selected report changes
        viewModel.getSelectedReport().observe(this, report -> {
            if (report != null) {
                updateReportUI(report);
            }
        });
        
        // Load report details (in a real app, this would fetch from server or database)
        if (reportId != null) {
            loadReportDetails(reportId);
        }
    }
    
    private void loadReportDetails(String reportId) {
        // In a real app, this would fetch the report details from the server
        // For now, we'll just use a mock report
        Map<String, Object> mockReport = Map.of(
            "id", reportId,
            "workflow_name", "Sample Workflow",
            "status", "success",
            "start_time", "2024-01-01 12:00:00",
            "duration", "30.5",
            "steps", "Step 1: Launch App - Success\nStep 2: Click Button - Success\nStep 3: Verify Result - Success"
        );
        
        viewModel.setSelectedReport(mockReport);
    }
    
    private void updateReportUI(Map<String, Object> report) {
        reportIdTextView.setText("Report ID: " + report.getOrDefault("id", "N/A"));
        workflowNameTextView.setText("Workflow: " + report.getOrDefault("workflow_name", "N/A"));
        statusTextView.setText("Status: " + report.getOrDefault("status", "N/A"));
        startTimeTextView.setText("Start Time: " + report.getOrDefault("start_time", "N/A"));
        durationTextView.setText("Duration: " + report.getOrDefault("duration", "N/A") + " seconds");
        stepsTextView.setText("Steps:\n" + report.getOrDefault("steps", "N/A"));
        
        // Set status text color based on status
        String status = (String) report.getOrDefault("status", "");
        if (status.equals("success")) {
            statusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (status.equals("failed")) {
            statusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            statusTextView.setTextColor(getResources().getColor(android.R.color.black));
        }
    }
}
