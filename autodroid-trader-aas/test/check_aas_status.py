#!/usr/bin/env python3
"""
Script to check AAS service status and guide user if needed
"""
import subprocess
import sys

def run_adb_command(command):
    """Execute an ADB command and return the output"""
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    return result.returncode, result.stdout, result.stderr

def is_aas_service_enabled():
    """Check if the AAS service is enabled in accessibility settings"""
    cmd = "adb shell settings get secure enabled_accessibility_services"
    returncode, stdout, stderr = run_adb_command(cmd)
    
    if returncode == 0 and "com.autodroid.aas/com.autodroid.aas.service.UIRecorderAccessibilityService" in stdout:
        print("✓ AAS service is enabled in accessibility settings")
        return True
    else:
        print("✗ AAS service is NOT enabled in accessibility settings")
        print("  To enable it, go to Settings > Accessibility > Special features > Trader AAS and enable it.")
        return False

def check_app_installed():
    """Check if the AAS app is installed"""
    cmd = "adb shell pm list packages | findstr autodroid.aas"
    returncode, stdout, stderr = run_adb_command(cmd)
    
    if returncode == 0 and "com.autodroid.aas" in stdout:
        print("✓ AAS app is installed")
        return True
    else:
        print("✗ AAS app is NOT installed")
        print("  Please install the AAS app first.")
        return False

def main():
    print("Checking AAS service status...")
    print("="*50)
    
    app_installed = check_app_installed()
    if not app_installed:
        print("\n❌ AAS app needs to be installed first!")
        return False
    
    service_enabled = is_aas_service_enabled()
    if not service_enabled:
        print("\n❌ Please enable the AAS service in Android Accessibility Settings!")
        print("   Go to Settings > Accessibility > Special features > Trader AAS")
        print("   Then toggle the service to ON")
        return False
    
    print("\n✅ AAS service is properly installed and enabled!")
    print("   You can now use the AAS functionality to capture UI events and element features.")
    return True

if __name__ == "__main__":
    success = main()
    if success:
        print("\n🎉 AAS service is ready to capture all user-operable elements!")
    else:
        print("\n❌ Please follow the instructions above to set up AAS service.")
        sys.exit(1)