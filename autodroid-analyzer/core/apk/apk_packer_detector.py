#!/usr/bin/env python3
"""
APK加固检测工具
用于判断APK是否被加固，支持多种加固厂商检测
"""

import os
import zipfile
import re
import json
import subprocess
import tempfile
from pathlib import Path
from typing import Dict, List, Optional, Tuple

class APKPackerDetector:
    """APK加固检测器"""
    
    def __init__(self):
        # 常见加固厂商特征库
        self.packer_signatures = {
            # 360加固
            "360加固": {
                "dex_patterns": [r"libjiagu\.so", r"jiagu"],
                "manifest_indicators": ["360", "qihoo"],
                "file_indicators": ["libjiagu.so", "assets/jiagu"]
            },
            # 腾讯加固
            "腾讯加固": {
                "dex_patterns": [r"libshell\.so", r"tpshell"],
                "manifest_indicators": ["tencent", "tpsdk"],
                "file_indicators": ["libshell.so", "assets/tp"]
            },
            # 爱加密
            "爱加密": {
                "dex_patterns": [r"libexec\.so", r"ijiami"],
                "manifest_indicators": ["ijiami", "aijiami"],
                "file_indicators": ["libexec.so", "assets/ijiami"]
            },
            # 梆梆加固
            "梆梆加固": {
                "dex_patterns": [r"libsecexe\.so", r"bangcle"],
                "manifest_indicators": ["bangcle", "secexe"],
                "file_indicators": ["libsecexe.so", "assets/bangcle"]
            },
            # 娜迦加固
            "娜迦加固": {
                "dex_patterns": [r"libnaga\.so", r"naga"],
                "manifest_indicators": ["naga", "nagaencrypt"],
                "file_indicators": ["libnaga.so", "assets/naga"]
            },
            # 通付盾
            "通付盾": {
                "dex_patterns": [r"libtpsecurity\.so", r"tpsecurity"],
                "manifest_indicators": ["tpsecurity", "tongfudun"],
                "file_indicators": ["libtpsecurity.so", "assets/tpsecurity"]
            },
            # 阿里聚安全
            "阿里聚安全": {
                "dex_patterns": [r"libmobisec\.so", r"alibaba"],
                "manifest_indicators": ["alibaba", "mobisec"],
                "file_indicators": ["libmobisec.so", "assets/alibaba"]
            }
        }
    
    def detect_packer(self, apk_path: str) -> Dict:
        """
        检测APK是否被加固
        
        Args:
            apk_path: APK文件路径
            
        Returns:
            Dict: 检测结果
        """
        if not os.path.exists(apk_path):
            return {"error": f"APK文件不存在: {apk_path}"}
        
        try:
            with zipfile.ZipFile(apk_path, 'r') as apk_zip:
                return self._analyze_apk(apk_zip, apk_path)
        except Exception as e:
            return {"error": f"APK文件解析失败: {str(e)}"}
    
    def detect_packer_from_device(self, package_name: str, device_id: Optional[str] = None) -> Dict:
        """
        从Android设备检测已安装应用的加固情况
        
        Args:
            package_name: 应用包名
            device_id: 可选设备ID，用于多设备环境
            
        Returns:
            Dict: 检测结果
        """
        try:
            # 从设备提取APK文件
            apk_path = self._extract_apk_from_device(package_name, device_id)
            if not apk_path or not os.path.exists(apk_path):
                return {"error": f"无法从设备提取APK文件: {package_name}"}
            
            # 使用现有的检测方法
            result = self.detect_packer(apk_path)
            
            # 添加设备信息
            result["package_name"] = package_name
            result["device_id"] = device_id
            
            # 清理临时文件
            try:
                os.remove(apk_path)
            except:
                pass
                
            return result
            
        except Exception as e:
            return {"error": f"设备APK检测失败: {str(e)}"}
    
    def _extract_apk_from_device(self, package_name: str, device_id: Optional[str] = None) -> Optional[str]:
        """
        从设备提取APK文件到临时目录
        
        Args:
            package_name: 应用包名
            device_id: 可选设备ID
            
        Returns:
            Optional[str]: 提取的APK文件路径，失败返回None
        """
        try:
            # 构建ADB命令前缀
            adb_prefix = ["adb"]
            if device_id:
                adb_prefix.extend(["-s", device_id])
            
            # 获取APK路径
            cmd = adb_prefix + ["shell", "pm", "path", package_name]
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
            
            if result.returncode != 0:
                return None
            
            # 解析APK路径（格式：package:/path/to/app.apk）
            apk_path_on_device = None
            for line in result.stdout.strip().split('\n'):
                if line.startswith("package:"):
                    apk_path_on_device = line.replace("package:", "").strip()
                    break
            
            if not apk_path_on_device:
                return None
            
            # 创建临时文件
            temp_dir = tempfile.gettempdir()
            local_apk_path = os.path.join(temp_dir, f"{package_name}_extracted.apk")
            
            # 从设备拉取APK文件
            cmd = adb_prefix + ["pull", apk_path_on_device, local_apk_path]
            result = subprocess.run(cmd, capture_output=True, timeout=60)
            
            if result.returncode == 0 and os.path.exists(local_apk_path):
                return local_apk_path
            
            return None
            
        except Exception as e:
            print(f"提取APK文件失败: {str(e)}")
            return None
    
    def _analyze_apk(self, apk_zip: zipfile.ZipFile, apk_path: str) -> Dict:
        """分析APK文件"""
        result = {
            "apk_path": apk_path,
            "is_packed": False,
            "packer_type": None,
            "confidence": 0.0,
            "indicators": [],
            "file_analysis": {},
            "detailed_analysis": {}
        }
        
        # 获取APK基本信息
        apk_info = self._get_apk_info(apk_zip)
        result.update(apk_info)
        
        # 分析文件结构
        file_analysis = self._analyze_files(apk_zip)
        result["file_analysis"] = file_analysis
        
        # 检测加固特征
        packer_detection = self._detect_packer_signatures(apk_zip)
        
        if packer_detection["is_packed"]:
            result["is_packed"] = True
            result["packer_type"] = packer_detection["packer_type"]
            result["confidence"] = packer_detection["confidence"]
            result["indicators"] = packer_detection["indicators"]
            result["detailed_analysis"] = packer_detection["detailed_analysis"]
        
        return result
    
    def _get_apk_info(self, apk_zip: zipfile.ZipFile) -> Dict:
        """获取APK基本信息"""
        file_list = apk_zip.namelist()
        
        # 统计各类文件数量
        dex_files = [f for f in file_list if f.endswith('.dex')]
        so_files = [f for f in file_list if f.endswith('.so')]
        xml_files = [f for f in file_list if f.endswith('.xml')]
        
        return {
            "total_files": len(file_list),
            "dex_files": len(dex_files),
            "native_libs": len(so_files),
            "xml_files": len(xml_files),
            "file_list": file_list[:50]  # 只显示前50个文件
        }
    
    def _analyze_files(self, apk_zip: zipfile.ZipFile) -> Dict:
        """分析APK文件结构"""
        file_list = apk_zip.namelist()
        
        analysis = {
            "suspicious_files": [],
            "encrypted_resources": [],
            "unusual_patterns": []
        }
        
        # 检测可疑文件
        suspicious_patterns = [
            r"lib.*\.so",  # 所有so文件
            r"assets/.*encrypt",  # 加密资源
            r"META-INF/.*\.RSA",  # 签名文件
            r"classes\d*\.dex"  # 多个dex文件
        ]
        
        for pattern in suspicious_patterns:
            matched_files = [f for f in file_list if re.search(pattern, f)]
            if matched_files:
                analysis["suspicious_files"].extend(matched_files)
        
        return analysis
    
    def _detect_packer_signatures(self, apk_zip: zipfile.ZipFile) -> Dict:
        """检测加固特征"""
        file_list = apk_zip.namelist()
        
        detection_result = {
            "is_packed": False,
            "packer_type": None,
            "confidence": 0.0,
            "indicators": [],
            "detailed_analysis": {}
        }
        
        max_confidence = 0.0
        detected_packer = None
        
        for packer_name, signatures in self.packer_signatures.items():
            confidence = 0.0
            indicators = []
            detailed = {}
            
            # 检查文件特征
            file_indicators = signatures.get("file_indicators", [])
            file_matches = []
            
            for indicator in file_indicators:
                if any(indicator in f for f in file_list):
                    file_matches.append(indicator)
                    confidence += 0.4
            
            if file_matches:
                indicators.extend([f"发现文件: {match}" for match in file_matches])
                detailed["file_matches"] = file_matches
            
            # 检查DEX特征（需要读取DEX文件内容）
            dex_patterns = signatures.get("dex_patterns", [])
            dex_matches = []
            
            for dex_file in [f for f in file_list if f.endswith('.dex')]:
                try:
                    with apk_zip.open(dex_file) as f:
                        content = f.read(1024)  # 读取前1KB
                        for pattern in dex_patterns:
                            if re.search(pattern.encode(), content):
                                dex_matches.append(f"{dex_file}: {pattern}")
                                confidence += 0.3
                except:
                    continue
            
            if dex_matches:
                indicators.extend(dex_matches)
                detailed["dex_matches"] = dex_matches
            
            # 检查manifest特征
            manifest_indicators = signatures.get("manifest_indicators", [])
            manifest_matches = []
            
            for manifest_file in [f for f in file_list if 'AndroidManifest.xml' in f]:
                try:
                    with apk_zip.open(manifest_file) as f:
                        content = f.read()
                        for indicator in manifest_indicators:
                            if indicator.encode() in content:
                                manifest_matches.append(f"{manifest_file}: {indicator}")
                                confidence += 0.3
                except:
                    continue
            
            if manifest_matches:
                indicators.extend(manifest_matches)
                detailed["manifest_matches"] = manifest_matches
            
            # 更新最高置信度的检测结果
            if confidence > max_confidence:
                max_confidence = confidence
                detected_packer = packer_name
                detection_result["indicators"] = indicators
                detection_result["detailed_analysis"] = detailed
        
        if max_confidence > 0.5:  # 置信度阈值
            detection_result["is_packed"] = True
            detection_result["packer_type"] = detected_packer
            detection_result["confidence"] = min(max_confidence, 1.0)
        
        return detection_result
    
    def generate_report(self, detection_result: Dict) -> str:
        """生成检测报告"""
        report = []
        report.append("=" * 60)
        report.append("APK加固检测报告")
        report.append("=" * 60)
        
        if "error" in detection_result:
            report.append(f"错误: {detection_result['error']}")
            return "\n".join(report)
        
        # 显示检测来源
        if "package_name" in detection_result:
            report.append(f"应用包名: {detection_result['package_name']}")
            if detection_result.get('device_id'):
                report.append(f"设备ID: {detection_result['device_id']}")
            report.append(f"检测方式: 设备提取")
        else:
            report.append(f"APK路径: {detection_result['apk_path']}")
            report.append(f"检测方式: 本地文件")
        
        report.append(f"文件总数: {detection_result['total_files']}")
        report.append(f"DEX文件: {detection_result['dex_files']}")
        report.append(f"原生库: {detection_result['native_libs']}")
        
        report.append("-" * 40)
        
        if detection_result["is_packed"]:
            report.append("🔴 检测结果: 该APK已被加固")
            report.append(f"加固类型: {detection_result['packer_type']}")
            report.append(f"置信度: {detection_result['confidence']:.2%}")
            
            if detection_result["indicators"]:
                report.append("检测到的特征:")
                for indicator in detection_result["indicators"]:
                    report.append(f"  • {indicator}")
        else:
            report.append("🟢 检测结果: 该APK未被加固")
            report.append("未发现明显的加固特征")
        
        # 显示可疑文件
        if detection_result["file_analysis"]["suspicious_files"]:
            report.append("-" * 40)
            report.append("可疑文件:")
            for file in detection_result["file_analysis"]["suspicious_files"][:10]:
                report.append(f"  • {file}")
        
        report.append("=" * 60)
        return "\n".join(report)


