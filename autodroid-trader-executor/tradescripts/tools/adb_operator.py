import sys
import argparse
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

from core.config import get_apks_path
from typing import Optional, Dict, List, Tuple

try:
    from .u2device import U2Device
    from .flow import FlowManager
    from .interactive_cli import InteractiveCLI
except ImportError:
    # 当直接运行脚本时使用绝对导入
    from u2device import U2Device
    from flow import FlowManager
    from interactive_cli import InteractiveCLI


class ADBAutoOpTool:
    def __init__(
        self,
        device_id: str = "TDCDU17905004388",
        apk_package: str = "com.tdx.androidCCZQ",
        flow_name: str = "general"
    ):
        self.device_id = device_id
        self.apk_package = apk_package
        self.flow_name = flow_name
        self.test_data: Dict[str, str] = {}
        self.runtime_context: Dict[str, str] = {}

        self.apk_dir = get_apks_path()
        print(f"📂 APK目录: {self.apk_dir}")
        print(f"📱 应用包名: {self.apk_package}")
        print(f"🔄 流程名称: {self.flow_name}")

        self.device_manager = U2Device(device_id, self.apk_dir)

        self.flow_manager = FlowManager(self.apk_dir, self.device_manager)
        self._load_flow_pages()

        self.cli = InteractiveCLI(self.flow_manager, self.device_manager)
        self.cli.current_apk_package = self.apk_package
        self.cli.current_flow_name = self.flow_name

    def _load_flow_pages(self):
        self.flow_manager.load_and_build_pages(self.apk_package, self.flow_name)

    def run_interactive_mode(self, default_method: int = 0):
        self.cli.run_interactive_mode(default_method)


def main():
    parser = argparse.ArgumentParser(description='ADB Auto Operator - 自动化操作工具')
    parser.add_argument('--device', '-d', type=str, default='TDCDU17905004388',
                        help='设备ID (默认: TDCDU17905004388)')
    parser.add_argument('--package', '-p', type=str, default='com.tdx.androidCCZQ',
                        help='应用包名 (默认: com.tdx.androidCCZQ)')
    parser.add_argument('--flow', '-f', type=str, default='general',
                        help='流程名称 (默认: general，可选: general, netgrid-trading)')
    parser.add_argument('--method', '-m', type=int, default=0,
                        help='页面识别方法 (默认: 0)')

    args = parser.parse_args()

    tool = ADBAutoOpTool(
        device_id=args.device,
        apk_package=args.package,
        flow_name=args.flow
    )
    tool.run_interactive_mode(default_method=args.method)


if __name__ == "__main__":
    main()
