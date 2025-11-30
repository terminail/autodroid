// WorkflowManager.java
package com.autodroid.proxy.managers;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.autodroid.proxy.R;
import com.autodroid.proxy.viewmodel.AppViewModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowManager {
    private static final String TAG = "WorkflowManager";
    private final Context context;
    private final AppViewModel viewModel;
    private final Gson gson;
    private final LayoutInflater inflater;

    public WorkflowManager(Context context, AppViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
        this.gson = new Gson();
        this.inflater = LayoutInflater.from(context);
    }

    public void handleWorkflows(String workflowsJson) {
        try {
            JsonElement workflowsElement = gson.fromJson(workflowsJson, JsonElement.class);
            List<Map<String, Object>> workflowsList = new ArrayList<>();

            if (workflowsElement.isJsonObject()) {
                workflowsList.add(parseWorkflowObject(workflowsElement.getAsJsonObject()));
            } else if (workflowsElement.isJsonArray()) {
                JsonArray workflowsArray = workflowsElement.getAsJsonArray();
                for (JsonElement workflowElement : workflowsArray) {
                    if (workflowElement.isJsonObject()) {
                        workflowsList.add(parseWorkflowObject(workflowElement.getAsJsonObject()));
                    }
                }
            }

            viewModel.setAvailableWorkflows(workflowsList);

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse workflows: " + e.getMessage());
        }
    }

    private Map<String, Object> parseWorkflowObject(JsonObject workflow) {
        Map<String, Object> workflowMap = new HashMap<>();
        if (workflow.has("name")) {
            workflowMap.put("name", workflow.get("name").getAsString());
        }
        if (workflow.has("description")) {
            workflowMap.put("description", workflow.get("description").getAsString());
        }
        if (workflow.has("id")) {
            workflowMap.put("id", workflow.get("id").getAsString());
        }
        return workflowMap;
    }

    public void updateWorkflowsUI(List<Map<String, Object>> workflows, LinearLayout container, TextView titleView) {
        container.removeAllViews();

        if (workflows == null || workflows.isEmpty()) {
            titleView.setText("No workflows available");
        } else {
            titleView.setText("Available Workflows");

            for (Map<String, Object> workflow : workflows) {
                View workflowItem = inflater.inflate(R.layout.workflow_item, null);

                TextView workflowName = workflowItem.findViewById(R.id.workflow_name);
                TextView workflowDescription = workflowItem.findViewById(R.id.workflow_description);

                workflowName.setText((String) workflow.get("name"));
                workflowDescription.setText((String) workflow.get("description"));

                container.addView(workflowItem);
            }
        }
    }
}