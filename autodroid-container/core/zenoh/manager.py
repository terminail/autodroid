import asyncio
import json
import logging
from typing import Dict, List, Callable, Optional

import zenoh
from zenoh import Session

logger = logging.getLogger(__name__)

class ZenohManager:
    def __init__(self):
        self.session: Optional[Session] = None
        self.devices: Dict[str, Dict] = {}
        self.device_callbacks: List[Callable[[Dict], None]] = []
        self.command_callbacks: List[Callable[[str, Dict], None]] = []
        
        # Zenoh configuration
        self.zenoh_listen_key = "autodroid/device/**"
        self.zenoh_device_info_key = "autodroid/device/{device_id}/info"
        self.zenoh_command_key = "autodroid/device/{device_id}/command"
    
    async def initialize(self):
        """Initialize Zenoh session and start listening for messages"""
        try:
            logger.info("Initializing Zenoh session...")
            
            # Create Zenoh session
            self.session = zenoh.open()
            logger.info("Zenoh session opened successfully")
            
            # Subscribe to device messages
            await self._subscribe_to_device_messages()
            
            logger.info("Zenoh manager initialized successfully")
            return True
        except Exception as e:
            logger.error(f"Failed to initialize Zenoh manager: {e}")
            return False
    
    async def _subscribe_to_device_messages(self):
        """Subscribe to device messages using Zenoh"""
        if not self.session:
            logger.error("Cannot subscribe: Zenoh session not initialized")
            return
        
        async def on_message(sample):
            """Handle incoming Zenoh messages"""
            key = sample.key_expr.as_str()
            value = sample.payload.decode()
            
            logger.info(f"Received Zenoh message on key '{key}': {value}")
            
            try:
                # Parse JSON payload
                message = json.loads(value)
                
                # Handle device info messages
                if key.endswith("/info"):
                    device_id = key.split("/")[2]  # Extract device_id from key
                    await self._handle_device_info(device_id, message)
                
                # Handle command responses
                elif key.endswith("/command"):
                    device_id = key.split("/")[2]  # Extract device_id from key
                    await self._handle_command_response(device_id, message)
                    
            except json.JSONDecodeError as e:
                logger.error(f"Failed to parse JSON message: {e}")
            except Exception as e:
                logger.error(f"Error handling Zenoh message: {e}")
        
        # Subscribe to the device info key pattern
        sub = self.session.declare_subscriber(
            self.zenoh_listen_key,
            on_message
        )
        
        logger.info(f"Subscribed to Zenoh key: {self.zenoh_listen_key}")
    
    async def _handle_device_info(self, device_id: str, message: Dict):
        """Handle device information messages"""
        # Update device information
        self.devices[device_id] = message
        
        logger.info(f"Updated device info for {device_id}: {message}")
        
        # Notify callbacks
        for callback in self.device_callbacks:
            callback(message)
    
    async def _handle_command_response(self, device_id: str, message: Dict):
        """Handle command response messages"""
        logger.info(f"Received command response from {device_id}: {message}")
        
        # Notify callbacks
        for callback in self.command_callbacks:
            callback(device_id, message)
    
    async def publish_command(self, device_id: str, command: Dict):
        """Publish a command to a specific device"""
        if not self.session:
            logger.error("Cannot publish command: Zenoh session not initialized")
            return False
        
        try:
            # Create command key for the specific device
            command_key = self.zenoh_command_key.format(device_id=device_id)
            
            # Convert command to JSON string
            command_json = json.dumps(command)
            
            # Publish command using Zenoh
            self.session.put(command_key, command_json.encode())
            
            logger.info(f"Published command to {device_id}: {command}")
            return True
        except Exception as e:
            logger.error(f"Failed to publish command to {device_id}: {e}")
            return False
    
    async def broadcast_command(self, command: Dict):
        """Broadcast a command to all devices"""
        if not self.session:
            logger.error("Cannot broadcast command: Zenoh session not initialized")
            return False
        
        try:
            # Use wildcard key to broadcast to all devices
            broadcast_key = "autodroid/device/*/command"
            
            # Convert command to JSON string
            command_json = json.dumps(command)
            
            # Publish command using Zenoh
            self.session.put(broadcast_key, command_json.encode())
            
            logger.info(f"Broadcasted command to all devices: {command}")
            return True
        except Exception as e:
            logger.error(f"Failed to broadcast command: {e}")
            return False
    
    def get_devices(self) -> Dict[str, Dict]:
        """Get all discovered devices"""
        return self.devices
    
    def get_device(self, device_id: str) -> Optional[Dict]:
        """Get information for a specific device"""
        return self.devices.get(device_id)
    
    def register_device_callback(self, callback: Callable[[Dict], None]):
        """Register a callback for device information updates"""
        self.device_callbacks.append(callback)
    
    def register_command_callback(self, callback: Callable[[str, Dict], None]):
        """Register a callback for command responses"""
        self.command_callbacks.append(callback)
    
    async def close(self):
        """Close Zenoh session"""
        if self.session:
            logger.info("Closing Zenoh session...")
            self.session.close()
            self.session = None
            logger.info("Zenoh session closed successfully")
    
    async def discover_devices(self, timeout: int = 5) -> List[str]:
        """Discover devices using Zenoh"""
        logger.info(f"Discovering devices with timeout {timeout} seconds...")
        
        # Clear existing devices
        self.devices.clear()
        
        # Wait for devices to send their information
        await asyncio.sleep(timeout)
        
        discovered_devices = list(self.devices.keys())
        logger.info(f"Discovered {len(discovered_devices)} devices: {discovered_devices}")
        
        return discovered_devices
