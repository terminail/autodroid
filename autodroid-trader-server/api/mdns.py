"""
mDNS service module for Autodroid Trader Server discovery
Handles registration and unregistration of mDNS services
"""

import socket
import asyncio
from typing import Optional, Dict, Any

# Try to import zeroconf, but handle gracefully if not available
try:
    from zeroconf import ServiceInfo
    from zeroconf.asyncio import AsyncZeroconf
    ZEROCONF_AVAILABLE = True
except ImportError:
    ZEROCONF_AVAILABLE = False
    print("⚠ zeroconf module not available, mDNS functionality will be disabled")


class MDNSService:
    """Manages mDNS service registration for Autodroid Trader Server discovery"""
    
    def __init__(self):
        self.zeroconf: Optional[AsyncZeroconf] = None
        self.service_info: Optional[ServiceInfo] = None
        self.is_registered = False
    
    async def register_service(
        self, 
        service_name: str = "Autodroid Server",
        service_type: str = "_autodroid._tcp.local.",
        port: int = 8001,
        properties: Optional[Dict[str, str]] = None
    ) -> bool:
        """
        Register the server with mDNS for discovery
        
        Args:
            service_name: Name of the service to register
            service_type: Type of service (must match what Android app expects)
            port: Port number the service is running on
            properties: Additional properties to include in the service record
            
        Returns:
            True if registration was successful, False otherwise
        """
        if not ZEROCONF_AVAILABLE:
            print("⚠ mDNS service skipped: zeroconf module not available")
            return False
            
        try:
            # Get server IP address
            server_ip = self._get_local_ip()
            print(f"Server IP: {server_ip}")
            
            # Default properties if not provided
            if properties is None:
                protocol = 'http'
                api_url = f"{protocol}://{server_ip}:{port}/api"
                properties = {
                    'version': '1.0',
                    'description': 'Autodroid Server',
                    'api_url': api_url,
                    'frontend_url': f"{protocol}://{server_ip}:{port}/app"
                }
            
            # Create ServiceInfo with correct format
            self.service_info = ServiceInfo(
                service_type,
                f"{service_name}.{service_type}",
                addresses=[socket.inet_aton(server_ip)],
                port=port,
                properties=properties
            )
            
            # Create and keep the AsyncZeroconf instance alive for persistent registration
            self.zeroconf = AsyncZeroconf()
            await self.zeroconf.async_register_service(self.service_info)
            self.is_registered = True
            
            print(f"✓ mDNS service registered: {service_name} at {server_ip}:{port}")
            print(f"✓ API URL: {properties.get('api_url', 'N/A')}")
            print("✓ Using correct async zeroconf API")
            
            return True
            
        except Exception as e:
            import traceback
            print(f"✗ Failed to register mDNS service: {e}")
            print(f"Full error traceback:")
            traceback.print_exc()
            print("⚠ WARNING: mDNS registration failed. The app will not be able to discover this server.")
            return False
    
    async def unregister_service(self) -> bool:
        """
        Unregister the mDNS service
        
        Returns:
            True if unregistration was successful, False otherwise
        """
        if not ZEROCONF_AVAILABLE or not self.is_registered:
            return False
            
        try:
            if self.zeroconf and self.service_info:
                await self.zeroconf.async_unregister_service(self.service_info)
                await self.zeroconf.async_close()
                self.is_registered = False
                print("✓ mDNS service unregistered")
                return True
        except Exception as e:
            print(f"✗ Failed to unregister mDNS service: {e}")
            return False
    
    def _get_local_ip(self) -> str:
        """
        Get the local IP address for mDNS registration
        
        Returns:
            The local IP address that can reach the network
        """
        try:
            # Create a socket to connect to an external address
            # This will return the local IP address that can reach the internet
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
                # Connect to a public DNS server (doesn't actually send data)
                s.connect(("8.8.8.8", 80))
                local_ip = s.getsockname()[0]
                return local_ip
        except Exception:
            # Fallback to hostname-based IP resolution
            try:
                hostname = socket.gethostname()
                return socket.gethostbyname(hostname)
            except Exception:
                # Last resort - return localhost
                return "127.0.0.1"
    
    @staticmethod
    def is_available() -> bool:
        """Check if mDNS functionality is available"""
        return ZEROCONF_AVAILABLE


async def register_mdns_from_config(config: Dict[str, Any]) -> MDNSService:
    """
    Register mDNS service from configuration
    
    Args:
        config: Configuration dictionary containing network and server settings
        
    Returns:
        MDNSService instance with registration status
    """
    # Get network configuration
    network_config = config.get('network', {})
    mdns_config = network_config.get('mdns', {})
    server_backend_config = config.get('server', {}).get('backend', {})
    
    # Use configuration values with defaults
    service_type = mdns_config.get('service_type', "_autodroid._tcp.local.")
    service_name = f"{mdns_config.get('service_name', 'Autodroid Server')}"
    
    # IMPORTANT: Use the actual server port from backend config, not mdns config
    # This ensures mDNS registration uses the same port the server is actually running on
    port = server_backend_config.get('port', 8004)
    
    print(f"🔧 mDNS registration configuration:")
    print(f"   - Service name: {service_name}")
    print(f"   - Service type: {service_type}")
    print(f"   - Server port: {port}")
    print(f"   - mDNS config port: {mdns_config.get('service_port', 'not set')}")
    
    # Build API URL from configuration
    api_base = server_backend_config.get('api_base', '/api')
    use_https = server_backend_config.get('use_https', False)
    protocol = 'https' if use_https else 'http'
    
    # Get server IP address
    server_ip = MDNSService()._get_local_ip()
    api_url = f"{protocol}://{server_ip}:{port}{api_base}"
    
    # Create properties that match what the Android app expects
    properties = {
        'version': '1.0',
        'description': 'Autodroid Server',
        'api_url': api_url,
        'frontend_url': f"{protocol}://{server_ip}:{port}/app"
    }
    
    # Create and register service
    mdns_service = MDNSService()
    await mdns_service.register_service(
        service_name=service_name,
        service_type=service_type,
        port=port,
        properties=properties
    )
    
    return mdns_service