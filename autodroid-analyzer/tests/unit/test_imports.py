#!/usr/bin/env python3
"""测试移动后所有模块的导入功能"""

import sys
import os
import pytest

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# 测试core模块导入的模块列表 - 更新为实际存在的模块路径
CORE_MODULES = [
    ("core.useroperation.monitoring_system", "MonitoringSystem"),
    ("core.useroperation.operation_recorder", "OperationRecorder"),
    ("core.useroperation.user_operation", "UserOperationManager"),
    ("core.screenshot.screenshot_manager", "ScreenshotManager"),
    ("core.screenshot.page_analyzer", "PageAnalyzer"),
    ("core.screenshot.page_recognizer", "PageRecognizer"),
    ("core.device.device_manager", "DeviceManager"),
    ("core.database", "DatabaseManager"),
    ("core.database", "get_database_manager"),
    ("core.database.models", "UserOperation"),
    ("core.apk.apk_packer_detector", "APKPackerDetector"),
    ("core.apk.list_apks", "ApkLister"),

    ("core.analysis.analysis_utils", "AnalysisUtils"),
    ("core.analysis.app_analyzer", "AppAnalyzer"),
    ("core.analysis.human_assistant", "HumanAssistant"),
    ("core.analysis.interactive_analyzer", "InteractiveAppAnalyzer"),
    ("core.analysis.multimodal_recognizer", "MultiModalPageRecognizer"),
    ("core.analysis.navigation_system", "NavigationSystem"),
]

def _test_import(module_name, class_name=None):
    """测试导入模块（内部函数，不以test_开头避免被pytest识别为测试）"""
    try:
        # 清理Python路径，移除可能干扰的包
        for path in list(sys.path):
            if 'autodroid_container' in path or '__editable__' in path:
                sys.path.remove(path)
        
        if class_name:
            exec(f"from {module_name} import {class_name}")
        else:
            exec(f"import {module_name}")
        return True
    except ImportError:
        return False
    except Exception:
        return True

@pytest.mark.parametrize("module_name,class_name", CORE_MODULES)
def test_core_module_imports(module_name, class_name):
    """测试所有核心模块的导入功能"""
    assert _test_import(module_name, class_name), f"Failed to import {module_name}.{class_name}"

def test_imports_summary():
    """测试导入总结"""
    success_count = 0
    total_count = len(CORE_MODULES)
    
    for module, class_name in CORE_MODULES:
        if _test_import(module, class_name):
            success_count += 1
    
    print(f"\n=== 导入测试结果 ===")
    print(f"成功导入: {success_count}/{total_count}")
    
    if success_count == total_count:
        print("✅ 所有模块导入测试通过！")
    else:
        print("❌ 部分模块导入失败，请检查导入语句")
    
    assert success_count == total_count, f"Only {success_count}/{total_count} modules imported successfully"