# AAS (Accessibility Automation Service) Verification and Auto-Fix Design

## 1. Overview
This document outlines the methodology for automatically verifying and fixing the AAS functionality to ensure proper UI event monitoring and element feature extraction as described in the main DESIGN.md.

## 2. Verification Process

### 2.1 Initial Checks
- Verify AAS service is enabled in system accessibility settings
- Confirm target app (com.tdx.androidCCZQ) is installed on device
- Check if database file exists and is accessible

### 2.2 Database Structure Validation
- Connect to the UI recorder database (`ui_recorder.db`)
- Validate that all required tables exist:
  - `ui_events` table for storing UI events
  - `element_features` table for storing element characteristics
  - `app_configs` table for storing application configurations

### 2.3 Event Recording Verification
- Launch the target application (com.tdx.androidCCZQ)
- Perform a sequence of UI interactions:
  - Click events on different UI elements
  - Text input operations
  - Scroll/swipe gestures
  - Long press actions
  - Focus changes
- Monitor database for newly recorded events and element features

### 2.4 Element Feature Extraction Verification
- Validate that element features are properly extracted and stored
- Verify all expected element properties are captured:
  - Element ID
  - Element type
  - Element text
  - Content description
  - Class name
  - Parent hierarchy
  - Sibling information
  - Bounds information

## 3. Verification Process

### 3.1 Current Status
- AAS service is successfully capturing UI events from target apps
- Database tables (ui_events, element_features) are being populated with event data
- Verification confirmed: Service generates NEW_EVENT broadcasts when UI interactions occur
- Element features are being extracted and stored in the database

### 3.2 Verification Steps
1. Install the AAS app on the device
2. Enable the accessibility service in settings
3. Launch a target app (e.g., com.tdx.androidCCZQ)
4. Interact with UI elements (click, input, scroll)
5. Check database for captured events and features
6. Verify the data structure and content

### 3.3 Verification Results
Recent tests confirmed:
- Service successfully captures UI events (verified via logcat NEW_EVENT broadcasts)
- Database is properly populated with event data
- Element features are being extracted during interactions
- Hardcoded target packages approach is working correctly

## 4. Auto-fix Strategies

### 4.1 Service Connection Issues
- If service is not enabled, provide guidance to user to enable in settings
- Restart the service programmatically if connection fails

### 4.2 Database Issues
- If database tables are missing, trigger database initialization
- If database is corrupted, recreate it with proper schema
- If permissions are insufficient, guide user to resolve

### 4.3 Event Capture Issues
- Adjust accessibility service configuration if events are not being captured
- Modify event filtering criteria if too many irrelevant events are captured
- Ensure proper package name targeting
- Confirm service is properly registered in AndroidManifest.xml

### 4.4 Element Feature Extraction Issues
- Verify that the ElementAnalyzer is properly configured
- Ensure all required element properties are being extracted
- Check that parent-child relationships are preserved in the database

### 4.5 Resolution Notes
The AAS service was successfully fixed by correcting the metadata reference in AndroidManifest.xml:
- Changed `android:name="Trader AAS"` to `android:name="android.accessibilityservice"`
- This allows Android system to properly recognize and configure the accessibility service
- Service now properly captures UI events and stores them in the database

### 4.6 Verification Summary
Based on our testing and verification:
- AAS service successfully generates NEW_EVENT broadcasts when UI interactions occur (confirmed via logcat)
- UI Events are being captured and stored (confirmed via ui_events.sql dump - 20+ entries found)
- Element features are being captured and stored (confirmed via element_features.sql dump - 3+ entries found)
- Database schema is correctly implemented with ui_events and element_features tables
- Service properly targets the hardcoded package names as requested
- Service now monitors expanded event types: CLICK, TEXT_CHANGED, FOCUS, SELECTION, LONG_CLICK, SCROLL, WINDOW_STATE_CHANGED, TEXT_SELECTION_CHANGED
- All required functionality is working as expected

## 5. Success Criteria

### 5.1 AAS Service Verification
- [x] AAS service is running and connected
- [x] Service captures UI events from target apps (confirmed via NEW_EVENT broadcasts)
- [x] Database tables are populated with event data
- [x] Element features are extracted and stored correctly (confirmed via SQL dumps)

### 5.2 Data Quality Requirements
- [x] Event data contains all required fields
- [x] Element features include bounds, text, and other properties
- [x] Data is properly linked between events and features

## 6. Troubleshooting Common Issues

### 6.1 Database Access Issues
- Use `run-as` command to access app-private database
- Verify app has proper permissions to create and modify database
- Check for database locks during concurrent access

### 6.2 Event Filtering Issues
- Adjust the target package filtering to ensure correct app is monitored
- Modify event type filters to capture all relevant UI events
- Ensure service has proper accessibility permissions

### 6.3 Element Analysis Issues
- Verify ElementAnalyzer is properly extracting all required properties
- Check that UI hierarchies are correctly traversed
- Ensure element bounds and relationships are accurately captured

### 6.4 Known Issue Resolution
- Manifest metadata issue: Fixed by correcting android:name attribute to "android.accessibilityservice"
- Service not capturing events: Resolved by ensuring proper manifest configuration

## 7. Implementation Notes

### 7.1 Verification Script Components
- Service status checker
- Database connectivity validator
- UI interaction simulator
- Result validator and reporter

### 7.2 Auto-fix Capabilities
- Automatic service restart
- Database repair/recreation
- Configuration adjustment
- Issue reporting and resolution guidance