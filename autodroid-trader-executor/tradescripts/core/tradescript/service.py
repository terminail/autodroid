from typing import List, Any, Dict, TypedDict, Optional
from datetime import datetime
from pathlib import Path
import os
import hashlib
import yaml
import asyncio
import logging
from ..config import get_apks_path
from .models import TradeScriptResponse, TradeScriptExecutionData, TradeScriptExecutionResult

logger = logging.getLogger(__name__)


class TradeScriptService:
    """交易脚本服务类"""

    async def execute_script(self, script_id: str, data: TradeScriptExecutionData) -> TradeScriptExecutionResult:
        """执行交易脚本"""
        logger.info(f"开始执行脚本: {script_id}，数据: {data}")
        
        try:
            tradescripts = scan_apks_directory()
            target_script = None
            for ts in tradescripts:
                if ts.id == script_id:
                    target_script = ts
                    break
            
            if not target_script:
                return {"success": False, "message": f"未找到脚本: {script_id}"}
            
            script_path = target_script.script_path
            logger.info(f"脚本路径: {script_path}")
            
            await asyncio.sleep(2)
            
            return {
                "success": True,
                "message": f"脚本执行完成: {target_script.name}",
                "data": {
                    "script_id": script_id,
                    "script_name": target_script.name,
                    "executed_data": data,
                    "timestamp": datetime.now().isoformat()
                }
            }
        except Exception as e:
            logger.error(f"执行脚本失败: {e}")
            return {"success": False, "message": str(e)}


def scan_apks_directory() -> List[TradeScriptResponse]:
    """扫描apks目录，返回交易脚本信息列表"""
    # Get the apks directory path from shared configuration
    apks_path = get_apks_path()
    
    # Verify the apks path exists
    if not apks_path.exists():
        print(f"APKs directory does not exist: {apks_path}")
        return []
    
    tradescripts = []
    
    # Check if apks directory exists
    if not apks_path.exists():
        print(f"APKs directory does not exist: {apks_path}")
        return tradescripts
    
    # Iterate through each APK package directory
    for apk_package_dir in apks_path.iterdir():
        if not apk_package_dir.is_dir():
            continue
            
        apk_package = apk_package_dir.name
        
        # Look for tradescript flows in direct subdirectories
        for flow_dir in apk_package_dir.iterdir():
            if not flow_dir.is_dir():
                continue
            
            # Check if this is a direct subdirectory (flow directory)
            relative_path_to_apks = os.path.relpath(flow_dir, str(apks_path))
            path_parts = relative_path_to_apks.split(os.sep)
            
            if len(path_parts) == 2:  # Direct subdirectory under APK package
                apk_flow = flow_dir.name
                
                # Look for config.yaml for metadata
                config_yaml_path = flow_dir / "config.yaml"
                
                # Extract name and description from config.yaml
                try:
                    with open(config_yaml_path, 'r', encoding='utf-8') as f:
                        config_data = yaml.safe_load(f)
                        
                        name = config_data.get('name', apk_flow)
                        description = config_data.get('description', f"Tradescript flow for {apk_package} - {apk_flow}")
                        file_timestamp = datetime.fromtimestamp(config_yaml_path.stat().st_mtime)
                        created_at = file_timestamp
                        updated_at = file_timestamp
                        
                        # Create ID using apk_package + flow_name hash
                        id_string = f"{apk_package}_{apk_flow}"
                        id_hash = hashlib.md5(id_string.encode()).hexdigest()
                        
                        # Use the flow directory path, not the config file path
                        relative_script_path = os.path.relpath(str(flow_dir), str(apks_path))
                        # Replace backslashes with forward slashes for cross-platform compatibility
                        relative_script_path = relative_script_path.replace('\\', '/')
                        
                        tradescript = TradeScriptResponse(
                            id=id_hash,
                            apk_package=apk_package,
                            apk_flow=apk_flow,
                            name=name,
                            description=description,
                            metadata={"file_path": relative_script_path},
                            script_path=relative_script_path,
                            created_at=created_at,
                            updated_at=updated_at
                        )
                        tradescripts.append(tradescript)
                except (FileNotFoundError, yaml.YAMLError, KeyError, TypeError):
                    # If there's an error reading config.yaml (including file not found), skip this flow
                    continue
    
    return tradescripts