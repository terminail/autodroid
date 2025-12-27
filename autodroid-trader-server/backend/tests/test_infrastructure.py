"""
Integration tests for infrastructure services.
This module contains tests for mDNS service discovery and network connectivity.
"""

import asyncio
import socket
import subprocess
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
            self.discovery_complete.set()


class TestMdnsDiscovery:
    """Tests for mDNS service discovery functionality"""
    
    def test_mdns_service_discovery(self):
        """
        Integration test that discovers autodroid server through mDNS. 
        This test acts as a client and verifies that the server is properly 
        publishing its service.
        """
        
        listener = AutodroidServiceListener()
        zc = Zeroconf()
        
        try:
            service_type = "_autodroid._tcp.local."
            zc.add_service_listener(service_type, listener)
            
            print(f"Started browsing for services of type: {service_type}")
            
            start_time = time.time()
            timeout = 30
            
            while time.time() - start_time < timeout:
                if listener.discovery_complete.is_set():
                    break
                time.sleep(0.5)
            
            assert listener.discovery_complete.is_set(), \
                "Timeout waiting for mDNS service discovery"
            
            print("Service discovery completed successfully")
            
            assert len(listener.discovered_services) > 0, "No services discovered"
            
            service = listener.discovered_services[0]
            assert service['type'] == "_autodroid._tcp.local."
            assert service['port'] == 8008
            assert 'Autodroid Server' in service['name']
            assert len(service['addresses']) > 0
            
            print(f"Successfully discovered service: {service['name']}")
            print(f"  Addresses: {service['addresses']}")
            print(f"  Port: {service['port']}")
            
        finally:
            zc.remove_service_listener(listener)
            zc.close()
    
    def test_mdns_service_discovery_multiple_attempts(self):
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
                self.test_mdns_service_discovery()
                print(f"Service discovered on attempt {attempt}")
                return
            except AssertionError as e:
                if attempt == max_attempts:
                    pytest.fail(f"Failed to discover service after {max_attempts} attempts: {e}")
                print(f"Attempt {attempt} failed: {e}")
                time.sleep(5)


class TestNetworkConnectivity:
    """Tests for network connectivity functionality"""
    
    def test_adb_connection(self):
        """Test ADB connection to Android device/emulator"""
        result = subprocess.run(
            ['adb', 'shell', 'echo', 'test'], 
            capture_output=True, text=True
        )
        assert result.returncode == 0, f"ADB connection failed: {result.stderr}"
        print("ADB connection: OK")
    
    def test_network_interfaces(self):
        """Test network interfaces on Android device"""
        result = subprocess.run(
            ['adb', 'shell', 'ifconfig'], 
            capture_output=True, text=True
        )
        assert result.returncode == 0, f"Failed to get network interfaces: {result.stderr}"
        print("Network interfaces:")
        print(result.stdout if result.stdout else "No interfaces found")
    
    def test_external_network_access(self):
        """Test external network access from Android device"""
        try:
            result = subprocess.run(
                ['adb', 'shell', 'ping', '-c', '1', '8.8.8.8'], 
                capture_output=True, text=True, timeout=10
            )
            assert result.returncode == 0, f"External network access failed: {result.stderr}"
            print("External network access: OK")
        except subprocess.TimeoutExpired:
            pytest.fail("Ping to 8.8.8.8 timed out")
    
    def test_host_network_access(self):
        """Test host machine access from Android device"""
        try:
            result = subprocess.run(
                ['adb', 'shell', 'ping', '-c', '1', '10.0.2.2'], 
                capture_output=True, text=True, timeout=10
            )
            assert result.returncode == 0, f"Host network access failed: {result.stderr}"
            print("Host network access: OK")
        except subprocess.TimeoutExpired:
            pytest.fail("Ping to 10.0.2.2 timed out")
    
    def test_local_server_status(self):
        """Test local server status"""
        try:
            import requests
            response = requests.get('http://127.0.0.1:8008/api/health', timeout=5)
            assert response.status_code == 200, f"Server returned status: {response.status_code}"
            print(f"Local server status: {response.status_code} - OK")
        except ImportError:
            pytest.skip("requests library not available")
        except Exception as e:
            pytest.fail(f"Server check error: {e}")


if __name__ == "__main__":
    import pytest
    pytest.main([__file__, "-v"])
