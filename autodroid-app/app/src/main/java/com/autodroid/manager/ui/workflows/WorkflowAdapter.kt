// WorkflowAdapter.kt
package com.autodroid.manager.ui.workflows

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.manager.R
import com.autodroid.manager.ui.workflows.WorkflowAdapter.WorkflowViewHolder

class WorkflowAdapter(
    private var workflows: MutableList<MutableMap<String?, Any?>>?,
    private val listener: OnWorkflowClickListener?
) : RecyclerView.Adapter<WorkflowViewHolder?>() {
    interface OnWorkflowClickListener {
        fun onWorkflowClick(workflow: MutableMap<String?, Any?>?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkflowViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.workflow_item, parent, false)
        return WorkflowViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkflowViewHolder, position: Int) {
        val workflow = workflows!!.get(position)
        holder.bind(workflow)
    }

    override fun getItemCount(): Int {
        return if (workflows != null) workflows!!.size else 0
    }

    fun updateWorkflows(newWorkflows: MutableList<MutableMap<String?, Any?>>?) {
        this.workflows = newWorkflows
        notifyDataSetChanged()
    }

    inner class WorkflowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView
        private val descriptionTextView: TextView
        private val packageTextView: TextView

        init {
            nameTextView = itemView.findViewById<TextView>(R.id.workflow_item_name)
            descriptionTextView = itemView.findViewById<TextView>(R.id.workflow_item_description)
            packageTextView = itemView.findViewById<TextView>(R.id.workflow_item_package)
        }

        fun bind(workflow: MutableMap<String?, Any?>) {
            nameTextView.setText(workflow.getOrDefault("name", "Unknown Workflow") as String?)

            val description = workflow.getOrDefault("description", "") as String?
            if (description!!.isEmpty()) {
                descriptionTextView.setVisibility(View.GONE)
            } else {
                descriptionTextView.setVisibility(View.VISIBLE)
                descriptionTextView.setText(description)
            }

            val metadata = workflow.getOrDefault("metadata", null) as MutableMap<String?, Any?>?
            if (metadata != null) {
                val packageName = metadata.getOrDefault("app_package", "") as String?
                if (!packageName!!.isEmpty()) {
                    packageTextView.setText(packageName)
                    packageTextView.setVisibility(View.VISIBLE)
                } else {
                    packageTextView.setVisibility(View.GONE)
                }
            } else {
                packageTextView.setVisibility(View.GONE)
            }

            itemView.setOnClickListener(View.OnClickListener { v: View? ->
                if (listener != null) {
                    listener.onWorkflowClick(workflow)
                }
            })
        }
    }
}