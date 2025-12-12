"""截图管理器模块，负责设备截图和图像处理"""

import time
import os
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass
import subprocess
import cv2
import numpy as np

from core.analysis.analysis_utils import AnalysisUtils


@dataclass
class ScreenshotInfo:
    """截图信息数据类"""
    screenshot_id: str
    file_path: str
    timestamp: float
    device_id: str
    app_package: str
    page_id: str
    image_size: Tuple[int, int]
    file_size: int


class ScreenshotManager:
    """截图管理器类"""
    
    def __init__(self, device_id: str, app_package: str, output_dir: Path):
        self.device_id = device_id
        self.app_package = app_package
        self.output_dir = output_dir
        
        # 截图存储目录
        self.screenshots_dir = output_dir / "screenshots"
        self.screenshots_dir.mkdir(exist_ok=True)
        
        # 截图信息记录
        self.screenshot_history: List[ScreenshotInfo] = []
        
        # 分析工具
        self.analysis_utils = AnalysisUtils()
        
        # 截图配置
        self.screenshot_format = "png"
        self.max_screenshots = 1000  # 最大截图数量限制
        self.compression_quality = 85  # JPEG压缩质量
    
    def capture_screenshot(self, page_id: str = "") -> Optional[ScreenshotInfo]:
        """捕获设备截图"""
        try:
            # 生成截图ID
            screenshot_id = f"screenshot_{int(time.time())}_{page_id}"
            
            # 生成文件名
            filename = f"{screenshot_id}.{self.screenshot_format}"
            file_path = self.screenshots_dir / filename
            
            # 使用ADB命令截图
            adb_command = [
                "adb", "-s", self.device_id, "shell", "screencap", "-p"
            ]
            
            # 执行截图命令
            result = subprocess.run(adb_command, capture_output=True, text=True)
            
            if result.returncode != 0:
                print(f"❌ ADB截图失败: {result.stderr}")
                return None
            
            # 保存截图数据
            with open(file_path, 'wb') as f:
                f.write(result.stdout.encode('latin1'))
            
            # 获取图像信息
            image_info = self._get_image_info(file_path)
            if not image_info:
                print("❌ 获取图像信息失败")
                return None
            
            # 创建截图信息
            screenshot_info = ScreenshotInfo(
                screenshot_id=screenshot_id,
                file_path=str(file_path),
                timestamp=time.time(),
                device_id=self.device_id,
                app_package=self.app_package,
                page_id=page_id,
                image_size=image_info['size'],
                file_size=image_info['file_size']
            )
            
            # 添加到历史记录
            self.screenshot_history.append(screenshot_info)
            
            # 检查截图数量限制
            if len(self.screenshot_history) > self.max_screenshots:
                self._cleanup_old_screenshots()
            
            print(f"📸 截图已保存: {file_path}")
            return screenshot_info
            
        except Exception as e:
            print(f"❌ 截图捕获失败: {e}")
            return None
    
    def _get_image_info(self, file_path: Path) -> Optional[Dict[str, Any]]:
        """获取图像信息"""
        try:
            # 使用OpenCV读取图像
            image = cv2.imread(str(file_path))
            if image is None:
                return None
            
            # 获取图像尺寸
            height, width = image.shape[:2]
            
            # 获取文件大小
            file_size = file_path.stat().st_size
            
            return {
                'size': (width, height),
                'file_size': file_size
            }
            
        except Exception as e:
            print(f"❌ 获取图像信息失败: {e}")
            return None
    
    def _cleanup_old_screenshots(self):
        """清理旧截图"""
        try:
            # 按时间戳排序
            self.screenshot_history.sort(key=lambda x: x.timestamp)
            
            # 删除最旧的截图
            while len(self.screenshot_history) > self.max_screenshots:
                old_screenshot = self.screenshot_history.pop(0)
                
                # 删除文件
                old_file_path = Path(old_screenshot.file_path)
                if old_file_path.exists():
                    old_file_path.unlink()
                    print(f"🗑️  已删除旧截图: {old_file_path}")
            
        except Exception as e:
            print(f"❌ 清理旧截图失败: {e}")
    
    def compare_screenshots(self, screenshot1: ScreenshotInfo, 
                           screenshot2: ScreenshotInfo) -> Dict[str, Any]:
        """比较两个截图"""
        try:
            comparison_result = {
                "similarity": 0.0,
                "differences": [],
                "structural_similarity": 0.0,
                "pixel_difference": 0.0
            }
            
            # 读取图像
            img1 = cv2.imread(screenshot1.file_path)
            img2 = cv2.imread(screenshot2.file_path)
            
            if img1 is None or img2 is None:
                comparison_result["error"] = "无法读取截图文件"
                return comparison_result
            
            # 调整图像尺寸一致
            if img1.shape != img2.shape:
                img2 = cv2.resize(img2, (img1.shape[1], img1.shape[0]))
            
            # 计算结构相似性 (SSIM)
            similarity = self._calculate_ssim(img1, img2)
            comparison_result["structural_similarity"] = similarity
            
            # 计算像素差异
            pixel_diff = self._calculate_pixel_difference(img1, img2)
            comparison_result["pixel_difference"] = pixel_diff
            
            # 计算总体相似度
            comparison_result["similarity"] = similarity * 0.7 + (1 - pixel_diff) * 0.3
            
            # 检测差异区域
            differences = self._detect_differences(img1, img2)
            comparison_result["differences"] = differences
            
            return comparison_result
            
        except Exception as e:
            print(f"❌ 截图比较失败: {e}")
            return {"error": f"截图比较失败: {e}"}
    
    def _calculate_ssim(self, img1: np.ndarray, img2: np.ndarray) -> float:
        """计算结构相似性指数"""
        try:
            # 转换为灰度图像
            gray1 = cv2.cvtColor(img1, cv2.COLOR_BGR2GRAY)
            gray2 = cv2.cvtColor(img2, cv2.COLOR_BGR2GRAY)
            
            # 计算SSIM
            from skimage.metrics import structural_similarity
            score, _ = structural_similarity(gray1, gray2, full=True)
            return float(score)
            
        except ImportError:
            print("⚠️  scikit-image未安装，使用简化相似度计算")
            # 简化版本：计算平均像素差异
            diff = cv2.absdiff(img1, img2)
            mean_diff = np.mean(diff)
            return max(0.0, 1.0 - mean_diff / 255.0)
        except Exception as e:
            print(f"❌ 计算SSIM失败: {e}")
            return 0.0
    
    def _calculate_pixel_difference(self, img1: np.ndarray, img2: np.ndarray) -> float:
        """计算像素差异"""
        try:
            # 计算绝对差异
            diff = cv2.absdiff(img1, img2)
            
            # 计算平均差异
            mean_diff = np.mean(diff)
            
            # 归一化到0-1范围
            normalized_diff = mean_diff / 255.0
            
            return float(normalized_diff)
            
        except Exception as e:
            print(f"❌ 计算像素差异失败: {e}")
            return 1.0
    
    def _detect_differences(self, img1: np.ndarray, img2: np.ndarray) -> List[Dict[str, Any]]:
        """检测差异区域"""
        try:
            differences = []
            
            # 计算差异图像
            diff = cv2.absdiff(img1, img2)
            
            # 转换为灰度
            gray_diff = cv2.cvtColor(diff, cv2.COLOR_BGR2GRAY)
            
            # 二值化
            _, thresh = cv2.threshold(gray_diff, 30, 255, cv2.THRESH_BINARY)
            
            # 查找轮廓
            contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            for i, contour in enumerate(contours):
                # 计算轮廓面积
                area = cv2.contourArea(contour)
                
                if area > 100:  # 忽略小面积差异
                    # 获取边界框
                    x, y, w, h = cv2.boundingRect(contour)
                    
                    differences.append({
                        "id": i,
                        "area": int(area),
                        "bounding_box": {
                            "x": x,
                            "y": y,
                            "width": w,
                            "height": h
                        }
                    })
            
            return differences
            
        except Exception as e:
            print(f"❌ 检测差异区域失败: {e}")
            return []
    
    def extract_text_from_screenshot(self, screenshot_info: ScreenshotInfo) -> Optional[Dict[str, Any]]:
        """从截图中提取文本"""
        try:
            # 读取图像
            image = cv2.imread(screenshot_info.file_path)
            if image is None:
                return None
            
            # 使用Tesseract OCR提取文本
            try:
                import pytesseract
                
                # 配置Tesseract
                custom_config = r'--oem 3 --psm 6'
                
                # 提取文本
                extracted_text = pytesseract.image_to_string(image, config=custom_config)
                
                # 清理文本
                cleaned_text = extracted_text.strip()
                
                return {
                    "extracted_text": cleaned_text,
                    "text_length": len(cleaned_text),
                    "confidence": 0.8  # 模拟置信度
                }
                
            except ImportError:
                print("⚠️  pytesseract未安装，无法进行OCR文本提取")
                return {
                    "extracted_text": "OCR功能未启用",
                    "text_length": 0,
                    "confidence": 0.0
                }
            
        except Exception as e:
            print(f"❌ 文本提取失败: {e}")
            return None
    
    def analyze_screenshot_layout(self, screenshot_info: ScreenshotInfo) -> Optional[Dict[str, Any]]:
        """分析截图布局"""
        try:
            # 读取图像
            image = cv2.imread(screenshot_info.file_path)
            if image is None:
                return None
            
            # 转换为灰度图像
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            
            # 边缘检测
            edges = cv2.Canny(gray, 50, 150)
            
            # 直线检测
            lines = cv2.HoughLinesP(edges, 1, np.pi/180, threshold=50, 
                                   minLineLength=50, maxLineGap=10)
            
            layout_analysis = {
                "image_size": screenshot_info.image_size,
                "edge_density": np.sum(edges > 0) / (edges.shape[0] * edges.shape[1]),
                "detected_lines": 0,
                "layout_complexity": 0.0
            }
            
            if lines is not None:
                layout_analysis["detected_lines"] = len(lines)
                
                # 计算布局复杂度（基于线条数量和分布）
                complexity = min(len(lines) / 100, 1.0)
                layout_analysis["layout_complexity"] = complexity
            
            return layout_analysis
            
        except Exception as e:
            print(f"❌ 布局分析失败: {e}")
            return None
    
    def get_screenshot_history(self) -> List[ScreenshotInfo]:
        """获取截图历史"""
        return self.screenshot_history.copy()
    
    def get_latest_screenshot(self) -> Optional[ScreenshotInfo]:
        """获取最新截图"""
        if self.screenshot_history:
            return self.screenshot_history[-1]
        return None
    
    def clear_screenshot_history(self):
        """清空截图历史"""
        try:
            # 删除所有截图文件
            for screenshot in self.screenshot_history:
                file_path = Path(screenshot.file_path)
                if file_path.exists():
                    file_path.unlink()
            
            # 清空历史记录
            self.screenshot_history.clear()
            
            print("🗑️  截图历史已清空")
            
        except Exception as e:
            print(f"❌ 清空截图历史失败: {e}")
    
    def export_screenshot_data(self) -> bool:
        """导出截图数据"""
        try:
            export_data = {
                "screenshot_history": [],
                "statistics": self._get_screenshot_statistics()
            }
            
            # 导出截图历史
            for screenshot in self.screenshot_history:
                export_data["screenshot_history"].append({
                    "screenshot_id": screenshot.screenshot_id,
                    "file_path": screenshot.file_path,
                    "timestamp": screenshot.timestamp,
                    "device_id": screenshot.device_id,
                    "app_package": screenshot.app_package,
                    "page_id": screenshot.page_id,
                    "image_size": screenshot.image_size,
                    "file_size": screenshot.file_size
                })
            
            # 保存到文件
            export_file = self.screenshots_dir / "screenshot_data.json"
            with open(export_file, 'w', encoding='utf-8') as f:
                import json
                json.dump(export_data, f, ensure_ascii=False, indent=2)
            
            print(f"💾 截图数据已导出到: {export_file}")
            return True
            
        except Exception as e:
            print(f"❌ 导出截图数据失败: {e}")
            return False
    
    def _get_screenshot_statistics(self) -> Dict[str, Any]:
        """获取截图统计信息"""
        try:
            stats = {
                "total_screenshots": len(self.screenshot_history),
                "total_file_size": 0,
                "average_file_size": 0,
                "screenshot_timeline": []
            }
            
            # 计算总文件大小
            total_size = 0
            for screenshot in self.screenshot_history:
                total_size += screenshot.file_size
                
                # 添加时间线数据
                stats["screenshot_timeline"].append({
                    "timestamp": screenshot.timestamp,
                    "page_id": screenshot.page_id,
                    "file_size": screenshot.file_size
                })
            
            stats["total_file_size"] = total_size
            
            # 计算平均文件大小
            if self.screenshot_history:
                stats["average_file_size"] = total_size / len(self.screenshot_history)
            
            return stats
            
        except Exception as e:
            print(f"❌ 获取截图统计信息失败: {e}")
            return {}


def create_screenshot_manager(device_id: str, app_package: str, output_dir: Path) -> ScreenshotManager:
    """创建截图管理器"""
    return ScreenshotManager(device_id, app_package, output_dir)