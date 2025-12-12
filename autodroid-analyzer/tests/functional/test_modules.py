"""
测试重构后的模块功能
"""

import sys
import os

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

def test_device_module():
    """测试设备模块功能"""
    print("=== 测试设备模块 ===")
    try:
        from core.device.service import DeviceManager
        from core.database.models import Device
        
        # 创建设备管理器实例
        device_manager = DeviceManager()
        print("✓ 设备管理器创建成功")
        
        # 测试获取设备列表（数据库连接在内部处理）
        devices = device_manager.get_all_devices()
        print(f"✓ 获取设备列表成功，共 {len(devices)} 个设备")
        
        # 测试设备计数
        device_count = device_manager.get_device_count()
        print(f"✓ 获取设备计数成功: {device_count}")
        
        # 测试模型导入
        print(f"✓ Device模型导入成功: {Device.__name__}")
        
        print("设备模块测试通过！")
        assert True, "设备模块测试通过"
        
    except Exception as e:
        import traceback
        print(f"✗ 设备模块测试失败: {str(e)}")
        print("详细错误信息:")
        traceback.print_exc()
        assert False, f"设备模块测试失败: {str(e)}"

def test_screenshot_module():
    """测试截屏模块功能"""
    print("\n=== 测试截屏模块 ===")
    try:
        from core.screenshot.service import ScreenshotManager
        from core.database.models import Screenshot
        
        # 创建截屏管理器实例
        screenshot_manager = ScreenshotManager()
        print("✓ 截屏管理器创建成功")
        
        # 测试获取最近截屏列表
        screenshots = screenshot_manager.get_recent_screenshots()
        print(f"✓ 获取最近截屏列表成功，共 {len(screenshots)} 个截屏")
        
        # 测试截屏分析功能
        analysis_result = screenshot_manager.analyze_screenshot("test_screenshot_001")
        print(f"✓ 截屏分析功能测试: {analysis_result}")
        
        # 测试模型导入
        print(f"✓ Screenshot模型导入成功: {Screenshot.__name__}")
        
        print("截屏模块测试通过！")
        assert True, "截屏模块测试通过"
        
    except Exception as e:
        import traceback
        print(f"✗ 截屏模块测试失败: {str(e)}")
        print("详细错误信息:")
        traceback.print_exc()
        assert False, f"截屏模块测试失败: {str(e)}"

def test_useroperation_module():
    """测试用户操作模块功能"""
    print("\n=== 测试用户操作模块 ===")
    try:
        from core.useroperation.service import UserOperationManager
        from core.database.models import UserOperation
        
        # 创建用户操作管理器实例
        userop_manager = UserOperationManager()
        print("✓ 用户操作管理器创建成功")
        
        # 测试获取最近操作记录
        operations = userop_manager.get_recent_operations()
        print(f"✓ 获取最近操作记录成功，共 {len(operations)} 条记录")
        
        # 测试操作计数
        operation_count = userop_manager.get_operation_count()
        print(f"✓ 获取操作计数成功: {operation_count}")
        
        # 测试模型导入
        print(f"✓ UserOperation模型导入成功: {UserOperation.__name__}")
        
        print("用户操作模块测试通过！")
        assert True, "用户操作模块测试通过"
        
    except Exception as e:
        import traceback
        print(f"✗ 用户操作模块测试失败: {str(e)}")
        print("详细错误信息:")
        traceback.print_exc()
        assert False, f"用户操作模块测试失败: {str(e)}"

def test_module_imports():
    """测试模块导入功能"""
    print("\n=== 测试模块导入 ===")
    try:
        # 测试导入所有模块
        from core.device.service import DeviceManager
        from core.database.models import Device
        from core.device.database import DeviceDatabase
        
        from core.screenshot.service import ScreenshotManager
        from core.database.models import Screenshot
        from core.screenshot.database import ScreenshotDatabase
        
        from core.useroperation.service import UserOperationManager
        from core.database.models import UserOperation
        from core.useroperation.database import UserOperationDatabase
        
        print("✓ 所有模块导入成功")
        print("✓ 所有服务类导入成功")
        print("✓ 所有数据库类导入成功")
        print("✓ 所有模型类导入成功")
        
        print("模块导入测试通过！")
        assert True, "模块导入测试通过"
        
    except Exception as e:
        import traceback
        print(f"✗ 模块导入测试失败: {str(e)}")
        print("详细错误信息:")
        traceback.print_exc()
        assert False, f"模块导入测试失败: {str(e)}"

def main():
    """主测试函数"""
    print("开始测试重构后的模块功能...")
    
    # 运行所有测试
    results = []
    results.append(test_device_module())
    results.append(test_screenshot_module())
    results.append(test_useroperation_module())
    results.append(test_module_imports())
    
    # 统计结果
    passed = sum(results)
    total = len(results)
    
    print(f"\n=== 测试结果 ===")
    print(f"通过: {passed}/{total}")
    print(f"失败: {total - passed}/{total}")
    
    if passed == total:
        print("🎉 所有测试通过！重构成功！")
        return True
    else:
        print("❌ 部分测试失败，请检查代码")
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)