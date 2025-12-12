#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
交互式应用分析器 - 配置驱动运行脚本
使用config.yaml中的配置运行分析器
"""

import sys
import os
import yaml
from typing import Dict, Any, Optional

# 添加当前目录到Python路径
current_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, current_dir)

try:
    from core.analysis.interactive_analyzer import InteractiveAppAnalyzer
except ImportError as e:
    print(f"❌ 导入错误: {e}")
    print(f"💡 当前目录: {current_dir}")
    print("💡 请确保在正确的目录下运行此脚本")
    sys.exit(1)

class AnalysisRunner:
    """分析器运行器"""
    
    def __init__(self):
        self.analyzer = None
        self.config = {}
    
    def load_config(self) -> bool:
        """
        从config.yaml加载配置
        
        Returns:
            True表示成功，False表示失败
        """
        try:
            config_path = os.path.join(current_dir, 'config.yaml')
            if not os.path.exists(config_path):
                print(f"❌ 配置文件不存在: {config_path}")
                return False
            
            with open(config_path, 'r', encoding='utf-8') as f:
                self.config = yaml.safe_load(f)
            
            print("✅ 配置加载成功")
            return True
            
        except Exception as e:
            print(f"❌ 加载配置失败: {e}")
            return False
    
    def create_analyzer(self) -> bool:
        """
        创建分析器实例
        
        Returns:
            True表示成功，False表示失败
        """
        try:
            # 获取设备ID和应用包名
            device_id = self.config.get('device_id')
            app_package = self.config.get('app_package')
            
            if not device_id or not app_package:
                print("❌ 需要提供设备ID和应用包名")
                print("💡 请在config.yaml中配置device_id和app_package，或通过命令行参数传递")
                return False
            
            print(f"🔧 创建分析器实例...")
            print(f"   设备: {device_id}")
            print(f"   应用: {app_package}")
            
            self.analyzer = InteractiveAppAnalyzer(device_id=device_id, app_package=app_package)
            
            # 检查分析器是否成功初始化
            if not hasattr(self.analyzer, 'app_package'):
                print("❌ 分析器初始化失败")
                return False
            
            print("✅ 分析器创建成功")
            return True
            
        except Exception as e:
            print(f"❌ 创建分析器失败: {e}")
            return False
    
    def run_analysis(self, max_depth: int = 3, enable_monitoring: bool = True) -> bool:
        """
        运行分析
        
        Args:
            max_depth: 最大探索深度
            enable_monitoring: 是否启用监控
            
        Returns:
            True表示成功，False表示失败
        """
        if not self.analyzer:
            print("❌ 分析器未初始化")
            return False
        
        try:
            # 检查设备连接
            print("\n🔌 检查设备连接...")
            if not self.analyzer.check_device_connection():
                print("⚠️ 设备连接失败，进入演示模式")
                print("💡 演示模式将使用模拟数据进行测试")
                
                # 演示模式：跳过设备相关操作，直接进行测试分析
                print("\n🚀 进入演示模式...")
                print("💡 演示模式将展示分析器的基本功能")
                
                # 创建模拟分析结果
                self._run_demo_mode(max_depth, enable_monitoring)
                return True
            
            print("✅ 设备连接成功")
            
            # 启动应用
            print("\n🚀 启动应用...")
            if not self.analyzer.launch_app():
                print("\n💡 请手动在手机上启动应用:")
                print(f"   1. 解锁手机屏幕")
                print(f"   2. 找到并点击 '{self.analyzer.app_package}' 应用")
                print(f"   3. 等待应用完全启动")
                print("   4. 确保应用界面显示在屏幕上")
                
                # 等待用户确认应用已启动
                input("\n📝 应用启动完成后，按回车键继续... ")
                
                # 再次检查应用是否已启动
                print("\n🔍 重新检查应用状态...")
                if not self.analyzer.launch_app():
                    print("❌ 应用仍未启动，请检查应用包名是否正确")
                    return False
            
            print("✅ 应用启动成功")
            
            # 等待应用完全启动
            print("⏳ 等待应用完全启动...")
            import time
            time.sleep(3)
            
            # 检查当前页面状态
            print("🔍 检查当前页面状态...")
            current_page = self.analyzer.get_current_page()
            if current_page:
                print(f"✅ 当前页面: {current_page.title}")
            else:
                print("⚠️ 无法获取当前页面信息")
            
            # 提示用户开始监控
            print("\n" + "=" * 50)
            print("🎯 准备就绪！")
            print("💡 应用已启动并运行在设备上")
            print("💡 请确保应用界面已显示在屏幕上")
            input("📝 继续监控(Y/n): ")
            print("\n🔍 开始交互式分析...")
            
            # 开始交互式分析
            print(f"   最大深度: {max_depth}")
            print(f"   启用监控: {'是' if enable_monitoring else '否'}")
            
            self.analyzer.analyze_with_user_interaction(
                max_depth=max_depth,
                enable_monitoring=enable_monitoring
            )
            
            # 生成分析报告
            print("\n📊 生成分析报告...")
            self.analyzer.generate_analysis_report()
            
            print("✅ 分析完成！")
            return True
            
        except KeyboardInterrupt:
            print("\n⚠️ 用户中断分析")
            return False
        except Exception as e:
            print(f"❌ 分析过程中出错: {e}")
            return False
    
    def _run_demo_mode(self, max_depth: int, enable_monitoring: bool):
        """
        演示模式：在没有设备连接时运行
        
        Args:
            max_depth: 最大探索深度
            enable_monitoring: 是否启用监控
        """
        try:
            print("\n🎭 演示模式启动")
            print("=" * 50)
            
            # 创建模拟页面数据
            demo_pages = [
                {
                    "page_id": "demo_login",
                    "title": "登录页面",
                    "activity_name": "com.autodroid.manager.LoginActivity",
                    "element_count": 8,
                    "elements": [
                        {"text": "用户名", "clickable": True, "class": "android.widget.EditText"},
                        {"text": "密码", "clickable": True, "class": "android.widget.EditText"},
                        {"text": "登录", "clickable": True, "class": "android.widget.Button"},
                        {"text": "忘记密码", "clickable": True, "class": "android.widget.TextView"}
                    ]
                },
                {
                    "page_id": "demo_main",
                    "title": "主页面",
                    "activity_name": "com.autodroid.manager.MainActivity",
                    "element_count": 15,
                    "elements": [
                        {"text": "设备列表", "clickable": True, "class": "android.widget.Button"},
                        {"text": "脚本管理", "clickable": True, "class": "android.widget.Button"},
                        {"text": "设置", "clickable": True, "class": "android.widget.Button"},
                        {"text": "帮助", "clickable": True, "class": "android.widget.Button"}
                    ]
                },
                {
                    "page_id": "demo_settings",
                    "title": "设置页面",
                    "activity_name": "com.autodroid.manager.SettingsActivity",
                    "element_count": 12,
                    "elements": [
                        {"text": "语言设置", "clickable": True, "class": "android.widget.TextView"},
                        {"text": "通知设置", "clickable": True, "class": "android.widget.TextView"},
                        {"text": "关于应用", "clickable": True, "class": "android.widget.TextView"}
                    ]
                }
            ]
            
            # 模拟分析过程
            print(f"🔍 模拟分析过程...")
            print(f"   最大深度: {max_depth}")
            print(f"   启用监控: {'是' if enable_monitoring else '否'}")
            
            # 模拟页面遍历
            for i, page_data in enumerate(demo_pages):
                if i >= max_depth:
                    break
                    
                print(f"\n📄 分析页面 {i+1}: {page_data['title']}")
                print(f"   活动名: {page_data['activity_name']}")
                print(f"   元素数量: {page_data['element_count']}")
                
                # 模拟多模态分析
                print("🔍 多模态分析中...")
                
                # 显示页面元素
                print("📋 页面元素:")
                for element in page_data['elements']:
                    clickable = "✅ 可点击" if element['clickable'] else "❌ 不可点击"
                    print(f"   - {element['text']} ({element['class']}) - {clickable}")
                
                # 模拟用户操作监控
                if enable_monitoring:
                    print("👀 模拟用户操作监控...")
                    print("   - 用户点击了 '设备列表' 按钮")
                    print("   - 用户输入了用户名和密码")
                    print("   - 用户导航到设置页面")
            
            # 生成演示报告
            print("\n📊 生成演示报告...")
            
            # 创建演示报告目录
            import os
            report_dir = os.path.join(os.getcwd(), 'analysis_output', 'demo_reports')
            os.makedirs(report_dir, exist_ok=True)
            
            # 生成简单的演示报告
            report_content = f"""
