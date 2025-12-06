// WorkflowAdapter.kt
package com.autodroid.manager.ui.workflows

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.manager.R
import com.autodroid.manager.model.Workflow
import com.autodroid.manager.ui.workflows.WorkflowAdapter.WorkflowViewHolder

class WorkflowAdapter(
    private var workflows: MutableList<Workflow>?,
    private val listener: OnWorkflowClickListener?
) : RecyclerView.Adapter<WorkflowViewHolder?>() {
    interface OnWorkflowClickListener {
        fun onWorkflowClick(workflow: Workflow?)
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

    fun updateWorkflows(newWorkflows: MutableList<Workflow>?) {
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

        fun bind(workflow: Workflow) {
            // Use title if available, otherwise use name
            val displayName = workflow.title ?: workflow.name ?: "Unknown Workflow"
            nameTextView.setText(displayName)

            // Use subtitle if available, otherwise use description
            val displayDescription = workflow.subtitle ?: workflow.description ?: ""
            if (displayDescription.isEmpty()) {
                descriptionTextView.setVisibility(View.GONE)
            } else {
                descriptionTextView.setVisibility(View.VISIBLE)
                descriptionTextView.setText(displayDescription)
            }

            // Show status if available
            if (!workflow.status.isNullOrEmpty()) {
                packageTextView.setText(workflow.status)
                packageTextView.setVisibility(View.VISIBLE)
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