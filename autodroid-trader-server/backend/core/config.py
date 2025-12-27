import yaml
import os
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


def get_server_config():
    """Get server configuration with defaults"""
    config = load_config()
    
    server_config = config.get('server', {})
    backend_config = server_config.get('backend', {})
    frontend_config = config.get('frontend', {})
    api_config = config.get('api', {})
    database_config = config.get('database', {})
    auth_config = config.get('authentication', {})
    
    return {
        'host': backend_config.get('host', '0.0.0.0'),
        'port': server_config.get('port', 8003),
        'log_level': config.get('logging', {}).get('level', 'info'),
        'reload': backend_config.get('reload', False),
        'frontend_mount_path': frontend_config.get('mount_path', '/app'),
        'cors_origins': api_config.get('cors_origins', ["*"]),
        'cors_credentials': api_config.get('cors_credentials', True),
        'cors_methods': api_config.get('cors_methods', ["*"]),
        'cors_headers': api_config.get('cors_headers', ["*"]),
        'database_path': database_config.get('path', 'autodroid.db'),
        'secret_key': auth_config.get('secret_key', "your-secret-key-change-in-production"),
        'token_expire_minutes': auth_config.get('token_expire_minutes', 60)
    }


def setup_logging(config):
    """Setup logging configuration based on config.yaml"""
    import logging
    from logging.handlers import RotatingFileHandler
    
    logging_config = config.get('logging', {})
    
    # Get log file path from config
    log_file_path = logging_config.get('file_path', 'logs/autodroid.log')
    log_level = logging_config.get('level', 'info').upper()
    max_file_size = logging_config.get('max_file_size', 10) * 1024 * 1024  # Convert MB to bytes
    backup_count = logging_config.get('backup_count', 5)
    log_api_requests = logging_config.get('log_api_requests', False)
    
    # Create logs directory if it doesn't exist
    log_dir = os.path.dirname(log_file_path)
    if log_dir and not os.path.exists(log_dir):
        os.makedirs(log_dir, exist_ok=True)
    
    # Convert string log level to logging constant
    numeric_level = getattr(logging, log_level, logging.INFO)
    
    # Configure root logger
    logger = logging.getLogger()
    logger.setLevel(numeric_level)
    
    # Remove existing handlers to avoid duplicates
    for handler in logger.handlers[:]:
        logger.removeHandler(handler)
    
    # Create formatter
    formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # Create file handler with rotation
    file_handler = RotatingFileHandler(
        log_file_path, 
        maxBytes=max_file_size, 
        backupCount=backup_count,
        encoding='utf-8'
    )
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)
    
    # Create console handler
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)
    
    print(f"✓ Logging configured: {log_level} level, file: {log_file_path}")
    return logger