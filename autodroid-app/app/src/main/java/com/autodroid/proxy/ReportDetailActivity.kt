// ReportDetailActivity.kt
package com.autodroid.proxy

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.autodroid.proxy.viewmodel.AppViewModel
import com.google.gson.Gson
import java.util.*

class ReportDetailActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ReportDetailActivity"
    }

    private lateinit var reportIdTextView: TextView
    private lateinit var workflowNameTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var startTimeTextView: TextView
    private lateinit var durationTextView: TextView
    private lateinit var stepsTextView: TextView

    private lateinit var viewModel: AppViewModel
    private lateinit var gson: Gson

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_detail)

        // Initialize Gson
        gson = Gson()

        // Initialize UI components
        reportIdTextView = findViewById(R.id.report_id)
        workflowNameTextView = findViewById(R.id.report_workflow_name)
        statusTextView = findViewById(R.id.report_status)
        startTimeTextView = findViewById(R.id.report_start_time)
        durationTextView = findViewById(R.id.report_duration)
        stepsTextView = findViewById(R.id.report_steps)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        // Get report ID from intent
        val reportId = intent.getStringExtra("report_id")

        // Observe selected report changes
        viewModel.selectedReport.observe(this) { report ->
            if (report != null) {
                updateReportUI(report as Map<String, Any>)
            }
        }

        // Load report details (in a real app, this would fetch from server or database)
        if (reportId != null) {
            loadReportDetails(reportId)
        }
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

        viewModel.setSelectedReport(mockReport as MutableMap<String?, Any?>?)
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
            "success" -> statusTextView.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            "failed" -> statusTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            else -> statusTextView.setTextColor(resources.getColor(android.R.color.black))
        }
    }
}