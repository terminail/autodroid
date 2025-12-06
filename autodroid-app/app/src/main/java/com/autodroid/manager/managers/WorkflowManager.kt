// WorkflowManager.java
package com.autodroid.manager.managers

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.autodroid.manager.R
import com.autodroid.manager.AppViewModel
import com.autodroid.manager.model.Workflow
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class WorkflowManager(private val context: Context?, private val viewModel: AppViewModel) {
    private val gson: Gson
    private val inflater: LayoutInflater

    init {
        this.gson = Gson()
        this.inflater = LayoutInflater.from(context)
    }

    fun handleWorkflows(workflowsJson: String?) {
        try {
            val workflowsElement =
                gson.fromJson<JsonElement>(workflowsJson, JsonElement::class.java)
            val workflowsList: MutableList<Workflow> =
                ArrayList<Workflow>()

            if (workflowsElement.isJsonObject()) {
                workflowsList.add(parseWorkflowObject(workflowsElement.getAsJsonObject()))
            } else if (workflowsElement.isJsonArray()) {
                val workflowsArray = workflowsElement.getAsJsonArray()
                for (workflowElement in workflowsArray) {
                    if (workflowElement.isJsonObject()) {
                        workflowsList.add(parseWorkflowObject(workflowElement.getAsJsonObject()))
                    }
                }
            }

            viewModel.setAvailableWorkflows(workflowsList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse workflows: " + e.message)
        }
    }

    private fun parseWorkflowObject(workflow: JsonObject): Workflow {
        return Workflow(
            id = if (workflow.has("id")) workflow.get("id").getAsString() else null,
            name = if (workflow.has("name")) workflow.get("name").getAsString() else null,
            title = if (workflow.has("title")) workflow.get("title").getAsString() else null,
            subtitle = if (workflow.has("subtitle")) workflow.get("subtitle").getAsString() else null,
            description = if (workflow.has("description")) workflow.get("description").getAsString() else null,
            status = if (workflow.has("status")) workflow.get("status").getAsString() else null
        )
    }

    fun updateWorkflowsUI(
        workflows: MutableList<Workflow>?,
        container: LinearLayout,
        titleView: TextView
    ) {
        container.removeAllViews()

        if (workflows == null || workflows.isEmpty()) {
            titleView.setText("No workflows available")
        } else {
            titleView.setText("Available Workflows")

            for (workflow in workflows) {
                val workflowItem = inflater.inflate(R.layout.workflow_item, null)

                val workflowName = workflowItem.findViewById<TextView>(R.id.workflow_item_name)
                val workflowDescription =
                    workflowItem.findViewById<TextView>(R.id.workflow_item_description)

                // Use title if available, otherwise use name
                val displayName = workflow.title ?: workflow.name ?: "Unknown Workflow"
                workflowName.setText(displayName)
                
                // Use subtitle if available, otherwise use description
                val displayDescription = workflow.subtitle ?: workflow.description ?: ""
                workflowDescription.setText(displayDescription)

                container.addView(workflowItem)
            }
        }
    }

    companion object {
        private const val TAG = "WorkflowManager"
    }
}