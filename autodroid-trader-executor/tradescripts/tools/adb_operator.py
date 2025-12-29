import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

from core.config import get_apks_path
import sys
import time
from typing import Optional, Dict, List, Tuple
from dataclasses import dataclass

from flow import FlowManager
from element import ElementExecutor, StepInfo, ActionType
from u2device import U2Device


@dataclass
class StepInfo:
    step: int
    action: str
    element: Dict
    name: str
    value: str
    save_to: str
    desc: str


class ADBAutoOpTool:
    def __init__(
        self,
        device_id: str = "TDCDU17905004388"
    ):
        self.device_id = device_id
        self.test_data: Dict[str, str] = {}
        self.runtime_context: Dict[str, str] = {}
        self.device = U2Device(device_id)

        self.apk_dir = get_apks_path()
        print(f"📂 APK目录: {self.apk_dir}")

        self.flow_manager = FlowManager(self.apk_dir)
        self._load_flow_pages()

    def _load_flow_pages(self, apk_package: str = "com.tdx.androidCCZQ", flow_name: str = "general"):
        self.flow_manager.load_and_build_fingerprints(apk_package, flow_name)

    def dump_hierarchy(self) -> str:
        return self.device.dump_hierarchy()

    def save_current_page(self, page_id: str, prefix: str = "live") -> str:
        live_xml = self.dump_hierarchy()
        timestamp = int(time.time())
        filename = f"{prefix}_{page_id}_{timestamp}.xml"
        filepath = Path(self.apk_dir).parent.parent / "debug_xmls" / filename
        filepath.parent.mkdir(exist_ok=True)
        filepath.write_text(live_xml, encoding="utf-8")
        print(f"💾 已保存当前页面: {filepath}")
        return str(filepath)

    def identify_page(self, live_xml: Optional[str] = None) -> Tuple[Optional[str], float, List[Tuple[str, float, Dict]]]:
        if live_xml is None:
            live_xml = self.dump_hierarchy()

        return self.flow_manager.identify_page(live_xml)

    def execute_page_steps(self, page_id: str, live_xml: str) -> bool:
        return self.flow_manager.execute_page_steps(page_id, live_xml, self.execute_action_callback, self.dump_hierarchy)

    def execute_action_callback(self, step: int, action: str, elem_info: Dict, live_elem) -> bool:
        step_info = StepInfo(
            step=step,
            action=action,
            element=elem_info,
            name=elem_info.get("name", ""),
            value=elem_info.get("value", ""),
            save_to=elem_info.get("save_to", ""),
            desc=elem_info.get("desc", "")
        )
        element_executor = ElementExecutor(self.device)
        return element_executor.execute_action(step_info, live_elem)

    def _print_menu(self):
        print("\n" + "=" * 60)
        print("🚀 ADB Auto Operator")
        print("=" * 60)

    def run_interactive_mode(self):
        self._print_menu()

        while True:
            try:
                user_input = input("\n[?] 是否自动操作当前页面? (y/n/q): ").strip().lower()

                if user_input == 'q':
                    print("\n👋 再见!")
                    break

                elif user_input == 'n':
                    print("⏸️ 等待手动操作...")
                    time.sleep(2)

                elif user_input == 'y':
                    live_xml = self.dump_hierarchy()
                    page_id, score = self.identify_page(live_xml)

                    if page_id:
                        print(f"\n📄 识别页面: {page_id} (匹配度: {score:.2%})")
                        print("-" * 60)
                        self.execute_page_steps(page_id, live_xml)
                    else:
                        print("\n⚠️ 未能识别当前页面")
                        time.sleep(2)
                else:
                    print("\n⚠️ 无效输入，请输入 y/n/q")        
                    time.sleep(1)

            except KeyboardInterrupt:
                print("\n👋 再见!")
                break
            except Exception as e:
                print(f"❌ 错误: {e}")
                time.sleep(2)

def main():
    tool = ADBAutoOpTool()
    tool.run_interactive_mode()


if __name__ == "__main__":
    main()
