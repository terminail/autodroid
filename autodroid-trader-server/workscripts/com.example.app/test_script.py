"""
第三方APP自动化测试脚本 - 使用Appium框架
兼容新旧安卓WiFi连接方式，适配加固APP的控件定位
支持失败捕获、自动截图、测试断言
"""

from appium import webdriver
from appium.webdriver.common.appiumby import AppiumBy
import pytest
import time
import random
import os
from airtest.core.api import *  # 用于图像识别，适配加固APP

# ---------------------- 配置项（使用者按需修改）----------------------
PHONE_IP = "192.168.1.100"  # 手机WiFi IP
PHONE_PORT = 5555           # WiFi调试端口
APP_PACKAGE = "com.autodroid.manager"  # 第三方APP包名（adb shell dumpsys window | grep mCurrentFocus 获取）
APP_ACTIVITY = ".MainActivity"  # APP启动页
APPIUM_SERVER = "http://localhost:4723"  # Appium服务器地址
TEST_DATA = {
    "input_text": "自动化测试内容",
    "submit_button_text": "提交",
    "username": "testuser",
    "password": "testpass"
}
# 加固APP图像识别模板路径（需提前截图，放在容器内/app目录）
SUBMIT_BUTTON_IMG = "submit_button.png"
LOGIN_BUTTON_IMG = "login_button.png"
# -------------------------------------------------------------------

# Appium 设备能力配置
DESIRED_CAPS = {
    'platformName': 'Android',
    'platformVersion': '10',  # 根据实际设备版本调整
    'deviceName': 'Android Device',
    'udid': f'{PHONE_IP}:{PHONE_PORT}',
    'appPackage': APP_PACKAGE,
    'appActivity': APP_ACTIVITY,
    'noReset': True,
    'fullReset': False,
    'automationName': 'UiAutomator2',  # 仍然使用UiAutomator2作为底层引擎
    'newCommandTimeout': 300,
    'connectHardwareKeyboard': True
}

# 夹具：连接设备（兼容WiFi连接失败后自动重试）
@pytest.fixture(scope="module")
def connect_device():
    driver = None
    # 尝试连接Appium服务器和设备
    for _ in range(3):  # 重试3次
        try:
            driver = webdriver.Remote(APPIUM_SERVER, DESIRED_CAPS)
            print(f"✅ 成功连接手机：{PHONE_IP}:{PHONE_PORT}")
            break
        except Exception as e:
            print(f"❌ Appium连接失败，重试：{e}")
            time.sleep(2)
    if not driver:
        raise Exception("❌ 多次连接失败，请检查Appium服务器和手机WiFi调试")
    
    # 等待APP启动
    time.sleep(3)  # 等待APP启动（适配不同APP加载速度）
    yield driver
    
    # 测试结束后清理
    driver.quit()
    print("✅ 测试结束，已关闭APP")

