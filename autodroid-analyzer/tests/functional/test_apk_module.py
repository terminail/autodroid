#!/usr/bin/env python3
"""
测试APK模块重构后的功能
"""

import sys
import os

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

def test_database_connection():
    """测试数据库连接"""
    try:
        from core.database.models import db, create_tables
        
        # 创建数据库表
        create_tables()
        print("✓ 数据库表创建成功")
        
        # 测试数据库连接
        with db:
            db.execute_sql("SELECT 1")
        print("✓ 数据库连接成功")
        
        assert True, "数据库连接测试通过"
    except Exception as e:
        print(f"✗ 数据库连接失败: {e}")
        assert False, f"数据库连接测试失败: {e}"

def test_apk_models():
    """测试APK模型"""
    try:
        from core.database.models import Apk
        
        # 测试创建APK记录
        apk_data = {
            'id': 'com.example.testapp',
            'app_name': 'Test Application',
            'version_name': '1.0.0',
            'version_code': 100,
            'install_time': '2023-01-01 12:00:00',
            'is_packed': False,
            'packer_type': None,
            'packer_confidence': 0.0
        }
        
        # 创建或获取APK
        apk, created = Apk.get_or_create(
            id=apk_data['id'],
            defaults=apk_data
        )
        
        if created:
            print("✓ APK记录创建成功")
        else:
            print("✓ APK记录获取成功")
        
        # 验证字段
        assert apk.id == apk_data['id']
        assert apk.app_name == apk_data['app_name']
        print("✓ APK模型字段验证成功")
        
        assert True, "APK模型测试通过"
    except Exception as e:
        print(f"✗ APK模型测试失败: {e}")
        assert False, f"APK模型测试失败: {e}"

def test_apk_database():
    """测试APK数据库类"""
    try:
        from core.apk.database import ApkDatabase
        
        db = ApkDatabase()
        
        # 测试注册APK
        apk_data = {
            'package_name': 'com.example.testapp2',
            'app_name': 'Test Application 2',
            'version': '2.0.0',
            'version_code': 200,
            'installed_time': '2023-01-02 12:00:00',
            'is_packed': True,
            'packer_type': 'TestPacker',
            'packer_confidence': 0.95
        }
        
        apk = db.register_apk(apk_data)
        if apk:
            print("✓ APK注册成功")
        else:
            print("✗ APK注册失败")
            assert False, "APK注册失败"
        
        # 测试获取APK
        retrieved_apk = db.get_apk('com.example.testapp2')
        if retrieved_apk:
            print("✓ APK获取成功")
        else:
            print("✗ APK获取失败")
            assert False, "APK获取失败"
        
        # 测试获取所有APK
        all_apks = db.get_all_apks()
        print(f"✓ 获取到 {len(all_apks)} 个APK记录")
        
        assert True, "APK数据库测试通过"
    except Exception as e:
        print(f"✗ APK数据库测试失败: {e}")
        assert False, f"APK数据库测试失败: {e}"

def test_apk_service():
    """测试APK服务类"""
    try:
        from core.apk.service import ApkManager
        from core.apk.models import ApkCreateRequest
        
        manager = ApkManager()
        
        # 测试创建APK请求
        request = ApkCreateRequest(
            id='com.example.testapp3',
            app_name='Test Application 3',
            version_name='3.0.0',
            version_code=300,
            install_time='2023-01-03 12:00:00'
        )
        
        apk_info = manager.register_apk(request)
        if apk_info:
            print("✓ APK服务注册成功")
        else:
            print("✗ APK服务注册失败")
            assert False, "APK服务注册失败"
        
        # 测试获取APK
        retrieved_apk = manager.get_apk('com.example.testapp3')
        if retrieved_apk:
            print("✓ APK服务获取成功")
        else:
            print("✗ APK服务获取失败")
            assert False, "APK服务获取失败"
        
        # 测试获取所有APK
        all_apks = manager.get_all_apks()
        print(f"✓ 服务获取到 {len(all_apks)} 个APK记录")
        
        assert True, "APK服务测试通过"
    except Exception as e:
        print(f"✗ APK服务测试失败: {e}")
        assert False, f"APK服务测试失败: {e}"

def main():
    """主测试函数"""
    print("开始测试重构后的APK模块...")
    print("=" * 50)
    
    tests = [
        ("数据库连接", test_database_connection),
        ("APK模型", test_apk_models),
        ("APK数据库类", test_apk_database),
        ("APK服务类", test_apk_service)
    ]
    
    passed = 0
    total = len(tests)
    
    for test_name, test_func in tests:
        print(f"\n测试: {test_name}")
        print("-" * 30)
        if test_func():
            passed += 1
        
        print("-" * 30)
    
    print("=" * 50)
    print(f"测试完成: {passed}/{total} 通过")
    
    if passed == total:
        print("🎉 所有测试通过！APK模块重构成功！")
        return 0
    else:
        print("❌ 部分测试失败，需要检查问题")
        return 1

if __name__ == "__main__":
    sys.exit(main())