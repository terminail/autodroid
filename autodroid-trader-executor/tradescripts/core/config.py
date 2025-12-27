import yaml
from pathlib import Path


def load_config():
    """Load configuration from config.yaml"""
    config_path = Path(__file__).parent.parent / "config.yaml"
    try:
        with open(config_path, 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        print(f"✓ Configuration loaded from {config_path}")
        return config
    except FileNotFoundError:
        print(f"⚠ Config file not found at {config_path}, using defaults")
        return {}
    except Exception as e:
        print(f"⚠ Error loading config: {e}, using defaults")
        return {}


def get_apks_path():
    """Get the apks directory path from configuration with fallbacks"""
    config = load_config()
    
    # Get the apks directory path from config, with fallback to default
    apks_config = config.get('apks', {})
    apks_path_str = apks_config.get('path', 'app/src/main/assets/apks')
    apks_path = Path(apks_path_str)
    
    # If path is not absolute, make it relative to the project
    if not apks_path.is_absolute():
        # First try relative to the tradescripts directory
        current_dir = Path(__file__).parent.parent  # This gets us to the core directory
        apks_path = current_dir.parent / apks_path_str
        
        # If not found, try relative to parent directory (executor root)
        if not apks_path.exists():
            apks_path = current_dir.parent.parent / apks_path_str
        
        # If still not found, try relative to current working directory
        if not apks_path.exists():
            apks_path = Path(apks_path_str)
    
    return apks_path.resolve()


def get_server_config():
    """Get server configuration with defaults"""
    config = load_config()
    
    server_config = config.get('server', {})
    backend_config = server_config.get('backend', {})
    trader_server_config = config.get('trader-server', {})
    
    trader_server_api_endpoint = trader_server_config.get('api_endpoint', 'http://localhost:8008/api')
    
    if not trader_server_api_endpoint.endswith('/api'):
        trader_server_api_endpoint = f"{trader_server_api_endpoint}/api".replace('//api', '/api')
    
    return {
        'host': backend_config.get('host', '0.0.0.0'),
        'port': server_config.get('port', 8018),
        'api_base': server_config.get('api_base', '/api'),
        'poll_interval': backend_config.get('poll_interval', 5),
        'trader_server_api_endpoint': trader_server_api_endpoint,
        'log_level': config.get('logging', {}).get('level', 'info'),
        'reload': backend_config.get('reload', False)
    }