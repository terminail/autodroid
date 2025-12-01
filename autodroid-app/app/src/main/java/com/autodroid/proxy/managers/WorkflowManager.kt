// WorkflowManager.java
package com.autodroid.proxy.managers

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.autodroid.proxy.R
import com.autodroid.proxy.viewmodel.AppViewModel
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
            val workflowsList: MutableList<MutableMap<String?, Any?>?> =
                ArrayList<MutableMap<String?, Any?>?>()

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

    private fun parseWorkflowObject(workflow: JsonObject): MutableMap<String?, Any?> {
        val workflowMap: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        if (workflow.has("name")) {
            workflowMap.put("name", workflow.get("name").getAsString())
        }
        if (workflow.has("description")) {
            workflowMap.put("description", workflow.get("description").getAsString())
        }
        if (workflow.has("id")) {
            workflowMap.put("id", workflow.get("id").getAsString())
        }
        return workflowMap
    }

    fun updateWorkflowsUI(
        workflows: MutableList<MutableMap<String?, Any?>>?,
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

                workflowName.setText(workflow.get("name") as String?)
                workflowDescription.setText(workflow.get("description") as String?)

                container.addView(workflowItem)
            }
        }
    }

    companion object {
        private const val TAG = "WorkflowManager"
    }
}