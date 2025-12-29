from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple, Callable
import xml.etree.ElementTree as ET
import time

from page import (
    PageFingerprint,
    preprocess_xml_for_parsing,
    parse_xml,
    build_page_fingerprint,
    build_parent_map,
    get_child_texts,
    compute_relative_path,
    follow_relative_path,
    parse_bounds,
    calculate_overlap,
)


class PageMatcher:
    def __init__(self, strategy: str = "fingerprint"):
        self._page_fingerprints: Dict[str, PageFingerprint] = {}
        self._strategy = strategy

    @property
    def page_fingerprints(self) -> Dict[str, PageFingerprint]:
        return self._page_fingerprints

    @property
    def strategy(self) -> str:
        return self._strategy

    def set_strategy(self, strategy: str):
        self._strategy = strategy

    def add_fingerprint(self, fingerprint: PageFingerprint):
        self._page_fingerprints[fingerprint.page_id] = fingerprint

    def add_fingerprint_from_xml(self, root: ET.Element, page_id: str) -> PageFingerprint:
        fingerprint = build_page_fingerprint(root, page_id)
        self.add_fingerprint(fingerprint)
        return fingerprint

    def load_pages_from_dir(self, flow_dir, preprocess_func=None):
        if not flow_dir.exists():
            return

        for xml_file in sorted(flow_dir.glob("*.xml")):
            try:
                with open(xml_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                if preprocess_func:
                    content = preprocess_func(content)
                root = ET.fromstring(content)
                page_id = xml_file.stem
                fingerprint = build_page_fingerprint(root, page_id)
                self._page_fingerprints[page_id] = fingerprint
            except ET.ParseError:
                pass

    def clear(self):
        self._page_fingerprints.clear()

    def calculate_multi_strategy_score(self, live_root: ET.Element, page_fingerprint: PageFingerprint) -> Dict[str, float]:
        live_bounds_set = set()
        live_resource_ids = set()
        live_texts = set()
        live_content_descs = set()
        live_clickable_count = 0

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            bounds = elem.get("bounds", "").strip()
            rid = elem.get("resource-id", "").strip()
            text = elem.get("text", "").strip()
            content_desc = elem.get("content-desc", "").strip()
            clickable = elem.get("clickable", "").strip() == "true"

            if bounds and clickable:
                live_bounds_set.add(bounds)
            if rid:
                live_resource_ids.add(rid)
            if text:
                live_texts.add(text)
            if content_desc:
                live_content_descs.add(content_desc)
            if clickable:
                live_clickable_count += 1

        page_bounds_set = set(page_fingerprint.bounds_set)
        page_resource_ids = set(page_fingerprint.resource_ids)
        page_texts = set(page_fingerprint.texts)
        page_content_descs = set(page_fingerprint.content_descs)
        page_clickable_count = page_fingerprint.clickable_count

        bounds_overlap = len(live_bounds_set & page_bounds_set)
        id_overlap = len(live_resource_ids & page_resource_ids)
        text_overlap = len(live_texts & page_texts)
        content_desc_overlap = len(live_content_descs & page_content_descs)

        bounds_score = bounds_overlap / len(page_bounds_set) if page_bounds_set else 0
        id_score = id_overlap / len(page_resource_ids) if page_resource_ids else 0
        text_score = text_overlap / len(page_texts) if page_texts else 0
        content_desc_score = content_desc_overlap / len(page_content_descs) if page_content_descs else 0
        clickable_score = 1.0 - abs(live_clickable_count - page_clickable_count) / max(page_clickable_count, 1) if page_clickable_count > 0 else 0.5

        return {
            "bounds_score": bounds_score,
            "id_score": id_score,
            "text_score": text_score,
            "content_desc_score": content_desc_score,
            "clickable_score": clickable_score,
            "bounds_overlap": bounds_overlap,
            "total_bounds": len(page_bounds_set),
        }

    def calculate_page_similarity(self, live_root: ET.Element, page_fingerprint: PageFingerprint) -> float:
        action_elements = page_fingerprint.action_elements
        if not action_elements:
            return 0.0

        matched_count = 0
        for offline_elem in action_elements:
            if self._find_in_live_xml(live_root, offline_elem, page_fingerprint):
                matched_count += 1

        return matched_count / len(action_elements)

    def _find_in_live_xml(self, live_root: ET.Element, offline_elem: Dict, page_fingerprint: PageFingerprint = None) -> Tuple[Optional[ET.Element], str]:
        offline_resource_id = offline_elem.get("resource_id", "").strip()
        offline_text = offline_elem.get("text", "").strip()
        offline_content_desc = offline_elem.get("content_desc", "").strip()
        offline_children = offline_elem.get("children", [])
        offline_bounds = offline_elem.get("bounds", "").strip()

        if not offline_resource_id and not offline_text and not offline_content_desc and not offline_children and not offline_bounds:
            return (None, "")

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            live_bounds = elem.get("bounds", "").strip()
            if offline_bounds and live_bounds == offline_bounds:
                return (elem, f"bounds定位: {offline_bounds}")

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            live_resource_id = elem.get("resource-id", "").strip()
            if offline_resource_id and live_resource_id == offline_resource_id:
                return (elem, f"resource-id定位: {offline_resource_id}")

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            live_text = elem.get("text", "").strip()
            if offline_text and live_text == offline_text:
                return (elem, f"text定位: {offline_text}")

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            live_content_desc = elem.get("content-desc", "").strip()
            if offline_content_desc and live_content_desc == offline_content_desc:
                return (elem, f"content-desc定位: {offline_content_desc}")

        if offline_children:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_children = get_child_texts(elem)
                if live_children:
                    if set(offline_children) & set(live_children):
                        return (elem, f"children文本定位: {offline_children}")

        if page_fingerprint and offline_elem.get("id"):
            found = self._find_action_via_help(live_root, offline_elem.get("id"), page_fingerprint)
            if found:
                return (found, f"help相对路径定位")

        if page_fingerprint and offline_elem.get("overlap"):
            found = self._find_action_via_overlap(live_root, offline_elem, page_fingerprint)
            if found:
                return (found, f"bounds重叠定位")

        return (None, "")

    def _find_action_via_help(self, live_root: ET.Element, action_id: str, page_fingerprint: PageFingerprint) -> Optional[ET.Element]:
        help_to_action = page_fingerprint.help_to_action
        if action_id not in help_to_action:
            return None

        path_info = help_to_action[action_id]
        relative_path = path_info.get("path", [])
        if not relative_path:
            return None

        target_help = None
        for help_info in page_fingerprint.help_elements:
            if help_info.get("help") == action_id:
                target_help = help_info
                break

        if not target_help:
            return None

        help_text = target_help.get("text", "").strip()
        help_resource_id = target_help.get("resource_id", "").strip()
        help_content_desc = target_help.get("content_desc", "").strip()

        help_elem = None
        if help_resource_id:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_resource_id = elem.get("resource-id", "").strip()
                if live_resource_id == help_resource_id:
                    help_elem = elem
                    break

        if not help_elem and help_text:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_text = elem.get("text", "").strip()
                if live_text == help_text:
                    help_elem = elem
                    break

        if not help_elem and help_content_desc:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_content_desc = elem.get("content-desc", "").strip()
                if live_content_desc == help_content_desc:
                    help_elem = elem
                    break

        if help_elem:
            parent_map = build_parent_map(live_root)
            action_elem = follow_relative_path(help_elem, relative_path, parent_map)
            if action_elem:
                return action_elem

        return None

    def _find_action_via_overlap(self, live_root: ET.Element, offline_elem: Dict, page_fingerprint: PageFingerprint) -> Optional[ET.Element]:
        overlap_info = offline_elem.get("overlap", {})
        if not overlap_info:
            return None

        target_text = overlap_info.get("text", "").strip()
        target_resource_id = overlap_info.get("resource_id", "").strip()
        target_content_desc = overlap_info.get("content_desc", "").strip()
        target_overlap = overlap_info.get("overlap")

        if not target_text and not target_resource_id and not target_content_desc:
            return None

        help_elem = None
        if target_resource_id:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_resource_id = elem.get("resource-id", "").strip()
                if live_resource_id == target_resource_id:
                    help_elem = elem
                    break

        if not help_elem and target_text:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_text = elem.get("text", "").strip()
                if live_text == target_text:
                    help_elem = elem
                    break

        if not help_elem and target_content_desc:
            for elem in live_root.iter():
                if elem.tag == "hierarchy":
                    continue
                live_content_desc = elem.get("content-desc", "").strip()
                if live_content_desc == target_content_desc:
                    help_elem = elem
                    break

        if help_elem and target_overlap:
            return help_elem

        return None

    def identify_page(self, live_root: ET.Element) -> Tuple[Optional[str], float, List[Tuple[str, float, Dict]]]:
        if not self._page_fingerprints:
            return (None, 0.0, [])

        if self._strategy == "bounds":
            return self._identify_by_bounds(live_root)
        elif self._strategy == "resource_id":
            return self._identify_by_resource_id(live_root)
        else:
            return self._identify_by_fingerprint(live_root)

    def _identify_by_bounds(self, live_root: ET.Element) -> Tuple[Optional[str], float, List[Tuple[str, float, Dict]]]:
        live_bounds_set = set()
        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            bounds = elem.get("bounds", "").strip()
            if bounds:
                live_bounds_set.add(bounds)

        all_scores = []
        for page_id, page_data in self._page_fingerprints.items():
            page_bounds_set = set(page_data.bounds_set)
            overlap = len(live_bounds_set & page_bounds_set)
            score = 1.0 if (page_bounds_set and page_bounds_set == live_bounds_set) else (overlap / len(page_bounds_set) if page_bounds_set else 0)
            all_scores.append((page_id, score, {"bounds_overlap": overlap, "total_bounds": len(page_bounds_set)}))

        all_scores.sort(key=lambda x: x[1], reverse=True)
        best = all_scores[0] if all_scores else (None, 0.0, {})
        return (best[0], best[1], all_scores)

    def _identify_by_resource_id(self, live_root: ET.Element) -> Tuple[Optional[str], float, List[Tuple[str, float, Dict]]]:
        live_resource_ids = set()
        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            rid = elem.get("resource-id", "").strip()
            if rid:
                live_resource_ids.add(rid)

        all_scores = []
        for page_id, page_data in self._page_fingerprints.items():
            page_resource_ids = set(page_data.resource_ids)
            if not page_resource_ids:
                all_scores.append((page_id, 0.0, {"id_overlap": 0, "total_ids": 0}))
                continue

            overlap = len(live_resource_ids & page_resource_ids)
            score = overlap / len(page_resource_ids)
            all_scores.append((page_id, score, {"id_overlap": overlap, "total_ids": len(page_resource_ids)}))

        all_scores.sort(key=lambda x: x[1], reverse=True)
        best = all_scores[0] if all_scores else (None, 0.0, {})
        return (best[0], best[1], all_scores)

    def _identify_by_fingerprint(self, live_root: ET.Element) -> Tuple[Optional[str], float, List[Tuple[str, float, Dict]]]:
        if not self._page_fingerprints:
            return (None, 0.0, [])

        live_bounds_set = set()
        live_resource_ids = set()
        live_texts = set()
        live_webview_texts = set()

        for elem in live_root.iter():
            if elem.tag == "hierarchy":
                continue
            bounds = elem.get("bounds", "").strip()
            rid = elem.get("resource-id", "").strip()
            text = elem.get("text", "").strip()
            class_name = elem.get("class", "").strip()
            clickable = elem.get("clickable", "").strip() == "true"

            if bounds and clickable:
                live_bounds_set.add(bounds)
            if rid:
                live_resource_ids.add(rid)
            if text:
                live_texts.add(text)
            if class_name == "android.webkit.WebView" and text:
                live_webview_texts.add(text)

        all_scores = []
        for page_id, page_data in self._page_fingerprints.items():
            page_bounds_set = set(page_data.bounds_set)
            page_resource_ids = set(page_data.resource_ids)
            page_texts = set(page_data.texts)
            page_webview_texts = set(page_data.webview_texts)

            bounds_overlap = len(live_bounds_set & page_bounds_set)
            bounds_score = bounds_overlap / len(page_bounds_set) if page_bounds_set else 0

            id_overlap = len(live_resource_ids & page_resource_ids)
            id_score = id_overlap / len(page_resource_ids) if page_resource_ids else 0

            text_overlap = len(live_texts & page_texts)
            text_score = text_overlap / len(page_texts) if page_texts else 0

            wv_overlap = len(live_webview_texts & page_webview_texts)
            wv_score = wv_overlap / len(page_webview_texts) if page_webview_texts else 0

            weights = {
                "bounds_score": 0.40,
                "id_score": 0.20,
                "text_score": 0.25,
                "content_desc_score": 0.10,
                "clickable_score": 0.05,
            }

            combined_score = (
                bounds_score * weights["bounds_score"] +
                id_score * weights["id_score"] +
                text_score * weights["text_score"]
            )

            all_scores.append((page_id, combined_score, {
                "bounds_overlap": bounds_overlap,
                "total_bounds": len(page_bounds_set),
                "id_overlap": id_overlap,
                "total_ids": len(page_resource_ids),
                "text_overlap": text_overlap,
                "total_texts": len(page_texts),
                "webview_overlap": wv_overlap,
                "webview_total": len(page_webview_texts),
            }))

        all_scores.sort(key=lambda x: x[1], reverse=True)
        best = all_scores[0] if all_scores else (None, 0.0, {})
        return (best[0], best[1], all_scores)

    def find_element(self, live_root: ET.Element, elem_info: Dict) -> Tuple[Optional[ET.Element], str]:
        return self._find_in_live_xml(live_root, elem_info)

    def execute_steps(
        self,
        page_id: str,
        live_root: ET.Element,
        execute_action: Callable,
        end_pages: Optional[List[str]] = None,
        refresh_page_callback: Optional[Callable[[], str]] = None
    ) -> bool:
        if page_id not in self._page_fingerprints:
            print(f"❌ 页面不存在: {page_id}")
            return False

        page_data = self._page_fingerprints[page_id]
        action_elements = page_data.action_elements
        total_steps = len(action_elements)

        if not action_elements:
            print(f"\n⚠️ 页面 {page_id} 已匹配，但无 autodroid:action 定义")
            return True

        print(f"\n📋 执行页面流程: {page_id}")
        print(f"   步骤数: {total_steps}")
        print("-" * 40)

        for idx, elem_info in enumerate(action_elements, 1):
            step = elem_info.get("step", idx)
            action = elem_info.get("action", "")
            desc = elem_info.get("desc", "")
            name = elem_info.get("name", "")
            value = elem_info.get("value", "")
            save_to = elem_info.get("save_to", "")
            wait_after = elem_info.get("wait_after", "")

            print(f"\n[{idx}/{total_steps}] 步骤 {step}: {action}")
            if desc:
                print(f"   描述: {desc}")
            if name:
                print(f"   数据键: {name}")
            if value:
                print(f"   默认值: {value}")

            live_elem, locate_method = self.find_element(live_root, elem_info)
            if not live_elem:
                elem_name = elem_info.get("text") or elem_info.get("resource_id", "").split("/")[-1] or "unknown"
                print(f"  ⚠️ 未找到元素: {elem_name}")
                return False

            elem_name = elem_info.get("desc") or elem_info.get("text") or elem_info.get("resource_id", "").split("/")[-1] or "unknown"
            print(f"  🔍 {locate_method}")
            print(f"  ✅ 找到元素: text='{live_elem.get('text', '')}', class='{live_elem.get('class', '')}'")

            if execute_action(step, action, elem_info, live_elem):
                print(f"  ✓ {action} 完成")
            else:
                print(f"\n⚠️ 步骤 {step} 执行失败")
                return False

            if wait_after:
                try:
                    wait_time = float(wait_after)
                    time.sleep(wait_time)
                    print(f"  ⏸️ 等待 {wait_time} 秒后继续")
                except:
                    pass

        print("\n" + "=" * 40)
        if end_pages:
            if refresh_page_callback:
                latest_xml = refresh_page_callback()
                current_page_id, _ = self.identify_page(parse_xml(latest_xml))
            else:
                current_page_id, _ = self.identify_page(live_root)
            if current_page_id in end_pages:
                print(f"🏁 已到达结束页面: {current_page_id}")
                print(f"✅ general 流程执行完成！\n")
            else:
                print(f"✅ 页面 {page_id} 执行完成 ({idx}/{total_steps})")
                print(f"   当前页面: {current_page_id}")
                print(f"   继续执行下一步...\n")
        else:
            print(f"✅ 页面 {page_id} 执行完成 ({idx}/{total_steps})\n")
        return True
