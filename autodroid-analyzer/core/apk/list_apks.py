"""从连接的devices上获取安装的用户apk信息包括基本信息及加固信息"""

import subprocess
import json
import sqlite3
import yaml
from pathlib import Path
from typing import Dict, List, Optional, Any
from datetime import datetime
import sys
import os
import shutil

# 添加项目根目录路径
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..'))
from config import ConfigManager

# 导入APK模块的数据库和服务类
from .database import ApkDatabase
from .service import ApkManager

# 导入统一的APK信息模型
from .models import ApkInfo


class ApkLister:
    """APK列表器类"""
    
    def __init__(self, config_manager: ConfigManager):
        self.config_manager = config_manager
        self.apk_database = ApkDatabase()
        self.apk_manager = ApkManager()
        self.apk_list: List[ApkInfo] = []
        self.device_id: Optional[str] = None
    
    def list_installed_apks(self, device_id: str, user_only: bool = True) -> List[ApkInfo]:
        """列出设备上安装的APK应用"""
        if not device_id:
            print("❌ 设备ID为空")
            return []
        
        print(f"📱 正在连接设备: {device_id}")
        
        # 检查设备连接状态
        if not self._check_device_connection(device_id):
            print("❌ 设备连接失败，请检查设备连接状态")
            print("💡 请确保设备已连接并启用USB调试，然后重新运行程序")
            return []
        
        # 获取APK列表
        apk_list = self._get_apk_list(device_id, user_only)
        print(f"✅ 成功获取 {len(apk_list)} 个APK应用信息")
        
        # 保存到实例变量中
        self.apk_list = apk_list
        
        return apk_list
    
    def _check_device_connection(self, device_id: str) -> bool:
        """检查设备连接状态"""
        try:
            result = subprocess.run(["adb", "-s", device_id, "shell", "echo", "connected"], 
                                  capture_output=True, text=True)
            return result.returncode == 0
        except Exception as e:
            print(f"❌ 检查设备连接失败: {e}")
            return False
    
    def _get_apk_list(self, device_id: str, user_only: bool = True) -> List[ApkInfo]:
        """获取APK列表"""
        try:
            self.device_id = device_id
            
            # 构建ADB命令
            command = ["adb", "-s", device_id, "shell", "pm", "list", "packages", "-f"]
            if user_only:
                command.append("-3")  # 只显示用户安装的应用
            
            result = subprocess.run(command, capture_output=True, text=True)
            if result.returncode != 0:
                print(f"❌ 获取APK列表失败: {result.stderr}")
                return []
            
            apk_list = []
            for line in result.stdout.strip().split('\n'):
                if line.startswith("package:"):
                    apk_info = self._parse_package_line(line)
                    if apk_info:
                        apk_list.append(apk_info)
            
            return apk_list
            
        except Exception as e:
            print(f"❌ 获取APK列表失败: {e}")
            return []
    
    def _parse_package_line(self, line: str) -> Optional[ApkInfo]:
        """解析包信息行"""
        try:
            # 格式: package:/path/to/app.apk=package.name
            print(f"🔧 解析包信息行: {line}")
            
            # 正确的解析方法：只分割最后一个等号
            if '=' not in line:
                print(f"❌ 包信息行格式错误: {line}")
                return None
            
            # 找到最后一个等号的位置
            last_equal_index = line.rfind('=')
            file_part = line[:last_equal_index].replace("package:", "")
            package_name = line[last_equal_index + 1:]
            
            print(f"✅ 解析成功 - 包名: {package_name}, 文件路径: {file_part}")
            
            # 获取应用详细信息
            app_info = self._get_app_info(package_name)
            if not app_info:
                print(f"❌ 获取应用信息失败: {package_name}")
                return None
            
            # 判断是否为系统应用
            # 系统应用通常安装在/system目录下，用户应用安装在/data目录下
            is_system_app = "/system/" in file_part or "/vendor/" in file_part or "/product/" in file_part
            
            # 创建APK信息对象
            # 转换时间格式为datetime对象
            install_time = None
            if app_info.get('install_time') and app_info['install_time'] != '未知':
                try:
                    install_time = datetime.strptime(app_info['install_time'], '%Y-%m-%d %H:%M:%S')
                except:
                    install_time = None
            
            # 转换版本代码为整数
            version_code = None
            if app_info.get('version_code') and app_info['version_code'].isdigit():
                version_code = int(app_info['version_code'])
            
            apk_info = ApkInfo(
                id=package_name,  # 使用包名作为ID
                app_name=app_info.get('app_name', package_name),
                version_name=app_info.get('version_name'),
                version_code=version_code,
                install_time=install_time,
                is_packed=is_system_app  # 临时使用is_system_app作为is_packed的占位符
            )
            
            return apk_info
            
        except Exception as e:
            print(f"❌ 解析包信息失败: {e}")
            return None
    
    def _get_app_info(self, package_name: str) -> Optional[Dict[str, Any]]:
        """获取应用详细信息"""
        try:
            print(f"🔍 获取应用 {package_name} 的详细信息...")
            app_info = {}
            
            # 调试信息：开始获取应用信息
            print(f"🔧 开始处理应用: {package_name}")
            
            # 获取应用名称
            name_command = ["adb"]
            if self.device_id:
                name_command.extend(["-s", self.device_id])
            name_command.extend(["shell", "dumpsys", "package", package_name, "|", "grep", "application-label"])
            
            result = subprocess.run(" ".join(name_command), shell=True, capture_output=True, text=True)
            if result.returncode == 0:
                app_info['app_name'] = result.stdout.strip().replace("application-label:", "").strip()
            else:
                app_info['app_name'] = package_name
            
            # 获取版本信息
            version_command = ["adb"]
            if self.device_id:
                version_command.extend(["-s", self.device_id])
            version_command.extend(["shell", "dumpsys", "package", package_name, "|", "grep", "version"])
            
            result = subprocess.run(" ".join(version_command), shell=True, capture_output=True, text=True)
            if result.returncode == 0:
                for line in result.stdout.split('\n'):
                    if "versionName" in line:
                        app_info['version_name'] = line.split('=')[1].strip()
                    elif "versionCode" in line:
                        app_info['version_code'] = line.split('=')[1].strip()
            
            # 获取安装时间
            install_command = ["adb"]
            if self.device_id:
                install_command.extend(["-s", self.device_id])
            install_command.extend(["shell", "dumpsys", "package", package_name, "|", "grep", "firstInstallTime"])
            
            result = subprocess.run(" ".join(install_command), shell=True, capture_output=True, text=True)
            if result.returncode == 0:
                install_time = result.stdout.strip().replace("firstInstallTime=", "")
                app_info['install_time'] = self._format_timestamp(install_time)
            
            # 获取更新时间
            update_command = ["adb"]
            if self.device_id:
                update_command.extend(["-s", self.device_id])
            update_command.extend(["shell", "dumpsys", "package", package_name, "|", "grep", "lastUpdateTime"])
            
            result = subprocess.run(" ".join(update_command), shell=True, capture_output=True, text=True)
            if result.returncode == 0:
                update_time = result.stdout.strip().replace("lastUpdateTime=", "")
                app_info['update_time'] = self._format_timestamp(update_time)
            
            # 系统应用判断已经在_parse_package_line方法中处理
            
            # 获取文件大小
            file_command = ["adb"]
            if self.device_id:
                file_command.extend(["-s", self.device_id])
            file_command.extend(["shell", "ls", "-l", "/data/app/*.apk", "|", "grep", package_name])
            
            result = subprocess.run(" ".join(file_command), shell=True, capture_output=True, text=True)
            if result.returncode == 0:
                # 解析文件大小
                file_info = result.stdout.strip().split()
                if len(file_info) >= 5:
                    app_info['file_size'] = int(file_info[4])
            
            return app_info
            
        except Exception as e:
            print(f"❌ 获取应用信息失败: {e}")
            return None
    
    def _format_timestamp(self, timestamp_str: str) -> str:
        """格式化时间戳"""
        try:
            if timestamp_str.isdigit():
                import datetime
                timestamp = int(timestamp_str) / 1000  # 转换为秒
                return datetime.datetime.fromtimestamp(timestamp).strftime('%Y-%m-%d %H:%M:%S')
            return timestamp_str
        except:
            return timestamp_str
    
    def filter_apks_by_keyword(self, keyword: str, device_id: str = None) -> List[ApkInfo]:
        """根据关键词过滤APK应用"""
        try:
            if not self.apk_list:
                if not device_id:
                    print("❌ 需要提供设备ID")
                    return []
                self.list_installed_apks(device_id)
            
            filtered_apks = []
            for apk in self.apk_list:
                if (keyword.lower() in apk.id.lower() or  # id字段对应package_name
                    keyword.lower() in apk.app_name.lower()):
                    filtered_apks.append(apk)
            
            return filtered_apks
            
        except Exception as e:
            print(f"❌ 过滤APK应用失败: {e}")
            return []
    
    def get_system_apks(self, device_id: str = None) -> List[ApkInfo]:
        """获取系统应用"""
        try:
            if not self.apk_list:
                if not device_id:
                    print("❌ 需要提供设备ID")
                    return []
                self.list_installed_apks(device_id, user_only=False)
            
            # 注意：现在使用is_packed字段临时存储系统应用标识
            system_apks = [apk for apk in self.apk_list if apk.is_packed]
            return system_apks
            
        except Exception as e:
            print(f"❌ 获取系统应用失败: {e}")
            return []
    
    def get_user_apks(self, device_id: str = None) -> List[ApkInfo]:
        """获取用户应用"""
        try:
            if not self.apk_list:
                if not device_id:
                    print("❌ 需要提供设备ID")
                    return []
                self.list_installed_apks(device_id)
            
            # 注意：现在使用is_packed字段临时存储系统应用标识
            user_apks = [apk for apk in self.apk_list if not apk.is_packed]
            return user_apks
            
        except Exception as e:
            print(f"❌ 获取用户应用失败: {e}")
            return []
    
    def export_apk_list(self, output_file: str, device_id: str = None) -> bool:
        """导出APK列表到文件"""
        try:
            if not self.apk_list:
                if not device_id:
                    print("❌ 需要提供设备ID")
                    return False
                print("❌ 未找到APK列表，请先调用list_installed_apks方法获取APK列表")
                return False
            
            export_data = {
                "total_apks": len(self.apk_list),
                "system_apks": len(self.get_system_apks(device_id)),
                "user_apks": len(self.get_user_apks(device_id)),
                "apk_list": []
            }
            
            for apk in self.apk_list:
                export_data["apk_list"].append({
                    "id": apk.id,
                    "app_name": apk.app_name,
                    "version_name": apk.version_name,
                    "version_code": apk.version_code,
                    "install_time": apk.install_time.isoformat() if apk.install_time else None,
                    "is_packed": apk.is_packed,
                    "packer_type": apk.packer_type,
                    "packer_confidence": apk.packer_confidence
                })
            
            # 保存到文件
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(export_data, f, ensure_ascii=False, indent=2)
            
            print(f"💾 APK列表已导出到: {output_file}")
            return True
            
        except Exception as e:
            print(f"❌ 导出APK列表失败: {e}")
            return False
    
    def print_apk_list(self, show_details: bool = False, device_id: str = None):
        """打印APK列表"""
        try:
            if not self.apk_list:
                if not device_id:
                    print("❌ 需要提供设备ID")
                    return
                print("❌ 未找到APK列表，请先调用list_installed_apks方法获取APK列表")
                return
            
            print(f"\n📱 设备上的应用列表 (共 {len(self.apk_list)} 个应用)")
            print("=" * 80)
            
            for i, apk in enumerate(self.apk_list, 1):
                print(f"{i:2d}. {apk.app_name}")
                print(f"     包名: {apk.id}")
                
                if show_details:
                    print(f"     版本: {apk.version_name} (代码: {apk.version_code})")
                    print(f"     安装时间: {apk.install_time}")
                    print(f"     是否被加固: {'是' if apk.is_packed else '否'}")
                    if apk.is_packed:
                        print(f"     加固类型: {apk.packer_type}")
                        print(f"     置信度: {apk.packer_confidence:.2f}")
                
                print()
            
        except Exception as e:
            print(f"❌ 打印APK列表失败: {e}")
    
    def _format_file_size(self, size_bytes: int) -> str:
        """格式化文件大小"""
        try:
            if size_bytes == 0:
                return "0 B"
            
            size_names = ["B", "KB", "MB", "GB"]
            i = 0
            while size_bytes >= 1024 and i < len(size_names) - 1:
                size_bytes /= 1024.0
                i += 1
            
            return f"{size_bytes:.2f} {size_names[i]}"
        except:
            return "未知"
    
    def extract_apk_from_device(self, package_name: str) -> Optional[str]:
        """从设备提取APK文件到本地"""
        try:
            print(f"🔍 开始从设备提取APK: {package_name}")
            
            # 获取APK文件路径
            path_command = ["adb"]
            if self.device_id:
                path_command.extend(["-s", self.device_id])
            path_command.extend(["shell", "pm", "path", package_name])
            
            result = subprocess.run(path_command, capture_output=True, text=True)
            if result.returncode != 0:
                print(f"❌ 获取APK路径失败: {result.stderr}")
                return None
            
            # 解析APK路径
            apk_path = result.stdout.strip().replace("package:", "")
            if not apk_path:
                print(f"❌ 未找到APK路径: {package_name}")
                return None
            
            # 创建本地临时目录
            temp_dir = Path("temp_apks")
            temp_dir.mkdir(exist_ok=True)
            
            # 本地保存路径
            local_apk_path = temp_dir / f"{package_name}.apk"
            
            # 从设备提取APK
            pull_command = ["adb"]
            if self.device_id:
                pull_command.extend(["-s", self.device_id])
            pull_command.extend(["pull", apk_path, str(local_apk_path)])
            
            result = subprocess.run(pull_command, capture_output=True, text=True)
            if result.returncode != 0:
                print(f"❌ 提取APK失败: {result.stderr}")
                return None
            
            print(f"✅ 已提取APK到本地: {local_apk_path}")
            return str(local_apk_path)
            
        except Exception as e:
            print(f"❌ 提取APK失败: {e}")
            return None
    
    def cleanup_temp_apks(self):
        """清理临时APK文件目录"""
        try:
            temp_dir = Path("temp_apks")
            if temp_dir.exists():
                shutil.rmtree(temp_dir)
                print("🧹 已清理临时APK文件目录")
            else:
                print("ℹ️  临时APK文件目录不存在，无需清理")
        except Exception as e:
            print(f"⚠️  清理临时目录失败: {e}")
    
    def analyze_packer_for_apk(self, apk_id: str, package_name: str) -> bool:
        """分析APK加固状态"""
        apk_file_path = None
        try:
            print(f"🔍 开始分析APK加固状态: {package_name}")
            
            # 从设备提取APK
            apk_file_path = self.extract_apk_from_device(package_name)
            if not apk_file_path:
                print(f"❌ 无法提取APK文件: {package_name}")
                return False
            
            # 使用APK模块的服务类进行加固检测
            from .service import ApkManager
            apk_manager = ApkManager()
            
            # 使用文件路径进行加固检测
            from .models import PackerDetectionRequest
            request = PackerDetectionRequest(apk_path=apk_file_path)
            result = apk_manager.detect_packer(request)
            
            if result and not result.error:
                print(f"✅ 加固检测完成: {package_name}")
                print(f"   是否被加固: {'是' if result.is_packed else '否'}")
                print(f"   加固类型: {result.packer_type or '无'}")
                print(f"   置信度: {result.confidence or 0:.2f}")
                
                # 保存检测结果到数据库
                from .database import ApkDatabase
                apk_db = ApkDatabase()
                detection_result = {
                    'is_packed': result.is_packed,
                    'packer_type': result.packer_type,
                    'confidence': result.confidence
                }
                apk_db.save_packer_detection_result(package_name, detection_result)
                
                return True
            else:
                error_msg = result.error if result else "未知错误"
                print(f"❌ 加固检测失败: {package_name}, 错误: {error_msg}")
                return False
                
        except Exception as e:
            print(f"❌ 加固分析失败: {e}")
            return False
        finally:
            # 无论成功与否，都清理临时APK文件
            if apk_file_path:
                try:
                    apk_path = Path(apk_file_path)
                    if apk_path.exists():
                        apk_path.unlink()
                        print(f"🧹 已清理临时APK文件: {apk_file_path}")
                except Exception as e:
                    print(f"⚠️  清理临时APK文件失败: {e}")
    
    def analyze_apk_packers(self, device_id: str = None) -> Dict[str, Any]:
        """分析所有APK的加固状态"""
        try:
            if not self.apk_list:
                if not device_id:
                    print("❌ 需要提供设备ID")
                    return {"error": "需要提供设备ID"}
                print("❌ 未找到APK列表，请先调用list_installed_apks方法获取APK列表")
                return {"error": "未找到APK列表"}
            
            print(f"🔍 开始分析所有APK的加固状态...")
            
            # 分析每个APK的加固状态
            analysis_results = {}
            analyzed_count = 0
            
            for apk in self.apk_list:
                try:
                    print(f"\n📱 正在分析APK: {apk.app_name} ({apk.id})")
                    
                    # 分析加固状态
                    success = self.analyze_packer_for_apk(apk.id, apk.id)
                    
                    if success:
                        analyzed_count += 1
                        analysis_results[apk.id] = {"status": "success"}
                    else:
                        analysis_results[apk.id] = {"status": "failed", "error": "加固检测失败"}
                        
                except Exception as e:
                    print(f"❌ 分析APK {apk.id} 时发生错误: {e}")
                    analysis_results[apk.id] = {"status": "error", "error": str(e)}
            
            print(f"✅ 加固分析完成，共分析 {analyzed_count}/{len(self.apk_list)} 个APK")
            
            # 批量分析完成后清理整个临时目录
            self.cleanup_temp_apks()
            
            return {
                "total_apks": len(self.apk_list),
                "analyzed_count": analyzed_count,
                "results": analysis_results
            }
            
        except Exception as e:
            print(f"❌ 批量加固分析失败: {e}")
            # 即使失败也要清理临时目录
            self.cleanup_temp_apks()
            return {"error": str(e)}
    
    def save_apks_to_database(self, device_id: str = None) -> bool:
        """将APK信息保存到数据库"""
        try:
            if not self.apk_list:
                if not device_id:
                    print("❌ 需要提供设备ID")
                    return False
                print("❌ 未找到APK列表，请先调用list_installed_apks方法获取APK列表")
                return False
            
            # 使用APK模块的数据库类
            from .database import ApkDatabase
            apk_db = ApkDatabase()
            
            print(f"💾 开始保存APK信息到数据库")
            
            # 保存每个APK信息
            saved_count = 0
            for apk in self.apk_list:
                try:
                    # 准备APK数据
                    apk_data = {
                        'id': apk.id,  # 使用id字段作为主键
                        'package_name': apk.id,  # 兼容package_name字段
                        'app_name': apk.app_name,
                        'version_name': apk.version_name,
                        'version_code': apk.version_code,
                        'install_time': apk.install_time,
                        'is_packed': apk.is_packed or False  # 确保有默认值
                    }
                    
                    # 使用ApkDatabase的register_apk方法
                    registered_apk = apk_db.register_apk(apk_data)
                    
                    if registered_apk:
                        saved_count += 1
                        print(f"✅ 已保存APK: {apk.app_name} ({apk.id})")
                    else:
                        print(f"❌ 保存APK失败: {apk.id}")
                        
                except Exception as e:
                    print(f"❌ 保存APK {apk.id} 失败: {e}")
            
            print(f"💾 已成功保存 {saved_count}/{len(self.apk_list)} 个APK信息到数据库")
            return saved_count > 0
            
        except Exception as e:
            print(f"❌ 保存APK信息到数据库失败: {e}")
            return False


