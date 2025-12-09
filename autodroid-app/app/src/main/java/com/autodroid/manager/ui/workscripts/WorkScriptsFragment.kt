// WorkScriptsFragment.kt
package com.autodroid.manager.ui.workscripts

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
import com.autodroid.manager.model.WorkScript
import com.google.gson.Gson

class WorkScriptsFragment : BaseFragment() {
    private var workScriptsTitleTextView: TextView? = null
    private var workScriptsRecyclerView: RecyclerView? = null
    private var adapter: WorkScriptAdapter? = null
    private var workScriptItems: MutableList<WorkScript>? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workScriptItems = ArrayList<WorkScript>()
        setupMockData()
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_workscripts
    }

    override fun initViews(view: View) {
        workScriptsTitleTextView = view?.findViewById<TextView?>(R.id.workscripts_title)
        workScriptsRecyclerView = view?.findViewById<RecyclerView>(R.id.workscripts_recycler_view)


        // Set up RecyclerView
        workScriptsRecyclerView!!.setLayoutManager(LinearLayoutManager(getContext()))
        adapter = WorkScriptAdapter(
            workScriptItems,
            object : WorkScriptAdapter.OnWorkScriptClickListener {
                override fun onWorkScriptClick(workScript: WorkScript?) {
                    // Handle item click - open workscript detail
                    openWorkScriptDetail(workScript)
                }
            }
        )
        workScriptsRecyclerView!!.setAdapter(adapter)
    }

    override fun setupObservers() {
        // Setup observers for workScripts data
    }

    private fun setupMockData() {
        // Add mock workscript items
        val workScript1 = WorkScript(
            id = "1",
            title = "Login WorkScript",
            subtitle = "com.example.app",
            status = "Active"
        )
        workScriptItems!!.add(workScript1)

        val workScript2 = WorkScript(
            id = "2",
            title = "Purchase WorkScript",
            subtitle = "com.shopping.app",
            status = "Inactive"
        )
        workScriptItems!!.add(workScript2)

        val workScript3 = WorkScript(
            id = "3",
            title = "Settings WorkScript",
            subtitle = "com.settings.app",
            status = "Active"
        )
        workScriptItems!!.add(workScript3)
    }

    private fun openWorkScriptDetail(workScript: WorkScript?) {
        // Get workscript ID
        val workScriptId = workScript?.id
        
        // Navigate to WorkScriptDetailFragment using Navigation Component
        if (workScriptId != null) {
            val action = WorkScriptsFragmentDirections.actionNavWorkscriptsToWorkscriptDetailFragment(workScriptId)
            findNavController().navigate(action)
        }
    }
}