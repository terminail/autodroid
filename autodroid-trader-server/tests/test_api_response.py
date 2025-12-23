#!/usr/bin/env python3
"""
Test script to check API response
"""

import requests
import json

def test_api_response():
    """Test the API response for device registration"""
    url = "http://localhost:8004/api/devices/"
    
    # Test with just serialno (like the app sends)
    device_data = {
        'serialno': 'TDCDU17905004388'
    }
    
    print(f"Testing API with: {device_data}")
    
    try:
        response = requests.post(url, json=device_data)
        print(f"Response status: {response.status_code}")
        print(f"Response body: {json.dumps(response.json(), indent=2)}")
        
        # Check if the response contains the device name
        response_data = response.json()
        if response_data.get('success') and response_data.get('device'):
            device = response_data['device']
            print(f"\nDevice name in response: {device.get('name')}")
            print(f"Device model in response: {device.get('model')}")
            print(f"Device serialno in response: {device.get('serialno')}")
        else:
            print("\nNo device info in response or registration failed")
            
    except Exception as e:
        print(f"API call failed: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    test_api_response()