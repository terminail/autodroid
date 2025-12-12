"""人工协助处理器，处理需要人工干预的场景"""

import time
import threading
from typing import Dict, List, Optional, Callable, Any
from dataclasses import dataclass
from pathlib import Path

from .app_analyzer import HumanAssistanceRequest


@dataclass
class AssistanceSession:
    """协助会话"""
    request: HumanAssistanceRequest
    start_time: float
    status: str  # pending, in_progress, completed, timeout, cancelled
    result: Optional[Dict[str, Any]] = None


class HumanAssistant:
    """人工协助处理器"""
    
    def __init__(self, interactive_mode: bool = True):
        self.interactive_mode = interactive_mode
        self.active_sessions: Dict[str, AssistanceSession] = {}
        self.session_timeout = 300  # 默认超时时间（秒）
        
        # 回调函数
        self.on_assistance_started: Optional[Callable] = None
        self.on_assistance_completed: Optional[Callable] = None
        self.on_assistance_timeout: Optional[Callable] = None
        
        # 监控线程
        self.monitor_thread = None
        self.monitor_running = False
    
    def start_monitoring(self):
        """开始监控协助会话"""
        if self.monitor_running:
            return
        
        self.monitor_running = True
        self.monitor_thread = threading.Thread(target=self._monitor_sessions, daemon=True)
        self.monitor_thread.start()
        
        print("🔍 人工协助监控已启动")
    
    def stop_monitoring(self):
        """停止监控协助会话"""
        self.monitor_running = False
        if self.monitor_thread:
            self.monitor_thread.join(timeout=5)
        
        print("🔍 人工协助监控已停止")
    
    def process_assistance_request(self, request: HumanAssistanceRequest) -> bool:
        """处理人工协助请求"""
        session_id = self._generate_session_id()
        
        session = AssistanceSession(
            request=request,
            start_time=time.time(),
            status="pending"
        )
        
        self.active_sessions[session_id] = session
        
        print(f"🆘 创建协助会话: {session_id}")
        print(f"   原因: {request.reason}")
        print(f"   期望操作: {request.expected_action}")
        print(f"   超时时间: {request.timeout}秒")
        
        # 通知协助开始
        if self.on_assistance_started:
            self.on_assistance_started(session_id, request)
        
        # 交互模式：等待用户输入
        if self.interactive_mode:
            return self._handle_interactive_request(session_id, session)
        else:
            # 非交互模式：自动处理或记录请求
            return self._handle_non_interactive_request(session_id, session)
    
    def complete_assistance(self, session_id: str, result: Optional[Dict[str, Any]] = None) -> bool:
        """标记协助完成"""
        if session_id not in self.active_sessions:
            print(f"❌ 协助会话不存在: {session_id}")
            return False
        
        session = self.active_sessions[session_id]
        session.status = "completed"
        session.result = result
        
        print(f"✅ 协助会话完成: {session_id}")
        
        # 通知协助完成
        if self.on_assistance_completed:
            self.on_assistance_completed(session_id, session.request, result)
        
        # 清理会话
        self._cleanup_session(session_id)
        
        return True
    
    def cancel_assistance(self, session_id: str) -> bool:
        """取消协助会话"""
        if session_id not in self.active_sessions:
            return False
        
        session = self.active_sessions[session_id]
        session.status = "cancelled"
        
        print(f"❌ 协助会话取消: {session_id}")
        
        # 清理会话
        self._cleanup_session(session_id)
        
        return True
    
    def get_pending_requests(self) -> List[Dict[str, Any]]:
        """获取待处理的协助请求"""
        pending_requests = []
        
        for session_id, session in self.active_sessions.items():
            if session.status in ["pending", "in_progress"]:
                pending_requests.append({
                    "session_id": session_id,
                    "request": session.request,
                    "start_time": session.start_time,
                    "elapsed_time": time.time() - session.start_time
                })
        
        return pending_requests
    
    def _handle_interactive_request(self, session_id: str, session: AssistanceSession) -> bool:
        """处理交互式协助请求"""
        session.status = "in_progress"
        
        print("\n" + "="*60)
        print("🆘 人工协助请求")
        print("="*60)
        print(f"原因: {session.request.reason}")
        print(f"期望操作: {session.request.expected_action}")
        
        if session.request.required_data:
            print("所需数据:")
            for key, value in session.request.required_data.items():
                print(f"  {key}: {value}")
        
        print(f"\n请在 {session.request.timeout} 秒内完成操作")
        print("="*60)
        
        # 显示操作指南
        self._display_operation_guide(session.request)
        
        # 等待用户确认
        try:
            input("\n按回车键继续，当操作完成后...")
            
            # 标记完成
            self.complete_assistance(session_id, {"completed_by": "user", "timestamp": time.time()})
            return True
            
        except KeyboardInterrupt:
            print("\n❌ 协助被用户中断")
            self.cancel_assistance(session_id)
            return False
    
    def _handle_non_interactive_request(self, session_id: str, session: AssistanceSession) -> bool:
        """处理非交互式协助请求"""
        print(f"📝 记录协助请求（非交互模式）: {session.request.reason}")
        
        # 记录到日志文件
        self._log_assistance_request(session_id, session)
        
        # 自动标记为完成（假设人工会处理）
        time.sleep(2)  # 模拟等待时间
        self.complete_assistance(session_id, {"auto_completed": True, "timestamp": time.time()})
        
        return True
    
    def _display_operation_guide(self, request: HumanAssistanceRequest):
        """显示操作指南"""
        reason = request.reason.lower()
        
        if "指纹" in reason or "fingerprint" in reason:
            print("\n📋 指纹登录操作指南:")
            print("1. 将手指放在指纹传感器上")
            print("2. 等待指纹识别完成")
            print("3. 确认登录成功")
            print("4. 返回应用主界面")
        
        elif "人脸" in reason or "face" in reason:
            print("\n📋 人脸识别操作指南:")
            print("1. 将面部对准摄像头")
            print("2. 保持面部在识别区域内")
            print("3. 等待识别完成")
            print("4. 确认登录成功")
        
        elif "验证码" in reason or "captcha" in reason:
            print("\n📋 验证码输入操作指南:")
            print("1. 查看验证码图片或短信")
            print("2. 在输入框中输入验证码")
            print("3. 点击确认或下一步")
            print("4. 等待验证通过")
        
        elif "扫码" in reason or "qr" in reason:
            print("\n📋 扫码操作指南:")
            print("1. 打开扫码功能")
            print("2. 对准二维码")
            print("3. 等待扫描完成")
            print("4. 确认扫码结果")
        
        else:
            print("\n📋 通用操作指南:")
            print("1. 按照屏幕提示完成操作")
            print("2. 确保操作成功完成")
            print("3. 返回应用正常界面")
    
    def _log_assistance_request(self, session_id: str, session: AssistanceSession):
        """记录协助请求到日志文件"""
        log_dir = Path("assistance_logs")
        log_dir.mkdir(exist_ok=True)
        
        log_file = log_dir / f"assistance_{int(time.time())}.log"
        
        log_data = {
            "session_id": session_id,
            "timestamp": time.time(),
            "request": {
                "reason": session.request.reason,
                "expected_action": session.request.expected_action,
                "required_data": session.request.required_data,
                "timeout": session.request.timeout
            },
            "status": session.status
        }
        
        import json
        with open(log_file, 'w', encoding='utf-8') as f:
            json.dump(log_data, f, ensure_ascii=False, indent=2)
    
    def _monitor_sessions(self):
        """监控协助会话状态"""
        while self.monitor_running:
            current_time = time.time()
            
            for session_id, session in list(self.active_sessions.items()):
                elapsed_time = current_time - session.start_time
                
                # 检查超时
                if (session.status in ["pending", "in_progress"] and 
                    elapsed_time > session.request.timeout):
                    
                    print(f"⏰ 协助会话超时: {session_id}")
                    session.status = "timeout"
                    
                    # 通知超时
                    if self.on_assistance_timeout:
                        self.on_assistance_timeout(session_id, session.request)
                    
                    # 清理会话
                    self._cleanup_session(session_id)
            
            time.sleep(5)  # 每5秒检查一次
    
    def _generate_session_id(self) -> str:
        """生成会话ID"""
        import uuid
        return f"session_{uuid.uuid4().hex[:8]}"
    
    def _cleanup_session(self, session_id: str):
        """清理会话"""
        if session_id in self.active_sessions:
            # 记录会话结果
            session = self.active_sessions[session_id]
            self._log_session_result(session_id, session)
            
            # 移除会话
            del self.active_sessions[session_id]
    
    def _log_session_result(self, session_id: str, session: AssistanceSession):
        """记录会话结果"""
        result_dir = Path("assistance_results")
        result_dir.mkdir(exist_ok=True)
        
        result_file = result_dir / f"{session_id}.json"
        
        result_data = {
            "session_id": session_id,
            "request": {
                "reason": session.request.reason,
                "expected_action": session.request.expected_action,
                "required_data": session.request.required_data,
                "timeout": session.request.timeout
            },
            "start_time": session.start_time,
            "end_time": time.time(),
            "status": session.status,
            "result": session.result
        }
        
        import json
        with open(result_file, 'w', encoding='utf-8') as f:
            json.dump(result_data, f, ensure_ascii=False, indent=2)


