#!/usr/bin/env python3
"""
简单设备连接测试脚本
"""

import subprocess
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def test_device_connection():
    """测试设备连接"""
    try:
        # 检查ADB设备
        result = subprocess.run(["adb", "devices"], capture_output=True, text=True)
        if result.returncode == 0:
            logger.info(f"ADB设备列表:\n{result.stdout}")
            
            # 检查emulator-5554是否在线
            if "emulator-5554" in result.stdout:
                logger.info("✓ emulator-5554 设备已连接")
                
                # 测试基本命令
                logger.info("测试设备基本命令...")
                
                # 获取设备信息
                result = subprocess.run(["adb", "-s", "emulator-5554", "shell", "getprop", "ro.product.model"], capture_output=True, text=True)
                if result.returncode == 0:
                    logger.info(f"设备型号: {result.stdout.strip()}")
                
                # 测试输入命令
                logger.info("测试输入命令...")
                result = subprocess.run(["adb", "-s", "emulator-5554", "shell", "input", "text", "test"], capture_output=True, text=True)
                if result.returncode == 0:
                    logger.info("✓ 输入命令测试成功")
                else:
                    logger.error(f"输入命令测试失败: {result.stderr}")
                
                return True
            else:
                logger.error("✗ emulator-5554 设备未找到")
                return False
        else:
            logger.error(f"ADB命令失败: {result.stderr}")
            return False
            
    except Exception as e:
        logger.error(f"设备连接测试失败: {e}")
        return False

if __name__ == "__main__":
    success = test_device_connection()
    if success:
        logger.info("设备连接测试通过！")
    else:
        logger.error("设备连接测试失败！")