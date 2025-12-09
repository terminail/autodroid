// WorkScriptManager.java
package com.autodroid.manager.managers

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.autodroid.manager.R
import com.autodroid.manager.AppViewModel
import com.autodroid.manager.model.WorkScript
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class WorkScriptManager(private val context: Context?, private val viewModel: AppViewModel) {
    private val gson: Gson
    private val inflater: LayoutInflater

    init {
        this.gson = Gson()
        this.inflater = LayoutInflater.from(context)
    }

    fun handleWorkScripts(workscriptsJson: String?) {
        try {
            val workscriptsElement =
                gson.fromJson<JsonElement>(workscriptsJson, JsonElement::class.java)
            val workscriptsList: MutableList<WorkScript> =
                ArrayList<WorkScript>()

            if (workscriptsElement.isJsonObject()) {
                workscriptsList.add(parseWorkScriptObject(workscriptsElement.getAsJsonObject()))
            } else if (workscriptsElement.isJsonArray()) {
                val workscriptsArray = workscriptsElement.getAsJsonArray()
                for (workscriptElement in workscriptsArray) {
                    if (workscriptElement.isJsonObject()) {
                        workscriptsList.add(parseWorkScriptObject(workscriptElement.getAsJsonObject()))
                    }
                }
            }

            viewModel.setAvailableWorkScripts(workscriptsList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse workscripts: " + e.message)
        }
    }

    private fun parseWorkScriptObject(workscript: JsonObject): WorkScript {
        return WorkScript(
            id = if (workscript.has("id")) workscript.get("id").getAsString() else null,
            name = if (workscript.has("name")) workscript.get("name").getAsString() else null,
            title = if (workscript.has("title")) workscript.get("title").getAsString() else null,
            subtitle = if (workscript.has("subtitle")) workscript.get("subtitle").getAsString() else null,
            description = if (workscript.has("description")) workscript.get("description").getAsString() else null,
            status = if (workscript.has("status")) workscript.get("status").getAsString() else null
        )
    }

    fun updateWorkScriptsUI(
        workscripts: MutableList<WorkScript>?,
        container: LinearLayout,
        titleView: TextView
    ) {
        container.removeAllViews()

        if (workscripts == null || workscripts.isEmpty()) {
            titleView.setText("No workscripts available")
        } else {
            titleView.setText("Available WorkScripts")

            for (workscript in workscripts) {
                val workscriptItem = inflater.inflate(R.layout.workscript_item, null)

                val workscriptName = workscriptItem.findViewById<TextView>(R.id.workscript_item_name)
                val workscriptDescription =
                    workscriptItem.findViewById<TextView>(R.id.workscript_item_description)

                // Use title if available, otherwise use name
                val displayName = workscript.title ?: workscript.name ?: "Unknown WorkScript"
                workscriptName.setText(displayName)
                
                // Use subtitle if available, otherwise use description
                val displayDescription = workscript.subtitle ?: workscript.description ?: ""
                workscriptDescription.setText(displayDescription)

                container.addView(workscriptItem)
            }
        }
    }

    companion object {
        private const val TAG = "WorkScriptManager"
    }
}