# 创建全局助手实例
_global_assistant = None


def get_global_assistant() -> HumanAssistant:
    """获取全局助手实例"""
    global _global_assistant
    if _global_assistant is None:
        _global_assistant = HumanAssistant(interactive_mode=True)
        _global_assistant.start_monitoring()
    return _global_assistant


def create_assistant_callback() -> Callable:
    """创建用于AppAnalyzer的回调函数"""
    def callback(request: HumanAssistanceRequest) -> bool:
        assistant = get_global_assistant()
        return assistant.process_assistance_request(request)
    
    return callback


# 示例用法
if __name__ == "__main__":
    # 创建助手
    assistant = HumanAssistant(interactive_mode=True)
    assistant.start_monitoring()
    
    # 设置回调函数
    def on_started(session_id, request):
        print(f"🔔 协助开始: {session_id}")
    
    def on_completed(session_id, request, result):
        print(f"🔔 协助完成: {session_id}")
    
    def on_timeout(session_id, request):
        print(f"🔔 协助超时: {session_id}")
    
    assistant.on_assistance_started = on_started
    assistant.on_assistance_completed = on_completed
    assistant.on_assistance_timeout = on_timeout
    
    # 模拟指纹登录请求
    fingerprint_request = HumanAssistanceRequest(
        reason="需要指纹登录",
        expected_action="完成指纹认证",
        required_data={"app": "银行应用", "user": "testuser"},
        timeout=120
    )
    
    # 处理请求
    success = assistant.process_assistance_request(fingerprint_request)
    
    if success:
        print("✅ 指纹登录协助成功")
    else:
        print("❌ 指纹登录协助失败")
    
    # 停止监控
    assistant.stop_monitoring()