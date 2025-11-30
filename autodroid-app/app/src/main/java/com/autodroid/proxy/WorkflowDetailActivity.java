package com.autodroid.proxy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkflowDetailActivity extends AppCompatActivity {

    private static final String TAG = "WorkflowDetailActivity";
    private Gson gson;
    
    private TextView workflowNameTextView;
    private TextView workflowDescriptionTextView;
    private TextView workflowPackageTextView;
    private TextView workflowVersionTextView;
    
    private Button startButton;
    private Button stopButton;
    private Button toggleParamsButton;
    private LinearLayout paramsContainer;
    private LinearLayout executionInfoContainer;
    
    private TextView statusTextView;
    private TextView startTimeTextView;
    private TextView endTimeTextView;
    private TextView durationTextView;
    
    private Map<String, Object> workflow;
    private ExecutorService executorService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workflow_detail);
        
        // Initialize Gson
        gson = new Gson();
        
        // Initialize executor service
        executorService = Executors.newSingleThreadExecutor();
        
        // Get workflow data from intent
        String workflowJson = getIntent().getStringExtra("workflow");
        if (workflowJson != null) {
            JsonElement workflowElement = gson.fromJson(workflowJson, JsonElement.class);
            workflow = gson.fromJson(workflowElement, Map.class);
        } else {
            Toast.makeText(this, "No workflow data provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize UI components
        initUI();
        
        // Populate workflow details
        populateWorkflowDetails();
        
        // Set up click listeners
        setupClickListeners();
    }
    
    private void initUI() {
        // Workflow info
        workflowNameTextView = findViewById(R.id.workflow_detail_name);
        workflowDescriptionTextView = findViewById(R.id.workflow_detail_description);
        workflowPackageTextView = findViewById(R.id.workflow_detail_package);
        workflowVersionTextView = findViewById(R.id.workflow_detail_version);
        
        // Execution controls
        startButton = findViewById(R.id.workflow_detail_start_button);
        stopButton = findViewById(R.id.workflow_detail_stop_button);
        
        // Parameter controls
        toggleParamsButton = findViewById(R.id.workflow_detail_toggle_params_button);
        paramsContainer = findViewById(R.id.workflow_detail_params_container);
        
        // Execution info
        executionInfoContainer = findViewById(R.id.workflow_detail_execution_info);
        statusTextView = findViewById(R.id.workflow_detail_status);
        startTimeTextView = findViewById(R.id.workflow_detail_start_time);
        endTimeTextView = findViewById(R.id.workflow_detail_end_time);
        durationTextView = findViewById(R.id.workflow_detail_duration);
    }
    
    private void populateWorkflowDetails() {
        if (workflow == null) return;
        
        // Set workflow basic info
        workflowNameTextView.setText((String) workflow.getOrDefault("name", "Unknown Workflow"));
        workflowDescriptionTextView.setText((String) workflow.getOrDefault("description", "No description available"));
        
        // Set metadata info
        Map<String, Object> metadata = (Map<String, Object>) workflow.getOrDefault("metadata", new HashMap<>());
        workflowPackageTextView.setText("Package: " + metadata.getOrDefault("app_package", "Unknown"));
        workflowVersionTextView.setText("Version Constraint: " + metadata.getOrDefault("version_constraint", "*"));
    }
    
    private void setupClickListeners() {
        // Toggle parameters button
        toggleParamsButton.setOnClickListener(v -> {
            if (paramsContainer.getVisibility() == View.GONE) {
                // Show parameters and generate UI
                generateDynamicParametersUI(paramsContainer, workflow);
                paramsContainer.setVisibility(View.VISIBLE);
                toggleParamsButton.setText("Hide Parameters");
            } else {
                // Hide parameters
                paramsContainer.setVisibility(View.GONE);
                toggleParamsButton.setText("Show Parameters");
            }
        });
        
        // Start button
        startButton.setOnClickListener(v -> {
            // Collect parameters
            Map<String, Object> parameters = collectDynamicParameterValues(paramsContainer);
            
            // Start workflow execution
            startWorkflowExecution(parameters);
        });
        
        // Stop button
        stopButton.setOnClickListener(v -> {
            stopWorkflowExecution();
        });
        
        // Back button
        Button backButton = findViewById(R.id.workflow_detail_back_button);
        backButton.setOnClickListener(v -> {
            finish();
        });
    }
    
    private void generateDynamicParametersUI(LinearLayout paramsContainer, Map<String, Object> workflow) {
        // Clear any existing views
        paramsContainer.removeAllViews();
        
        // Check if workflow has parameter definitions
        if (workflow.containsKey("parameter_definitions")) {
            List<Map<String, Object>> paramDefinitions = (List<Map<String, Object>>) workflow.get("parameter_definitions");
            
            for (Map<String, Object> paramDef : paramDefinitions) {
                // Create parameter UI based on definition
                View paramView = createParameterView(paramDef, workflow);
                if (paramView != null) {
                    paramsContainer.addView(paramView);
                }
            }
        } else {
            // No parameter definitions, show message
            TextView noParamsText = new TextView(this);
            noParamsText.setText("No parameters defined for this workflow");
            noParamsText.setTextSize(14f);
            noParamsText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            noParamsText.setPadding(0, 8, 0, 8);
            paramsContainer.addView(noParamsText);
        }
    }
    
    private View createParameterView(Map<String, Object> paramDef, Map<String, Object> workflow) {
        // Get parameter definition details
        String paramName = (String) paramDef.get("name");
        String paramType = (String) paramDef.getOrDefault("type", "string");
        Object defaultValue = paramDef.get("default");
        String description = (String) paramDef.getOrDefault("description", "");
        
        // Create parameter container
        LinearLayout paramContainer = new LinearLayout(this);
        paramContainer.setOrientation(LinearLayout.VERTICAL);
        paramContainer.setPadding(0, 0, 0, 16);
        
        // Create parameter label
        TextView paramLabel = new TextView(this);
        paramLabel.setText(paramName + (description.isEmpty() ? "" : " - " + description));
        paramLabel.setTextSize(14f);
        paramLabel.setTypeface(paramLabel.getTypeface(), android.graphics.Typeface.BOLD);
        paramLabel.setTextColor(getResources().getColor(android.R.color.black));
        paramLabel.setPadding(0, 0, 0, 4);
        paramContainer.addView(paramLabel);
        
        // Create input based on parameter type
        View inputView = null;
        
        switch (paramType.toLowerCase()) {
            case "string":
                inputView = createStringInput(paramName, defaultValue, workflow);
                break;
            case "number":
                inputView = createNumberInput(paramName, defaultValue, workflow);
                break;
            case "boolean":
                inputView = createBooleanInput(paramName, defaultValue, workflow);
                break;
            case "enum":
                inputView = createEnumInput(paramName, paramDef, defaultValue, workflow);
                break;
            default:
                // Unsupported type, create string input as fallback
                inputView = createStringInput(paramName, defaultValue, workflow);
                break;
        }
        
        if (inputView != null) {
            paramContainer.addView(inputView);
        }
        
        return paramContainer;
    }
    
    private EditText createStringInput(String paramName, Object defaultValue, Map<String, Object> workflow) {
        EditText editText = new EditText(this);
        editText.setHint("Enter " + paramName);
        editText.setText(defaultValue != null ? defaultValue.toString() : "");
        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        editText.setBackgroundResource(R.drawable.edittext_background);
        editText.setPadding(12, 12, 12, 12);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT));
        editText.setTag("param_" + paramName);
        return editText;
    }
    
    private EditText createNumberInput(String paramName, Object defaultValue, Map<String, Object> workflow) {
        EditText editText = new EditText(this);
        editText.setHint("Enter " + paramName);
        editText.setText(defaultValue != null ? defaultValue.toString() : "");
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setBackgroundResource(R.drawable.edittext_background);
        editText.setPadding(12, 12, 12, 12);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT));
        editText.setTag("param_" + paramName);
        return editText;
    }
    
    private Switch createBooleanInput(String paramName, Object defaultValue, Map<String, Object> workflow) {
        Switch switchView = new Switch(this);
        switchView.setChecked(defaultValue != null && (boolean) defaultValue);
        switchView.setText("Enable " + paramName);
        switchView.setTextSize(14f);
        switchView.setTag("param_" + paramName);
        return switchView;
    }
    
    private Spinner createEnumInput(String paramName, Map<String, Object> paramDef, Object defaultValue, Map<String, Object> workflow) {
        Spinner spinner = new Spinner(this);
        
        // Get enum options from parameter definition
        List<String> options = (List<String>) paramDef.getOrDefault("options", new ArrayList<>());
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        
        // Set default value if exists
        if (defaultValue != null) {
            int position = adapter.getPosition(defaultValue.toString());
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
        
        spinner.setBackgroundResource(R.drawable.edittext_background);
        spinner.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT));
        spinner.setTag("param_" + paramName);
        return spinner;
    }
    
    private Map<String, Object> collectDynamicParameterValues(LinearLayout paramsContainer) {
        Map<String, Object> parameters = new HashMap<>();
        
        // Iterate through all parameter views and collect values
        for (int i = 0; i < paramsContainer.getChildCount(); i++) {
            View paramContainer = paramsContainer.getChildAt(i);
            
            if (paramContainer instanceof LinearLayout) {
                LinearLayout paramLayout = (LinearLayout) paramContainer;
                
                for (int j = 0; j < paramLayout.getChildCount(); j++) {
                    View inputView = paramLayout.getChildAt(j);
                    
                    // Skip labels, only process input views
                    if (inputView instanceof TextView && !(inputView instanceof EditText) && !(inputView instanceof Switch)) {
                        continue;
                    }
                    
                    // Get parameter name from tag
                    String tag = (String) inputView.getTag();
                    if (tag != null && tag.startsWith("param_")) {
                        String paramName = tag.substring(6); // Remove "param_" prefix
                        
                        // Get value based on input type
                        Object value = null;
                        
                        if (inputView instanceof EditText) {
                            EditText editText = (EditText) inputView;
                            String textValue = editText.getText().toString();
                            // Try to parse as number if possible
                            try {
                                value = Double.parseDouble(textValue);
                            } catch (NumberFormatException e) {
                                value = textValue;
                            }
                        } else if (inputView instanceof Switch) {
                            Switch switchView = (Switch) inputView;
                            value = switchView.isChecked();
                        } else if (inputView instanceof Spinner) {
                            Spinner spinner = (Spinner) inputView;
                            value = spinner.getSelectedItem().toString();
                        }
                        
                        if (value != null) {
                            parameters.put(paramName, value);
                        }
                    }
                }
            }
        }
        
        return parameters;
    }
    
    private void startWorkflowExecution(Map<String, Object> parameters) {
        // Update UI to show running state
        statusTextView.setText("Running");
        statusTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        executionInfoContainer.setVisibility(View.VISIBLE);
        
        // Set start time
        String startTime = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        startTimeTextView.setText("Start: " + startTime);
        endTimeTextView.setText("End: N/A");
        durationTextView.setText("Duration: 00:00:00");
        
        // Log workflow start with parameters
        Log.d(TAG, "Starting workflow: " + workflow.get("name") + " with parameters: " + parameters.toString());
        
        // Simulate workflow execution
        simulateWorkflowExecution(parameters);
    }
    
    private void stopWorkflowExecution() {
        // Update UI to show stopped state
        statusTextView.setText("Stopped");
        statusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        
        // Set end time
        String endTime = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        endTimeTextView.setText("End: " + endTime);
        
        Log.d(TAG, "Stopping workflow: " + workflow.get("name"));
    }
    
    private void simulateWorkflowExecution(Map<String, Object> parameters) {
        // Simulate workflow execution by changing status after delay
        executorService.submit(() -> {
            try {
                // Simulate running for 5 seconds
                Thread.sleep(5000);
                
                // Update UI to show completed state
                runOnUiThread(() -> {
                    statusTextView.setText("Completed");
                    statusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    
                    // Set end time
                    String endTime = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                    endTimeTextView.setText("End: " + endTime);
                    
                    // Update duration
                    durationTextView.setText("Duration: 00:00:05");
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}