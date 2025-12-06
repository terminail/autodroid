// WorkflowsFragment.kt
package com.autodroid.manager.ui.workflows

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.manager.R
import com.autodroid.manager.ui.BaseFragment
import com.autodroid.manager.ui.adapters.BaseItemAdapter
import com.autodroid.manager.AppViewModel
import com.autodroid.manager.model.Workflow
import com.google.gson.Gson

class WorkflowsFragment : BaseFragment() {
    private var workflowsTitleTextView: TextView? = null
    private var workflowsRecyclerView: RecyclerView? = null
    private var adapter: WorkflowAdapter? = null
    private var workflowItems: MutableList<Workflow>? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workflowItems = ArrayList<Workflow>()
        setupMockData()
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_workflows
    }

    override fun initViews(view: View) {
        workflowsTitleTextView = view?.findViewById<TextView?>(R.id.workflows_title)
        workflowsRecyclerView = view?.findViewById<RecyclerView>(R.id.workflows_recycler_view)


        // Set up RecyclerView
        workflowsRecyclerView!!.setLayoutManager(LinearLayoutManager(getContext()))
        adapter = WorkflowAdapter(
            workflowItems,
            object : WorkflowAdapter.OnWorkflowClickListener {
                override fun onWorkflowClick(workflow: Workflow?) {
                    // Handle item click - open workflow detail
                    openWorkflowDetail(workflow)
                }
            }
        )
        workflowsRecyclerView!!.setAdapter(adapter)
    }

    override fun setupObservers() {
        // Setup observers for workflows data
    }

    private fun setupMockData() {
        // Add mock workflow items
        val workflow1 = Workflow(
            id = "1",
            title = "Login Workflow",
            subtitle = "com.example.app",
            status = "Active"
        )
        workflowItems!!.add(workflow1)

        val workflow2 = Workflow(
            id = "2",
            title = "Purchase Workflow",
            subtitle = "com.shopping.app",
            status = "Inactive"
        )
        workflowItems!!.add(workflow2)

        val workflow3 = Workflow(
            id = "3",
            title = "Settings Workflow",
            subtitle = "com.settings.app",
            status = "Active"
        )
        workflowItems!!.add(workflow3)
    }

    private fun openWorkflowDetail(workflow: Workflow?) {
        // Get workflow ID
        val workflowId = workflow?.id
        
        // Navigate to WorkflowDetailFragment using Navigation Component
        if (workflowId != null) {
            val action = WorkflowsFragmentDirections.actionNavWorkflowsToWorkflowDetailFragment(workflowId)
            findNavController().navigate(action)
        }
    }
}