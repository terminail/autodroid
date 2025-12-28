import os
import re
import xml.etree.ElementTree as ET
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass, field
from pathlib import Path
import logging

logger = logging.getLogger(__name__)


@dataclass
class PageFingerprint:
    page_id: str
    xml_path: str
    features: Dict[str, set] = field(default_factory=dict)
    element_count: int = 0


@dataclass
class MatchResult:
    page_id: Optional[str]
    score: float
    matched_features: int
    total_features: int
    matched_elements: List[str]


class PageMatcher:
    def __init__(self, apks_dir: str, match_threshold: float = 0.6):
        self.apks_dir = Path(apks_dir)
        self.match_threshold = match_threshold
        self.page_fingerprints: Dict[str, PageFingerprint] = {}
        self._load_all_pages()

    def _load_all_pages(self):
        if not self.apks_dir.exists():
            logger.warning(f"APKs directory does not exist: {self.apks_dir}")
            return

        for apk_package_dir in self.apks_dir.iterdir():
            if not apk_package_dir.is_dir():
                continue

            for flow_dir in apk_package_dir.iterdir():
                if not flow_dir.is_dir():
                    continue

                xml_files = list(flow_dir.glob("*.xml"))
                for xml_file in xml_files:
                    try:
                        self._load_page_fingerprint(xml_file)
                    except Exception as e:
                        logger.warning(f"Failed to load page XML {xml_file}: {e}")

    def _sanitize_xml(self, xml_content: str) -> str:
        xml_content = re.sub(r'\sautodroid:[a-z_.-]+="[^"]*"', '', xml_content)
        return xml_content

    def _load_page_fingerprint(self, xml_path: Path):
        with open(xml_path, 'r', encoding='utf-8') as f:
            xml_content = f.read()
        
        xml_content = self._sanitize_xml(xml_content)
        
        try:
            tree = ET.ElementTree(ET.fromstring(xml_content))
            root = tree.getroot()
        except ET.ParseError as e:
            logger.warning(f"Failed to parse XML {xml_path}: {e}")
            raise
        
        page_id = root.get("autodroid:page_id") or root.get("page_id")
        if not page_id:
            page_id = xml_path.stem

        fingerprint = PageFingerprint(
            page_id=page_id,
            xml_path=str(xml_path)
        )

        features = {
            "resource_ids": set(),
            "texts": set(),
            "classes": set(),
            "content_descs": set(),
            "clickables": set(),
        }

        element_count = 0
        for elem in root.iter():
            if elem.tag == "hierarchy":
                continue

            element_count += 1

            if elem.get("resource-id"):
                rid = elem.get("resource-id").split("/")[-1]
                features["resource_ids"].add(rid)

            if elem.get("text"):
                text = elem.get("text").strip()
                if text:
                    features["texts"].add(text)

            if elem.get("class"):
                features["classes"].add(elem.get("class"))

            if elem.get("content-desc"):
                features["content_descs"].add(elem.get("content-desc"))

            clickable = elem.get("clickable", "false")
            features["clickables"].add(clickable)

        fingerprint.features = features
        fingerprint.element_count = element_count

        self.page_fingerprints[page_id] = fingerprint
        logger.info(f"Loaded page fingerprint: {page_id} ({element_count} elements, "
                   f"resource_ids={len(features['resource_ids'])}, texts={len(features['texts'])})")

    def identify_current_page(self, live_xml_source: str) -> MatchResult:
        try:
            live_root = ET.fromstring(live_xml_source.encode("utf-8"))
        except ET.ParseError as e:
            logger.error(f"Failed to parse live XML: {e}")
            return MatchResult(None, 0.0, 0, 0, [])

        live_features = self._extract_features(live_root)
        logger.debug(f"[PageMatcher] Live page features: resource_ids={len(live_features['resource_ids'])}, "
                    f"texts={len(live_features['texts'])}, classes={len(live_features['classes'])}")

        best_match_id = None
        best_score = 0.0
        best_matched_elements = []

        for page_id, fingerprint in self.page_fingerprints.items():
            score, matched_elements = self._calculate_similarity(
                live_features, fingerprint.features
            )
            logger.debug(f"[PageMatcher] Page {page_id}: score={score:.4f}, "
                        f"matched={len(matched_elements)}/{sum(len(f) for f in fingerprint.features.values())}")

            if score > best_score:
                best_score = score
                best_match_id = page_id
                best_matched_elements = matched_elements

        total_features = sum(
            len(f) for f in live_features.values() if isinstance(f, (set, list))
        )
        matched_count = len(best_matched_elements)

        if best_match_id and best_score >= self.match_threshold:
            logger.info(
                f"[PageMatcher] 页面识别成功: {best_match_id} (得分: {best_score:.4f}, "
                f"匹配特征: {matched_count}/{total_features})"
            )
            logger.debug(f"[PageMatcher] 匹配的元素: {best_matched_elements[:10]}...")
            return MatchResult(
                page_id=best_match_id,
                score=best_score,
                matched_features=matched_count,
                total_features=total_features,
                matched_elements=best_matched_elements,
            )
        else:
            logger.warning(
                f"[PageMatcher] 页面识别失败 (最佳得分: {best_score:.4f}, 阈值: {self.match_threshold})"
            )
            if best_matched_elements:
                logger.debug(f"[PageMatcher] 部分匹配的特征: {best_matched_elements[:10]}")
            return MatchResult(
                page_id=None,
                score=best_score,
                matched_features=matched_count,
                total_features=total_features,
                matched_elements=best_matched_elements,
            )

    def _extract_features(self, root: ET.Element) -> Dict[str, set]:
        features = {
            "resource_ids": set(),
            "texts": set(),
            "classes": set(),
            "content_descs": set(),
            "clickables": set(),
        }

        element_count = 0
        for elem in root.iter():
            if elem.tag == "hierarchy":
                continue

            element_count += 1

            if elem.get("resource-id"):
                rid = elem.get("resource-id").split("/")[-1]
                if rid:
                    features["resource_ids"].add(rid)

            if elem.get("text"):
                text = elem.get("text").strip()
                if text:
                    features["texts"].add(text)

            if elem.get("class"):
                features["classes"].add(elem.get("class"))

            if elem.get("content-desc"):
                cd = elem.get("content-desc").strip()
                if cd:
                    features["content_descs"].add(cd)

            clickable = elem.get("clickable", "false")
            features["clickables"].add(clickable)

        logger.debug(f"[PageMatcher] 从 {element_count} 个元素中提取特征: "
                    f"resource_ids={len(features['resource_ids'])}, texts={len(features['texts'])}")

        return features

    def _calculate_similarity(
        self, live_features: Dict[str, set], offline_features: Dict[str, set]
    ) -> Tuple[float, List[str]]:
        if not offline_features or not live_features:
            logger.debug("[PageMatcher] 特征为空: live={}, offline={}".format(
                bool(live_features), bool(offline_features)))
            return 0.0, []

        matched_elements = []
        total_weight = 0.0
        matched_weight = 0.0

        feature_weights = {
            "resource_ids": 3.0,
            "texts": 2.0,
            "classes": 1.0,
            "content_descs": 2.5,
            "clickables": 0.5,
        }

        feature_details = {}

        for feature_name, weight in feature_weights.items():
            live_set = live_features.get(feature_name, set())
            offline_set = offline_features.get(feature_name, set())

            total_weight += weight * len(offline_set)

            feature_matches = []
            for item in offline_set:
                if item in live_set:
                    matched_weight += weight
                    matched_elements.append(f"{feature_name}:{item}")
                    feature_matches.append(item)

            feature_details[feature_name] = {
                "offline_count": len(offline_set),
                "live_count": len(live_set),
                "matched": feature_matches,
                "weight": weight
            }

        if total_weight == 0:
            logger.debug("[PageMatcher] 总权重为0，无法计算相似度")
            return 0.0, matched_elements

        similarity = matched_weight / total_weight

        logger.debug("[PageMatcher] 相似度计算详情: "
                    f"matched_weight={matched_weight}, total_weight={total_weight}, "
                    f"similarity={similarity:.4f}")

        return similarity, matched_elements

    def get_page_path(self, page_id: str) -> Optional[str]:
        if page_id in self.page_fingerprints:
            return self.page_fingerprints[page_id].xml_path
        return None

    def list_pages(self) -> List[str]:
        return list(self.page_fingerprints.keys())

    def get_page_info(self, page_id: str) -> Optional[PageFingerprint]:
        return self.page_fingerprints.get(page_id)

    def debug_page_features(self, page_id: str) -> Optional[Dict]:
        if page_id not in self.page_fingerprints:
            return None
        
        fingerprint = self.page_fingerprints[page_id]
        return {
            "page_id": fingerprint.page_id,
            "xml_path": fingerprint.xml_path,
            "element_count": fingerprint.element_count,
            "features": {
                k: list(v) for k, v in fingerprint.features.items()
            }
        }
