(TraeAI-41) /d/git/autodroid [0] $ adb kill-server

(TraeAI-41) /d/git/autodroid [0] $ adb start-server
* daemon not running; starting now at tcp:5037
* daemon started successfully

(TraeAI-41) /d/git/autodroid [0] $ appium --use-plugins=inspector --allow-cors
[Appium] Attempting to load plugin inspector...
[Appium] Requiring plugin at C:\Users\Administrator\.appium\node_modules\appium-inspector-plugin\index.mjs
[Appium] AppiumInspectorPlugin has been successfully loaded in 0.349s
[Appium] Welcome to Appium v3.1.1
[Appium] Non-default server args:
[Appium] {
  allowCors: true,
  usePlugins: [
    'inspector'
  ]
}
[Appium] The autodetected Appium home path: C:\Users\Administrator\.appium
[Appium] Attempting to load driver uiautomator2...
[Appium] Requiring driver at C:\Users\Administrator\.appium\node_modules\appium-uiautomator2-driver\build\index.js
[Appium] AndroidUiautomator2Driver has been successfully loaded in 0.453s
[Appium] You have enabled CORS requests from any host. Be careful not to visit sites which could maliciously try to start Appium sessions on your machine
[Appium] Appium REST http interface listener started on http://0.0.0.0:4723        
[Appium] You can provide the following URLs in your client code to connect to this server:
        http://192.168.1.59:4723/
        http://127.0.0.1:4723/ (only accessible from the same host)
