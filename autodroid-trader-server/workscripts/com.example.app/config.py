"""
自动化测试配置文件
包含设备连接、APP信息和测试数据配置
支持环境变量覆盖配置
"""

import os
from typing import Dict, Any

# ---------------------- 设备连接配置 ----------------------
DEVICE_CONFIG: Dict[str, Any] = {
    "phone_ip": os.getenv("PHONE_IP", "192.168.1.100"),  # 手机WiFi IP
    "phone_port": int(os.getenv("PHONE_PORT", "5555")),  # WiFi调试端口
    "connection_timeout": 30,  # 连接超时时间（秒）
    "retry_attempts": 3,  # 连接重试次数
    "retry_delay": 2,  # 重试延迟（秒）
}

# ---------------------- APP配置 ----------------------
APP_CONFIG: Dict[str, Any] = {
    "package_name": os.getenv("APP_PACKAGE", "com.autodroid.manager"),  # APP包名
    "main_activity": os.getenv("APP_ACTIVITY", ".MainActivity"),  # 主Activity
    "launch_timeout": 5,  # APP启动超时时间（秒）
    "operation_delay": {  # 操作延迟配置
        "min": 0.5,  # 最小延迟（秒）
        "max": 2.0,  # 最大延迟（秒）
    }
}

# ---------------------- 测试数据 ----------------------
TEST_DATA: Dict[str, Any] = {
    "login": {
        "username": os.getenv("TEST_USERNAME", "testuser"),
        "password": os.getenv("TEST_PASSWORD", "testpass"),
    },
    "input_text": os.getenv("TEST_INPUT_TEXT", "自动化测试内容"),
    "button_texts": {
        "login": "登录",
        "submit": "提交",
        "confirm": "确定",
        "cancel": "取消",
    },
    "timeout": {
        "element_wait": 10,  # 元素等待超时（秒）
        "page_load": 5,  # 页面加载超时（秒）
        "assertion": 3,  # 断言超时（秒）
    }
}

# ---------------------- 图像识别配置 ----------------------
IMAGE_RECOGNITION: Dict[str, Any] = {
    "template_dir": "/app/templates",  # 模板图片目录
    "confidence_threshold": 0.8,  # 识别置信度阈值
    "templates": {
        "login_button": "login_button.png",
        "submit_button": "submit_button.png",
        "confirm_button": "confirm_button.png",
    }
}

# ---------------------- 截图配置 ----------------------
SCREENSHOT_CONFIG: Dict[str, Any] = {
    "save_dir": "/app/screenshots",  # 截图保存目录
    "naming_prefix": "fail_screenshot_",  # 失败截图命名前缀
    "timestamp_format": "%Y%m%d_%H%M%S",  # 时间戳格式
}

# ---------------------- 日志配置 ----------------------
LOGGING_CONFIG: Dict[str, Any] = {
    "level": "INFO",  # 日志级别
    "format": "%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    "console_output": True,  # 是否输出到控制台
    "file_output": True,  # 是否输出到文件
    "log_file": "/app/test_log.txt",  # 日志文件路径
}

# ---------------------- 断言配置 ----------------------
ASSERTION_CONFIG: Dict[str, Any] = {
    "success_indicators": [  # 成功标识元素列表
        "登录成功",
        "首页",
        "主页面",
        "操作成功",
        "完成",
    ],
    "failure_indicators": [  # 失败标识元素列表
        "登录失败",
        "错误",
        "异常",
        "网络错误",
        "用户名或密码错误",
    ]
}