#!/usr/bin/env python3
"""
数据库字段重命名迁移脚本
用于重命名数据库表字段，去掉debug_前缀
"""

import os
import sys
import sqlite3
from peewee import *
from playhouse.migrate import *

# 添加项目根目录到Python路径
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from core.database.models import db, get_db_path

def rename_fields():
    """执行字段重命名"""
    print("开始字段重命名迁移...")
    
    # 获取数据库路径
    db_path = get_db_path()
    print(f"数据库路径: {db_path}")
    
    # 执行迁移操作
    try:
        # SQLite不支持直接重命名列，需要重建表
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # 1. 创建临时表
        cursor.execute("""
        CREATE TABLE device_temp (
            udid TEXT NOT NULL PRIMARY KEY,
            user_id TEXT,
            device_name TEXT DEFAULT 'Unknown Device',
            name TEXT,
            model TEXT,
            manufacturer TEXT,
            android_version TEXT DEFAULT 'Unknown',
            api_level INTEGER,
            platform TEXT DEFAULT 'Android',
            brand TEXT,
            device TEXT,
            product TEXT,
            ip TEXT,
            screen_width INTEGER,
            screen_height INTEGER,
            battery_level INTEGER DEFAULT 50,
            is_online INTEGER DEFAULT 0,
            connection_type TEXT DEFAULT 'network',
            usb_debug_enabled INTEGER DEFAULT 0,
            wifi_debug_enabled INTEGER DEFAULT 0,
            check_status TEXT DEFAULT 'UNKNOWN',
            check_message TEXT,
            check_time DATETIME,
            apps TEXT,
            registered_at DATETIME,
            created_at DATETIME,
            updated_at DATETIME,
            FOREIGN KEY (user_id) REFERENCES user (id)
        )
        """)
        
        # 2. 复制数据
        cursor.execute("""
        INSERT INTO device_temp (
            udid, user_id, device_name, name, model, manufacturer, android_version,
            api_level, platform, brand, device, product, ip, screen_width, screen_height,
            battery_level, is_online, connection_type, usb_debug_enabled, wifi_debug_enabled,
            check_status, check_message, check_time, apps, registered_at, created_at, updated_at
        )
        SELECT 
            udid, user_id, device_name, name, model, manufacturer, android_version,
            api_level, platform, brand, device, product, ip, screen_width, screen_height,
            battery_level, is_online, connection_type, usb_debug_enabled, wifi_debug_enabled,
            debug_check_status, debug_check_message, debug_check_time, apps, registered_at, created_at, updated_at
        FROM device
        """)
        
        # 3. 删除原表
        cursor.execute("DROP TABLE device")
        
        # 4. 重命名临时表
        cursor.execute("ALTER TABLE device_temp RENAME TO device")
        
        # 5. 重建索引
        cursor.execute("CREATE INDEX device_user_id ON device (user_id)")
        
        conn.commit()
        conn.close()
        
        print("字段重命名迁移完成！")
        return True
        
    except Exception as e:
        print(f"迁移失败: {str(e)}")
        return False

if __name__ == "__main__":
    rename_fields()