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

from core.tradescript.adb_driver import ADBManager
from tools.page import parse_xml, PageMatcher


class ADBDumper:
    def __init__(self, device_id: str = "TDCDU17905004388"):
        self.device_id = device_id
        self.adb_manager = ADBManager()
        self.output_dir = Path(r"d:\git\autodroid\autodroid-trader-executor\tradescripts\dump-pages")
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.page_matcher = None
        self._init_page_matcher()

    def _init_page_matcher(self):
        """初始化页面匹配器并加载页面指纹"""
        try:
            apk_dir = Path(r"d:\git\autodroid\autodroid-trader-executor\app\src\main\assets\apks\com.tdx.androidCCZQ")
            self.page_matcher = PageMatcher(apk_dir)
            self.page_matcher.load_and_build_fingerprints("com.tdx.androidCCZQ", "general")
            print(f"✓ 已加载 {len(self.page_matcher._page_fingerprints)} 个页面指纹")
        except Exception as e:
            print(f"⚠ 页面指纹加载失败: {e}")
            self.page_matcher = None

    def _get_timestamp(self) -> str:
        return time.strftime("%Y%m%d_%H%M%S")

    def _validate_xml(self, file_path: Path) -> bool:
        """验证 XML 文件是否有效"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            if not content.strip():
                return False
            return True
        except Exception as e:
            print(f"    XML 验证失败: {e}")
            return False

    def dump_xml_with_retry(self, xml_path: Path, max_retries: int = 3, retry_delay: float = 1.0) -> bool:
        """导出 XML，带验证和重试机制"""
        for attempt in range(1, max_retries + 1):
            print(f"    第 {attempt}/{max_retries} 次尝试...")
            if self.adb_manager.dump_page(self.device_id, str(xml_path)):
                if xml_path.exists() and self._validate_xml(xml_path):
                    print("    ✓ XML 导出完成")
                    return True
            if attempt < max_retries:
                print(f"    等待 {retry_delay:.1f} 秒后重试...")
                time.sleep(retry_delay)
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
