#!/bin/bash

# 权限授予脚本 - com.tdx.androidCCZQ 应用
# 根据 Appium Inspector 会话日志中的权限列表

APP_PACKAGE="com.tdx.androidCCZQ"
DEVICE_ID="TDCDU17905004388"
ADB_PORT="5037"

# 检查设备连接
echo "检查设备连接..."
adb -P $ADB_PORT devices

# 授予权限列表（从 Appium 日志中提取）
echo "开始授予权限..."

# 有效权限列表（经过测试可授予的权限）
adb -P $ADB_PORT -s $DEVICE_ID shell pm grant $APP_PACKAGE android.permission.WRITE_EXTERNAL_STORAGE
adb -P $ADB_PORT -s $DEVICE_ID shell pm grant $APP_PACKAGE android.permission.READ_PHONE_STATE
adb -P $ADB_PORT -s $DEVICE_ID shell pm grant $APP_PACKAGE android.permission.READ_EXTERNAL_STORAGE
adb -P $ADB_PORT -s $DEVICE_ID shell pm grant $APP_PACKAGE android.permission.RECORD_AUDIO
adb -P $ADB_PORT -s $DEVICE_ID shell pm grant $APP_PACKAGE android.permission.CAMERA
adb -P $ADB_PORT -s $DEVICE_ID shell pm grant $APP_PACKAGE android.permission.CALL_PHONE
adb -P $ADB_PORT -s $DEVICE_ID shell pm grant $APP_PACKAGE android.permission.ACCESS_COARSE_LOCATION
adb -P $ADB_PORT -s $DEVICE_ID shell pm grant $APP_PACKAGE android.permission.ACCESS_FINE_LOCATION
adb -P $ADB_PORT -s $DEVICE_ID shell pm grant $APP_PACKAGE android.permission.READ_CALENDAR
adb -P $ADB_PORT -s $DEVICE_ID shell pm grant $APP_PACKAGE android.permission.WRITE_CALENDAR

# 以下权限已被验证为无效或无法授予，已移除
# android.permission.READ_PRIVILEGED_PHONE_STATE (特权权限)
# android.permission.READ_APP_BADGE (未知权限)
# android.permission.MANAGE_EXTERNAL_STORAGE (未知权限)
# android.permission.READ_SETTINGS (未知权限)
# android.permission.MOUNT_UNMOUNT_FILESYSTEMS (不可更改权限类型)
# android.permission.WRITE_SETTINGS (不可更改权限类型)
# android.permission.BLUETOOTH_CONNECT (未知权限)
# android.permission.RECEIVE_USER_PRESENT (未知权限)
# android.permission.ACCESS_BACKGROUND_LOCATION (未知权限)
# android.permission.SCHEDULE_EXACT_ALARM (未知权限)
# android.launcher.permission.CHANGE_BADGE (未知权限)
# android.permission.FOREGROUND_SERVICE (未知权限)
# android.permission.REQUEST_INSTALL_PACKAGES (未知权限)
# android.permission.POST_NOTIFICATIONS (未知权限)
# android.permission.READ_MEDIA_IMAGES (未知权限)
# android.launcher.permission.READ_SETTINGS (未知权限)
# android.launcher.permission.WRITE_SETTINGS (未知权限)

echo "权限授予完成！"
echo "验证权限状态..."
adb -P $ADB_PORT -s $DEVICE_ID shell dumpsys package $APP_PACKAGE | grep -A 50 "runtime permissions"