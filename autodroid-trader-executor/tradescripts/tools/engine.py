from pathlib import Path
from pydantic import BaseModel
from typing import  List, Optional
from .element import ElementInfo
from .page import PageInfo


class PageStepInfo(BaseModel):
    step: int
    action: str
    element: ElementInfo
    name: Optional[str] = None
    desc: Optional[str] = None
    value: Optional[str] = None
    save_to: Optional[str] = None
    status: Optional[str] = None  # 状态: "pending", "executing", "completed", "failed"


class FlowStepInfo(BaseModel):
    step: int
    action: str
    name: Optional[str] = None
    desc: Optional[str] = None
    page: PageInfo # 页面信息
    steps: List[PageStepInfo] = []  # FlowStepInfo 包含多个 PageStepInfo
    status: Optional[str] = None  # 状态: "pending", "executing", "completed", "failed"


class FlowInfo(BaseModel):
    apk_package: str
    flow_name: str
    flow_dir: Path
    steps: List[FlowStepInfo] = []
    status: Optional[str] = None  # 状态: "pending", "executing", "completed", "failed"

from .page import PageMatcher, PageExecutor, PageInfo
from .element import ElementExecutor
from .u2device import U2Device
from enum import Enum
from typing import Dict, Optional, Callable, Tuple


class ExecutionStatus(str, Enum):
    PENDING = "pending"
    EXECUTING = "executing"
    COMPLETED = "completed"
    FAILED = "failed"


class FlowManager:
    def __init__(self, flow_info: FlowInfo, device: Optional[U2Device] = None):
        self.flow_info = flow_info
        self.device = device
        self._page_matcher = PageMatcher()
        self._page_executor = PageExecutor(self._page_matcher)
        self._element_executor = ElementExecutor(device, self._page_matcher) if device else None
        
        # 状态回调
        self._status_callback: Optional[Callable[[str, str, str], None]] = None

    def set_status_callback(self, callback: Callable[[str, str, str], None]):
        """设置状态变化回调函数 (element_id, old_status, new_status)"""
        self._status_callback = callback

    def _update_status(self, obj, new_status: str):
        """更新对象状态并触发回调"""
        old_status = obj.status
        obj.status = new_status
        if self._status_callback and old_status != new_status:
            # 根据对象类型获取标识符
            if hasattr(obj, 'step'):
                obj_id = f"step_{obj.step}"
            elif hasattr(obj, 'page_id'):
                obj_id = obj.page_id
            else:
                obj_id = "flow"
            self._status_callback(obj_id, old_status or "unknown", new_status)

    def execute_flow(self) -> bool:
        """执行整个流程"""
        if not self.flow_info or not self.flow_info.steps:
            return False

        self._update_status(self.flow_info, ExecutionStatus.EXECUTING)
        
        try:
            for flow_step in self.flow_info.steps:
                self._update_status(flow_step, ExecutionStatus.EXECUTING)
                
                # 如果有页面信息，更新页面状态
                if flow_step.page:
                    self._update_status(flow_step.page, ExecutionStatus.EXECUTING)
                
                # 执行FlowStep中的步骤
                step_success = self._execute_flow_step(flow_step)
                
                if step_success:
                    # 如果有页面信息，更新页面完成状态
                    if flow_step.page:
                        self._update_status(flow_step.page, ExecutionStatus.COMPLETED)
                    self._update_status(flow_step, ExecutionStatus.COMPLETED)
                else:
                    self._update_status(page_info, ExecutionStatus.FAILED)
                    self._update_status(flow_step, ExecutionStatus.FAILED)
                    self._update_status(self.flow_info, ExecutionStatus.FAILED)
                    return False
            
            # 检查是否所有步骤都已完成
            all_completed = all(step.status == ExecutionStatus.COMPLETED for step in self.flow_info.steps)
            if all_completed:
                self._update_status(self.flow_info, ExecutionStatus.COMPLETED)
                return True
            else:
                self._update_status(self.flow_info, ExecutionStatus.FAILED)
                return False
                
        except Exception as e:
            self._update_status(self.flow_info, ExecutionStatus.FAILED)
            print(f"执行流程时发生错误: {e}")
            return False

    def _execute_flow_step(self, flow_step: 'FlowStepInfo') -> bool:
        """执行FlowStep中的所有步骤"""
        if not flow_step.steps:
            return True

        for page_step_info in flow_step.steps:
            if page_step_info.status == ExecutionStatus.COMPLETED:
                continue  # 跳过已完成的步骤
                
            self._update_status(page_step_info, ExecutionStatus.EXECUTING)
            
            # 使用工具类执行步骤
            if self._element_executor:
                success = self._execute_single_step(page_step_info)
                if success:
                    self._update_status(page_step_info, ExecutionStatus.COMPLETED)
                else:
                    self._update_status(page_step_info, ExecutionStatus.FAILED)
                    return False
        
        return True

    def _execute_single_step(self, step_info: PageStepInfo) -> bool:
        """执行单个步骤"""
        if not self._element_executor:
            return False
            
        # 使用ElementExecutor执行具体操作
        from .element import StepInfo
        step_obj = StepInfo(
            step=1, 
            action=step_info.action, 
            element=step_info.element, 
            name=step_info.name,
            value=step_info.value,
            save_to=step_info.save_to,
            desc=step_info.desc
        )
        success = self._element_executor.execute_action(step_obj, None)
        return success

    def get_execution_status(self) -> Dict:
        """获取整个执行流程的状态摘要"""
        total_steps = len(self.flow_info.steps) if self.flow_info.steps else 0
        completed_steps = len([s for s in self.flow_info.steps if s.status == ExecutionStatus.COMPLETED]) if self.flow_info.steps else 0
        
        # 计算所有子步骤的总数和完成数
        total_sub_steps = 0
        completed_sub_steps = 0
        for flow_step in self.flow_info.steps:
            for step in flow_step.steps:
                total_sub_steps += 1
                if step.status == ExecutionStatus.COMPLETED:
                    completed_sub_steps += 1
        
        return {
            "flow_status": self.flow_info.status,
            "total_steps": total_steps,
            "completed_steps": completed_steps,
            "progress": f"{completed_steps}/{total_steps}" if total_steps > 0 else "0/0",
            "total_sub_steps": total_sub_steps,
            "completed_sub_steps": completed_sub_steps,
            "sub_progress": f"{completed_sub_steps}/{total_sub_steps}" if total_sub_steps > 0 else "0/0"
        }

    def reset_flow(self):
        """重置整个流程的状态为待执行"""
        self._update_status(self.flow_info, ExecutionStatus.PENDING)
        
        for flow_step in self.flow_info.steps:
            self._update_status(flow_step, ExecutionStatus.PENDING)
            
            # 重置FlowStep中的步骤状态
            for step_info in flow_step.steps:
                self._update_status(step_info, ExecutionStatus.PENDING)
            
            # 如果有页面信息，也重置页面信息中的步骤状态
            if flow_step.page:
                self._update_status(flow_step.page, ExecutionStatus.PENDING)
                for page_step_info in flow_step.page.steps:
                    self._update_status(page_step_info, ExecutionStatus.PENDING)


