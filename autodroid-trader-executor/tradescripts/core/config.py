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
    
    apks_config = config.get('apks', {})
    apks_path_str = apks_config.get('path', 'app/src/main/assets/apks')
    apks_path = Path(apks_path_str)
    
    if not apks_path.is_absolute():
        current_dir = Path.cwd()
        apks_path = current_dir / apks_path_str
        
        if not apks_path.exists():
            script_dir = Path(__file__).parent.parent
            apks_path = script_dir / apks_path_str
        
        if not apks_path.exists():
            executor_root = Path(__file__).parent.parent.parent
            apks_path = executor_root / apks_path_str
        
        if not apks_path.exists():
            apks_path = Path(apks_path_str)
    
    return apks_path.resolve()


def get_server_config():
    """Get server configuration with defaults"""
    config = load_config()
    
    server_config = config.get('server', {})
    backend_config = server_config.get('backend', {})
    trader_server_config = config.get('trader-server', {})
    development_config = config.get('development', {})
    
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
        'reload': backend_config.get('reload', False),
        'docs_url': development_config.get('docs_url', '/docs'),
        'redoc_url': development_config.get('redoc_url', '/redoc'),
        'enable_docs': development_config.get('enable_docs', True)
    }