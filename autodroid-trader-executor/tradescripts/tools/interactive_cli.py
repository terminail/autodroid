import time
from typing import Optional

try:
    from .flow import FlowManager
    from .u2device import U2Device
except ImportError:
    # 当直接运行脚本时使用绝对导入
    from flow import FlowManager
    from u2device import U2Device


def _get_method_name(method: int) -> str:
    names = {
        0: "选择器方案"
    }
    return names.get(method, "选择器方案")


def _print_menu():
    print("\n" + "=" * 60)
    print("🚀 ADB Auto Operator")
    print("=" * 60)


class InteractiveCLI:
    def __init__(self, flow_manager: FlowManager, u2_device: U2Device):
        self.flow_manager = flow_manager
        self.u2_device = u2_device
        self.current_apk_package = "com.tdx.androidCCZQ"
        self.current_flow_name = "general"

    def run_interactive_mode(self, default_method: int = 0):
        _print_menu()
        print(f"📊 当前匹配模式: {_get_method_name(default_method)}")
        print(f"📱 当前流程: {self.current_apk_package}/{self.current_flow_name}")
        current_method = default_method

        while True:
            try:
                user_input = input("\n[?] 是否自动操作当前页面? (Y/n/q/m/f): ").strip().lower()
                if user_input == '':
                    user_input = 'y'

                if user_input == 'q':
                    print("\n👋 再见!")
                    break

                elif user_input == 'm':
                    print("\n🎯 当前匹配模式: 选择器方案 (autodroid:fingerprint=\"true\")")
                    print("⚠️ 仅支持选择器方案，无需切换")

                elif user_input == 'f':
                    print("\n🔄 选择流程:")
                    print("  1 - general (通用流程)")
                    print("  2 - wang-ge-jiao-yi (网格交易流程)")
                    flow_input = input("请输入流程编号 (1/2): ").strip()
                    if flow_input == '1':
                        self.current_flow_name = "general"
                        self._reload_flow()
                        print(f"✓ 已切换到: {self.current_apk_package}/{self.current_flow_name}")
                    elif flow_input == '2':
                        self.current_flow_name = "wang-ge-jiao-yi"
                        self._reload_flow()
                        print(f"✓ 已切换到: {self.current_apk_package}/{self.current_flow_name}")
                    else:
                        print("⚠️ 无效输入，保持当前流程")

                elif user_input == 'n':
                    print("⏸️ 等待手动操作...")
                    time.sleep(2)

                elif user_input == 'y':
                    # 使用选择器方案进行快速页面识别，无需下载完整livexml
                    page_id, _, _ = self.flow_manager.identify_page()
                    
                    if page_id:
                        print(f"\n📄 识别页面: {page_id} (选择器方案)")
                        print("-" * 60)
                        # 执行一个步骤后暂停，等待用户确认
                        self.flow_manager.run_flow_single_step(page_id)
                    else:
                        print("\n⚠️ 未能识别当前页面")
                        time.sleep(2)
                else:
                    print("\n⚠️ 无效输入，请输入 y/n/q/m/f")        
                    time.sleep(1)

            except KeyboardInterrupt:
                print("\n👋 再见!")
                break
            except Exception as e:
                print(f"❌ 错误: {e}")
                time.sleep(2)

    def _reload_flow(self):
        """重新加载当前流程的页面指纹"""
        self.flow_manager.load_and_build_pages(self.current_apk_package, self.current_flow_name)
