import time
import logging
from typing import Dict, List, Optional, Any
from dataclasses import dataclass
from enum import Enum
from pathlib import Path

from .page_matcher import PageMatcher, MatchResult
from .data_executor import DataDrivenExecutor, ExecutionResult

logger = logging.getLogger(__name__)


class FlowStatus(str, Enum):
    PENDING = "PENDING"
    RUNNING = "RUNNING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    SKIPPED = "SKIPPED"


@dataclass
class FlowStepResult:
    page_id: str
    status: FlowStatus
    execution_results: List[ExecutionResult]
    matched: bool
    next_page_id: Optional[str] = None
    error_message: Optional[str] = None


class TradeflowCoordinator:
    def __init__(
        self,
        page_matcher: PageMatcher,
        data_executor: DataDrivenExecutor,
        max_navigation_attempts: int = 3,
    ):
        self.page_matcher = page_matcher
        self.data_executor = data_executor
        self.max_navigation_attempts = max_navigation_attempts
        self.current_page_id: Optional[str] = None
        self.flow_history: List[FlowStepResult] = []
        self.execution_stats: Dict[str, int] = {
            "total_steps": 0,
            "completed_steps": 0,
            "failed_steps": 0,
            "skipped_steps": 0,
        }

    def execute_flow(
        self,
        start_page_id: Optional[str] = None,
        target_pages: Optional[List[str]] = None,
        test_data: Optional[Dict[str, Any]] = None,
    ) -> List[FlowStepResult]:
        logger.info(f"Starting flow execution")
        logger.info(f"Start page: {start_page_id}")
        logger.info(f"Target pages: {target_pages}")
        logger.info(f"Test data keys: {list(test_data.keys()) if test_data else []}")

        self.flow_history.clear()
        self.execution_stats = {
            "total_steps": 0,
            "completed_steps": 0,
            "failed_steps": 0,
            "skipped_steps": 0,
        }

        if test_data:
            self.data_executor.set_test_data(test_data)

        if start_page_id:
            self.current_page_id = start_page_id
        else:
            self.current_page_id = self._detect_start_page()

        iteration_count = 0
        max_iterations = 100

        while iteration_count < max_iterations:
            iteration_count += 1

            if target_pages and self.current_page_id in target_pages:
                logger.info(f"Reached target page: {self.current_page_id}")
                break

            if not self.current_page_id:
                logger.warning("Could not identify current page, attempting navigation")
                navigation_success = self._attempt_global_navigation(start_page_id)
                if not navigation_success:
                    logger.error("Failed to navigate to start page")
                    break

                time.sleep(2)
                self.current_page_id = self._detect_start_page()
                if not self.current_page_id:
                    logger.error("Still cannot identify page after navigation attempt")
                    break

            step_result = self._execute_current_page_step()

            if step_result:
                self.flow_history.append(step_result)

                if step_result.status == FlowStatus.COMPLETED:
                    self.execution_stats["completed_steps"] += 1
                elif step_result.status == FlowStatus.FAILED:
                    self.execution_stats["failed_steps"] += 1
                elif step_result.status == FlowStatus.SKIPPED:
                    self.execution_stats["skipped_steps"] += 1

                if step_result.next_page_id:
                    logger.info(
                        f"Transitioning from {self.current_page_id} to {step_result.next_page_id}"
                    )
                    self.current_page_id = step_result.next_page_id
                    time.sleep(1)
                else:
                    break
            else:
                break

        logger.info(f"Flow execution completed after {iteration_count} iterations")
        logger.info(f"Execution stats: {self.execution_stats}")

        return self.flow_history

    def _detect_start_page(self) -> Optional[str]:
        try:
            page_source = self.data_executor.driver.page_source
        except Exception as e:
            logger.error(f"Failed to get page source: {e}")
            return None

        match_result = self.page_matcher.identify_current_page(page_source)

        if match_result.page_id:
            return match_result.page_id

        return None

    def _execute_current_page_step(self) -> Optional[FlowStepResult]:
        if not self.current_page_id:
            return None

        page_path = self.page_matcher.get_page_path(self.current_page_id)
        if not page_path:
            logger.warning(f"No XML definition found for page: {self.current_page_id}")
            return FlowStepResult(
                page_id=self.current_page_id,
                status=FlowStatus.SKIPPED,
                execution_results=[],
                matched=False,
                error_message=f"No XML definition found",
            )

        try:
            page_source = self.data_executor.driver.page_source
            match_result = self.page_matcher.identify_current_page(page_source)

            if match_result.page_id != self.current_page_id:
                if match_result.page_id:
                    logger.info(
                        f"Page mismatch: expected {self.current_page_id}, "
                        f"detected {match_result.page_id}"
                    )
                    return FlowStepResult(
                        page_id=self.current_page_id,
                        status=FlowStatus.SKIPPED,
                        execution_results=[],
                        matched=False,
                        next_page_id=match_result.page_id,
                        error_message=f"Page mismatch, detected {match_result.page_id}",
                    )
                else:
                    logger.warning(
                        f"Current page does not match {self.current_page_id}"
                    )
                    return FlowStepResult(
                        page_id=self.current_page_id,
                        status=FlowStatus.SKIPPED,
                        execution_results=[],
                        matched=False,
                        error_message="Page does not match expected",
                    )

        except Exception as e:
            logger.error(f"Failed to identify page: {e}")

        self.execution_stats["total_steps"] += 1

        execution_results = self.data_executor.execute_page_flow(self.current_page_id)

        all_success = all(result.success for result in execution_results)

        if all_success:
            next_page_id = self._predict_next_page(self.current_page_id)
            return FlowStepResult(
                page_id=self.current_page_id,
                status=FlowStatus.COMPLETED,
                execution_results=execution_results,
                matched=True,
                next_page_id=next_page_id,
            )
        else:
            failed_count = sum(
                1 for r in execution_results if not r.success
            )
            return FlowStepResult(
                page_id=self.current_page_id,
                status=FlowStatus.FAILED,
                execution_results=execution_results,
                matched=True,
                error_message=f"{failed_count} steps failed",
            )

    def execute_page_flow(
        self,
        page_id: str,
        driver=None,
    ) -> List[ExecutionResult]:
        logger.info(f"Executing page flow for: {page_id}")
        return self.data_executor.execute_page_flow(page_id, driver)

    def _predict_next_page(self, current_page_id: str) -> Optional[str]:
        page_transitions = {
            "home": "entry-xzsg",
            "entry-xzsg": "netgrid-trading",
            "netgrid-trading": "stock-search",
            "stock-search": "stock-search-result",
            "stock-search-result": "netgrid-trading-with-stock",
            "netgrid-trading-with-stock": "netgrid-trading-confirm",
            "netgrid-trading-confirm": "netgrid-trading-success",
            "netgrid-trading-success": "my-condition-orders",
        }

        return page_transitions.get(current_page_id)

    def _attempt_global_navigation(self, target_page_id: Optional[str]) -> bool:
        logger.info(f"Attempting global navigation to: {target_page_id}")

        for attempt in range(self.max_navigation_attempts):
            try:
                self.data_executor.driver.press_keycode(4)
                time.sleep(1)

                current_source = self.data_executor.driver.page_source
                match_result = self.page_matcher.identify_current_page(current_source)

                if match_result.page_id:
                    self.current_page_id = match_result.page_id
                    if target_page_id and match_result.page_id == target_page_id:
                        return True
                    elif not target_page_id:
                        return True

            except Exception as e:
                logger.warning(f"Navigation attempt {attempt + 1} failed: {e}")
                continue

        return False

    def get_flow_summary(self) -> Dict[str, Any]:
        return {
            "total_iterations": len(self.flow_history),
            "current_page": self.current_page_id,
            "stats": self.execution_stats,
            "flow_history": [
                {
                    "page_id": step.page_id,
                    "status": step.status.value,
                    "matched": step.matched,
                    "next_page": step.next_page_id,
                    "error": step.error_message,
                }
                for step in self.flow_history
            ],
        }

    def run_to_target(
        self,
        target_page_id: str,
        test_data: Optional[Dict[str, Any]] = None,
        max_pages: int = 20,
    ) -> bool:
        logger.info(f"Running to target page: {target_page_id}")

        self.flow_history.clear()
        if test_data:
            self.data_executor.set_test_data(test_data)

        pages_visited = 0

        while pages_visited < max_pages:
            pages_visited += 1

            current_source = self.data_executor.driver.page_source
            match_result = self.page_matcher.identify_current_page(current_source)

            if match_result.page_id == target_page_id:
                logger.info(f"Reached target page: {target_page_id}")
                return True

            if not match_result.page_id:
                logger.warning("Could not identify current page")
                return False

            step_result = self._execute_current_page_step()
            if step_result:
                self.flow_history.append(step_result)

                if step_result.next_page_id:
                    self.current_page_id = step_result.next_page_id
                    time.sleep(1)
                else:
                    break
            else:
                break

        logger.warning(f"Failed to reach target page: {target_page_id}")
        return False
