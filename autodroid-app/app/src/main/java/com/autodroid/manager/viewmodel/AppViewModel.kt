package com.autodroid.manager.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AppViewModel : ViewModel() {
    // Getters for LiveData
    // Server information - unified object from mDNS discovery
    val serverInfo = MutableLiveData<MutableMap<String?, Any?>?>()
    
    // Discovery status information
    val discoveryInProgress = MutableLiveData<Boolean>()
    val discoveryRetryCount = MutableLiveData<Int>()
    val discoveryMaxRetries = MutableLiveData<Int>()
    val discoveryFailed = MutableLiveData<Boolean>()

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
    val deviceInfo = MutableLiveData<MutableMap<String?, Any?>?>()

    // WiFi information
    val wifiInfo = MutableLiveData<MutableMap<String?, Any?>?>()
    
    // Network information
    val networkInfo = MutableLiveData<MutableMap<String?, Any?>?>()
    
    // APK information
    val apkInfo = MutableLiveData<MutableMap<String?, Any?>?>()
    val apkScanStatus = MutableLiveData<String?>()

    // Setters
    fun setServerInfo(info: MutableMap<String?, Any?>?) {
        serverInfo.setValue(info)
    }
    
    fun setWifiInfo(info: MutableMap<String?, Any?>?) {
        wifiInfo.setValue(info)
    }
    
    fun setNetworkInfo(info: MutableMap<String?, Any?>?) {
        networkInfo.setValue(info)
    }
    
    fun setDeviceInfo(info: MutableMap<String?, Any?>?) {
        deviceInfo.setValue(info)
    }
    
    fun setDiscoveryStatus(inProgress: Boolean, retryCount: Int, maxRetries: Int) {
        discoveryInProgress.setValue(inProgress)
        discoveryRetryCount.setValue(retryCount)
        discoveryMaxRetries.setValue(maxRetries)
    }
    
    fun setDiscoveryFailed(failed: Boolean) {
        discoveryFailed.setValue(failed)
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
    
    fun setApkInfo(info: MutableMap<String?, Any?>?) {
        apkInfo.setValue(info)
    }
    
    fun setApkScanStatus(status: String?) {
        apkScanStatus.setValue(status)
    }
}