def main():
    """主函数"""
    try:
        # 创建配置管理器
        config_manager = ConfigManager()
        
        # 获取连接的设备列表
        connected_devices = config_manager.get_connected_devices()
        if not connected_devices:
            print("❌ 未检测到连接的设备，请确保设备已连接并启用USB调试")
            return
        
        # 从配置中获取参数
        db_path = config_manager.get_database_path()
        output_file = config_manager.get_output_file()
        should_export = config_manager.should_export()
        
        # 从配置中获取检测模式设置
        save_basic_info = config_manager.should_save_basic_info()
        
        print("🚀 开始执行APK列表工具...")
        print(f"📱 检测到 {len(connected_devices)} 个连接的设备: {connected_devices}")
        print(f"💾 数据库路径: {db_path}")
        print(f"📄 输出文件: {output_file}")
        print(f"💾 APK基础信息保存: {'开启' if save_basic_info else '关闭'}")
        
        # 处理每个连接的设备
        for device_id in connected_devices:
            print(f"\n📱 正在处理设备: {device_id}")
            
            # 创建APK列表器
            apk_lister = ApkLister(config_manager)
            
            # 列出APK应用
            print("📋 开始列出已安装的APK应用...")
            apk_list = apk_lister.list_installed_apks(device_id, user_only=True)
            
            if not apk_list:
                print(f"❌ 设备 {device_id} 上未找到用户APK应用")
                continue
            
            # 打印应用列表
            if save_basic_info:
                print("📄 开始打印应用列表...")
                apk_lister.print_apk_list(show_details=True, device_id=device_id)
            
            # 保存到数据库（如果需要）
            if save_basic_info:
                success = apk_lister.save_apks_to_database(device_id=device_id)
                if success:
                    print(f"✅ APK信息已保存到数据库: {db_path}")
            
            # 导出到文件
            if should_export and save_basic_info:
                # 确保输出目录存在
                Path(output_file).parent.mkdir(parents=True, exist_ok=True)
                apk_lister.export_apk_list(output_file, device_id=device_id)
                print(f"✅ APK列表已导出到: {output_file}")
            
            if save_basic_info:
                print("\n📊 统计信息:")
                print(f"   用户安装应用数: {len(apk_lister.apk_list)}")
                print(f"   系统应用: 0 (已过滤)")
                print(f"   用户应用: {len(apk_lister.apk_list)}")
        
    except Exception as e:
        print(f"❌ 主函数执行失败: {e}")


if __name__ == "__main__":
    main()