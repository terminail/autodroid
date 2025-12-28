#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
明佣宝 App 冷启动与网格交易页面导航测试
验证 Tradescript Engine 的完整执行流程
"""

import sys
import time
import logging
import json
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from core.tradescript.adb_driver import ADBManager
from core.tradescript.page_matcher import PageMatcher
from core.tradescript.data_executor import DataDrivenExecutor
from core.tradescript.flow_coordinator import TradeflowCoordinator

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler("test_execution.log", encoding="utf-8"),
    ],
)
logger = logging.getLogger("VerificationTest")


class VerificationResult:
    def __init__(self):
        self.test_name = ""
        self.success = False
        self.message = ""
        self.execution_time = 0.0
        self.details = {}
        self.timestamp = time.strftime("%Y-%m-%d %H:%M:%S")

    def to_dict(self):
        return {
            "test_name": self.test_name,
            "success": self.success,
            "message": self.message,
            "execution_time": f"{self.execution_time:.2f}s",
            "timestamp": self.timestamp,
            "details": self.details,
        }


class TradeScriptVerifier:
    def __init__(
        self,
        apks_dir: str = "app/src/main/assets/apks/com.tdx.androidCCZQ/netgrid-trading",
        device_id: str = "TDCDU17905004388",
    ):
        self.apks_dir = Path(apks_dir)
        self.device_id = device_id
        self.package_name = "com.tdx.androidCCZQ"

        self.adb_manager = ADBManager()
        self.page_matcher = None
        self.data_executor = None
        self.flow_coordinator = None

    def initialize(self) -> bool:
        logger.info(f"初始化测试环境，设备ID: {self.device_id}")

        try:
            self.page_matcher = PageMatcher(
                apks_dir=str(self.apks_dir.parent),
                match_threshold=0.7,
            )

            self.data_executor = DataDrivenExecutor(
                adb_manager=self.adb_manager,
                device_id=self.device_id,
                apks_dir=str(self.apks_dir.parent),
                wait_timeout=30,
            )

            self.flow_coordinator = TradeflowCoordinator(
                data_executor=self.data_executor,
                page_matcher=self.page_matcher,
                adb_manager=self.adb_manager,
                device_id=self.device_id,
                max_navigation_attempts=3,
            )

            logger.info("测试环境初始化成功")
            return True

        except Exception as e:
            logger.error(f"初始化失败: {e}")
            return False

    def dump_page_source(self) -> str:
        result = self.adb_manager.run_command([
            "-s", self.device_id, "shell", "uiautomator", "dump", "/sdcard/window_dump.xml"
        ])
        if result.returncode != 0:
            logger.error(f"页面抓取失败: {result.stderr}")
            return ""

        result = self.adb_manager.run_command([
            "-s", self.device_id, "shell", "cat", "/sdcard/window_dump.xml"
        ])
        if result.returncode == 0:
            return result.stdout.strip()
        return ""

    def cold_start_app(self) -> VerificationResult:
        result = VerificationResult()
        result.test_name = "冷启动测试"
        start_time = time.time()

        logger.info(f"执行冷启动测试: {self.package_name}")

        try:
            self.adb_manager.run_command(
                ["-s", self.device_id, "shell", "am", "force-stop", self.package_name]
            )
            time.sleep(1)

            result_start = self.adb_manager.start_app(
                device_id=self.device_id,
                app_package=self.package_name,
                app_activity=".MainActivity",
                wait=True,
                timeout=10,
            )

            if not result_start:
                result.success = False
                result.message = "应用启动失败"
                result.details["error"] = "ADB start_app returned False"
                return result

            time.sleep(3)

            xml_source = self.dump_page_source()
            if not xml_source:
                result.success = False
                result.message = "无法获取页面 XML"
                result.details["error"] = "ADB dump returned empty XML"
                return result

            page_match = self.page_matcher.identify_current_page(xml_source)
            logger.info(f"识别页面: {page_match.page_id} (置信度: {page_match.score:.2f})")

            result.success = True
            result.message = f"应用冷启动成功，当前页面: {page_match.page_id}"
            result.details = {
                "app_package": self.package_name,
                "current_page": page_match.page_id,
                "page_score": page_match.score,
                "elements_count": len(page_match.matched_elements),
            }

        except Exception as e:
            result.success = False
            result.message = f"冷启动测试失败: {str(e)}"
            result.details["error"] = str(e)

        result.execution_time = time.time() - start_time
        return result

    def navigate_to_grid_trading(self) -> VerificationResult:
        result = VerificationResult()
        result.test_name = "导航到网格交易测试"
        start_time = time.time()

        logger.info("执行导航到网格交易测试")

        try:
            current_xml = self.dump_page_source()
            if not current_xml:
                result.success = False
                result.message = "无法获取当前页面 XML"
                result.details["error"] = "ADB dump returned empty XML"
                return result

            current_page = self.page_matcher.identify_current_page(current_xml)
            logger.info(f"当前页面: {current_page.page_id}")

            home_xml_path = self.apks_dir / "home.xml"
            if home_xml_path.exists():
                logger.info(f"执行首页流程: {home_xml_path}")
                flow_result = self.flow_coordinator.execute_page_flow(
                    page_id="home",
                )
                logger.info(f"首页流程执行结果: {'成功' if flow_result.success else '失败'}")

                if flow_result.success:
                    time.sleep(2)
                    new_xml = self.dump_page_source()
                    if not new_xml:
                        result.success = False
                        result.message = "无法获取导航后页面 XML"
                        result.details["error"] = "ADB dump returned empty XML after navigation"
                        return result

                    new_page = self.page_matcher.identify_current_page(new_xml)
                    logger.info(f"导航后页面: {new_page.page_id}")

                    if new_page.page_id in ["entry-xzsg", "netgrid-trading"]:
                        result.success = True
                        result.message = f"成功导航到 {new_page.page_id} 页面"
                        result.details = {
                            "start_page": current_page.page_id,
                            "end_page": new_page.page_id,
                            "navigation_success": True,
                        }

                        if new_page.page_id == "entry-xzsg":
                            entry_xml_path = self.apks_dir / "entry-xzsg.xml"
                            if entry_xml_path.exists():
                                logger.info(f"执行条件委托页面流程: {entry_xml_path}")
                                entry_result = self.flow_coordinator.execute_page_flow(
                                    page_id="entry-xzsg",
                                )
                                logger.info(f"条件委托页面流程执行结果: {'成功' if entry_result.success else '失败'}")

                                if entry_result.success:
                                    time.sleep(2)
                                    final_xml = self.dump_page_source()
                                    if final_xml:
                                        final_page = self.page_matcher.identify_current_page(final_xml)
                                        logger.info(f"最终页面: {final_page.page_id}")
                                        result.details["final_page"] = final_page.page_id
                    else:
                        result.success = False
                        result.message = f"导航后页面不是预期页面，实际: {new_page.page_id}"
                        result.details = {
                            "start_page": current_page.page_id,
                            "actual_page": new_page.page_id,
                            "expected_pages": ["entry-xzsg", "netgrid-trading"],
                        }
                else:
                    result.success = False
                    result.message = f"首页流程执行失败: {flow_result.message}"
                    result.details = {
                        "start_page": current_page.page_id,
                        "flow_error": flow_result.message,
                    }
            else:
                result.success = False
                result.message = f"首页 XML 文件不存在: {home_xml_path}"
                result.details["xml_path"] = str(home_xml_path)

        except Exception as e:
            result.success = False
            result.message = f"导航测试失败: {str(e)}"
            result.details["error"] = str(e)

        result.execution_time = time.time() - start_time
        return result

    def run_full_verification(self) -> dict:
        logger.info("=" * 60)
        logger.info("开始执行明佣宝自动化测试完整验证")
        logger.info("=" * 60)

        results = {
            "test_summary": {
                "device_id": self.device_id,
                "app_package": self.package_name,
                "test_start_time": time.strftime("%Y-%m-%d %H:%M:%S"),
            },
            "tests": [],
            "overall_success": False,
        }

        if not self.initialize():
            results["error"] = "测试环境初始化失败"
            return results

        cold_start_result = self.cold_start_app()
        results["tests"].append(cold_start_result.to_dict())
        logger.info(f"冷启动测试: {cold_start_result.message}")

        if cold_start_result.success:
            navigation_result = self.navigate_to_grid_trading()
            results["tests"].append(navigation_result.to_dict())
            logger.info(f"导航测试: {navigation_result.message}")

            results["overall_success"] = (
                cold_start_result.success and navigation_result.success
            )
        else:
            results["overall_success"] = False
            logger.warning("冷启动测试失败，跳过导航测试")

        results["test_summary"]["test_end_time"] = time.strftime("%Y-%m-%d %H:%M:%S")

        total_time = sum(
            float(t["execution_time"].rstrip('s')) for t in results["tests"]
        )
        results["test_summary"]["total_execution_time"] = f"{total_time:.2f}s"

        logger.info("=" * 60)
        logger.info(f"验证完成，整体结果: {'成功' if results['overall_success'] else '失败'}")
        logger.info(f"总执行时间: {results['test_summary']['total_execution_time']}")
        logger.info("=" * 60)

        return results


def success_str(success: bool) -> str:
    return "成功" if success else "失败"


if __name__ == "__main__":
    verifier = TradeScriptVerifier()
    results = verifier.run_full_verification()

    print("\n" + "=" * 60)
    print("验证结果摘要:")
    print(f"整体成功: {'是' if results['overall_success'] else '否'}")
    for test in results["tests"]:
        print(f"  - {test['test_name']}: {'通过' if test['success'] else '失败'}")
        print(f"    {test['message']}")
    print("=" * 60)

    with open("verification_results.json", "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    print(f"\n详细结果已保存到 verification_results.json")
