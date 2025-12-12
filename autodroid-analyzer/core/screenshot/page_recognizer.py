"""页面识别器模块，负责识别和分类应用页面"""

import time
import json
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass
import hashlib

from core.analysis.analysis_utils import AnalysisUtils


@dataclass
class PageSignature:
    """页面签名数据类"""
    page_id: str
    title_hash: str
    element_count: int
    activity_name: str
    screenshot_hash: Optional[str] = None
    ui_hierarchy_hash: Optional[str] = None


@dataclass
class PageCategory:
    """页面分类数据类"""
    category_id: str
    category_name: str
    description: str
    keywords: List[str]
    confidence_threshold: float = 0.7


class PageRecognizer:
    """页面识别器类"""
    
    def __init__(self, device_id: str, app_package: str, output_dir: Path):
        self.device_id = device_id
        self.app_package = app_package
        self.output_dir = output_dir
        
        # 页面签名数据库
        self.page_signatures: Dict[str, PageSignature] = {}
        
        # 页面分类器
        self.page_categories: Dict[str, PageCategory] = {}
        
        # 分析工具
        self.analysis_utils = AnalysisUtils()
        
        # 创建页面识别输出目录
        self.recognition_dir = output_dir / "page_recognition"
        self.recognition_dir.mkdir(exist_ok=True)
        
        # 初始化默认页面分类
        self._initialize_default_categories()
    
    def _initialize_default_categories(self):
        """初始化默认页面分类"""
        default_categories = [
            PageCategory(
                category_id="login",
                category_name="登录页面",
                description="用户登录认证页面",
                keywords=["登录", "登陆", "sign in", "login", "用户名", "密码", "验证码"]
            ),
            PageCategory(
                category_id="main",
                category_name="主页面",
                description="应用主界面或首页",
                keywords=["首页", "主页", "main", "home", "dashboard", "欢迎"]
            ),
            PageCategory(
                category_id="settings",
                category_name="设置页面",
                description="应用设置和配置页面",
                keywords=["设置", "配置", "settings", "preferences", "选项", "个人中心"]
            ),
            PageCategory(
                category_id="list",
                category_name="列表页面",
                description="显示项目列表的页面",
                keywords=["列表", "清单", "list", "items", "商品", "产品", "文章"]
            ),
            PageCategory(
                category_id="detail",
                category_name="详情页面",
                description="显示项目详情的页面",
                keywords=["详情", "详细", "detail", "信息", "内容", "描述"]
            ),
            PageCategory(
                category_id="form",
                category_name="表单页面",
                description="包含输入表单的页面",
                keywords=["表单", "填写", "form", "输入", "提交", "注册", "申请"]
            ),
            PageCategory(
                category_id="search",
                category_name="搜索页面",
                description="搜索功能页面",
                keywords=["搜索", "查找", "search", "查询", "筛选", "过滤"]
            ),
            PageCategory(
                category_id="profile",
                category_name="个人资料页面",
                description="用户个人资料页面",
                keywords=["个人", "资料", "profile", "账户", "信息", "头像"]
            ),
            PageCategory(
                category_id="payment",
                category_name="支付页面",
                description="支付和结算页面",
                keywords=["支付", "付款", "payment", "结算", "订单", "金额"]
            ),
            PageCategory(
                category_id="notification",
                category_name="通知页面",
                description="消息和通知页面",
                keywords=["通知", "消息", "notification", "提醒", "公告", "动态"]
            )
        ]
        
        for category in default_categories:
            self.page_categories[category.category_id] = category
    
    def _calculate_hash(self, text: str) -> str:
        """计算文本哈希值"""
        return hashlib.md5(text.encode('utf-8')).hexdigest()
    
    def _calculate_file_hash(self, file_path: Path) -> Optional[str]:
        """计算文件哈希值"""
        try:
            if file_path.exists():
                with open(file_path, 'rb') as f:
                    return hashlib.md5(f.read()).hexdigest()
            return None
        except Exception as e:
            print(f"❌ 计算文件哈希失败: {e}")
            return None
    
    def create_page_signature(self, page_data: Dict[str, Any]) -> Optional[PageSignature]:
        """创建页面签名"""
        try:
            page_id = page_data.get('page_id', f"page_{int(time.time())}")
            
            # 计算标题哈希
            title = page_data.get('title', '')
            title_hash = self._calculate_hash(title)
            
            # 计算截图哈希
            screenshot_path = page_data.get('screenshot_path')
            screenshot_hash = None
            if screenshot_path:
                screenshot_hash = self._calculate_file_hash(Path(screenshot_path))
            
            # 计算UI层次结构哈希
            ui_hierarchy_path = page_data.get('ui_hierarchy_path')
            ui_hierarchy_hash = None
            if ui_hierarchy_path:
                ui_hierarchy_hash = self._calculate_file_hash(Path(ui_hierarchy_path))
            
            # 创建页面签名
            signature = PageSignature(
                page_id=page_id,
                title_hash=title_hash,
                element_count=page_data.get('element_count', 0),
                activity_name=page_data.get('activity_name', ''),
                screenshot_hash=screenshot_hash,
                ui_hierarchy_hash=ui_hierarchy_hash
            )
            
            # 保存到数据库
            self.page_signatures[page_id] = signature
            
            print(f"🔍 已创建页面签名: {page_id}")
            return signature
            
        except Exception as e:
            print(f"❌ 创建页面签名失败: {e}")
            return None
    
    def recognize_page(self, page_data: Dict[str, Any]) -> Dict[str, Any]:
        """识别页面类型"""
        try:
            recognition_result = {
                "page_id": page_data.get('page_id', 'unknown'),
                "categories": [],
                "primary_category": None,
                "confidence_scores": {},
                "similar_pages": []
            }
            
            # 获取页面文本内容
            page_text = self._extract_page_text(page_data)
            
            # 分类识别
            for category_id, category in self.page_categories.items():
                confidence = self._calculate_category_confidence(page_text, category)
                recognition_result["confidence_scores"][category_id] = confidence
                
                if confidence >= category.confidence_threshold:
                    recognition_result["categories"].append({
                        "category_id": category_id,
                        "category_name": category.category_name,
                        "confidence": confidence
                    })
            
            # 确定主要分类
            if recognition_result["categories"]:
                primary_category = max(recognition_result["categories"], 
                                     key=lambda x: x["confidence"])
                recognition_result["primary_category"] = primary_category
            
            # 查找相似页面
            recognition_result["similar_pages"] = self._find_similar_pages(page_data)
            
            return recognition_result
            
        except Exception as e:
            print(f"❌ 页面识别失败: {e}")
            return {"error": f"页面识别失败: {e}"}
    
    def _extract_page_text(self, page_data: Dict[str, Any]) -> str:
        """提取页面文本内容"""
        try:
            text_parts = []
            
            # 页面标题
            title = page_data.get('title', '')
            if title:
                text_parts.append(title)
            
            # 活动名称
            activity_name = page_data.get('activity_name', '')
            if activity_name:
                text_parts.append(activity_name)
            
            # 元素文本
            elements = page_data.get('elements', [])
            for element in elements:
                element_text = element.get('text', '')
                if element_text:
                    text_parts.append(element_text)
                
                content_desc = element.get('content_desc', '')
                if content_desc:
                    text_parts.append(content_desc)
            
            return ' '.join(text_parts)
            
        except Exception as e:
            print(f"❌ 提取页面文本失败: {e}")
            return ""
    
    def _calculate_category_confidence(self, page_text: str, category: PageCategory) -> float:
        """计算分类置信度"""
        try:
            if not page_text:
                return 0.0
            
            # 关键词匹配
            keyword_matches = 0
            for keyword in category.keywords:
                if keyword.lower() in page_text.lower():
                    keyword_matches += 1
            
            # 计算置信度
            if keyword_matches == 0:
                return 0.0
            
            # 基于匹配关键词数量和总关键词数量的比例
            confidence = keyword_matches / len(category.keywords)
            
            # 考虑文本长度因素
            text_length_factor = min(len(page_text) / 100, 1.0)
            confidence *= text_length_factor
            
            return min(confidence, 1.0)
            
        except Exception as e:
            print(f"❌ 计算分类置信度失败: {e}")
            return 0.0
    
    def _find_similar_pages(self, current_page_data: Dict[str, Any]) -> List[Dict[str, Any]]:
        """查找相似页面"""
        try:
            similar_pages = []
            
            # 创建当前页面签名
            current_signature = self.create_page_signature(current_page_data)
            if not current_signature:
                return similar_pages
            
            # 比较与已记录页面的相似度
            for page_id, signature in self.page_signatures.items():
                if page_id == current_signature.page_id:
                    continue  # 跳过自身
                
                similarity = self._calculate_signature_similarity(current_signature, signature)
                
                if similarity > 0.6:  # 相似度阈值
                    similar_pages.append({
                        "page_id": page_id,
                        "similarity": similarity,
                        "activity_name": signature.activity_name,
                        "element_count": signature.element_count
                    })
            
            # 按相似度排序
            similar_pages.sort(key=lambda x: x["similarity"], reverse=True)
            
            return similar_pages[:5]  # 返回前5个最相似的页面
            
        except Exception as e:
            print(f"❌ 查找相似页面失败: {e}")
            return []
    
    def _calculate_signature_similarity(self, sig1: PageSignature, sig2: PageSignature) -> float:
        """计算签名相似度"""
        try:
            similarity_score = 0.0
            total_weights = 0
            
            # 标题哈希相似度（权重：0.3）
            if sig1.title_hash == sig2.title_hash:
                similarity_score += 0.3
            total_weights += 0.3
            
            # 活动名称相似度（权重：0.3）
            if sig1.activity_name and sig2.activity_name:
                if sig1.activity_name == sig2.activity_name:
                    similarity_score += 0.3
            total_weights += 0.3
            
            # 元素数量相似度（权重：0.2）
            if sig1.element_count > 0 and sig2.element_count > 0:
                count_ratio = min(sig1.element_count, sig2.element_count) / max(sig1.element_count, sig2.element_count)
                similarity_score += 0.2 * count_ratio
            total_weights += 0.2
            
            # 截图哈希相似度（权重：0.1）
            if sig1.screenshot_hash and sig2.screenshot_hash:
                if sig1.screenshot_hash == sig2.screenshot_hash:
                    similarity_score += 0.1
            total_weights += 0.1
            
            # UI层次结构哈希相似度（权重：0.1）
            if sig1.ui_hierarchy_hash and sig2.ui_hierarchy_hash:
                if sig1.ui_hierarchy_hash == sig2.ui_hierarchy_hash:
                    similarity_score += 0.1
            total_weights += 0.1
            
            # 归一化相似度分数
            if total_weights > 0:
                return similarity_score / total_weights
            else:
                return 0.0
            
        except Exception as e:
            print(f"❌ 计算签名相似度失败: {e}")
            return 0.0
    
    def add_custom_category(self, category: PageCategory) -> bool:
        """添加自定义页面分类"""
        try:
            if category.category_id in self.page_categories:
                print(f"⚠️  分类ID已存在: {category.category_id}")
                return False
            
            self.page_categories[category.category_id] = category
            print(f"✅ 已添加自定义分类: {category.category_name}")
            return True
            
        except Exception as e:
            print(f"❌ 添加自定义分类失败: {e}")
            return False
    
    def export_recognition_data(self) -> bool:
        """导出识别数据"""
        try:
            export_data = {
                "page_categories": {},
                "page_signatures": {},
                "recognition_statistics": self._get_recognition_statistics()
            }
            
            # 导出页面分类
            for category_id, category in self.page_categories.items():
                export_data["page_categories"][category_id] = {
                    "category_name": category.category_name,
                    "description": category.description,
                    "keywords": category.keywords,
                    "confidence_threshold": category.confidence_threshold
                }
            
            # 导出页面签名
            for page_id, signature in self.page_signatures.items():
                export_data["page_signatures"][page_id] = {
                    "title_hash": signature.title_hash,
                    "element_count": signature.element_count,
                    "activity_name": signature.activity_name,
                    "screenshot_hash": signature.screenshot_hash,
                    "ui_hierarchy_hash": signature.ui_hierarchy_hash
                }
            
            # 保存到文件
            export_file = self.recognition_dir / "recognition_data.json"
            with open(export_file, 'w', encoding='utf-8') as f:
                json.dump(export_data, f, ensure_ascii=False, indent=2)
            
            print(f"💾 识别数据已导出到: {export_file}")
            return True
            
        except Exception as e:
            print(f"❌ 导出识别数据失败: {e}")
            return False
    
    def _get_recognition_statistics(self) -> Dict[str, Any]:
        """获取识别统计信息"""
        try:
            stats = {
                "total_pages": len(self.page_signatures),
                "total_categories": len(self.page_categories),
                "category_distribution": {},
                "average_elements_per_page": 0
            }
            
            # 分类分布统计
            for category_id in self.page_categories:
                stats["category_distribution"][category_id] = 0
            
            # 计算平均元素数量
            total_elements = 0
            for signature in self.page_signatures.values():
                total_elements += signature.element_count
            
            if self.page_signatures:
                stats["average_elements_per_page"] = total_elements / len(self.page_signatures)
            
            return stats
            
        except Exception as e:
            print(f"❌ 获取识别统计信息失败: {e}")
            return {}


def create_page_recognizer(device_id: str, app_package: str, output_dir: Path) -> PageRecognizer:
    """创建页面识别器"""
    return PageRecognizer(device_id, app_package, output_dir)