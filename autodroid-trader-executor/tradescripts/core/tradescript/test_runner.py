import sys
import time
import logging
from pathlib import Path
from typing import List, Dict, Any, Optional

from .adb_driver import ADBManager
from .page_matcher import PageMatcher
from .data_executor import DataDrivenExecutor
from .flow_coordinator import TradeflowCoordinator
from .test_data import (
    TestCase,
    TestScenario,
    DeviceConfig,
    AppConfig,
    get_test_case_by_scenario,
    ALL_TEST_CASES,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


class TestResult:
    def __init__(
        self,
        test_case: TestCase,
        success: bool = False,
        message: str = "",
        execution_time: float = 0.0,
        details: Optional[Dict[str, Any]] = None,
    ):
        self.test_case = test_case
        self.success = success
        self.message = message
        self.execution_time = execution_time
        self.details = details or {}
        self.timestamp = time.strftime("%Y-%m-%d %H:%M:%S")

    def to_dict(self) -> Dict[str, Any]:
        return {
            "scenario": self.test_case.scenario.value,
            "description": self.test_case.description,
            "success": self.success,
            "message": self.message,
            "execution_time": f"{self.execution_time:.2f}s",
            "timestamp": self.timestamp,
            "details": self.details,
        }


class TradeScriptTestRunner:
    def __init__(
        self,
        apks_dir: str = "app/src/main/assets/apks",
        output_dir: Optional[str] = None,
    ):
        self.apks_dir = Path(apks_dir)
        self.output_dir = Path(output_dir) if output_dir else Path("test_output")
        
        self.adb_manager = ADBManager()
        self.page_matcher: Optional[PageMatcher] = None
        self.data_executor: Optional[DataDrivenExecutor] = None
        self.flow_coordinator: Optional[TradeflowCoordinator] = None
        
        self.test_results: List[TestResult] = []

    def initialize(self, device_id: str) -> bool:
        logger.info(f"Initializing test runner for device: {device_id}")
        
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        try:
            self.page_matcher = PageMatcher(
                apks_dir=str(self.apks_dir),
                match_threshold=0.7,
            )
            
            self.data_executor = DataDrivenExecutor(
                adb_manager=self.adb_manager,
                device_id=device_id,
                apks_dir=str(self.apks_dir),
                wait_timeout=30,
            )
            
            self.flow_coordinator = TradeflowCoordinator(
                data_executor=self.data_executor,
                page_matcher=self.page_matcher,
                adb_manager=self.adb_manager,
                device_id=device_id,
                max_iterations=100,
            )
            
            logger.info("Test runner initialized successfully")
            return True
            
        except Exception as e:
            logger.error(f"Failed to initialize test runner: {e}")
            return False

    def dump_page_source(self, device_id: str) -> str:
        result = self.adb_manager.run_command([
            "-s", device_id, "shell", "uiautomator", "dump", "/sdcard/window_dump.xml"
        ])
        if result.returncode != 0:
            logger.error(f"Failed to dump page: {result.stderr}")
            return ""
        
        result = self.adb_manager.run_command([
            "-s", device_id, "shell", "cat", "/sdcard/window_dump.xml"
        ])
        if result.returncode == 0:
            return result.stdout.strip()
        return ""

    def run_test_case(self, test_case: TestCase) -> TestResult:
        logger.info(f"Running test case: {test_case.scenario.value}")
        start_time = time.time()
        
        result = TestResult(
            test_case=test_case,
            success=False,
            message="",
            execution_time=0.0,
        )
        
        try:
            device_id = test_case.device_config.device_id
            
            if test_case.scenario == TestScenario.COLD_START_APP:
                result = self._run_cold_start_test(test_case)
            elif test_case.scenario == TestScenario.NAVIGATE_TO_GRID:
                result = self._run_navigation_test(test_case)
            elif test_case.scenario == TestScenario.FULL_FLOW:
                result = self._run_full_flow_test(test_case)
            else:
                result.message = f"Unknown test scenario: {test_case.scenario}"
                
        except Exception as e:
            logger.error(f"Test execution failed: {e}")
            result.message = f"Test failed with error: {str(e)}"
            result.details["error"] = str(e)
            
        result.execution_time = time.time() - start_time
        self.test_results.append(result)
        
        return result

    def _run_cold_start_test(self, test_case: TestCase) -> TestResult:
        device_id = test_case.device_config.device_id
        app_config = test_case.app_config
        test_data = test_case.test_data
        
        logger.info(f"Cold starting app: {app_config.package_name}")
        
        success = self.adb_manager.cold_start_app(
            device_id=device_id,
            app_package=app_config.package_name,
            app_activity=app_config.main_activity,
            clear_data=app_config.clear_data_on_start,
            wait_timeout=app_config.launch_timeout,
        )
        
        if not success:
            return TestResult(
                test_case=test_case,
                success=False,
                message=f"Failed to cold start app: {app_config.package_name}",
                details={"app_package": app_config.package_name},
            )
        
        time.sleep(test_data.get("idle_timeout", 3.0))
        
        try:
            xml_source = self.dump_page_source(device_id)
            
            if not xml_source:
                return TestResult(
                    test_case=test_case,
                    success=False,
                    message="Failed to get page XML via ADB",
                    details={"device_id": device_id},
                )
            
            if test_data.get("capture_xml"):
                xml_path = self.output_dir / "cold_start_home.xml"
                with open(xml_path, "w", encoding="utf-8") as f:
                    f.write(xml_source)
                logger.info(f"Saved XML to: {xml_path}")
            
            page_match = self.page_matcher.identify_current_page(xml_source)
            logger.info(f"Identified page: {page_match.page_id} (score: {page_match.score:.2f})")
            
            return TestResult(
                test_case=test_case,
                success=True,
                message="App started and home page loaded successfully",
                details={
                    "app_package": app_config.package_name,
                    "current_page": page_match.page_id,
                    "page_score": page_match.score,
                },
            )
                
        except Exception as e:
            return TestResult(
                test_case=test_case,
                success=False,
                message=f"Cold start verification failed: {str(e)}",
                details={"error": str(e)},
            )

    def _run_navigation_test(self, test_case: TestCase) -> TestResult:
        device_id = test_case.device_config.device_id
        app_config = test_case.app_config
        steps = test_case.navigation_steps
        
        logger.info(f"Running navigation test with {len(steps)} steps")
        
        results = []
        current_step = 0
        
        for step in steps:
            current_step += 1
            step_result = {
                "step_id": step.step_id,
                "page_id": step.page_id,
                "action": step.action_type,
                "target": step.target_element,
                "success": False,
                "message": "",
            }
            
            logger.info(f"Executing step {current_step}: {step.description}")
            
            try:
                if step.action_type == "click":
                    success = self.data_executor.handle_click(
                        step.target_element, step.timeout
                    )
                    if success:
                        step_result["success"] = True
                        step_result["message"] = "Click successful"
                        
                        time.sleep(step.wait_after)
                        
                        if step.expected_page_after:
                            xml_source = self.dump_page_source(device_id)
                            if xml_source:
                                page_match = self.page_matcher.identify_current_page(xml_source)
                                
                                if page_match.page_id == step.expected_page_after:
                                    step_result["expected_page_reached"] = True
                                    step_result["current_page"] = page_match.page_id
                                else:
                                    step_result["expected_page_reached"] = False
                                    step_result["current_page"] = page_match.page_id
                                    step_result["message"] = (
                                        f"Expected page {step.expected_page_after}, "
                                        f"got {page_match.page_id}"
                                    )
                    else:
                        step_result["success"] = False
                        step_result["message"] = "Click failed - element not found"
                        
                elif step.action_type == "input":
                    input_text = step.input_value
                    success = self.data_executor.handle_input(
                        step.target_element, input_text, step.timeout
                    )
                    if success:
                        step_result["success"] = True
                        step_result["message"] = f"Input '{input_text}' successful"
                    else:
                        step_result["success"] = False
                        step_result["message"] = "Input failed - element not found"
                        
                elif step.action_type == "wait":
                    wait_time = step.wait_time or 2.0
                    time.sleep(wait_time)
                    step_result["success"] = True
                    step_result["message"] = f"Waited {wait_time}s"
                    
                else:
                    step_result["success"] = False
                    step_result["message"] = f"Unknown action type: {step.action_type}"
                    
            except Exception as e:
                step_result["success"] = False
                step_result["message"] = f"Action failed: {str(e)}"
                
            results.append(step_result)
            
        all_success = all(r["success"] for r in results)
        
        return TestResult(
            test_case=test_case,
            success=all_success,
            message=f"Navigation test completed: {sum(1 for r in results if r['success'])}/{len(results)} steps successful",
            details={"steps": results},
        )

    def _run_full_flow_test(self, test_case: TestCase) -> TestResult:
        device_id = test_case.device_config.device_id
        
        logger.info(f"Running full flow test")
        
        try:
            cold_start_result = self._run_cold_start_test(test_case)
            
            if not cold_start_result.success:
                return cold_start_result
            
            if test_case.navigation_steps:
                navigation_result = self._run_navigation_test(test_case)
                
                return TestResult(
                    test_case=test_case,
                    success=cold_start_result.success and navigation_result.success,
                    message=f"Full flow test completed. Cold start: {cold_start_result.message}. Navigation: {navigation_result.message}",
                    details={
                        "cold_start": cold_start_result.to_dict(),
                        "navigation": navigation_result.to_dict(),
                    },
                )
            else:
                return cold_start_result
                
        except Exception as e:
            return TestResult(
                test_case=test_case,
                success=False,
                message=f"Full flow test failed: {str(e)}",
                details={"error": str(e)},
            )

    def run_all_tests(self) -> Dict[str, TestResult]:
        logger.info(f"Running all {len(ALL_TEST_CASES)} test cases")
        
        results = {}
        for test_case in ALL_TEST_CASES:
            logger.info(f"\n{'='*60}")
            logger.info(f"Running test: {test_case.description}")
            logger.info(f"{'='*60}")
            
            result = self.run_test_case(test_case)
            results[test_case.scenario.value] = result
            
            logger.info(f"Result: {'PASS' if result.success else 'FAIL'}")
            logger.info(f"Message: {result.message}")
            
        passed = sum(1 for r in results.values() if r.success)
        logger.info(f"\n{'='*60}")
        logger.info(f"Test suite completed: {passed}/{len(results)} tests passed")
        logger.info(f"{'='*60}")
        
        return results

    def generate_report(self, output_path: str = "test_report.json") -> None:
        report = {
            "test_run_timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
            "total_tests": len(self.test_results),
            "passed": sum(1 for r in self.test_results if r.success),
            "failed": sum(1 for r in self.test_results if not r.success),
            "results": [r.to_dict() for r in self.test_results],
        }
        
        with open(output_path, "w", encoding="utf-8") as f:
            import json
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        logger.info(f"Test report generated: {output_path}")


if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="TradeScript Test Runner")
    parser.add_argument("--apks-dir", default="app/src/main/assets/apks", help="APKs directory path")
    parser.add_argument("--output-dir", default="test_output", help="Output directory for reports")
    parser.add_argument("--device-id", required=True, help="Device ID for ADB connection")
    parser.add_argument("--scenario", choices=[s.value for s in TestScenario], help="Specific scenario to run")
    parser.add_argument("--report", default="test_report.json", help="Output report file path")
    
    args = parser.parse_args()
    
    runner = TradeScriptTestRunner(
        apks_dir=args.apks_dir,
        output_dir=args.output_dir,
    )
    
    if not runner.initialize(args.device_id):
        logger.error("Failed to initialize test runner")
        sys.exit(1)
    
    if args.scenario:
        test_case = get_test_case_by_scenario(TestScenario(args.scenario))
        if test_case:
            result = runner.run_test_case(test_case)
            print(f"\nTest Result: {'PASS' if result.success else 'FAIL'}")
            print(f"Message: {result.message}")
        else:
            logger.error(f"Test case not found for scenario: {args.scenario}")
    else:
        results = runner.run_all_tests()
        runner.generate_report(args.report)
        
        print(f"\n{'='*60}")
        print("Test Results Summary:")
        print(f"{'='*60}")
        for scenario, result in results.items():
            print(f"  {scenario}: {'PASS' if result.success else 'FAIL'}")
        print(f"{'='*60}")
