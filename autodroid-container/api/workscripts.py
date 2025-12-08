"""
Workscripts API endpoints

This module handles workscript discovery and management for APKs.
Each APK can have multiple workscripts defined in a workscripts.yaml file
within the APK's directory under workscripts/.
"""

import os
import yaml
from pathlib import Path
from typing import List, Optional
from fastapi import APIRouter, HTTPException, Path as PathParam
from api.models import WorkscriptList, Workscript, WorkscriptMetadata, WorkscriptParameter

router = APIRouter(tags=["workscripts"])

# Base directory for workscripts
WORKSCRIPTS_DIR = Path("workscripts")

def load_workscripts_yaml(app_package: str) -> Optional[dict]:
    """Load workscripts.yaml file for a given app package"""
    yaml_path = WORKSCRIPTS_DIR / app_package / "workscripts.yaml"
    
    if not yaml_path.exists():
        return None
    
    try:
        with open(yaml_path, 'r', encoding='utf-8') as f:
            return yaml.safe_load(f)
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error loading workscripts.yaml for {app_package}: {str(e)}"
        )

@router.get("/{app_package}/workscripts", response_model=WorkscriptList, summary="获取应用工作脚本列表", tags=["workscripts"])
async def get_workscripts(
    app_package: str = PathParam(..., description="App package name (e.g., com.autodroid.manager)")
):
    """
    Get all workscripts available for a specific APK
    
    Returns a list of workscripts with their metadata, parameters, and execution details
    """
    # Replace dots with underscores for directory naming
    dir_package = app_package.replace('.', '_')
    
    # First try the exact package name, then try with underscores
    workscripts_data = load_workscripts_yaml(app_package)
    if workscripts_data is None:
        workscripts_data = load_workscripts_yaml(dir_package)
    
    if workscripts_data is None:
        # Check if the directory exists at all
        app_dir = WORKSCRIPTS_DIR / app_package
        if not app_dir.exists():
            raise HTTPException(
                status_code=404,
                detail=f"No workscripts found for app package: {app_package}"
            )
        
        # Directory exists but no workscripts.yaml, return empty list
        return WorkscriptList(
            workscripts=[],
            metadata=WorkscriptMetadata(
                app_package=app_package,
                last_updated="",
                version="",
                author=""
            ),
            total_count=0
        )
    
    # Convert to response model
    workscripts = []
    for ws in workscripts_data.get('workscripts', []):
        parameters = [
            WorkscriptParameter(**param)
            for param in ws.get('parameters', [])
        ]
        
        workscript = Workscript(
            id=ws['id'],
            name=ws['name'],
            description=ws['description'],
            script_file=ws['script_file'],
            category=ws['category'],
            priority=ws['priority'],
            tags=ws.get('tags', []),
            parameters=parameters,
            requirements=ws.get('requirements', []),
            estimated_duration=ws['estimated_duration']
        )
        workscripts.append(workscript)
    
    metadata_data = workscripts_data.get('metadata', {})
    metadata = WorkscriptMetadata(
        app_package=metadata_data.get('app_package', app_package),
        last_updated=metadata_data.get('last_updated', ''),
        version=metadata_data.get('version', ''),
        author=metadata_data.get('author', '')
    )
    
    return WorkscriptList(
        workscripts=workscripts,
        metadata=metadata,
        total_count=len(workscripts)
    )

@router.get("/{app_package}/workscripts/{workscript_id}", summary="获取工作脚本详情", tags=["workscripts"])
async def get_workscript_detail(
    app_package: str = PathParam(..., description="App package name"),
    workscript_id: str = PathParam(..., description="Workscript ID")
):
    """
    Get detailed information about a specific workscript
    """
    workscripts_list = await get_workscripts(app_package)
    
    for workscript in workscripts_list.workscripts:
        if workscript.id == workscript_id:
            return workscript
    
    raise HTTPException(
        status_code=404,
        detail=f"Workscript '{workscript_id}' not found for app: {app_package}"
    )

@router.get("/workscripts", summary="发现所有工作脚本", tags=["workscripts"])
async def discover_workscripts():
    """
    Discover all available workscripts across all apps
    """
    all_workscripts = {}
    
    if not WORKSCRIPTS_DIR.exists():
        return {"apps": {}, "total_apps": 0}
    
    # Iterate through all app directories
    for app_dir in WORKSCRIPTS_DIR.iterdir():
        if app_dir.is_dir() and not app_dir.name.startswith('.'):
            app_package = app_dir.name
            try:
                workscripts_data = await get_workscripts(app_package)
                if workscripts_data.workscripts:
                    all_workscripts[app_package] = {
                        "workscript_count": workscripts_data.total_count,
                        "metadata": workscripts_data.metadata.dict(),
                        "workscripts": [ws.dict() for ws in workscripts_data.workscripts]
                    }
            except HTTPException:
                # Skip apps without workscripts
                continue
    
    return {
        "apps": all_workscripts,
        "total_apps": len(all_workscripts)
    }