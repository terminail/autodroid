import sqlite3
import os
from typing import Optional, Dict, List, Any
from datetime import datetime

class ApkDatabase:
    """APK数据库管理类"""
    
    def __init__(self, db_path: str = "autodroid.db"):
        self.db_path = db_path
        self._init_db()
    
    def _init_db(self):
        """初始化数据库表"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        # 创建APK表
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS apks (
                apkid TEXT PRIMARY KEY,
                package_name TEXT NOT NULL,
                app_name TEXT NOT NULL,
                version TEXT NOT NULL,
                version_code TEXT NOT NULL,
                installed_time INTEGER NOT NULL,
                is_system BOOLEAN DEFAULT 0,
                icon_path TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        # 创建APK-设备关联表
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS apk_devices (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                apkid TEXT NOT NULL,
                device_udid TEXT NOT NULL,
                installed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (apkid) REFERENCES apks (apkid),
                UNIQUE(apkid, device_udid)
            )
        ''')
        
        conn.commit()
        conn.close()
    
    def create_apk(self, apkid: str, package_name: str, app_name: str, 
                   version: str, version_code: str, installed_time: int,
                   is_system: bool = False, icon_path: Optional[str] = None) -> bool:
        """创建新APK记录"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
                INSERT OR REPLACE INTO apks 
                (apkid, package_name, app_name, version, version_code, installed_time, is_system, icon_path, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ''', (apkid, package_name, app_name, version, version_code, installed_time, is_system, icon_path))
            
            conn.commit()
            conn.close()
            return True
            
        except sqlite3.Error:
            return False
    
    def get_apk(self, apkid: str) -> Optional[Dict[str, Any]]:
        """获取特定APK信息"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
                SELECT apkid, package_name, app_name, version, version_code, 
                       installed_time, is_system, icon_path, created_at, updated_at
                FROM apks WHERE apkid = ?
            ''', (apkid,))
            
            apk_data = cursor.fetchone()
            conn.close()
            
            if apk_data:
                return {
                    "apkid": apk_data[0],
                    "package_name": apk_data[1],
                    "app_name": apk_data[2],
                    "version": apk_data[3],
                    "version_code": apk_data[4],
                    "installed_time": apk_data[5],
                    "is_system": bool(apk_data[6]),
                    "icon_path": apk_data[7],
                    "created_at": apk_data[8],
                    "updated_at": apk_data[9]
                }
            return None
            
        except sqlite3.Error:
            return None
    
    def get_all_apks(self) -> List[Dict[str, Any]]:
        """获取所有APK信息"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
                SELECT apkid, package_name, app_name, version, version_code, 
                       installed_time, is_system, icon_path, created_at, updated_at
                FROM apks ORDER BY app_name
            ''')
            
            apk_data_list = cursor.fetchall()
            conn.close()
            
            apks = []
            for apk_data in apk_data_list:
                apks.append({
                    "apkid": apk_data[0],
                    "package_name": apk_data[1],
                    "app_name": apk_data[2],
                    "version": apk_data[3],
                    "version_code": apk_data[4],
                    "installed_time": apk_data[5],
                    "is_system": bool(apk_data[6]),
                    "icon_path": apk_data[7],
                    "created_at": apk_data[8],
                    "updated_at": apk_data[9]
                })
            
            return apks
            
        except sqlite3.Error:
            return []
    
    def update_apk(self, apkid: str, update_data: Dict[str, Any]) -> bool:
        """更新APK信息"""
        if not update_data:
            return False
            
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            # 构建动态更新语句
            set_clauses = []
            values = []
            
            for key, value in update_data.items():
                if key in ["package_name", "app_name", "version", "version_code", 
                          "installed_time", "is_system", "icon_path"]:
                    set_clauses.append(f"{key} = ?")
                    values.append(value)
            
            if not set_clauses:
                return False
                
            # 添加更新时间
            set_clauses.append("updated_at = CURRENT_TIMESTAMP")
            values.append(apkid)  # WHERE条件
            
            cursor.execute(f'''
                UPDATE apks SET {", ".join(set_clauses)} WHERE apkid = ?
            ''', values)
            
            conn.commit()
            conn.close()
            return cursor.rowcount > 0
            
        except sqlite3.Error:
            return False
    
    def delete_apk(self, apkid: str) -> bool:
        """删除APK记录"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            # 先删除关联的设备记录
            cursor.execute("DELETE FROM apk_devices WHERE apkid = ?", (apkid,))
            
            # 再删除APK记录
            cursor.execute("DELETE FROM apks WHERE apkid = ?", (apkid,))
            
            conn.commit()
            conn.close()
            return cursor.rowcount > 0
            
        except sqlite3.Error:
            return False
    
    def search_apks(self, query: str) -> List[Dict[str, Any]]:
        """搜索APK"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            search_pattern = f"%{query}%"
            cursor.execute('''
                SELECT apkid, package_name, app_name, version, version_code, 
                       installed_time, is_system, icon_path, created_at, updated_at
                FROM apks 
                WHERE package_name LIKE ? OR app_name LIKE ?
                ORDER BY app_name
            ''', (search_pattern, search_pattern))
            
            apk_data_list = cursor.fetchall()
            conn.close()
            
            apks = []
            for apk_data in apk_data_list:
                apks.append({
                    "apkid": apk_data[0],
                    "package_name": apk_data[1],
                    "app_name": apk_data[2],
                    "version": apk_data[3],
                    "version_code": apk_data[4],
                    "installed_time": apk_data[5],
                    "is_system": bool(apk_data[6]),
                    "icon_path": apk_data[7],
                    "created_at": apk_data[8],
                    "updated_at": apk_data[9]
                })
            
            return apks
            
        except sqlite3.Error:
            return []
    
    def associate_apk_with_device(self, apkid: str, device_udid: str) -> bool:
        """将APK与设备关联"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
                INSERT OR IGNORE INTO apk_devices (apkid, device_udid)
                VALUES (?, ?)
            ''', (apkid, device_udid))
            
            conn.commit()
            conn.close()
            return True
            
        except sqlite3.Error:
            return False
    
    def dissociate_apk_from_device(self, apkid: str, device_udid: str) -> bool:
        """取消APK与设备的关联"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
                DELETE FROM apk_devices WHERE apkid = ? AND device_udid = ?
            ''', (apkid, device_udid))
            
            conn.commit()
            conn.close()
            return True
            
        except sqlite3.Error:
            return False
    
    def get_devices_for_apk(self, apkid: str) -> List[str]:
        """获取安装了特定APK的设备列表"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
                SELECT device_udid FROM apk_devices WHERE apkid = ?
            ''', (apkid,))
            
            device_data = cursor.fetchall()
            conn.close()
            
            return [device[0] for device in device_data]
            
        except sqlite3.Error:
            return []
    
    def get_apks_for_device(self, device_udid: str) -> List[Dict[str, Any]]:
        """获取设备上安装的所有APK"""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
                SELECT a.apkid, a.package_name, a.app_name, a.version, a.version_code, 
                       a.installed_time, a.is_system, a.icon_path, a.created_at, a.updated_at
                FROM apks a
                JOIN apk_devices ad ON a.apkid = ad.apkid
                WHERE ad.device_udid = ?
                ORDER BY a.app_name
            ''', (device_udid,))
            
            apk_data_list = cursor.fetchall()
            conn.close()
            
            apks = []
            for apk_data in apk_data_list:
                apks.append({
                    "apkid": apk_data[0],
                    "package_name": apk_data[1],
                    "app_name": apk_data[2],
                    "version": apk_data[3],
                    "version_code": apk_data[4],
                    "installed_time": apk_data[5],
                    "is_system": bool(apk_data[6]),
                    "icon_path": apk_data[7],
                    "created_at": apk_data[8],
                    "updated_at": apk_data[9]
                })
            
            return apks
            
        except sqlite3.Error:
            return []