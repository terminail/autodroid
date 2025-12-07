"""
Pydantic models for Autodroid API

This module contains all the data models used in the Autodroid API.
Separating models from the main application logic improves code organization and maintainability.
"""

from pydantic import BaseModel
from typing import Dict, List, Any, Optional


class WorkflowCreate(BaseModel):
    """Model for creating a new workflow"""
    name: str
    description: str
    metadata: Dict[str, str]
    device_selection: Dict[str, Any]
    steps: List[Dict[str, Any]]
    schedule: Optional[Dict[str, Any]] = None


class WorkflowPlanCreate(BaseModel):
    """Model for creating a new workflow plan"""
    workflow_id: str
    device_udid: str
    enabled: bool = True
    schedule: Dict[str, Any]
    priority: int = 0


class EventTrigger(BaseModel):
    """Model for triggering events"""
    event_type: str
    event_data: Dict[str, Any]


class DeviceRegistration(BaseModel):
    """Model for device registration"""
    udid: str
    name: str
    platform: str
    model: str
    ipAddress: str
    port: int


class WorkflowExecution(BaseModel):
    """Model for workflow execution request"""
    workflow_id: str
    device_udid: str
    parameters: Optional[Dict[str, Any]] = None


class ExecutionResult(BaseModel):
    """Model for workflow execution result"""
    success: bool
    message: str
    execution_id: str
    duration: float
    error: Optional[str] = None
    logs: Optional[List[str]] = None


class ServerInfo(BaseModel):
    """Model for server information response"""
    name: str
    hostname: str
    ipAddress: str
    platform: str
    apiEndpoint: str
    services: Dict[str, str]
    capabilities: Dict[str, bool]


class HealthCheck(BaseModel):
    """Model for health check response"""
    status: str
    timestamp: float
    services: Dict[str, str]


class WiFiNetwork(BaseModel):
    """Model for WiFi network information"""
    ssid: str
    bssid: str
    frequency: int
    signal_level: int
    capabilities: str
    ipAddress: Optional[str] = None


class WiFiList(BaseModel):
    """Model for list of WiFi networks"""
    networks: List[WiFiNetwork]
    server_ip: str
    recommendation: Optional[str] = None