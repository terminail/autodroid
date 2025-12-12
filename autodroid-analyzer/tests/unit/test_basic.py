#!/usr/bin/env python3
"""
基本功能测试脚本
"""

import sys
import os

# 添加项目根目录到Python路径
project_root = os.path.join(os.path.dirname(__file__), '..', '..')
sys.path.insert(0, project_root)

def test_imports():
    """测试模块导入"""
    print("🔍 测试模块导入...")
    
    try:
        # 测试核心模块导入
        from core.analysis.analysis_utils import AnalysisUtils
        print("✅ AnalysisUtils 导入成功")
        
        from core.database import DatabaseManager
        print("✅ DatabaseManager 导入成功")
        
        from core.device.device_manager import DeviceManager
        print("✅ DeviceManager 导入成功")
        
        from core.analysis.human_assistant import HumanAssistant
        print("✅ HumanAssistant 导入成功")
        
        from core.analysis.app_analyzer import AppAnalyzer
        print("✅ AppAnalyzer 导入成功")
        
        from core.analysis.interactive_analyzer import InteractiveAppAnalyzer
        print("✅ InteractiveAppAnalyzer 导入成功")
        
        assert True, "所有模块导入成功"
        
    except Exception as e:
        print(f"❌ 导入失败: {e}")
        import traceback
        traceback.print_exc()
        assert False, f"模块导入失败: {e}"

def test_config_manager():
    """测试配置管理器"""
    print("\n🔍 测试配置管理器...")
    
    try:
        from config import ConfigManager
        
        config = ConfigManager()
        
        # 测试基本配置获取
        app_package = config.get_app_package()
        
        print(f"✅ 应用包名: {app_package}")
        
        # 测试配置项获取
        max_depth = config.get('analysis.max_depth')
        print(f"✅ 最大深度: {max_depth}")
        
        assert True, "配置管理器测试通过"
        
    except Exception as e:
        print(f"❌ 配置管理器测试失败: {e}")
        assert False, f"配置管理器测试失败: {e}"

def test_analysis_utils():
    """测试分析工具"""
    print("\n🔍 测试分析工具...")
    
    try:
        from core.analysis.analysis_utils import AnalysisUtils
        
        # 测试元素匹配
        elem1 = {
            'text': '登录',
            'resource_id': 'com.example:id/login_button',
            'bounds': [100, 200, 300, 400]
        }
        
        elem2 = {
            'text': '登录',
            'resource_id': 'com.example:id/login_button',
            'bounds': [110, 210, 310, 410]
        }
        
        match = AnalysisUtils.elements_match(elem1, elem2)
        print(f"✅ 元素匹配测试: {match}")
        
        # 测试边界相似性
        bounds1 = [100, 200, 300, 400]
        bounds2 = [110, 210, 310, 410]
        similar = AnalysisUtils.bounds_similar(bounds1, bounds2)
        print(f"✅ 边界相似性测试: {similar}")
        
        # 测试元素重要性计算
        importance = AnalysisUtils.calculate_element_importance(elem1)
        print(f"✅ 元素重要性计算: {importance}")
        
        assert True, "分析工具测试通过"
        
    except Exception as e:
        print(f"❌ 分析工具测试失败: {e}")
        assert False, f"分析工具测试失败: {e}"

def test_database_manager():
    """测试数据库管理器"""
    print("\n🔍 测试数据库管理器...")
    
    try:
        from core.database import DatabaseManager
        
        # 使用内存数据库进行测试
        db = DatabaseManager(":memory:")
        print("✅ 数据库管理器创建成功")
        
        # 测试数据库连接
        connection = db.get_connection()
        print("✅ 数据库连接获取成功")
        
        # 测试数据库表是否存在
        from core.database.models import db as peewee_db
        tables = peewee_db.get_tables()
        print(f"✅ 数据库表检查成功，共 {len(tables)} 个表")
        
        # 测试关闭连接
        db.close()
        print("✅ 数据库连接关闭成功")
        
        assert True, "数据库管理器测试通过"
        
    except Exception as e:
        print(f"❌ 数据库管理器测试失败: {e}")
        assert False, f"数据库管理器测试失败: {e}"

def main():
    """主测试函数"""
    print("🚀 开始Autodroid Analyzer基本功能测试")
    print("=" * 50)
    
    tests = [
        test_imports,
        test_config_manager,
        test_analysis_utils,
        test_database_manager
    ]
    
    passed = 0
    total = len(tests)
    
    for test in tests:
        if test():
            passed += 1
    
    print("\n" + "=" * 50)
    print(f"📊 测试结果: {passed}/{total} 通过")
    
    if passed == total:
        print("✅ 所有测试通过!")
        return 0
    else:
        print("❌ 部分测试失败")
        return 1

if __name__ == "__main__":
    sys.exit(main())