[Appium] Available drivers:
[Appium]   - uiautomator2@6.7.1 (automationName 'UiAutomator2')
[Appium] Available plugins:
[Appium]   - inspector@2025.11.1 (ACTIVE)
[HTTP] --> POST /session {"capabilities":{"alwaysMatch":{"platformName":"Android","appium:automationName":"UiAutomator2","appium:udid":"TDCDU17905004388","appium:appPackage":"com.tdx.androidCCZQ","appium:appActivity":"com.tdx.Android.TdxAndroidActivity","appium:noReset":false,"appium:autoGrantPermissions":true,"appium:skipServerInstallation":true,"appium:remoteAppsCacheLimit":0,"appium:dontStopAppOnReset":false,"appium:ensureWebviewsHavePages":true,"appium:newCommandTimeout":300,"appium:nativeWebScreenshot":true,"appium:connectHardwareKeyboard":true},"firstMatch":[{}]}}     
[AppiumDriver@5c1b] Calling AppiumDriver.createSession() with args: [{"alwaysMatch":{"platformName":"Android","appium:automationName":"UiAutomator2","appium:udid":"TDCDU17905004388","appium:appPackage":"com.tdx.androidCCZQ","appium:appActivity":"com.tdx.Android.TdxAndroidActivity","appium:noReset":false,"appium:autoGrantPermissions":true,"appium:skipServerInstallation":true,"appium:remoteAppsCacheLimit":0,"appium:dontStopAppOnReset":false,"appium:ensureWebviewsHavePages":true,"appium:newCommandTimeout":300,"appium:nativeWebScreenshot":true,"appium:connectHardwareKeyboard":true},"firstMatch":[{}]},{"alwaysMatch":{"platformName":"Android","appium:automationName":"UiAutomator2","appium:udid":"TDCDU17905004388","appium:appPackage":"com.tdx.androidCCZQ","appium:appActivity":"com.tdx.Android.TdxAndroidActivity","appium:noReset":false,"appium:autoGrantPermissions":true,"appium:skipServerInstallation":true,"appium:remoteAppsCacheLimit":0,"appium:dontStopAppOnReset":false,"appium:ensureWebviewsHavePages":true,"appium:newCommandTimeout":300,"appium:nativeWebScreenshot":true,"appium:...
[AppiumDriver@5c1b] Event 'newSessionRequested' logged at 1766061093833 (20:31:33 GMT+0800 (中国标准时间))
[Appium] Attempting to find matching driver for automationName 'UiAutomator2' and platformName 'Android'
[Appium] The 'uiautomator2' driver was installed and matched caps.
[Appium] Will require it at C:\Users\Administrator\.appium\node_modules\appium-uiautomator2-driver
[Appium] Requiring driver at C:\Users\Administrator\.appium\node_modules\appium-uiautomator2-driver\build\index.js
[AppiumDriver@5c1b] Appium v3.1.1 creating new AndroidUiautomator2Driver (v6.7.1) session
[AppiumDriver@5c1b] Checking BaseDriver versions for Appium and AndroidUiautomator2Driver
[AppiumDriver@5c1b] Appium's BaseDriver version is 10.1.1
[AppiumDriver@5c1b] AndroidUiautomator2Driver's BaseDriver version is 10.1.2       
[AndroidUiautomator2Driver@c270] 
[AndroidUiautomator2Driver@c270] Creating session with W3C capabilities: {
  "alwaysMatch": {
    "platformName": "Android",
    "appium:automationName": "UiAutomator2",
    "appium:udid": "TDCDU17905004388",
    "appium:appPackage": "com.tdx.androidCCZQ",
    "appium:appActivity": "com.tdx.Android.TdxAndroidActivity",
    "appium:noReset": false,
    "appium:autoGrantPermissions": true,
    "appium:skipServerInstallation": true,
    "appium:remoteAppsCacheLimit": 0,
    "appium:dontStopAppOnReset": false,
    "appium:ensureWebviewsHavePages": true,
    "appium:newCommandTimeout": 300,
    "appium:nativeWebScreenshot": true,
    "appium:connectHardwareKeyboard": true
  },
  "firstMatch": [
    {}
  ]
}
[AndroidUiautomator2Driver@c270] The following provided capabilities were not recognized by this driver:
[AndroidUiautomator2Driver@c270]   connectHardwareKeyboard
[e24b7502][AndroidUiautomator2Driver@c270] Session created with session id: e24b7502-58a7-4070-9c2b-39fa02eb6a03
[e24b7502][ADB] Found 6 'build-tools' folders under 'C:\Android\SDK' (newest first):
[e24b7502][ADB]     C:\Android\SDK\build-tools\36.1.0
[e24b7502][ADB]     C:\Android\SDK\build-tools\35.0.0
[e24b7502][ADB]     C:\Android\SDK\build-tools\33.0.3
[e24b7502][ADB]     C:\Android\SDK\build-tools\32.0.0
[e24b7502][ADB]     C:\Android\SDK\build-tools\31.0.0
[e24b7502][ADB]     C:\Android\SDK\build-tools\30.0.3
[e24b7502][ADB] Using 'adb.exe' from 'C:\Android\SDK\platform-tools\adb.exe'       
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 start-server'
[e24b7502][AndroidUiautomator2Driver@c270] Retrieving device list
[e24b7502][ADB] Trying to find connected Android devices
[e24b7502][ADB] Getting connected devices
[e24b7502][ADB] Connected devices: [{"udid":"TDCDU17905004388","state":"device"}]
[e24b7502][AndroidUiautomator2Driver@c270] Using device: TDCDU17905004388
[e24b7502][ADB] Using 'adb.exe' from 'C:\Android\SDK\platform-tools\adb.exe'       
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 start-server'
[e24b7502][ADB] Setting device id to TDCDU17905004388
[e24b7502][AndroidUiautomator2Driver@c270] Starting 'com.tdx.androidCCZQ' directly on the device
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell getprop ro.build.version.sdk'
[e24b7502][ADB] Current device property 'ro.build.version.sdk': 26
[e24b7502][ADB] Device API level: 26
[e24b7502][AndroidUiautomator2Driver@c270] Pushing settings apk to the device...   
[e24b7502][ADB] Getting package info for 'io.appium.settings'
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell dumpsys package io.appium.settings'
[e24b7502][ADB] Using 'aapt2.exe' from 'C:\Android\SDK\build-tools\36.1.0\aapt2.exe'
[e24b7502][ADB] Reading package manifest: ''C:\Android\SDK\build-tools\36.1.0\aapt2.exe' dump badging 'C:\Users\Administrator\.appium\node_modules\appium-uiautomator2-driver\node_modules\io.appium.settings\apks\settings_apk-debug.apk''
[e24b7502][ADB] The version name of the installed 'io.appium.settings' is greater or equal to the application version name ('7.0.6' >= '7.0.6')
[e24b7502][ADB] There is no need to install/upgrade 'C:\Users\Administrator\.appium\node_modules\appium-uiautomator2-driver\node_modules\io.appium.settings\apks\settings_apk-debug.apk'
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell dumpsys activity services io.appium.settings'
[e24b7502][AndroidUiautomator2Driver@c270] io.appium.settings is already running. There is no need to reset its permissions.
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell appops set io.appium.settings android:mock_location allow'
[e24b7502][Logcat] Starting logs capture with command: 'C:\Android\SDK\platform-tools\adb.exe' -P 5037 -s TDCDU17905004388 logcat -v threadtime
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell "[ -e '/data/local/tmp/mock_apps.json' ] && echo __PASS__"'
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell cat /data/local/tmp/mock_apps.json'
[e24b7502][AndroidUiautomator2Driver@c270] Forwarding UiAutomator2 Server port 6790 to local port 8200
[e24b7502][ADB] Forwarding system: 8200 to device: 6790
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 forward tcp:8200 tcp:6790'
[e24b7502][AndroidUiautomator2Driver@c270] UIA2Proxy options: {"server":"127.0.0.1","port":8200,"keepAlive":true,"scheme":"http","base":"","reqBasePath":"","sessionId":null,"timeout":240000}
[e24b7502][AndroidUiautomator2Driver@c270] 'skipServerInstallation' is set. Skipping UIAutomator2 server installation.
[e24b7502][AndroidUiautomator2Driver@c270] No app capability. Assuming it is already on the device
[e24b7502][ADB] Getting install status for com.tdx.androidCCZQ
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell cmd package list packages'
[e24b7502][ADB] 'com.tdx.androidCCZQ' is installed
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell am force-stop com.tdx.androidCCZQ'
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell pm clear com.tdx.androidCCZQ'
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell dumpsys package com.tdx.androidCCZQ'
[e24b7502][ADB] Retrieving requested permissions
[e24b7502][ADB] Retrieved 70 permissions from requested group
[e24b7502][ADB] Retrieving granted permissions
[e24b7502][ADB] Retrieved 21 permissions from install,runtime groups
[e24b7502][ADB] Granting permissions ["android.permission.READ_PRIVILEGED_PHONE_STATE","android.permission.WRITE_EXTERNAL_STORAGE","android.permission.READ_PHONE_STATE","android.permission.MOUNT_UNMOUNT_FILESYSTEMS","android.permission.READ_EXTERNAL_STORAGE","android.permission.RECORD_AUDIO","android.permission.CAMERA","android.permission.CALL_PHONE","android.permission.ACCESS_COARSE_LOCATION","android.permission.ACCESS_FINE_LOCATION","android.permission.READ_CALENDAR","android.permission.WRITE_CALENDAR","android.permission.WRITE_SETTINGS","android.permission.BLUETOOTH_CONNECT","android.permission.RECEIVE_USER_PRESENT","android.permission.ACCESS_BACKGROUND_LOCATION","android.permission.SCHEDULE_EXACT_ALARM","android.launcher.permission.CHANGE_BADGE","android.permission.FOREGROUND_SERVICE","android.permission.REQUEST_INSTALL_PACKAGES","android.permission.POST_NOTIFICATIONS","android.permission.READ_MEDIA_IMAGES","android.launcher.permission.READ_SETTINGS","android.launcher.permission.READ_SETTINGS","android.launcher.permission.WRITE_SETTINGS","android.permission.READ_APP_BADGE","android.permission.MANAGE_EXTERNAL_STORAGE","android.permission.READ_SETTINGS"] to 'com.tdx.androidCCZQ'
[e24b7502][ADB] Got the following command chunks to execute: [["pm","grant","com.tdx.androidCCZQ","android.permission.READ_PRIVILEGED_PHONE_STATE",";","pm","grant","com.tdx.androidCCZQ","android.permission.WRITE_EXTERNAL_STORAGE",";","pm","grant","com.tdx.androidCCZQ","android.permission.READ_PHONE_STATE",";","pm","grant","com.tdx.androidCCZQ","android.permission.MOUNT_UNMOUNT_FILESYSTEMS",";","pm","grant","com.tdx.androidCCZQ","android.permission.READ_EXTERNAL_STORAGE",";","pm","grant","com.tdx.androidCCZQ","android.permission.RECORD_AUDIO",";","pm","grant","com.tdx.androidCCZQ","android.permission.CAMERA",";","pm","grant","com.tdx.androidCCZQ","android.permission.CALL_PHONE",";","pm","grant","com.tdx.androidCCZQ","android.permission.ACCESS_COARSE_LOCATION",";","pm","grant","com.tdx.androidCCZQ","android.permission.ACCESS_FINE_LOCATION",";","pm","grant","com.tdx.androidCCZQ","android.permission.READ_CALENDAR",";","pm","grant","com.tdx.androidCCZQ","android.permission.WRITE_CALENDAR",";","pm","grant","com.tdx.androidCCZQ","android.permission.WRITE_SETTINGS",";","pm","grant","com.tdx.androidCCZQ","android.permission.BLUETOOTH_CONNECT",";"],["pm","grant","com.tdx.androidCCZQ","android.permission.RECEIVE_USER_PRESENT",";","pm","grant","com.tdx.androidCCZQ","android.permission.ACCESS_BACKGROUND_LOCATION",";","pm","grant","com.tdx.androidCCZQ","android.permission.SCHEDULE_EXACT_ALARM",";","pm","grant","com.tdx.androidCCZQ","android.launcher.permission.CHANGE_BADGE",";","pm","grant","com.tdx.androidCCZQ","android.permission.FOREGROUND_SERVICE",";","pm","grant","com.tdx.androidCCZQ","android.permission.REQUEST_INSTALL_PACKAGES",";","pm","grant","com.tdx.androidCCZQ","android.permission.POST_NOTIFICATIONS",";","pm","grant","com.tdx.androidCCZQ","android.permission.READ_MEDIA_IMAGES",";","pm","grant","com.tdx.androidCCZQ","android.launcher.permission.READ_SETTINGS",";","pm","grant","com.tdx.androidCCZQ","android.launcher.permission.READ_SETTINGS",";","pm","grant","com.tdx.androidCCZQ","android.launcher.permission.WRITE_SETTINGS",";","pm","grant","com.tdx.androidCCZQ","android.permission.READ_APP_BADGE",";","pm","grant","com.tdx.androidCCZQ","android.permission.MANAGE_EXTERNAL_STORAGE",";","pm","grant","com.tdx.androidCCZQ","android.permission.READ_SETTINGS",";"]]
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell pm grant com.tdx.androidCCZQ android.permission.READ_PRIVILEGED_PHONE_STATE ; pm grant com.tdx.androidCCZQ android.permission.WRITE_EXTERNAL_STORAGE ; pm grant com.tdx.androidCCZQ android.permission.READ_PHONE_STATE ; pm grant com.tdx.androidCCZQ android.permission.MOUNT_UNMOUNT_FILESYSTEMS ; pm grant com.tdx.androidCCZQ android.permission.READ_EXTERNAL_STORAGE ; pm grant com.tdx.androidCCZQ android.permission.RECORD_AUDIO ; pm grant com.tdx.androidCCZQ android.permission.CAMERA ; pm grant com.tdx.androidCCZQ android.permission.CALL_PHONE ; pm grant com.tdx.androidCCZQ android.permission.ACCESS_COARSE_LOCATION ; pm grant com.tdx.androidCCZQ android.permission.ACCESS_FINE_LOCATION ; pm grant com.tdx.androidCCZQ android.permission.READ_CALENDAR ; pm grant com.tdx.androidCCZQ android.permission.WRITE_CALENDAR ; pm grant com.tdx.androidCCZQ android.permission.WRITE_SETTINGS ; pm grant com.tdx.androidCCZQ android.permission.BLUETOOTH_CONNECT ;'
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell pm grant com.tdx.androidCCZQ android.permission.RECEIVE_USER_PRESENT ; pm grant com.tdx.androidCCZQ android.permission.ACCESS_BACKGROUND_LOCATION ; pm grant com.tdx.androidCCZQ android.permission.SCHEDULE_EXACT_ALARM ; pm grant com.tdx.androidCCZQ android.launcher.permission.CHANGE_BADGE ; pm grant com.tdx.androidCCZQ android.permission.FOREGROUND_SERVICE ; pm grant com.tdx.androidCCZQ android.permission.REQUEST_INSTALL_PACKAGES ; pm grant com.tdx.androidCCZQ android.permission.POST_NOTIFICATIONS ; pm grant com.tdx.androidCCZQ android.permission.READ_MEDIA_IMAGES ; pm grant com.tdx.androidCCZQ android.launcher.permission.READ_SETTINGS ; pm grant com.tdx.androidCCZQ android.launcher.permission.READ_SETTINGS ; pm grant com.tdx.androidCCZQ android.launcher.permission.WRITE_SETTINGS ; pm grant com.tdx.androidCCZQ android.permission.READ_APP_BADGE ; pm grant com.tdx.androidCCZQ android.permission.MANAGE_EXTERNAL_STORAGE ; pm grant com.tdx.androidCCZQ android.permission.READ_SETTINGS ;'
[e24b7502][AndroidUiautomator2Driver@c270] Performed fast reset on the installed 'com.tdx.androidCCZQ' application (stop and clear)
[e24b7502][AndroidUiautomator2Driver@c270] Performing shallow cleanup of automation leftovers
[e24b7502][AndroidUiautomator2Driver@c270] The following obsolete sessions are still running: c7ef3521-7746-4355-8b33-8b2fc3c929fc
[e24b7502][AndroidUiautomator2Driver@c270] Cleaning up 1 obsolete session
[e24b7502][ADB] Getting IDs of all 'io.appium.uiautomator2.server' processes
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell ps --help'
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell ps -A'
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell am force-stop io.appium.uiautomator2.server'
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell am force-stop io.appium.uiautomator2.server.test'
[e24b7502][AndroidUiautomator2Driver@c270] 'skipServerInstallation' is set. Attempting to use UIAutomator2 server from the device
[e24b7502][AndroidUiautomator2Driver@c270] Waiting up to 30000ms for UiAutomator2 to be online...
[e24b7502][ADB] Creating ADB subprocess with args: ["-P","5037","-s","TDCDU17905004388","shell","am","instrument","-w","-e","disableAnalytics","true","io.appium.uiautomator2.server.test/androidx.test.runner.AndroidJUnitRunner"]
[e24b7502][AndroidUiautomator2Driver@c270] Matched '/status' to command name 'getStatus'
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /status] to [GET http://127.0.0.1:8200/status] with no body
[e24b7502][AndroidUiautomator2Driver@c270] socket hang up
[e24b7502][AndroidUiautomator2Driver@c270] Matched '/status' to command name 'getStatus'
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /status] to [GET http://127.0.0.1:8200/status] with no body
[e24b7502][AndroidUiautomator2Driver@c270] socket hang up
[e24b7502][AndroidUiautomator2Driver@c270] [Instrumentation] 
[e24b7502][AndroidUiautomator2Driver@c270] Matched '/status' to command name 'getStatus'
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /status] to [GET http://127.0.0.1:8200/status] with no body
[e24b7502][AndroidUiautomator2Driver@c270] Got response with status 200: {"sessionId":"None","value":{"build":{"version":"9.9.0","versionCode":244},"message":"UiAutomator2 Server is ready to accept commands","ready":true}}
[e24b7502][AndroidUiautomator2Driver@c270] The initialization of the instrumentation process took 2359ms
[e24b7502][AndroidUiautomator2Driver@c270] Matched '/session' to command name 'createSession'
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [POST /session] to [POST http://127.0.0.1:8200/session] with body: {"capabilities":{"firstMatch":[{"platformName":"Android","automationName":"UiAutomator2","udid":"TDCDU17905004388","appPackage":"com.tdx.androidCCZQ","appActivity":"com.tdx.Android.TdxAndroidActivity","noReset":false,"autoGrantPermissions":true,"skipServerInstallation":true,"remoteAppsCacheLimit":0,"dontStopAppOnReset":false,"ensureWebviewsHavePages":true,"newCommandTimeout":300,"nativeWebScreenshot":true,"connectHardwareKeyboard":true,"platform":"LINUX","webStorageEnabled":false,"takesScreenshot":true,"javascriptEnabled":true,"databaseEnabled":false,"networkConnectionEnabled":true,"locationContextEnabled":false,"warnings":{},"desired":{"platformName":"Android","automationName":"UiAutomator2","udid":"TDCDU17905004388","appPackage":"com.tdx.androidCCZQ","appActivity":"com.tdx.Android.TdxAndroidActivity","noReset":false,"autoGrantPermissions":true,"skipServerInstallation":true,"remoteAppsCacheLimit":0,"dontStopAppOnReset":false,"ensureWebviewsHavePages":true,"newCommandTimeout":300,"nativeWebScreenshot":true...
[e24b7502][AndroidUiautomator2Driver@c270] Got response with status 200: {"sessionId":"e9087046-a323-4958-87d1-7fcf6ea48f7f","value":{"capabilities":{"firstMatch":[{"platformName":"Android","automationName":"UiAutomator2","udid":"TDCDU17905004388","appPackage":"com.tdx.androidCCZQ","appActivity":"com.tdx.Android.TdxAndroidActivity","noReset":false,"autoGrantPermissions":true,"skipServerInstallation":true,"remoteAppsCacheLimit":0,"dontStopAppOnReset":false,"ensureWebviewsHavePages":true,"newCommandTimeout":300,"nativeWebScreenshot":true,"connectHardwareKeyboard":true,"platform":"LINUX","webStorageEnabled":false,"takesScreenshot":true,"javascriptEnabled":true,"databaseEnabled":false,"networkConnectionEnabled":true,"locationContextEnabled":false,"warnings":{},"desired":{"platformName":"Android","automationName":"UiAutomator2","udid":"TDCDU17905004388","appPackage":"com.tdx.androidCCZQ","appActivity":"com.tdx.Android.TdxAndroidActivity","noReset":false,"autoGrantPermissions":true,"skipServerInstallation":true,"remoteAppsCacheLimit":0,"dontStopAppOnReset":false,"ensureWebviewsHavePa...
[e24b7502][AndroidUiautomator2Driver@c270] Determined the downstream protocol as 'W3C'
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /appium/device/pixel_ratio] to [GET http://127.0.0.1:8200/session/e9087046-a323-4958-87d1-7fcf6ea48f7f/appium/device/pixel_ratio] with no body
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /appium/device/system_bars] to [GET http://127.0.0.1:8200/session/e9087046-a323-4958-87d1-7fcf6ea48f7f/appium/device/system_bars] with no body
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /window/current/size] to [GET http://127.0.0.1:8200/session/e9087046-a323-4958-87d1-7fcf6ea48f7f/window/current/size] with no body
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /appium/device/info] to [GET http://127.0.0.1:8200/session/e9087046-a323-4958-87d1-7fcf6ea48f7f/appium/device/info] with no body
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell dumpsys window'
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell dumpsys power'
[e24b7502][AndroidUiautomator2Driver@c270] Got response with status 200: {"sessionId":"e9087046-a323-4958-87d1-7fcf6ea48f7f","value":3}
[e24b7502][AndroidUiautomator2Driver@c270] Got response with status 200: {"sessionId":"e9087046-a323-4958-87d1-7fcf6ea48f7f","value":{"statusBar":72}}
[e24b7502][AndroidUiautomator2Driver@c270] Got response with status 200: {"sessionId":"e9087046-a323-4958-87d1-7fcf6ea48f7f","value":{"height":1920,"width":1080}}    
[e24b7502][AndroidUiautomator2Driver@c270] Screen already unlocked, doing nothing
[e24b7502][AndroidUiautomator2Driver@c270] Starting 'com.tdx.androidCCZQ/com.tdx.Android.TdxAndroidActivity' and waiting for 'com.tdx.androidCCZQ/com.tdx.Android.TdxAndroidActivity'
[e24b7502][ADB] Running 'C:\Android\SDK\platform-tools\adb.exe -P 5037 -s TDCDU17905004388 shell am start-activity -W -n com.tdx.androidCCZQ/com.tdx.Android.TdxAndroidActivity -S -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000'
[e24b7502][AndroidUiautomator2Driver@c270] Got response with status 200: {"sessionId":"e9087046-a323-4958-87d1-7fcf6ea48f7f","value":{"androidId":"c1b2131765f94900","apiVersion":"26","bluetooth":{"state":"OFF"},"brand":"HONOR","carrierName":"","displayDensity":480,"locale":"zh_CN_#Hans","manufacturer":"HUAWEI","model":"KNT-AL10","networks":[{"capabilities":{"SSID":null,"linkDownBandwidthKbps":1048576,"linkUpstreamBandwidthKbps":1048576,"networkCapabilities":"NET_CAPABILITY_NOT_METERED,NET_CAPABILITY_INTERNET,NET_CAPABILITY_NOT_RESTRICTED,NET_CAPABILITY_TRUSTED,NET_CAPABILITY_NOT_VPN,NET_CAPABILITY_VALIDATED,NET_CAPABILITY_NOT_ROAMING","signalStrength":-44,"transportTypes":"TRANSPORT_WIFI"},"detailedState":"CONNECTED","extraInfo":"\"CMCC-Gqx4\"","isAvailable":true,"isConnected":true,"isFailover":false,"isRoaming":false,"state":"CONNECTED","subtype":0,"subtypeName":"","type":1,"typeName":"WIFI"}],"platformVersion":"8.0.0","realDisplaySize":"1080x1920","timeZone":"Asia/Shanghai"}}    
[e24b7502][AppiumDriver@5c1b] New AndroidUiautomator2Driver session created successfully, session e24b7502-58a7-4070-9c2b-39fa02eb6a03 added to master session list   
[e24b7502][AppiumDriver@5c1b] Event 'newSessionStarted' logged at 1766061117175 (20:31:57 GMT+0800 (中国标准时间))
[e24b7502][AppiumDriver@5c1b] Promoting 1 sessionless plugins to be attached to session ID e24b7502-58a7-4070-9c2b-39fa02eb6a03
[e24b7502][AndroidUiautomator2Driver@c270] Cached the protocol value 'W3C' for the new session e24b7502-58a7-4070-9c2b-39fa02eb6a03
[e24b7502][AndroidUiautomator2Driver@c270] Responding to client with driver.createSession() result: {"capabilities":{"platformName":"Android","automationName":"UiAutomator2","udid":"TDCDU17905004388","appPackage":"com.tdx.androidCCZQ","appActivity":"com.tdx.Android.TdxAndroidActivity","noReset":false,"autoGrantPermissions":true,"skipServerInstallation":true,"remoteAppsCacheLimit":0,"dontStopAppOnReset":false,"ensureWebviewsHavePages":true,"newCommandTimeout":300,"nativeWebScreenshot":true,"connectHardwareKeyboard":true,"platform":"LINUX","webStorageEnabled":false,"takesScreenshot":true,"javascriptEnabled":true,"databaseEnabled":false,"networkConnectionEnabled":true,"locationContextEnabled":false,"warnings":{},"desired":{"platformName":"Android","automationName":"UiAutomator2","udid":"TDCDU17905004388","appPackage":"com.tdx.androidCCZQ","appActivity":"com.tdx.Android.TdxAndroidActivity","noReset":false,"autoGrantPermissions":true,"skipServerInstallation":true,"remoteAppsCacheLimit":0,"dontStopAppOnReset":false,"ensureWebviewsHavePages":true,"newCommandTimeout":300,"nativeWebScreenshot":true,"connectHardwa...
[e24b7502][HTTP] <-- POST /session 200 23348 ms - 1421
[e24b7502][HTTP] --> GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/appium/settings
[e24b7502][AndroidUiautomator2Driver@c270] Calling AppiumDriver.getSettings() with args: ["e24b7502-58a7-4070-9c2b-39fa02eb6a03"]
[e24b7502][AndroidUiautomator2Driver@c270] Matched '/appium/settings' to command name 'getSettings'
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /appium/settings] to [GET http://127.0.0.1:8200/session/e9087046-a323-4958-87d1-7fcf6ea48f7f/appium/settings] with no body
[e24b7502][AndroidUiautomator2Driver@c270] Got response with status 200: {"sessionId":"e9087046-a323-4958-87d1-7fcf6ea48f7f","value":{"allowInvisibleElements":false,"elementResponseAttributes":"name,text","snapshotMaxDepth":70,"includeA11yActionsInPageSource":false,"mjpegBilinearFiltering":false,"waitForSelectorTimeout":10000,"serverPort":6790,"ignoreUnimportantViews":false,"simpleBoundsCalculation":false,"enableNotificationListener":true,"limitXPathContextScope":true,"includeExtrasInPageSource":false,"normalizeTagNames":false,"trackScrollEvents":true,"scrollAcknowledgmentTimeout":200,"enableTopmostWindowFromActivePackage":false,"enableMultiWindows":false,"useResourcesForOrientationDetection":false,"currentDisplayId":0,"shouldUseCompactResponses":true,"wakeLockTimeout":86395602,"shutdownOnPowerDisconnect":true,"mjpegServerPort":7810,"mjpegScalingFactor":50,"alwaysTraversableViewClasses":"","disableIdLocatorAutocompletion":false,"enforceXPath1":false,"actionAcknowledgmentTimeout":3000,"mjpegServerScreenshotQuality":50,"keyInjectionDelay":0,"waitForIdleTimeout":10000,"mjpegServer...
[e24b7502][AndroidUiautomator2Driver@c270] Responding to client with driver.getSettings() result: {"ignoreUnimportantViews":false,"allowInvisibleElements":false,"elementResponseAttributes":"name,text","snapshotMaxDepth":70,"includeA11yActionsInPageSource":false,"mjpegBilinearFiltering":false,"waitForSelectorTimeout":10000,"serverPort":6790,"simpleBoundsCalculation":false,"enableNotificationListener":true,"limitXPathContextScope":true,"includeExtrasInPageSource":false,"normalizeTagNames":false,"trackScrollEvents":true,"scrollAcknowledgmentTimeout":200,"enableTopmostWindowFromActivePackage":false,"enableMultiWindows":false,"useResourcesForOrientationDetection":false,"currentDisplayId":0,"shouldUseCompactResponses":true,"wakeLockTimeout":86395602,"shutdownOnPowerDisconnect":true,"mjpegServerPort":7810,"mjpegScalingFactor":50,"alwaysTraversableViewClasses":"","disableIdLocatorAutocompletion":false,"enforceXPath1":false,"actionAcknowledgmentTimeout":3000,"mjpegServerScreenshotQuality":50,"keyInjectionDelay":0,"waitForIdleTimeout":10000,"mjpegServerFramerate":10}      
[e24b7502][HTTP] <-- GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/appium/settings 200 55 ms - 985
[e24b7502][HTTP] --> GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/screenshot 
[e24b7502][AndroidUiautomator2Driver@c270] Calling AppiumDriver.getScreenshot() with args: ["e24b7502-58a7-4070-9c2b-39fa02eb6a03"]
[e24b7502][AndroidUiautomator2Driver@c270] Matched '/screenshot' to command name 'getScreenshot'
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /screenshot] to [GET http://127.0.0.1:8200/session/e9087046-a323-4958-87d1-7fcf6ea48f7f/screenshot] with no body
[e24b7502][HTTP] --> GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/window/rect 
[e24b7502][AndroidUiautomator2Driver@c270] Calling AppiumDriver.getWindowRect() with args: ["e24b7502-58a7-4070-9c2b-39fa02eb6a03"]
[e24b7502][HTTP] --> GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/source      
[e24b7502][AndroidUiautomator2Driver@c270] Driver proxy active, passing request on via HTTP proxy
[e24b7502][AndroidUiautomator2Driver@c270] Scheduling the 'proxyReqRes' command to the AndroidUiautomator2Driver commands queue. 1 queue item is already waiting for execution.
[e24b7502][AndroidUiautomator2Driver@c270] Got response with status 200: {"sessionId":"e9087046-a323-4958-87d1-7fcf6ea48f7f","value":"iVBORw0KGgoAAAANSUhEUgAABDgAAAeACAYAAAArYecKAAAABHNCSVQICAgIfAhkiAAAIABJREFUeJzs3XlcU3e+P/7XyUrCThAE2dxABRTcAbWt2Fat1l27aztb7Z2Z3m/b78ztnd57O99pO7+7dGZ+vY+2t9NNp99utnW31na0ixXUagV33JFNVglCAmQ53z9CjglJIIEESPt6zqNjSM4GJCHndd6f90cAIIKIiIiIiIiIKIjJBvsAiIiIiIiIiIj6iwEHEREREREREQU9BhxEREREREREFPQYcBARERERERFR0GPAQURERERERERBjwEHEREREREREQU9BhxEREREREREFPQYcBARERERERFR0GPAQURERERERERBjwEHEREREREREQU9BhxEREREREREFPQYcBARERERERFR0GPAQURERERERERBjwEHEREREREREQU9BhxEREREREREFPQYcBARERERERFR0GPAQURERERERERBTzHYB0A/LAIAQQCiwwVMGyNDdJgAiACsYtcSIppbgSOXRTS2ihCttofph2NiSjRGx0cgRCn3+7YNnWacq9HjTJXeb9sUAEyIiUJGdBRUchkEv23Z9tzutFhx9nozzjQ187lORERERBRAAnh+SX4gAIgKE7A0X4kx8QJClMCIGECjBGCFLeAQRUAE2jtEVDUBhk4rLtUD245a0dQ6+E/E6ISR0EboPD5uaGnE9ZrLA3hEvlEqlZg6dSqOHDkCk8k04Pu/dfxwzMtOwPAoDWLC1FDI3BeI+fqm47i8yWJF440OXNMb8cWJauw/W9vn4w2Ry7EgNQnT4mIxTKNBrEYNueD/gMMiWtFg7EC90YjDdQ3YXV6JDovFj3shIiJf6HQ66HTOf++NRiMqKioG9Di0Wi0WLVqE9PR0JCcne72ewWDARx99hKKiogAeHRFRcGLAEURmzZqFS5cu...
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /window/current/size] to [GET http://127.0.0.1:8200/session/e9087046-a323-4958-87d1-7fcf6ea48f7f/window/current/size] with no body
[e24b7502][AndroidUiautomator2Driver@c270] Responding to client with driver.getScreenshot() result: "iVBORw0KGgoAAAANSUhEUgAABDgAAAeACAYAAAArYecKAAAABHNCSVQICAgIfAhkiAAAIABJREFUeJzs3XlcU3e+P/7XyUrCThAE2dxABRTcAbWt2Fat1l27aztb7Z2Z3m/b78ztnd57O99pO7+7dGZ+vY+2t9NNp99utnW31na0ixXUagV33JFNVglCAmQ53z9CjglJIIEESPt6zqNjSM4GJCHndd6f90cAIIKIiIiIiIiIKIjJBvsAiIiIiIiIiIj6iwEHEREREREREQU9BhxEREREREREFPQYcBARERERERFR0GPAQURERERERERBjwEHEREREREREQU9BhxEREREREREFPQYcBARERERERFR0GPAQURERERERERBjwEHEREREREREQU9BhxEREREREREFPQYcBARERERERFR0GPAQURERERERERBjwEHEREREREREQU9BhxEREREREREFPQYcBARERERERFR0GPAQURERERERERBTzHYB0A/LAIAQQCiwwVMGyNDdJgAiACsYtcSIppbgSOXRTS2ihCttofph2NiSjRGx0cgRCn3+7YNnWacq9HjTJXeb9sUAEyIiUJGdBRUchkEv23Z9tzutFhx9nozzjQ187lORERERBRAAnh+SX4gAIgKE7A0X4kx8QJClMCIGECjBGCFLeAQRUAE2jtEVDUBhk4rLtUD245a0dQ6+E/E6ISR0EboPD5uaGnE9ZrLA3hEvlEqlZg6dSqOHDkCk8k04Pu/dfxwzMtOwPAoDWLC1FDI3BeI+fqm47i8yWJF440OXNMb8cWJauw/W9vn4w2Ry7EgNQnT4mIxTKNBrEYNueD/gMMiWtFg7EC90YjDdQ3YXV6JDovFj3shIiJf6HQ66HTOf++NRiMqKioG9Di0Wi0WLVqE9PR0JCcne72ewWDARx99hKKiogAeHRFRcGLAEURmzZqFS5cuobq6erAPxYlcDvz2nhBMHyPrCjJw819RtAUcotjtdtcyXZUdxypE/J/tIsRB...
[e24b7502][HTTP] <-- GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/screenshot 200 557 ms - 86288
[e24b7502][AndroidUiautomator2Driver@c270] Got response with status 200: {"sessionId":"e9087046-a323-4958-87d1-7fcf6ea48f7f","value":{"height":1920,"width":1080}}    
[e24b7502][AndroidUiautomator2Driver@c270] Matched '/session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/source' to command name 'getPageSource'
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/source] to [GET http://127.0.0.1:8200/session/e9087046-a323-4958-87d1-7fcf6ea48f7f/source] with no body
[e24b7502][AndroidUiautomator2Driver@c270] Responding to client with driver.getWindowRect() result: {"width":1080,"height":1920,"x":0,"y":0}
[e24b7502][HTTP] <-- GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/window/rect 200 589 ms - 50
[e24b7502][AndroidUiautomator2Driver@c270] Got response with status 200: {"sessionId":"e9087046-a323-4958-87d1-7fcf6ea48f7f","value":"<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\r\n<hierarchy index=\"0\" class=\"hierarchy\" rotation=\"0\" width=\"1080\" height=\"1812\">\r\n  <android.widget.FrameLayout index=\"0\" package=\"com.tdx.androidCCZQ\" class=\"android.widget.FrameLayout\" text=\"\" checkable=\"false\" checked=\"false\" clickable=\"false\" enabled=\"true\" focusable=\"false\" focused=\"false\" long-clickable=\"false\" password=\"false\" scrollable=\"false\" selected=\"false\" bounds=\"[0,0][1080,1812]\" displayed=\"true\" a11y-important=\"true\" drawing-order=\"0\" showing-hint=\"false\" dismissable=\"false\" a11y-focused=\"false\" live-region=\"0\" context-clickable=\"false\" content-invalid=\"false\" window-id=\"1244\">\r\n    <android.widget.LinearLayout index=\"0\" package=\"com.tdx.androidCCZQ\" class=\"android.widget.LinearLayout\" text=\"\" checkable=\"false\" checked=\"false\" clickable=\"false\" enabled=\"true\" focusable=\"false\" focused=\"false...
[e24b7502][AndroidUiautomator2Driver@c270] Replacing sessionId e9087046-a323-4958-87d1-7fcf6ea48f7f with e24b7502-58a7-4070-9c2b-39fa02eb6a03
[e24b7502][HTTP] <-- GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/source 200 663 ms - 2014
[e24b7502][HTTP] --> GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/screenshot 
[e24b7502][AndroidUiautomator2Driver@c270] Calling AppiumDriver.getScreenshot() with args: ["e24b7502-58a7-4070-9c2b-39fa02eb6a03"]
[e24b7502][AndroidUiautomator2Driver@c270] Matched '/screenshot' to command name 'getScreenshot'
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /screenshot] to [GET http://127.0.0.1:8200/session/e9087046-a323-4958-87d1-7fcf6ea48f7f/screenshot] with no body
[e24b7502][HTTP] --> GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/window/rect 
[e24b7502][AndroidUiautomator2Driver@c270] Calling AppiumDriver.getWindowRect() with args: ["e24b7502-58a7-4070-9c2b-39fa02eb6a03"]
[e24b7502][HTTP] --> GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/source      
[e24b7502][AndroidUiautomator2Driver@c270] Driver proxy active, passing request on via HTTP proxy
[e24b7502][AndroidUiautomator2Driver@c270] Scheduling the 'proxyReqRes' command to the AndroidUiautomator2Driver commands queue. 1 queue item is already waiting for execution.
[e24b7502][AndroidUiautomator2Driver@c270] Got response with status 200: {"sessionId":"e9087046-a323-4958-87d1-7fcf6ea48f7f","value":"iVBORw0KGgoAAAANSUhEUgAABDgAAAeACAYAAAArYecKAAAABHNCSVQICAgIfAhkiAAAIABJREFUeJzs3XlclPe5N/7PPSsz7AyCIJsb4IKCO6DGCEnUatw1azVp0iY5p815kvza0zbnOe3TJP2dpcsv59XYNJu2v2wm0bjFLNWYmIAajaCigiuyySqDMAPMcj9/DHM7w8zADMwAk37efaUOM/cG3Pcw3+u+vtclABBBRERERERERBTEZMN9AEREREREREREg8UABxEREREREREFPQY4iIiIiIiIiCjoMcBBREREREREREGPAQ4iIiIiIiIiCnoMcBARERERERFR0GOAg4iIiIiIiIiCHgMcRERERERERBT0GOAgIiIiIiIioqDHAAcRERERERERBT0GOIiIiIiIiIgo6DHAQURERERERERBjwEOIiIiIiIiIgp6DHAQERERERERUdBjgIOIiIiIiIiIgh4DHEREREREREQU9BjgICIiIiIiIqKgpxjuA6DvFgGAIADR4QJmT5AhOkwARABWsWcJEa3twPErIprbRYhW28v03TEtJRrj4yMQopT7fduGbjMq6vQ4V6P32zYFAJNjopARHQWVXAbBb1u2ndvdFivO32jFuZZWnutERERERAEkgONL8gMBQFSYgFV5SkyIFxCiBMbEABolACtsAQ5RBESgs0tETQtg6LbiciOw64QVLe3DfyJGJ4yFNkLn8XVDWzNu1F0ZwiPyjVKpxKxZs3D8+HGYTKYh3/+iSaNRmJWA0VEaxISpoZC5TxDz9U3HcXmTxYrmm124rjfis9O1OHy+fsDHGyKXY2lqEmbHxWKURoNYjRpywf8BDotoRZOxC41GI441NGF/ZTW6LBY/7oWIiHyh0+mg0zn/vTcajaiqqhrS49BqtVi+fDnS09ORnJzs9XoGgwHvvfceioqKAnh0RETBiQGOIDJ/...
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /window/current/size] to [GET http://127.0.0.1:8200/session/e9087046-a323-4958-87d1-7fcf6ea48f7f/window/current/size] with no body
[e24b7502][AndroidUiautomator2Driver@c270] Responding to client with driver.getScreenshot() result: "iVBORw0KGgoAAAANSUhEUgAABDgAAAeACAYAAAArYecKAAAABHNCSVQICAgIfAhkiAAAIABJREFUeJzs3XlclPe5N/7PPSsz7AyCIJsb4IKCO6DGCEnUatw1azVp0iY5p815kvza0zbnOe3TJP2dpcsv59XYNJu2v2wm0bjFLNWYmIAajaCigiuyySqDMAPMcj9/DHM7w8zADMwAk37efaUOM/cG3Pcw3+u+vtclABBBRERERERERBTEZMN9AEREREREREREg8UABxEREREREREFPQY4iIiIiIiIiCjoMcBBREREREREREGPAQ4iIiIiIiIiCnoMcBARERERERFR0GOAg4iIiIiIiIiCHgMcRERERERERBT0GOAgIiIiIiIioqDHAAcRERERERERBT0GOIiIiIiIiIgo6DHAQURERERERERBjwEOIiIiIiIiIgp6DHAQERERERERUdBjgIOIiIiIiIiIgh4DHEREREREREQU9BjgICIiIiIiIqKgpxjuA6DvFgGAIADR4QJmT5AhOkwARABWsWcJEa3twPErIprbRYhW28v03TEtJRrj4yMQopT7fduGbjMq6vQ4V6P32zYFAJNjopARHQWVXAbBb1u2ndvdFivO32jFuZZWnutERERERAEkgONL8gMBQFSYgFV5SkyIFxCiBMbEABolACtsAQ5RBESgs0tETQtg6LbiciOw64QVLe3DfyJGJ4yFNkLn8XVDWzNu1F0ZwiPyjVKpxKxZs3D8+HGYTKYh3/+iSaNRmJWA0VEaxISpoZC5TxDz9U3HcXmTxYrmm124rjfis9O1OHy+fsDHGyKXY2lqEmbHxWKURoNYjRpywf8BDotoRZOxC41GI441NGF/ZTW6LBY/7oWIiHyh0+mg0zn/vTcajaiqqhrS49BqtVi+fDnS09ORnJzs9XoGgwHvvfceioqKAnh0RETBiQGOIDJ//nxcvnwZtbW1w30oTuRy4Gf3hGDOBFlPIAO3/hVFW4BDFHs97lmmJ7PjZJWI...
[e24b7502][HTTP] <-- GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/screenshot 200 775 ms - 441844
[e24b7502][AndroidUiautomator2Driver@c270] Got response with status 200: {"sessionId":"e9087046-a323-4958-87d1-7fcf6ea48f7f","value":{"height":1920,"width":1080}}    
[e24b7502][AndroidUiautomator2Driver@c270] Matched '/session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/source' to command name 'getPageSource'
[e24b7502][AndroidUiautomator2Driver@c270] Proxying [GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/source] to [GET http://127.0.0.1:8200/session/e9087046-a323-4958-87d1-7fcf6ea48f7f/source] with no body
[e24b7502][AndroidUiautomator2Driver@c270] Responding to client with driver.getWindowRect() result: {"width":1080,"height":1920,"x":0,"y":0}
[e24b7502][HTTP] <-- GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/window/rect 304 806 ms - -
[e24b7502][AndroidUiautomator2Driver@c270] Got response with status 200: {"sessionId":"e9087046-a323-4958-87d1-7fcf6ea48f7f","value":"<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\r\n<hierarchy index=\"0\" class=\"hierarchy\" rotation=\"0\" width=\"1080\" height=\"1812\">\r\n  <android.widget.FrameLayout index=\"0\" package=\"com.tdx.androidCCZQ\" class=\"android.widget.FrameLayout\" text=\"\" checkable=\"false\" checked=\"false\" clickable=\"false\" enabled=\"true\" focusable=\"false\" focused=\"false\" long-clickable=\"false\" password=\"false\" scrollable=\"false\" selected=\"false\" bounds=\"[81,308][999,1576]\" displayed=\"true\" a11y-important=\"true\" drawing-order=\"0\" showing-hint=\"false\" dismissable=\"false\" a11y-focused=\"false\" live-region=\"0\" context-clickable=\"false\" content-invalid=\"false\" window-id=\"1245\">\r\n    <android.widget.FrameLayout index=\"0\" package=\"com.tdx.androidCCZQ\" class=\"android.widget.FrameLayout\" text=\"\" checkable=\"false\" checked=\"false\" clickable=\"false\" enabled=\"true\" focusable=\"false\" focused=\"false...
[e24b7502][AndroidUiautomator2Driver@c270] Replacing sessionId e9087046-a323-4958-87d1-7fcf6ea48f7f with e24b7502-58a7-4070-9c2b-39fa02eb6a03
[e24b7502][HTTP] <-- GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/source 200 1114 ms - 12853
[e24b7502][HTTP] --> GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/timeouts 
[e24b7502][AndroidUiautomator2Driver@c270] Calling AppiumDriver.getTimeouts() with args: ["e24b7502-58a7-4070-9c2b-39fa02eb6a03"]
[e24b7502][AndroidUiautomator2Driver@c270] Responding to client with driver.getTimeouts() result: {"command":300000,"implicit":0}
[e24b7502][HTTP] <-- GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/timeouts 200
 4 ms - 41
[e24b7502][HTTP] --> GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/timeouts 
[e24b7502][AndroidUiautomator2Driver@c270] Calling AppiumDriver.getTimeouts() with args: ["e24b7502-58a7-4070-9c2b-39fa02eb6a03"]
[e24b7502][AndroidUiautomator2Driver@c270] Responding to client with driver.getTimeouts() result: {"command":300000,"implicit":0}
[e24b7502][HTTP] <-- GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/timeouts 304
 3 ms - -
[e24b7502][HTTP] --> GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/timeouts 
[e24b7502][AndroidUiautomator2Driver@c270] Calling AppiumDriver.getTimeouts() with args: ["e24b7502-58a7-4070-9c2b-39fa02eb6a03"]
[e24b7502][AndroidUiautomator2Driver@c270] Responding to client with driver.getTimeouts() result: {"command":300000,"implicit":0}
[e24b7502][HTTP] <-- GET /session/e24b7502-58a7-4070-9c2b-39fa02eb6a03/timeouts 304
 1 ms - -
