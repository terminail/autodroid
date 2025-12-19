"""
Integration test for mDNS service discovery.
This test verifies that the autodroid server properly publishes its service through mDNS.
"""

import asyncio
import socket
import pytest
import time
from typing import List, Dict, Any
from zeroconf import ServiceListener, Zeroconf


class AutodroidServiceListener(ServiceListener):
    """Service listener for autodroid mDNS services"""
    
    def __init__(self):
        self.discovered_services: List[Dict[str, Any]] = []
        self.discovery_complete = asyncio.Event()
    
    def update_service(self, zc: Zeroconf, type_: str, name: str) -> None:
        """Called when a service is updated"""
        print(f"Service updated: {name} ({type_})")
    
    def remove_service(self, zc: Zeroconf, type_: str, name: str) -> None:
        """Called when a service is removed"""
        print(f"Service removed: {name} ({type_})")
    
    def add_service(self, zc: Zeroconf, type_: str, name: str) -> None:
        """Called when a service is discovered"""
        print(f"Service discovered: {name} ({type_})")
        
        # Get service info
        info = zc.get_service_info(type_, name)
        if info:
            service_data = {
                'name': name,
                'type': type_,
                'addresses': [socket.inet_ntoa(addr) for addr in info.addresses],
                'port': info.port,
                'properties': info.properties
            }
            self.discovered_services.append(service_data)
            print(f"Service details: {service_data}")
            
            # Signal that we've discovered at least one service
            self.discovery_complete.set()


def test_mdns_service_discovery():
    """
    Integration test that discovers autodroid server through mDNS. 
    This test acts as a client and verifies that the server is properly publishing its service.
    """
    
    # Create service listener
    listener = AutodroidServiceListener()
    
    # Create zeroconf instance
    zc = Zeroconf()
    
    try:
        # Start browsing for autodroid services
        service_type = "_autodroid._tcp.local."
        zc.add_service_listener(service_type, listener)
        
        print(f"Started browsing for services of type: {service_type}")
        
        # Wait for service discovery with timeout (30 seconds)
        start_time = time.time()
        timeout = 30
        
        while time.time() - start_time < timeout:
            if listener.discovery_complete.is_set():
                break
            time.sleep(0.5)
        
        if not listener.discovery_complete.is_set():
            pytest.fail("Timeout waiting for mDNS service discovery")
        
        print("Service discovery completed successfully")
        
        # Verify we discovered at least one service
        assert len(listener.discovered_services) > 0, "No services discovered"
        
        # Verify service details
        service = listener.discovered_services[0]
        assert service['type'] == "_autodroid._tcp.local."
        assert service['port'] == 8004
        assert 'Autodroid Server' in service['name']
        assert len(service['addresses']) > 0
        
        print(f"✓ Successfully discovered service: {service['name']}")
        print(f"  Addresses: {service['addresses']}")
        print(f"  Port: {service['port']}")
        print(f"  Properties: {service['properties']}")
        
    finally:
        # Clean up
        zc.remove_service_listener(listener)
        zc.close()


def test_mdns_service_discovery_multiple_attempts():
    """
    Test that tries multiple attempts to discover the service.
    This is useful if the service takes some time to register.
    """
    
    max_attempts = 3
    attempt = 0
    
    while attempt < max_attempts:
        attempt += 1
        print(f"Attempt {attempt}/{max_attempts} to discover service...")
        
        try:
            test_mdns_service_discovery()
            print(f"✓ Service discovered on attempt {attempt}")
            return
        except AssertionError as e:
            if attempt == max_attempts:
                pytest.fail(f"Failed to discover service after {max_attempts} attempts: {e}")
            print(f"Attempt {attempt} failed: {e}")
            time.sleep(5)  # Wait 5 seconds before retry


if __name__ == "__main__":
    # Run the test directly
    print("Running mDNS service discovery test...")
    test_mdns_service_discovery()
    print("Test completed successfully!")