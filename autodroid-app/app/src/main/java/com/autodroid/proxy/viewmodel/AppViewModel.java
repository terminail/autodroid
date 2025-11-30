package com.autodroid.proxy.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Map;

public class AppViewModel extends ViewModel {
    // Server information
    private final MutableLiveData<String> serverIp = new MutableLiveData<>();
    private final MutableLiveData<Boolean> serverConnected = new MutableLiveData<>(false);
    private final MutableLiveData<Map<String, Object>> serverInfo = new MutableLiveData<>();
    
    // Workflows information
    private final MutableLiveData<List<Map<String, Object>>> availableWorkflows = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> selectedWorkflow = new MutableLiveData<>();
    
    // Test reports information
    private final MutableLiveData<List<Map<String, Object>>> testReports = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> selectedReport = new MutableLiveData<>();
    
    // Real-time execution information
    private final MutableLiveData<Map<String, Object>> currentExecution = new MutableLiveData<>();
    private final MutableLiveData<String> executionLog = new MutableLiveData<>();
    
    // Device information
    private final MutableLiveData<String> deviceIp = new MutableLiveData<>();
    
    // Getters for LiveData
    public MutableLiveData<String> getServerIp() {
        return serverIp;
    }
    
    public MutableLiveData<Boolean> getServerConnected() {
        return serverConnected;
    }
    
    public MutableLiveData<Map<String, Object>> getServerInfo() {
        return serverInfo;
    }
    
    public MutableLiveData<List<Map<String, Object>>> getAvailableWorkflows() {
        return availableWorkflows;
    }
    
    public MutableLiveData<Map<String, Object>> getSelectedWorkflow() {
        return selectedWorkflow;
    }
    
    public MutableLiveData<List<Map<String, Object>>> getTestReports() {
        return testReports;
    }
    
    public MutableLiveData<Map<String, Object>> getSelectedReport() {
        return selectedReport;
    }
    
    public MutableLiveData<Map<String, Object>> getCurrentExecution() {
        return currentExecution;
    }
    
    public MutableLiveData<String> getExecutionLog() {
        return executionLog;
    }
    
    public MutableLiveData<String> getDeviceIp() {
        return deviceIp;
    }
    
    // Setters
    public void setServerIp(String ip) {
        serverIp.setValue(ip);
    }
    
    public void setServerConnected(boolean connected) {
        serverConnected.setValue(connected);
    }
    
    public void setServerInfo(Map<String, Object> info) {
        serverInfo.setValue(info);
        // Extract server IP from info if available
        if (info != null && info.containsKey("ip")) {
            serverIp.setValue(info.get("ip").toString());
        }
    }
    
    public void setAvailableWorkflows(List<Map<String, Object>> workflows) {
        availableWorkflows.setValue(workflows);
    }
    
    public void setSelectedWorkflow(Map<String, Object> workflow) {
        selectedWorkflow.setValue(workflow);
    }
    
    public void setTestReports(List<Map<String, Object>> reports) {
        testReports.setValue(reports);
    }
    
    public void setSelectedReport(Map<String, Object> report) {
        selectedReport.setValue(report);
    }
    
    public void setCurrentExecution(Map<String, Object> execution) {
        currentExecution.setValue(execution);
    }
    
    public void appendExecutionLog(String logEntry) {
        String currentLog = executionLog.getValue() != null ? executionLog.getValue() : "";
        executionLog.setValue(currentLog + logEntry + "\n");
    }
    
    public void clearExecutionLog() {
        executionLog.setValue("");
    }
    
    public void setDeviceIp(String ip) {
        deviceIp.setValue(ip);
    }
}
