// WorkflowDetailFragment.kt
package com.autodroid.proxy.ui.workflows.detail

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.autodroid.proxy.R
import com.autodroid.proxy.ui.BaseDetailFragment

class WorkflowDetailFragment : BaseDetailFragment() {
    companion object {
        private const val ARG_WORKFLOW_ID = "workflow_id"
        
        fun newInstance(workflowId: String): WorkflowDetailFragment {
            val fragment = WorkflowDetailFragment()
            val args = Bundle()
            args.putString(ARG_WORKFLOW_ID, workflowId)
            fragment.arguments = args
            return fragment
        }
    }
    
    private lateinit var workflowName: TextView
    private lateinit var workflowDescription: TextView

    override fun getLayoutId(): Int {
        return R.layout.fragment_workflow_detail
    }

    override fun initViews(view: View) {
        workflowName = view.findViewById(R.id.workflow_detail_name)
        workflowDescription = view.findViewById(R.id.workflow_detail_description)
        
        // Get workflow ID from arguments
        val args = arguments
        if (args != null) {
            val workflowId = args.getString(ARG_WORKFLOW_ID)
            if (workflowId != null) {
                workflowName.text = "Workflow ID: $workflowId"
                workflowDescription.text = "This is a detailed view of workflow #$workflowId."
            }
        }
        
        // Set up back button
        val backButton = view.findViewById<TextView>(R.id.workflow_detail_back_button)
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun setupObservers() {
        // No observers needed for this simple implementation
    }
}