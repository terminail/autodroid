"""
WiFi调试指导模块

根据设备的Android版本和当前连接方式，提供相应的WiFi调试开通指导。
"""

from typing import Dict, List, Optional
from api.models import WifiDebugGuide


def get_wifi_debug_guide(android_version: str, connection_type: str) -> WifiDebugGuide:
    """
    根据Android版本和连接类型获取WiFi调试指导
    
    Args:
        android_version: Android版本号，如"11.0.0"
        connection_type: 当前连接类型，如"USB"或"WiFi"
        
    Returns:
        WifiDebugGuide: WiFi调试指导信息
    """
    
    # 解析Android版本号
    try:
        major_version = int(android_version.split('.')[0])
    except (ValueError, IndexError):
        major_version = 0
        
    # 如果已经是WiFi连接，返回相应指导
    if connection_type.upper() == "WIFI":
        return WifiDebugGuide(
            android_version=android_version,
            connection_type=connection_type,
            supported=True,
            steps=[
                {
                    "title": "WiFi调试已启用",
                    "description": "设备已通过WiFi连接，无需额外设置"
                }
            ],
            estimated_time="已完成"
        )
    
    # 根据Android版本提供不同的指导
    if major_version >= 11:
        return _get_android_11_guide(android_version)
    elif major_version >= 9:
        return _get_android_9_10_guide(android_version)
    elif major_version >= 6:
        return _get_android_6_8_guide(android_version)
    else:
        return _get_old_android_guide(android_version)


def _get_android_11_guide(android_version: str) -> WifiDebugGuide:
    """Android 11及以上版本的WiFi调试指导"""
    return WifiDebugGuide(
        android_version=android_version,
        connection_type="USB",
        supported=True,
        steps=[
            {
                "title": "步骤1：打开开发者选项",
                "description": "在设备设置中找到\"关于手机\"，连续点击\"版本号\"7次，直到看到\"您已处于开发者模式\"的提示"
            },
            {
                "title": "步骤2：启用无线调试",
                "description": "返回设置主菜单，进入\"系统\" -> \"开发者选项\"，找到并启用\"无线调试\"选项"
            },
            {
                "title": "步骤3：获取配对信息",
                "description": "在\"无线调试\"中点击\"使用配对码配对设备\"，记下显示的IP地址、端口和配对码"
            },
            {
                "title": "步骤4：执行配对命令",
                "description": "在电脑上执行以下命令进行配对："
            }
        ],
        commands=[
            "adb pair <设备IP地址>:<端口>",
            "# 输入显示的配对码"
        ],
        requirements=[
            "设备已启用开发者选项",
            "设备和电脑在同一WiFi网络"
        ],
        estimated_time="2-3分钟"
    )


def _get_android_9_10_guide(android_version: str) -> WifiDebugGuide:
    """Android 9-10版本的WiFi调试指导"""
    return WifiDebugGuide(
        android_version=android_version,
        connection_type="USB",
        supported=True,
        steps=[
            {
                "title": "步骤1：确保USB调试已启用",
                "description": "在设备设置中启用\"开发者选项\"和\"USB调试\""
            },
            {
                "title": "步骤2：获取设备IP地址",
                "description": "在设备设置中找到\"关于手机\" -> \"状态信息\"，记下IP地址"
            },
            {
                "title": "步骤3：切换到TCP/IP模式",
                "description": "在电脑上执行以下命令将ADB切换到TCP/IP模式："
            },
            {
                "title": "步骤4：连接WiFi调试",
                "description": "断开USB连接，执行以下命令通过WiFi连接设备："
            }
        ],
        commands=[
            "adb tcpip 5555",
            "adb connect <设备IP地址>:5555"
        ],
        requirements=[
            "设备已启用USB调试",
            "设备和电脑在同一WiFi网络",
            "USB连接正常"
        ],
        estimated_time="1-2分钟"
    )


def _get_android_6_8_guide(android_version: str) -> WifiDebugGuide:
    """Android 6-8版本的WiFi调试指导"""
    return WifiDebugGuide(
        android_version=android_version,
        connection_type="USB",
        supported=True,
        steps=[
            {
                "title": "步骤1：启用开发者选项",
                "description": "在设置中找到\"关于手机\"，连续点击\"版本号\"7次启用开发者选项"
            },
            {
                "title": "步骤2：启用USB调试",
                "description": "在\"开发者选项\"中启用\"USB调试\""
            },
            {
                "title": "步骤3：获取IP地址",
                "description": "在\"关于手机\" -> \"状态信息\"中找到设备的IP地址"
            },
            {
                "title": "步骤4：设置ADB端口",
                "description": "通过USB连接执行以下命令设置ADB端口："
            },
            {
                "title": "步骤5：WiFi连接",
                "description": "断开USB，执行连接命令："
            }
        ],
        commands=[
            "adb tcpip 5555",
            "adb connect <IP地址>:5555"
        ],
        requirements=[
            "设备已启用开发者选项",
            "USB调试已启用",
            "设备和电脑在同一网络",
            "部分设备可能需要root权限"
        ],
        estimated_time="2-3分钟"
    )


def _get_old_android_guide(android_version: str) -> WifiDebugGuide:
    """Android 5及以下版本的WiFi调试指导"""
    return WifiDebugGuide(
        android_version=android_version,
        connection_type="USB",
        supported=False,
        steps=[
            {
                "title": "限制说明",
                "description": f"Android {android_version} 版本较旧，WiFi调试支持有限"
            },
            {
                "title": "基本要求",
                "description": "设备需要root权限才能启用WiFi调试功能"
            },
            {
                "title": "建议方案",
                "description": "建议使用USB连接进行调试，或考虑升级设备系统版本"
            },
            {
                "title": "高级用户选项",
                "description": "如果设备已root，可以尝试使用第三方ADB WiFi应用，但安全性较低"
            }
        ],
        requirements=[
            "设备已获取root权限",
            "了解ADB和网络安全风险",
            "接受使用第三方工具的风险"
        ],
        estimated_time="需要root权限，不推荐"
    )


def get_wifi_debug_status(device_id: str, connection_type: str) -> Dict[str, any]:
    """
    获取设备的WiFi调试状态
    
    Args:
        device_id: 设备ID
        connection_type: 当前连接类型
        
    Returns:
        包含状态信息的字典
    """
    return {
        "device_id": device_id,
        "current_connection": connection_type,
        "wifi_debug_available": connection_type.upper() != "WIFI",
        "recommendation": "建议启用WiFi调试以获得更好的使用体验" if connection_type.upper() == "USB" else "WiFi调试已启用"
    }