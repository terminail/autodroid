"""
模型配置模块
提供 PhoneAgent 所需的 ModelConfig 类
"""

from dataclasses import dataclass
from typing import Optional, Dict, Any

@dataclass
class ModelConfig:
    """模型配置类，兼容 PhoneAgent 的需求"""
    base_url: str = "http://localhost:8000/v1"
    api_key: str = "EMPTY"
    model_name: str = "autoglm-phone-9b"
    max_tokens: int = 3000
    temperature: float = 0.1
    
    def __post_init__(self):
        """初始化后处理"""
        # 确保 base_url 格式正确
        if not self.base_url.startswith('http'):
            self.base_url = f"http://{self.base_url}"
        
        # 确保 base_url 以 /v1 结尾
        if not self.base_url.endswith('/v1'):
            if self.base_url.endswith('/'):
                self.base_url = f"{self.base_url}v1"
            else:
                self.base_url = f"{self.base_url}/v1"
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            'base_url': self.base_url,
            'api_key': self.api_key,
            'model_name': self.model_name,
            'max_tokens': self.max_tokens,
            'temperature': self.temperature
        }
    
    @classmethod
    def from_dict(cls, config_dict: Dict[str, Any]) -> 'ModelConfig':
        """从字典创建实例"""
        return cls(
            base_url=config_dict.get('base_url', 'http://localhost:8000/v1'),
            api_key=config_dict.get('api_key', 'EMPTY'),
            model_name=config_dict.get('model_name', 'autoglm-phone-9b'),
            max_tokens=config_dict.get('max_tokens', 3000),
            temperature=config_dict.get('temperature', 0.1)
        )

@dataclass 
class AgentConfig:
    """代理配置类"""
    max_steps: int = 50
    device_id: Optional[str] = None
    verbose: bool = True
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            'max_steps': self.max_steps,
            'device_id': self.device_id,
            'verbose': self.verbose
        }
    
    @classmethod
    def from_dict(cls, config_dict: Dict[str, Any]) -> 'AgentConfig':
        """从字典创建实例"""
        return cls(
            max_steps=config_dict.get('max_steps', 50),
            device_id=config_dict.get('device_id'),
            verbose=config_dict.get('verbose', True)
        )

# 创建配置管理器
class ConfigManager:
    """配置管理器"""
    
    def __init__(self):
        self.model_config = None
        self.agent_config = None
    
    def load_from_workplan(self, workplan: Dict[str, Any]) -> bool:
        """从工作配置加载配置"""
        try:
            autoglm_config = workplan.get('autoglm_config', {})
            
            # 加载模型配置
            model_config_dict = autoglm_config.get('model_config', {})
            self.model_config = ModelConfig.from_dict(model_config_dict)
            
            # 加载代理配置
            agent_config_dict = autoglm_config.get('agent_config', {})
            self.agent_config = AgentConfig.from_dict(agent_config_dict)
            
            return True
            
        except Exception as e:
            print(f"加载配置失败: {e}")
            return False
    
    def get_model_config(self) -> ModelConfig:
        """获取模型配置"""
        if self.model_config is None:
            self.model_config = ModelConfig()
        return self.model_config
    
    def get_agent_config(self) -> AgentConfig:
        """获取代理配置"""
        if self.agent_config is None:
            self.agent_config = AgentConfig()
        return self.agent_config
    
    def update_device_id(self, device_id: str):
        """更新设备ID"""
        if self.agent_config:
            self.agent_config.device_id = device_id
        else:
            self.agent_config = AgentConfig(device_id=device_id)

# 全局配置管理器实例
config_manager = ConfigManager()

def get_config_manager() -> ConfigManager:
    """获取全局配置管理器"""
    return config_manager