# 交互式应用分析器 - 演示报告

## 分析概要
- 分析时间: 演示模式
- 分析应用: com.autodroid.manager
- 最大深度: {max_depth}
- 启用监控: {enable_monitoring}

## 发现的页面
"""
            
            for page_data in demo_pages:
                report_content += f"""
### {page_data['title']}
- 页面ID: {page_data['page_id']}
- 活动名: {page_data['activity_name']}
- 元素数量: {page_data['element_count']}

**主要元素:**
"""
                for element in page_data['elements']:
                    report_content += f"- {element['text']} ({element['class']})\n"
            
            report_content += """
## 分析结论
演示模式成功展示了分析器的基本功能。在实际使用时，请确保设备连接正常。
"""
            
            # 保存报告
            report_path = os.path.join(report_dir, 'demo_analysis_report.md')
            with open(report_path, 'w', encoding='utf-8') as f:
                f.write(report_content)
            
            print(f"✅ 演示报告已保存: {report_path}")
            print("\n🎉 演示模式完成！")
            print("💡 在实际使用时，请确保Android设备或模拟器已连接")
            
        except Exception as e:
            print(f"❌ 演示模式运行失败: {e}")
    
    def run(self) -> bool:
        """
        运行分析流程
        
        Returns:
            True表示成功，False表示失败
        """
        print("🎯 交互式应用分析器")
        print("=" * 50)
        
        # 加载配置
        if not self.load_config():
            return False
        
        # 获取分析配置
        analysis_config = self.config.get('analysis', {})
        max_depth = analysis_config.get('max_depth', 3)
        enable_monitoring = analysis_config.get('enable_monitoring', True)
        
        print(f"📋 配置信息:")
        print(f"   最大深度: {max_depth}")
        print(f"   启用监控: {'是' if enable_monitoring else '否'}")
        
        # 创建分析器
        if not self.create_analyzer():
            return False
        
        # 运行分析
        return self.run_analysis(max_depth, enable_monitoring)

def main():
    """主函数"""
    # 创建运行器并执行
    runner = AnalysisRunner()
    success = runner.run()
    
    if success:
        print("\n🎉 分析任务完成！")
        sys.exit(0)
    else:
        print("\n❌ 分析任务失败")
        sys.exit(1)

if __name__ == "__main__":
    main()