#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
ADB Dump Tool - 交互式页面转储工具
持续运行，等待用户命令，支持截图和XML页面结构导出，并进行页面识别
"""

import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from tools.u2device import U2Device
from tools.page import parse_xml
from tools.flow import FlowManager


class ADBDumper:
    def __init__(self, device_id: str = "TDCDU17905004388"):
        self.device_id = device_id
        self.adb_manager = U2Device(device_id)
        self.output_dir = Path(r"d:\git\autodroid\autodroid-trader-executor\tradescripts\dump-pages")
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.flow_manager = None
        self._init_flow_manager()

    def _init_flow_manager(self):
        """初始化流程管理器并加载页面指纹"""
        try:
            apk_dir = Path(r"d:\git\autodroid\autodroid-trader-executor\app\src\main\assets\apks")
            self.flow_manager = FlowManager(apk_dir)
            self.flow_manager.load_and_build_pages("com.tdx.androidCCZQ", "general")
            print(f"✓ 已加载页面数据")
        except Exception as e:
            print(f"⚠ 页面指纹加载失败: {e}")
            self.flow_manager = None

    def _get_timestamp(self) -> str:
        return time.strftime("%Y%m%d_%H%M%S")

    def _validate_xml(self, file_path: Path) -> bool:
        """验证 XML 文件是否有效（宽松验证）"""
        try:
            # 检查文件是否存在
            if not file_path.exists():
                print(f"    ⚠ 文件不存在: {file_path}")
                return False
            
            # 检查文件大小
            file_size = file_path.stat().st_size
            if file_size < 10:
                print(f"    ⚠ 文件太小: {file_size} bytes")
                return False
            
            # 尝试读取文件内容
            for encoding in ['utf-8', 'latin-1', 'cp1252']:
                try:
                    with open(file_path, 'r', encoding=encoding, errors='replace') as f:
                        content = f.read()
                    
                    # 宽松验证：只要包含基本XML结构就认为是有效的
                    if content.strip():
                        print(f"    ✓ 文件有内容，长度: {len(content)} 字符")
                        if '<?xml' in content or '<hierarchy' in content:
                            print(f"    ✓ 包含XML结构")
                            return True
                        else:
                            print(f"    ⚠ 不包含XML结构，前100字符: {content[:100]}")
                    else:
                        print(f"    ⚠ 文件内容为空")
                        
                except Exception as e:
                    print(f"    ⚠ 读取失败 ({encoding}): {e}")
                    continue
            
            return False
        except Exception as e:
            print(f"    XML 验证失败: {e}")
            return False

    def dump_xml_with_retry(self, xml_path: Path, max_retries: int = 3, retry_delay: float = 1.0) -> bool:
        """导出 XML，带验证和重试机制，以最后一次成功为准"""
        last_valid_xml = None
        
        for attempt in range(1, max_retries + 1):
            print(f"    第 {attempt}/{max_retries} 次尝试...")
            
            # 创建临时文件路径
            temp_xml_path = xml_path.with_suffix(f".temp{attempt}.xml")
            
            if self.adb_manager.dump_page_xml(self.device_id, str(temp_xml_path)):
                if temp_xml_path.exists() and self._validate_xml(temp_xml_path):
                    # 读取并保存有效的XML内容
                    try:
                        with open(temp_xml_path, 'r', encoding='utf-8', errors='replace') as f:
                            last_valid_xml = f.read()
                        print(f"    ✓ 第{attempt}次dump有效")
                    except Exception as e:
                        print(f"    ⚠ 读取XML失败: {e}")
                
                # 删除临时文件
                try:
                    temp_xml_path.unlink()
                except (OSError, FileNotFoundError):
                    pass
            
            if attempt < max_retries:
                print(f"    等待 {retry_delay:.1f} 秒后重试...")
                time.sleep(retry_delay)
        
        # 将最后一次有效的XML写入目标文件
        if last_valid_xml:
            try:
                with open(xml_path, 'w', encoding='utf-8', errors='replace') as f:
                    f.write(last_valid_xml)
                print("    ✓ XML 导出完成（以最后一次有效dump为准）")
                return True
            except Exception as e:
                print(f"    ✗ 写入最终XML失败: {e}")
        
        return False

    def dump_both(self) -> bool:
        timestamp = self._get_timestamp()
        success = True

        print(f"\n[{timestamp}] 开始导出...")

        screenshot_path = self.output_dir / f"screenshot_{timestamp}.png"
        print(f"  截图: {screenshot_path.name}")
        if not self.adb_manager.take_screenshot(self.device_id, str(screenshot_path)):
            print("  ✗ 截图失败")
            success = False
        else:
            print("  ✓ 截图完成")

        xml_path = self.output_dir / f"page_{timestamp}.xml"
        print(f"  XML: {xml_path.name}")
        if not self.dump_xml_with_retry(xml_path):
            print("  ✗ XML 导出失败，已重试 3 次")
            success = False
        else:
            print("  ✓ XML 导出完成")

        return success

    def run_interactive(self):
        print("=" * 50)
        print("ADB Dump Tool - 交互式页面转储工具")
        print("=" * 50)
        print(f"设备ID: {self.device_id}")
        print(f"输出目录: {self.output_dir}")
        print("\n命令: y=导出截图+XML, q=退出")
        print("-" * 50)

        while True:
            try:
                user_input = input("\n[?] 是否导出当前页面? (y/n/q): ").strip().lower()

            except (KeyboardInterrupt, EOFError):
                print("\n\n退出程序")
                break

            if user_input == 'q':
                print("退出程序")
                break
            elif user_input == 'y':
                self.dump_both()
            elif user_input == 'n':
                print("跳过")
            else:
                print("无效命令，请输入 y、n 或 q")


if __name__ == "__main__":
    dumper = ADBDumper()
    dumper.run_interactive()