def main():
    """主函数"""
    import sys
    
    if len(sys.argv) < 2 or sys.argv[1] in ['-h', '--help']:
        print("APK加固检测工具")
        print("=" * 60)
        print("用法:")
        print("  1. 检测本地APK文件:")
        print("     python apk_packer_detector.py <apk文件路径>")
        print("  2. 检测设备上已安装的应用:")
        print("     python apk_packer_detector.py --device <包名> [设备ID]")
        print("\n示例:")
        print("  检测本地APK: python apk_packer_detector.py app.apk")
        print("  检测设备应用: python apk_packer_detector.py --device com.example.app")
        print("  指定设备检测: python apk_packer_detector.py --device com.example.app emulator-5554")
        print("\n支持的加固厂商:")
        print("  • 360加固 • 腾讯加固 • 爱加密 • 梆梆加固")
        print("  • 娜迦加固 • 通付盾 • 阿里聚安全")
        sys.exit(1)
    
    detector = APKPackerDetector()
    
    if sys.argv[1] == '--device':
        # 设备检测模式
        if len(sys.argv) < 3:
            print("错误: 请提供应用包名")
            sys.exit(1)
        
        package_name = sys.argv[2]
        device_id = sys.argv[3] if len(sys.argv) > 3 else None
        
        print(f"正在从设备{' ' + device_id if device_id else ''}提取应用 {package_name}...")
        result = detector.detect_packer_from_device(package_name, device_id)
    else:
        # 本地APK检测模式
        apk_path = sys.argv[1]
        print(f"正在分析APK文件: {apk_path}")
        result = detector.detect_packer(apk_path)
    
    report = detector.generate_report(result)
    print(report)
    
    # 保存详细结果到JSON文件
    if "error" not in result:
        if "package_name" in result:
            output_file = f"{result['package_name']}_packer_analysis.json"
        else:
            output_file = f"{Path(result['apk_path']).stem}_packer_analysis.json"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        
        print(f"\n详细分析结果已保存到: {output_file}")


if __name__ == "__main__":
    main()