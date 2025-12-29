import time
from typing import Optional

from flow import FlowManager
from u2device import U2Device


class InteractiveCLI:
    def __init__(self, flow_manager: FlowManager, device_manager: U2Device):
        self.flow_manager = flow_manager
        self.device_manager = device_manager

    def run_interactive_mode(self, default_method: int = 0):
        self._print_menu()
        print(f"📊 当前匹配模式: {self._get_method_name(default_method)}")
        current_method = default_method

        while True:
            try:
                user_input = input("\n[?] 是否自动操作当前页面? (y/n/q/m): ").strip().lower()

                if user_input == 'q':
                    print("\n👋 再见!")
                    break

                elif user_input == 'm':
                    print("\n📊 选择匹配模式:")
                    print("  0 - 混合方案 (快速精确匹配 + 结构化特征匹配)")
                    print("  1 - 快速精确匹配 (仅使用resource-id)")
                    print("  2 - 结构化特征匹配 (基于特征工程)")
                    method_input = input("请输入模式编号 (0/1/2): ").strip()
                    if method_input in ['0', '1', '2']:
                        current_method = int(method_input)
                        print(f"✓ 已切换到: {self._get_method_name(current_method)}")
                    else:
                        print("⚠️ 无效输入，保持当前模式")

                elif user_input == 'n':
                    print("⏸️ 等待手动操作...")
                    time.sleep(2)

                elif user_input == 'y':
                    live_xml = self.device_manager.dump_hierarchy()
                    page_id, score, all_scores = self.flow_manager.identify_page(live_xml, current_method)

                    if page_id:
                        print(f"\n📄 识别页面: {page_id} (匹配度: {score:.2%})")
                        if all_scores:
                            print(f"📊 匹配详情: {all_scores[0][2].get('method', 'unknown')}")
                        print("-" * 60)
                        self.flow_manager.execute_page_steps(page_id, live_xml, self.device_manager.dump_hierarchy)
                    else:
                        print("\n⚠️ 未能识别当前页面")
                        time.sleep(2)
                else:
                    print("\n⚠️ 无效输入，请输入 y/n/q/m")        
                    time.sleep(1)

            except KeyboardInterrupt:
                print("\n👋 再见!")
                break
            except Exception as e:
                print(f"❌ 错误: {e}")
                time.sleep(2)

    def _print_menu(self):
        print("\n" + "=" * 60)
        print("🚀 ADB Auto Operator")
        print("=" * 60)

    def _get_method_name(self, method: int) -> str:
        names = {
            0: "混合方案",
            1: "快速精确匹配",
            2: "结构化特征匹配"
        }
        return names.get(method, "未知模式")
