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
import com.autodroid.manager.viewmodel.AppViewModel
import com.google.gson.Gson

class WorkflowsFragment : BaseFragment() {
    private var workflowsTitleTextView: TextView? = null
    private var workflowsRecyclerView: RecyclerView? = null
    private var adapter: BaseItemAdapter? = null
    private var workflowItems: MutableList<MutableMap<String?, Any?>?>? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel =
            ViewModelProvider(requireActivity()).get<AppViewModel>(AppViewModel::class.java)
        workflowItems = ArrayList<MutableMap<String?, Any?>?>()
        setupMockData()
    }

    override val layoutId: Int
        get() = R.layout.fragment_workflows

    override fun initViews(view: View?) {
        workflowsTitleTextView = view?.findViewById<TextView?>(R.id.workflows_title)
        workflowsRecyclerView = view?.findViewById<RecyclerView>(R.id.workflows_recycler_view)


        // Set up RecyclerView
        workflowsRecyclerView!!.setLayoutManager(LinearLayoutManager(getContext()))
        adapter = BaseItemAdapter(
            workflowItems,
            BaseItemAdapter.OnItemClickListener { item: MutableMap<String?, Any?>? ->
                // Handle item click - open workflow detail
                openWorkflowDetail(item)
            },
            R.layout.item_generic
        )
        workflowsRecyclerView!!.setAdapter(adapter)
    }

    override fun setupObservers() {
        // Setup observers for workflows data
    }

    private fun setupMockData() {
        // Add mock workflow items
        val workflow1: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        workflow1.put("title", "Login Workflow")
        workflow1.put("subtitle", "com.example.app")
        workflow1.put("status", "Active")
        workflow1.put("id", "1")
        workflowItems!!.add(workflow1)

        val workflow2: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        workflow2.put("title", "Purchase Workflow")
        workflow2.put("subtitle", "com.shopping.app")
        workflow2.put("status", "Inactive")
        workflow2.put("id", "2")
        workflowItems!!.add(workflow2)

        val workflow3: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        workflow3.put("title", "Settings Workflow")
        workflow3.put("subtitle", "com.settings.app")
        workflow3.put("status", "Active")
        workflow3.put("id", "3")
        workflowItems!!.add(workflow3)
    }

    private fun openWorkflowDetail(workflow: MutableMap<String?, Any?>?) {
        // Get workflow ID
        val workflowId = workflow?.get("id") as String?
        
        // Navigate to WorkflowDetailFragment using Navigation Component
        if (workflowId != null) {
            val action = WorkflowsFragmentDirections.actionNavWorkflowsToWorkflowDetailFragment(workflowId)
            findNavController().navigate(action)
        }
    }
}