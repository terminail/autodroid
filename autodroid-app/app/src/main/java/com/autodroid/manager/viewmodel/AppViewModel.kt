package com.autodroid.manager.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AppViewModel : ViewModel() {
    // Getters for LiveData
    // Server information - unified object from mDNS discovery
    val serverInfo = MutableLiveData<MutableMap<String?, Any?>?>()

    // Workflows information
    val availableWorkflows = MutableLiveData<MutableList<MutableMap<String?, Any?>?>?>()
    val selectedWorkflow = MutableLiveData<MutableMap<String?, Any?>?>()

    // Test reports information
    val testReports = MutableLiveData<MutableList<MutableMap<String?, Any?>?>?>()
    val selectedReport = MutableLiveData<MutableMap<String?, Any?>?>()

    // Real-time execution information
    val currentExecution = MutableLiveData<MutableMap<String?, Any?>?>()
    val executionLog = MutableLiveData<String?>()

    // Device information
    val deviceIp = MutableLiveData<String?>()

    // Setters
    fun setServerInfo(info: MutableMap<String?, Any?>?) {
        serverInfo.setValue(info)
    }

    fun setAvailableWorkflows(workflows: MutableList<MutableMap<String?, Any?>?>?) {
        availableWorkflows.setValue(workflows)
    }

    fun setSelectedWorkflow(workflow: MutableMap<String?, Any?>?) {
        selectedWorkflow.setValue(workflow)
    }

    fun setTestReports(reports: MutableList<MutableMap<String?, Any?>?>?) {
        testReports.setValue(reports)
    }

    fun setSelectedReport(report: MutableMap<String?, Any?>?) {
        selectedReport.setValue(report)
    }

    fun setCurrentExecution(execution: MutableMap<String?, Any?>?) {
        currentExecution.setValue(execution)
    }

    fun appendExecutionLog(logEntry: String?) {
        val currentLog = if (executionLog.getValue() != null) executionLog.getValue() else ""
        executionLog.setValue(currentLog + logEntry + "\n")
    }

    fun clearExecutionLog() {
        executionLog.setValue("")
    }

    fun setDeviceIp(ip: String?) {
        deviceIp.setValue(ip)
    }
}
