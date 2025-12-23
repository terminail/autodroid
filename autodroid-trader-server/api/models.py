"""
Pydantic models for Autodroid API

This module contains all the data models used in the Autodroid API.
Separating models from the main application logic improves code organization and maintainability.
"""

from pydantic import BaseModel
from typing import Dict, List, Any, Optional
from datetime import datetime


class EventTrigger(BaseModel):
    """Model for triggering events"""
    event_type: str
    event_data: Dict[str, Any]


class DeviceRegistration(BaseModel):
    """Model for device registration"""
    serialno: str
    name: str
    platform: str
    model: str
    ipAddress: str
    port: int


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