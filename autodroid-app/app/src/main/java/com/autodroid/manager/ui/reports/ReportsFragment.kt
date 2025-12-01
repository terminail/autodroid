// ReportsFragment.kt
package com.autodroid.manager.ui.reports

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

class ReportsFragment : BaseFragment() {
    private var reportsTitleTextView: TextView? = null
    private var reportsRecyclerView: RecyclerView? = null
    private var adapter: BaseItemAdapter? = null
    private var reportItems: MutableList<MutableMap<String?, Any?>?>? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel =
            ViewModelProvider(requireActivity()).get<AppViewModel>(AppViewModel::class.java)
        reportItems = ArrayList<MutableMap<String?, Any?>?>()
        setupMockData()
    }

    override val layoutId: Int
        get() = R.layout.fragment_reports

    override fun initViews(view: View?) {
        reportsTitleTextView = view?.findViewById<TextView?>(R.id.reports_title)
        reportsRecyclerView = view?.findViewById<RecyclerView>(R.id.reports_recycler_view)


        // Set up RecyclerView
        reportsRecyclerView!!.setLayoutManager(LinearLayoutManager(getContext()))
        adapter = BaseItemAdapter(
            reportItems,
            BaseItemAdapter.OnItemClickListener { item: MutableMap<String?, Any?>? ->
                // Handle item click - open report detail
                openReportDetail(item!!)
            },
            R.layout.item_generic
        )
        reportsRecyclerView!!.setAdapter(adapter)
    }

    override fun setupObservers() {
        // Setup observers for reports data
    }

    private fun setupMockData() {
        // Add mock report items
        val report1: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        report1.put("title", "Login Test Report")
        report1.put("subtitle", "Completed - 2024-01-01 10:30")
        report1.put("status", "Success")
        report1.put("id", "1")
        reportItems!!.add(report1)

        val report2: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        report2.put("title", "Purchase Test Report")
        report2.put("subtitle", "Completed - 2024-01-01 11:45")
        report2.put("status", "Failed")
        report2.put("id", "2")
        reportItems!!.add(report2)

        val report3: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        report3.put("title", "Settings Test Report")
        report3.put("subtitle", "Running - 2024-01-01 12:15")
        report3.put("status", "In Progress")
        report3.put("id", "3")
        reportItems!!.add(report3)
    }

    private fun openReportDetail(report: MutableMap<String?, Any?>) {
        // Navigate to ReportDetailFragment using Navigation Component
        val reportId = report.get("id") as String?
        
        if (reportId != null) {
            val action = ReportsFragmentDirections.actionNavReportsToReportDetailFragment(reportId)
            findNavController().navigate(action)
        }
    }
}