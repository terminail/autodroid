"""配置管理模块 - 统一管理所有配置项"""

import os
import yaml
from pathlib import Path
from typing import Dict, Any, Optional, List


class ConfigManager:
    """配置管理器"""
    
    _instance = None
    _config = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(ConfigManager, cls).__new__(cls)
        return cls._instance
    
    def __init__(self):
        if self._config is None:
            self._config = self._load_config()
    
    def _load_config(self) -> Dict[str, Any]:
        """加载配置文件"""
        # 使用项目根目录的主配置文件
        config_path = Path(__file__).parent / "config.yaml"
        
        if not config_path.exists():
            # 如果主配置文件不存在，使用默认配置
            print(f"⚠ 主配置文件不存在: {config_path}，使用默认配置")
            return self._get_default_config()
        
        try:
            with open(config_path, 'r', encoding='utf-8') as f:
                config = yaml.safe_load(f)
                return self._validate_config(config)
        except Exception as e:
            print(f"❌ 配置文件加载失败: {e}")
            return self._get_default_config()
    
    def _save_config(self, config: Dict[str, Any]):
        """保存配置文件到主配置文件"""
        config_path = Path(__file__).parent / "config.yaml"
        try:
            with open(config_path, 'w', encoding='utf-8') as f:
                yaml.dump(config, f, default_flow_style=False, allow_unicode=True, indent=2)
            print(f"✅ 配置已保存到主配置文件: {config_path}")
        except Exception as e:
            print(f"❌ 配置文件保存失败: {e}")
    
    def _get_default_config(self) -> Dict[str, Any]:
        """获取默认配置"""
        return {
            'device': {
                'id': 'emulator-5554'
            },
            'app': {
                'package_name': 'com.autodroid.manager'
            },
            'output': {
                'screenshots_dir': 'analysis_output/screenshots',
                'reports_dir': 'analysis_output/reports',
                'ui_hierarchy_dir': 'analysis_output/ui_hierarchy',
                'database_path': 'analysis_output/analyzer.db'
            },
            'analysis': {
                'max_depth': 5,
                'enable_monitoring': True,
                'screenshot_interval': 2.0,
                'page_change_threshold': 0.8,
                'multimodal_modes': {
                    'uiautomator2': True,
                    'screenshot': True,
                    'user_monitoring': True
                }
            },
            'database': {
                'auto_create_tables': True,
                'enable_foreign_keys': True,
                'journal_mode': 'WAL',
                'synchronous': 'NORMAL'
            },
            'logging': {
                'level': 'INFO',
                'format': '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
                'file': 'analysis_output/analysis.log'
            },
            'ui': {
                'show_element_importance': True,
                'max_elements_display': 10,
                'enable_color_output': True,
                'interactive_mode': True
            }
        }
    
    def _validate_config(self, config: Dict[str, Any]) -> Dict[str, Any]:
        """验证配置项"""
        default_config = self._get_default_config()
        
        # 递归合并配置，确保所有必需字段都存在
        def merge_dicts(default: Dict, provided: Dict) -> Dict:
            result = default.copy()
            for key, value in provided.items():
                if key in result and isinstance(result[key], dict) and isinstance(value, dict):
                    result[key] = merge_dicts(result[key], value)
                else:
                    result[key] = value
            return result
        
        return merge_dicts(default_config, config)
    
    def get(self, key: str, default: Any = None) -> Any:
        """获取配置项"""
        keys = key.split('.')
        value = self._config
        
        for k in keys:
            if isinstance(value, dict) and k in value:
                value = value[k]
            else:
                return default
        
        return value
    
    def set(self, key: str, value: Any):
        """设置配置项"""
        keys = key.split('.')
        config = self._config
        
        # 遍历到最后一个键
        for k in keys[:-1]:
            if k not in config:
                config[k] = {}
            config = config[k]
        
        # 设置值
        config[keys[-1]] = value
        
        # 保存到文件
        self._save_config(self._config)
    

    
    # 应用包名配置已移除，应通过参数传递而不是配置文件设置
    
    def get_output_dirs(self) -> Dict[str, Path]:
        """获取输出目录"""
        base_dir = Path(__file__).parent
        return {
            'screenshots': base_dir / self.get('output.screenshots_dir'),
            'reports': base_dir / self.get('output.reports_dir'),
            'ui_hierarchy': base_dir / self.get('output.ui_hierarchy_dir'),
            'database': base_dir / self.get('output.database_path')
        }
    
    def get_analysis_config(self) -> Dict[str, Any]:
        """获取分析配置"""
        return {
            'max_depth': self.get('analysis.max_depth', 5),
            'enable_monitoring': self.get('analysis.enable_monitoring', True),
            'screenshot_interval': self.get('analysis.screenshot_interval', 2.0),
            'page_change_threshold': self.get('analysis.page_change_threshold', 0.8),
            'multimodal_modes': self.get('analysis.multimodal_modes', {})
        }
    
    def get_database_config(self) -> Dict[str, Any]:
        """获取数据库配置"""
        return {
            'auto_create_tables': self.get('database.auto_create_tables', True),
            'enable_foreign_keys': self.get('database.enable_foreign_keys', True),
            'journal_mode': self.get('database.journal_mode', 'WAL'),
            'synchronous': self.get('database.synchronous', 'NORMAL')
        }
    
    def get_database_path(self) -> str:
        """获取数据库文件路径"""
        output_dirs = self.get_output_dirs()
        return str(output_dirs['database'])
    
    def get_logging_config(self) -> Dict[str, Any]:
        """获取日志配置"""
        return {
            'level': self.get('logging.level', 'INFO'),
            'format': self.get('logging.format', '%(asctime)s - %(name)s - %(levelname)s - %(message)s'),
            'file': self.get('logging.file', 'analysis_output/analysis.log')
        }
    
    def get_ui_config(self) -> Dict[str, Any]:
        """获取UI配置"""
        return {
            'show_element_importance': self.get('ui.show_element_importance', True),
            'max_elements_display': self.get('ui.max_elements_display', 10),
            'enable_color_output': self.get('ui.enable_color_output', True),
            'interactive_mode': self.get('ui.interactive_mode', True)
        }
    
    def get_connected_devices(self) -> List[str]:
        """获取所有连接的设备ID"""
        import subprocess
        try:
            result = subprocess.run(["adb", "devices"], capture_output=True, text=True)
            if result.returncode != 0:
                print(f"❌ 获取设备列表失败: {result.stderr}")
                return []
            
            devices = []
            for line in result.stdout.strip().split('\n')[1:]:  # 跳过第一行标题
                if line.strip() and '\tdevice' in line:
                    device_id = line.split('\t')[0].strip()
                    if device_id:
                        devices.append(device_id)
            
            print(f"📱 检测到 {len(devices)} 个连接的设备: {devices}")
            return devices
        except Exception as e:
            print(f"❌ 获取设备列表失败: {e}")
            return []
    
    def get_output_file(self) -> str:
        """获取输出文件路径"""
        return str(Path(__file__).parent / self.get('output.reports_dir', 'analysis_output/reports') / 'apk_list.json')
    
    def should_export(self) -> bool:
        """是否导出到文件"""
        return self.get('output.reports_dir') is not None
    
    def should_save_basic_info(self) -> bool:
        """是否保存APK基础信息到数据库"""
        return self.get('apk.save_basic_info', True)
    
    def ensure_directories(self):
        """确保所有输出目录存在"""
        output_dirs = self.get_output_dirs()
        
        for dir_type, dir_path in output_dirs.items():
            if dir_type != 'database':  # 数据库文件路径，不是目录
                dir_path.parent.mkdir(parents=True, exist_ok=True)
            else:
                dir_path.parent.mkdir(parents=True, exist_ok=True)
        
        print("✅ 输出目录已确保存在")
    
    def reload(self):
        """重新加载配置"""
        self._config = self._load_config()
        print("✅ 配置已重新加载")
    
    def get_all_config(self) -> Dict[str, Any]:
        """获取所有配置"""
        return self._config.copy()
    
    def reset_to_default(self):
        """重置为默认配置"""
        self._config = self._get_default_config()
        self._save_config(self._config)
        print("✅ 配置已重置为默认值")