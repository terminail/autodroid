from dataclasses import dataclass
from typing import Dict, List, Optional, Callable, Tuple
from pathlib import Path
import xml.etree.ElementTree as ET
import yaml

try:
    from .page import PageMatcher, PageExecutor, preprocess_xml_for_parsing, parse_xml, PageInfo
    from .element import ElementExecutor, StepInfo, ActionType
    from .u2device import U2Device
except ImportError:
    # 当直接运行脚本时使用绝对导入
    from page import PageMatcher, PageExecutor, preprocess_xml_for_parsing, parse_xml, PageInfo
    from element import ElementExecutor, StepInfo, ActionType
    from u2device import U2Device


from typing import Dict


@dataclass
class LoadResult:
    loaded_count: int
    page_info: Dict[str, int]


class FlowManager:
    def __init__(self, apk_dir: Path, device: Optional[U2Device] = None):
        self.apk_dir = apk_dir
        self.device = device
        self._page_matcher = PageMatcher()
        self._page_executor = PageExecutor(self._page_matcher)
        self._element_executor = None
        if device:
            self._element_executor = ElementExecutor(device, self._page_matcher)
        self._end_pages: List[str] = []
        self._executed_steps_by_page: Dict[str, set] = {}  # 按页面跟踪执行的步骤
        self._total_steps: int = 0
        self._page_infos: Dict[str, PageInfo] = {}
        self._page_executor.set_executed_steps_callback(self._on_step_executed)
        self._page_executor.set_status_callback(self.get_execution_status)
        self._current_executing_page_id: Optional[str] = None

    @property
    def page_matcher(self) -> PageMatcher:
        return self._page_matcher

    @property
    def end_pages(self) -> List[str]:
        return self._end_pages

    def get_flow_dir(self, apk_package: str, flow_name: str) -> Path:
        return self.apk_dir / apk_package / flow_name

    def _is_current_page_matched(self, page_id: str, page_info: PageInfo) -> bool:
        """检查当前页面是否匹配预期的页面定义，优先使用fingerprint元素进行精确匹配"""
        if not self.device:
            return False
        
        # 优先使用fingerprint元素进行精确匹配
        fingerprint_elements = page_info.fingerprint_elements
        if fingerprint_elements:
            print(f"  🔍 页面 {page_id}: 使用 {len(fingerprint_elements)} 个fingerprint元素进行精确匹配")
            
            # 检查所有fingerprint元素，必须所有元素都存在才认为页面匹配
        found_count = 0
        total_count = len(fingerprint_elements)
        
        for fp_elem in fingerprint_elements:
            resource_id = fp_elem.resource_id.strip()
            text = fp_elem.text.strip()
            content_desc = fp_elem.content_desc.strip()
            bounds = fp_elem.bounds.strip()
            
            print(f"    - 检查fingerprint元素: resource_id='{resource_id}', text='{text}', content_desc='{content_desc}', bounds='{bounds}'")
            
            # 使用选择器检查元素是否存在
            if resource_id:
                selector = f'resourceId("{resource_id}")'
                if self.device.check_element_exists(selector):
                    print(f"    ✓ 找到fingerprint元素 {selector}")
                    found_count += 1
                else:
                    print(f"    ✗ 未找到fingerprint元素: {selector}")
            elif text:
                selector = f'text("{text}")'
                if self.device.check_element_exists(selector):
                    print(f"    ✓ 找到fingerprint元素 {selector}")
                    found_count += 1
                else:
                    print(f"    ✗ 未找到fingerprint元素: {selector}")
            elif content_desc:
                selector = f'description("{content_desc}")'
                if self.device.check_element_exists(selector):
                    print(f"    ✓ 找到fingerprint元素 {selector}")
                    found_count += 1
                else:
                    print(f"    ✗ 未找到fingerprint元素: {selector}")
            elif bounds:
                # 如果没有其他标识符，尝试使用bounds进行匹配
                print(f"    🔍 尝试通过bounds匹配: {bounds}")
                try:
                    # 获取当前页面的XML并查找具有指定bounds的元素
                    live_xml = self.device.dump_hierarchy()
                    live_root = ET.fromstring(live_xml.encode('utf-8'))
                    
                    element_found = False
                    for elem in live_root.iter():
                        if elem.get("bounds") == bounds:
                            # 检查是否还有其他匹配条件需要验证
                            if (not fp_elem.class_name or elem.get("class", "") == fp_elem.class_name) and \
                               (not fp_elem.clickable or elem.get("clickable", "") == fp_elem.clickable):
                                element_found = True
                                print(f"    ✓ 找到fingerprint元素 (bounds匹配): {bounds}")
                                break
                    
                    if element_found:
                        found_count += 1
                    else:
                        print(f"    ✗ 未找到fingerprint元素 (bounds: {bounds})")
                except Exception as e:
                    print(f"    ✗ bounds匹配失败: {e}")
        
        if found_count == total_count:
            print(f"  ✓ 页面 {page_id}: 所有fingerprint元素都匹配 ({found_count}/{total_count})")
            return True
        else:
            if found_count == 0:
                print(f"  ✗ 页面 {page_id}: 所有fingerprint元素都未找到 (0/{total_count})")
            else:
                print(f"  ✗ 页面 {page_id}: 部分fingerprint元素未找到 ({found_count}/{total_count})")
            return False

    def _load_flow_config(self, apk_package: str, flow_name: str) -> List[str]:
        flow_dir = self.get_flow_dir(apk_package, flow_name)
        config_path = flow_dir / "config.yaml"

        end_pages = []
        if config_path.exists():
            try:
                with open(config_path, 'r', encoding='utf-8') as f:
                    config = yaml.safe_load(f)
                if config and 'ends' in config:
                    for end in config['ends']:
                        layout = end.get('layout', '')
                        if layout:
                            end_pages.append(layout.replace('.xml', ''))
            except Exception as e:
                print(f"  ⚠️ 加载流程配置失败: {e}")

        return end_pages

    def load_and_build_pages(
        self,
        apk_package: str = "com.tdx.androidCCZQ",
        flow_name: str = "general",
        preprocess_func=None
    ) -> LoadResult:
        self.reset_execution_state()
        flow_dir = self.get_flow_dir(apk_package, flow_name)
        print(f"📂 加载业务流程页面: {flow_dir}")

        if not flow_dir.exists():
            print(f"❌ 目录不存在: {flow_dir}")
            return LoadResult(loaded_count=0, page_info={})

        loaded_count = 0
        page_info: Dict[str, int] = {}

        for xml_file in sorted(flow_dir.glob("*.xml")):
            try:
                with open(xml_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                if preprocess_func:
                    content = preprocess_func(content)
                content = preprocess_xml_for_parsing(content)
                root = ET.fromstring(content)

                page_id = xml_file.stem
                # 使用选择器方案加载页面
                self._page_matcher.add_page_info_from_xml(root, page_id)
                
                # 获取动作元素数量
                action_elements = [elem for elem in root.iter() 
                                 if elem.get("{{{}}}action".format("https://autodroid.example.com"))]
                action_count = len(action_elements)
                page_info[page_id] = action_count
                
                if action_count > 0:
                    print(f"  ✓ {xml_file.name} -> {page_id} ({action_count} 个动作)")
                else:
                    print(f"  ⚠️ {xml_file.name} -> {page_id} (无 autodroid:action)")
                loaded_count += 1

            except ET.ParseError as e:
                print(f"  ✗ {xml_file.name}: XML解析错误 - {e}")
            except Exception as e:
                print(f"  ✗ {xml_file.name}: {e}")

        print(f"\n✅ 加载完成: {loaded_count} 个页面\n")

        self._end_pages = self._load_flow_config(apk_package, flow_name)
        if self._end_pages:
            print(f"🏁 结束页面: {self._end_pages}")

        self._total_steps = self._calculate_total_steps()
        print(f"📊 流程总步骤数: {self._total_steps}")

        # 从PageMatcher获取页面信息
        self._page_infos = self._page_matcher.page_infos

        return LoadResult(loaded_count=loaded_count, page_info=page_info)

    def identify_page(self) -> Tuple[Optional[str], float, List[Tuple[str, float, Dict]]]:
        """
        识别当前页面（flow层职责）- 使用选择器方案
        
        Returns:
            (page_id, score, all_scores)
        """
        if not self.device:
            print("⚠️ 未初始化设备，无法进行页面识别")
            return (None, 0.0, [])
        
        try:
            print("🔍 开始选择器方案页面识别...")
            
            # 遍历所有页面，检查是否有匹配的元素
            for page_id, page_info in self._page_infos.items():
                print(f"  🔍 检查页面: {page_id}")
                if self._is_current_page_matched(page_id, page_info):
                    print(f"✅ 识别到页面: {page_id} (选择器方案)")
                    return (page_id, 1.0, [(page_id, 1.0, {"method": "selector"})])
            
            # 未找到匹配的页面
            print("❌ 未找到匹配的页面")
            print("⚠️ 请用户手动操作到已知页面，或检查页面XML是否定义了fingerprint元素")
            print("   提示：可以使用 autodroid:fingerprint='true' 标记页面唯一元素以提高识别准确率")
            return (None, 0.0, [])
        except Exception as e:
            print(f"⚠️ 页面识别失败: {e}")
            import traceback
            traceback.print_exc()
            return (None, 0.0, [])

    def _execute_action_callback(self, step: int, action: str, elem_info, live_elem) -> bool:
        if not self._element_executor:
            print(f"  ⚠️ 未初始化 ElementExecutor，无法执行动作")
            return False
        
        step_info = StepInfo(
            step=step,
            action=action,
            element=elem_info,
            name=elem_info.name,
            value=elem_info.value,
            save_to=elem_info.save_to,
            desc=elem_info.desc
        )
        
        return self._element_executor.execute_action(step_info, live_elem)

    def run_flow(self, start_page: str = None, max_iterations: int = 1000, step_by_step: bool = False) -> bool:
        """运行流程主循环 - FlowManager总控
        1. 启动时识别当前页面（应该是start_page）
        2. 循环：
           - 识别当前页面
           - 如果是结束页，检查流程是否完成
           - 如果不是结束页，调用对应页面的 next_step()
           - 步骤执行后更新当前页面
           
        Args:
            step_by_step: 如果为True，每执行一步后暂停等待用户确认
        """
        import time
        import sys
        
        print("\n" + "=" * 60)
        print("🚀 启动流程执行")
        print("=" * 60)
        
        if not self.device:
            print("❌ 未初始化设备，无法执行流程")
            return False
        
        if not self._page_infos:
            print("❌ 未加载任何页面")
            return False
        
        iteration_count = 0
        consecutive_not_found_count = 0
        last_not_found_page = None
        
        while iteration_count < max_iterations:
            iteration_count += 1
            
            current_page_id, _, _ = self.identify_page()
            
            if not current_page_id:
                if last_not_found_page == current_page_id:
                    consecutive_not_found_count += 1
                else:
                    consecutive_not_found_count = 1
                    last_not_found_page = current_page_id
                
                if consecutive_not_found_count > 3:
                    print(f"\n❌ 连续 {consecutive_not_found_count} 次未能识别页面，退出流程")
                    return False
                
                print(f"\n⚠️ 未能识别当前页面 (第 {iteration_count} 次迭代)")
                time.sleep(1)
                continue
            
            consecutive_not_found_count = 0
            last_not_found_page = None
            
            print(f"\n🔍 当前页面: {current_page_id}")
            
            is_end_page = current_page_id in self._end_pages
            
            if is_end_page:
                status = self.get_execution_status()
                print(f"\n🏁 到达结束页面: {current_page_id}")
                print(f"📊 流程执行状态:")
                print(f"   已执行步骤: {status['executed_steps']}/{status['total_steps']}")
                print(f"   完成率: {status['completion_rate']:.1%}")
                
                if status['executed_steps'] == 0:
                    print(f"\n⚠️ 到达结束页面但尚未执行任何步骤")
                    print(f"   流程可能未正确启动")
                    return False
                
                if status['is_complete']:
                    print(f"\n✅ 流程执行完成！")
                    return True
                else:
                    print(f"\n⚠️ 流程未完整执行，部分步骤未完成")
                    return False
            
            has_more_steps = self._page_executor.has_more_steps(current_page_id)
            
            if not has_more_steps:
                print(f"\n⚠️ 页面 {current_page_id} 没有更多步骤可执行")
                time.sleep(0.5)
                continue
            
            next_elem = self._page_executor.get_next_elem_info(current_page_id)
            if next_elem:
                print(f"   待执行步骤: {next_elem.action}")
            
            # 获取当前页面的数据和执行状态
            if current_page_id not in self._page_matcher._page_infos:
                print(f"  ⚠️ 页面不存在: {current_page_id}")
                return False
                
            page_data = self._page_matcher._page_infos[current_page_id]
            current_page_executed_steps = self._executed_steps_by_page.get(current_page_id, set())
            
            step_success = self._page_executor.next_step(
                current_page_id,
                page_data,
                current_page_executed_steps,
                self._execute_action_callback,
                self.device,
                self.refresh_current_page
            )
            
            # 如果步骤执行成功，执行状态已在PageExecutor中更新（通过step_executed属性）
            # 无需额外更新执行状态
            
            if not step_success:
                print(f"\n⚠️ 步骤执行失败，页面: {current_page_id}")
                return False
            
            time.sleep(0.3)
            
            if step_by_step:
                print("\n" + "-" * 60)
                user_input = input("[?] 是否继续执行下一步? (Y/n): ").strip().lower()
                if user_input == '' or user_input == 'y':
                    continue
                elif user_input == 'n':
                    print("⏸️ 暂停执行，等待手动操作...")
                    return True
                else:
                    print("⚠️ 无效输入，继续执行...")
                    continue
        
        print(f"\n⚠️ 达到最大迭代次数 {max_iterations}，退出流程")
        return False

    def run_flow_single_step(self, start_page: str = None) -> bool:
        """执行单个步骤后暂停，等待用户确认
        流程：
        1. 识别当前页面
        2. 显示待执行步骤（如果是redirect显示目标页面）
        3. 提示用户确认
        4. 执行一步后返回
        """
        import time
        
        if not self.device:
            print("❌ 未初始化设备，无法执行流程")
            return False
        
        if not self._page_infos:
            print("❌ 未加载任何页面")
            return False
        
        current_page_id, _, _ = self.identify_page()
        
        if not current_page_id:
            print(f"\n⚠️ 未能识别当前页面")
            return False
        
        print(f"\n🔍 当前页面: {current_page_id}")
        
        is_end_page = current_page_id in self._end_pages
        
        if is_end_page:
            status = self.get_execution_status()
            print(f"\n🏁 到达结束页面: {current_page_id}")
            print(f"📊 流程执行状态:")
            print(f"   已执行步骤: {status['executed_steps']}/{status['total_steps']}")
            print(f"   完成率: {status['completion_rate']:.1%}")
            if status['is_complete']:
                print(f"\n✅ 流程执行完成！")
            return True
        
        has_more_steps = self._page_executor.has_more_steps(current_page_id)
        
        if not has_more_steps:
            print(f"\n⚠️ 页面 {current_page_id} 没有更多步骤可执行")
            return False
        
        next_elem = self._page_executor.get_next_elem_info(current_page_id)
        if next_elem:
            print(f"   待执行步骤: {next_elem.action}")
            if next_elem.action == "redirect" and next_elem.value:
                print(f"   ⟶ 目标页面: {next_elem.value}")
        
        print("\n" + "-" * 60)
        user_input = input("[?] 是否自动操作当前页面? (Y/n/q/m/f): ").strip().lower()
        
        if user_input == 'q':
            print("👋 再见!")
            return False
        
        if user_input == 'm':
            print("\n🎯 当前匹配模式: 选择器方案 (autodroid:fingerprint=\"true\")")
            print("⚠️ 仅支持选择器方案，无需切换")
            return True
        
        if user_input == 'n':
            print("⏸️ 等待手动操作...")
            return True
        
        if user_input == '' or user_input == 'y':
            # 获取当前页面的数据和执行状态
            if current_page_id not in self._page_matcher._page_infos:
                print(f"  ⚠️ 页面不存在: {current_page_id}")
                return False
                
            page_data = self._page_matcher._page_infos[current_page_id]
            current_page_executed_steps = self._executed_steps_by_page.get(current_page_id, set())
            
            success, action_type, target_page = self._page_executor.next_step(
                current_page_id,
                page_data,
                current_page_executed_steps,
                self._execute_action_callback,
                self.device,
                self.refresh_current_page
            )
            
            # 如果步骤执行成功，执行状态已在PageExecutor中更新（通过step_executed属性）
            # 无需额外更新执行状态
            
            if not success:
                print(f"\n⚠️ 步骤执行失败，页面: {current_page_id}")
                return False
            
            print(f"  ✓ {action_type} 完成")
            
            if action_type == "redirect" and target_page:
                print(f"\n⟶ 等待目标页面: {target_page}")
                if self._wait_for_page(target_page, timeout=15):
                    # 检查目标页面是否有待执行的步骤
                    next_elem_info = self._page_executor.get_next_elem_info(target_page)
                    if next_elem_info:
                        step_info = next_elem_info.step or "未知"
                        print(f"  ✓ 成功到达目标页面: {target_page}，准备执行第{step_info}步")
                    else:
                        print(f"  ✓ 成功到达目标页面: {target_page}，无待执行步骤")
                else:
                    print(f"  ⚠️ 等待目标页面超时: {target_page}")
            
            return True
        
        print("⚠️ 无效输入")
        return True

    def _wait_for_page(self, target_page_id: str, timeout: int = 15) -> bool:
        """等待目标页面出现"""
        import time
        start_time = time.time()
        
        while time.time() - start_time < timeout:
            page_id, _, _ = self.identify_page()
            if page_id == target_page_id:
                return True
            time.sleep(0.5)
        
        return False

    def refresh_current_page(self) -> str:
        """刷新当前页面识别"""
        page_id, _, _ = self.identify_page()
        return page_id if page_id else ""

    def check_current_page(self) -> Tuple[Optional[str], bool]:
        current_page_id, score, _ = self.identify_page()
        is_end_page = current_page_id in self._end_pages if current_page_id else False
        return current_page_id, is_end_page

    def reset_execution_state(self):
        """重置执行状态"""
        self._executed_steps_by_page = {}
        self._total_steps = 0

    def _on_step_executed(self, step: int, page_id: str = None):
        """步骤执行回调 - 记录已执行的步骤"""
        if page_id:
            if page_id not in self._executed_steps_by_page:
                self._executed_steps_by_page[page_id] = set()
            self._executed_steps_by_page[page_id].add(step)

    def _calculate_total_steps(self) -> int:
        """计算流程中所有页面的总步骤数"""
        total = 0
        # 使用PageMatcher的方法获取总步骤数
        return self._page_matcher.get_total_steps()

    def get_execution_status(self) -> Dict:
        """获取流程执行状态"""
        # 计算所有页面的已执行步骤总数
        total_executed = sum(len(steps) for steps in self._executed_steps_by_page.values())
        return {
            "executed_steps": total_executed,
            "total_steps": self._total_steps,
            "completion_rate": total_executed / self._total_steps if self._total_steps > 0 else 0.0,
            "is_complete": total_executed == self._total_steps and self._total_steps > 0
        }
