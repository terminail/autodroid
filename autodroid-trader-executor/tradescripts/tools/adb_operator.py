import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

from core.config import get_apks_path
from typing import Optional, Dict, List, Tuple

from u2device import U2Device
from flow import FlowManager
from interactive_cli import InteractiveCLI


class ADBAutoOpTool:
    def __init__(
        self,
        device_id: str = "TDCDU17905004388"
    ):
        self.device_id = device_id
        self.test_data: Dict[str, str] = {}
        self.runtime_context: Dict[str, str] = {}

        self.apk_dir = get_apks_path()
        print(f"📂 APK目录: {self.apk_dir}")

        self.device_manager = U2Device(device_id, self.apk_dir)

        self.flow_manager = FlowManager(self.apk_dir, self.device_manager)
        self._load_flow_pages()

        self.cli = InteractiveCLI(self.flow_manager, self.device_manager)

    def _load_flow_pages(self, apk_package: str = "com.tdx.androidCCZQ", flow_name: str = "general"):
        self.flow_manager.load_and_build_fingerprints(apk_package, flow_name)

    def dump_hierarchy(self) -> str:
        return self.device_manager.dump_hierarchy()

    def save_current_page(self, page_id: str, prefix: str = "live") -> str:
        return self.device_manager.save_current_page(page_id, prefix)

    def identify_page(self, live_xml: Optional[str] = None, method: int = 0) -> Tuple[Optional[str], float, List[Tuple[str, float, Dict]]]:
        return self.flow_manager.identify_page(live_xml, method)

    def execute_page_steps(self, page_id: str, live_xml: str) -> bool:
        return self.flow_manager.execute_page_steps(page_id, live_xml, self.device_manager.dump_hierarchy)

    def run_interactive_mode(self, default_method: int = 0):
        self.cli.run_interactive_mode(default_method)


def main():
    tool = ADBAutoOpTool()
    tool.run_interactive_mode()


if __name__ == "__main__":
    main()
