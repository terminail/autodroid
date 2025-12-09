// ReportDetailFragment.kt
package com.autodroid.manager.ui.reports.detail

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.autodroid.manager.R
import com.autodroid.manager.model.Report
import com.autodroid.manager.ui.BaseFragment

class ReportDetailFragment : BaseFragment() {
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
    private lateinit var workScriptNameTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var startTimeTextView: TextView
    private lateinit var durationTextView: TextView
    private lateinit var stepsTextView: TextView

    override fun getLayoutId(): Int {
        return R.layout.fragment_report_detail
    }

    override fun initViews(view: View) {
        reportIdTextView = view.findViewById(R.id.report_id)
        workScriptNameTextView = view.findViewById(R.id.report_workscript_name)
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
        val mockReport = Report.mock().copy(id = reportId)
        updateReportUI(mockReport)
    }

    private fun updateReportUI(report: Report) {
        reportIdTextView.text = "Report ID: ${report.id ?: "N/A"}"
        workScriptNameTextView.text = "WorkScript: ${report.workScript ?: "N/A"}"
        statusTextView.text = "Status: ${report.status ?: "N/A"}"
        startTimeTextView.text = "Start Time: ${report.createdAt ?: "N/A"}"
        durationTextView.text = "Duration: ${report.duration ?: "N/A"}"
        
        // Format steps for display
        val stepsText = report.steps?.joinToString("\n") { step ->
            "Step ${step.stepNumber}: ${step.action} - ${step.status} (${step.duration})"
        } ?: "N/A"
        stepsTextView.text = "Steps:\n$stepsText"

        // Set status text color based on status
        val status = report.status ?: ""
        when (status.uppercase()) {
            "COMPLETED", "SUCCESS" -> statusTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            "FAILED" -> statusTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            else -> statusTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
    }
}