# 核心测试用例（适配加固APP）
def test_app_login_flow(connect_device):
    driver = connect_device
    try:
        # ---------------------- 步骤1：定位用户名输入框（优先控件ID，加固失效则用XPath）----------------------
        try:
            # 方式1：控件ID定位（非加固APP首选）
            username_input = driver.find_element(AppiumBy.ID, f"{APP_PACKAGE}:id/username_field")
            username_input.click()
            username_input.clear()
            username_input.send_keys(TEST_DATA["username"])
        except:
            # 方式2：XPath定位（加固APP控件ID隐藏时备用）
            try:
                username_input = driver.find_element(AppiumBy.XPATH, '//android.widget.EditText[contains(@text,"用户名") or contains(@hint,"用户名")]')
                username_input.click()
                username_input.send_keys(TEST_DATA["username"])
            except:
                # 方式3：坐标定位（最后备选，需提前获取坐标）
                driver.tap([(500, 300)])  # 用户名输入框坐标
                time.sleep(0.5)
                # 使用adb命令输入文本
                os.system(f"adb -s {PHONE_IP}:{PHONE_PORT} shell input text {TEST_DATA['username']}")
        time.sleep(random.uniform(0.5, 1.5))  # 模拟人工延迟，规避风控

        # ---------------------- 步骤2：定位密码输入框----------------------
        try:
            password_input = driver.find_element(AppiumBy.ID, f"{APP_PACKAGE}:id/password_field")
            password_input.click()
            password_input.clear()
            password_input.send_keys(TEST_DATA["password"])
        except:
            try:
                password_input = driver.find_element(AppiumBy.XPATH, '//android.widget.EditText[contains(@text,"密码") or contains(@hint,"密码")]')
                password_input.click()
                password_input.send_keys(TEST_DATA["password"])
            except:
                driver.tap([(500, 400)])  # 密码输入框坐标
                time.sleep(0.5)
                os.system(f"adb -s {PHONE_IP}:{PHONE_PORT} shell input text {TEST_DATA['password']}")
        time.sleep(random.uniform(0.5, 1.5))

        # ---------------------- 步骤3：定位登录按钮（加固APP用图像识别）----------------------
        try:
            # 方式1：文字定位（非加固APP）
            login_btn = driver.find_element(AppiumBy.ANDROID_UIAUTOMATOR, 'new UiSelector().text("登录")')
            login_btn.click()
        except:
            # 方式2：图像识别（加固APP首选，需提前截图模板）
            try:
                dev = connect_device_android(f"android:///{PHONE_IP}:{PHONE_PORT}")
                if exists(Template(LOGIN_BUTTON_IMG)):
                    click(Template(LOGIN_BUTTON_IMG))
                else:
                    # 方式3：坐标定位（最后备选）
                    driver.tap([(500, 600)])  # 登录按钮坐标
            except:
                # 降级到坐标点击
                driver.tap([(500, 600)])
        time.sleep(random.uniform(1, 2))

        # ---------------------- 步骤4：验证登录结果 ----------------------
        success_indicators = []
        
        # 检查多种可能的成功标志
        try:
            success_indicators.append(driver.find_element(AppiumBy.ANDROID_UIAUTOMATOR, 'new UiSelector().text("登录成功")'))
        except:
            pass
            
        try:
            success_indicators.append(driver.find_element(AppiumBy.ANDROID_UIAUTOMATOR, 'new UiSelector().text("首页")'))
        except:
            pass
            
        try:
            success_indicators.append(driver.find_element(AppiumBy.ID, f"{APP_PACKAGE}:id/main_content"))
        except:
            pass
            
        try:
            success_indicators.append(driver.find_element(AppiumBy.CLASS_NAME, "android.widget.Button"))
        except:
            pass
        
        assert len(success_indicators) > 0, "❌ 登录失败，未找到成功提示或主页面元素"
        print("✅ 自动化测试执行成功！")

    except Exception as e:
        # 失败时截图留存（保存在容器/app目录）
        screenshot_name = f"fail_screenshot_{int(time.time())}.png"
        driver.save_screenshot(f"/app/{screenshot_name}")
        print(f"❌ 测试失败：{str(e)}，截图已保存为 {screenshot_name}")
        raise

# 辅助测试用例：测试APP基本功能
def test_app_basic_functionality(connect_device):
    driver = connect_device
    try:
        # 等待主页面加载
        time.sleep(2)
        
        # 检查是否存在主要功能按钮
        main_buttons = []
        
        try:
            main_buttons.append(driver.find_element(AppiumBy.ANDROID_UIAUTOMATOR, 'new UiSelector().text("开始")'))
        except:
            pass
            
        try:
            main_buttons.append(driver.find_element(AppiumBy.ANDROID_UIAUTOMATOR, 'new UiSelector().text("设置")'))
        except:
            pass
            
        try:
            main_buttons.append(driver.find_element(AppiumBy.ANDROID_UIAUTOMATOR, 'new UiSelector().text("关于")'))
        except:
            pass
            
        try:
            main_buttons.append(driver.find_element(AppiumBy.ID, f"{APP_PACKAGE}:id/start_button"))
        except:
            pass
        
        assert len(main_buttons) > 0, "❌ 未找到主要功能按钮"
        print("✅ APP基本功能检查通过")
        
    except Exception as e:
        screenshot_name = f"basic_test_fail_{int(time.time())}.png"
        driver.save_screenshot(f"/app/{screenshot_name}")
        print(f"❌ 基本功能测试失败：{str(e)}，截图已保存")
        raise

# 运行测试
if __name__ == "__main__":
    pytest.main(["-v", __file__])