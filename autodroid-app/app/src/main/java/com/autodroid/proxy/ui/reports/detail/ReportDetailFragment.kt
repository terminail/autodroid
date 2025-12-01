// ReportDetailFragment.kt
package com.autodroid.proxy.ui.reports.detail

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.autodroid.proxy.R
import com.autodroid.proxy.ui.BaseDetailFragment
import java.util.*

class ReportDetailFragment : BaseDetailFragment() {
    companion object {
        private const val ARG_REPORT_ID = "report_id"
        
        fun newInstance(reportId: String): ReportDetailFragment {
            val fragment = ReportDetailFragment()
            val args = Bundle()
            args.putString(ARG_REPORT_ID, reportId)
            fragment.arguments = args
            return fragment
        }
    }
    
    private lateinit var reportIdTextView: TextView
    private lateinit var workflowNameTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var startTimeTextView: TextView
    private lateinit var durationTextView: TextView
    private lateinit var stepsTextView: TextView

    override fun getLayoutId(): Int {
        return R.layout.fragment_report_detail
    }

    override fun initViews(view: View) {
        reportIdTextView = view.findViewById(R.id.report_id)
        workflowNameTextView = view.findViewById(R.id.report_workflow_name)
        statusTextView = view.findViewById(R.id.report_status)
        startTimeTextView = view.findViewById(R.id.report_start_time)
        durationTextView = view.findViewById(R.id.report_duration)
        stepsTextView = view.findViewById(R.id.report_steps)
        
        // Get report ID from arguments
        val args = arguments
        if (args != null) {
            val reportId = args.getString(ARG_REPORT_ID)
            if (reportId != null) {
                loadReportDetails(reportId)
            }
        }
        
        // Set up back button
        val backButton: TextView = view.findViewById(R.id.report_back_button)
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun setupObservers() {
        // Setup observers for report detail data
    }

    private fun loadReportDetails(reportId: String) {
        // In a real app, this would fetch the report details from the server
        // For now, we'll just use a mock report
        val mockReport: MutableMap<String, Any> = HashMap()
        mockReport["id"] = reportId
        mockReport["workflow_name"] = "Sample Workflow"
        mockReport["status"] = "success"
        mockReport["start_time"] = "2024-01-01 12:00:00"
        mockReport["duration"] = "30.5"
        mockReport["steps"] = "Step 1: Launch App - Success\nStep 2: Click Button - Success\nStep 3: Verify Result - Success"

        updateReportUI(mockReport)
    }

    private fun updateReportUI(report: Map<String, Any>) {
        reportIdTextView.text = "Report ID: ${report.getOrDefault("id", "N/A")}"
        workflowNameTextView.text = "Workflow: ${report.getOrDefault("workflow_name", "N/A")}"
        statusTextView.text = "Status: ${report.getOrDefault("status", "N/A")}"
        startTimeTextView.text = "Start Time: ${report.getOrDefault("start_time", "N/A")}"
        durationTextView.text = "Duration: ${report.getOrDefault("duration", "N/A")} seconds"
        stepsTextView.text = "Steps:\n${report.getOrDefault("steps", "N/A")}"

        // Set status text color based on status
        val status = report.getOrDefault("status", "").toString()
        when (status) {
            "success" -> statusTextView.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            "failed" -> statusTextView.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
            else -> statusTextView.setTextColor(requireContext().getColor(android.R.color.black))
        }
    }
}