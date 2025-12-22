#!/usr/bin/env python3
"""
数据库迁移脚本
用于更新数据库表结构，添加新字段
"""

import os
import sys
import sqlite3
from peewee import *
from playhouse.migrate import *

# 添加项目根目录到Python路径
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from core.database.models import db, get_db_path

def migrate_database():
    """执行数据库迁移"""
    print("开始数据库迁移...")
    
    # 获取数据库路径
    db_path = get_db_path()
    print(f"数据库路径: {db_path}")
    
    # 创建迁移器
    migrator = SqliteMigrator(SqliteDatabase(db_path))
    
    # 执行迁移操作
    try:
        # 检查并添加新字段
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # 检查Device表是否有debug_check_status字段
        cursor.execute("PRAGMA table_info(device)")
        columns = [column[1] for column in cursor.fetchall()]
        
        # 检查是否需要迁移主键从udid到serialno
        if 'udid' in columns and 'serialno' not in columns:
            print("检测到需要迁移主键从udid到serialno...")
            
            # 1. 添加serialno字段
            print("添加serialno字段...")
            cursor.execute("ALTER TABLE device ADD COLUMN serialno TEXT")
            
            # 2. 复制udid的值到serialno
            print("复制udid值到serialno...")
            cursor.execute("UPDATE device SET serialno = udid WHERE udid IS NOT NULL")
            
            # 3. 检查外键依赖
            print("检查外键依赖...")
            cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
            tables = [row[0] for row in cursor.fetchall()]
            
            # 处理DeviceApk表的外键
            if 'deviceapk' in tables:
                print("更新DeviceApk表的外键引用...")
                # 添加新的device_serialno字段
                cursor.execute("ALTER TABLE deviceapk ADD COLUMN device_serialno TEXT")
                
                # 更新外键引用
                cursor.execute("""
                    UPDATE deviceapk 
                    SET device_serialno = (SELECT serialno FROM device WHERE device.id = deviceapk.device_id)
                    WHERE device_id IN (SELECT id FROM device WHERE serialno IS NOT NULL)
                """)
                
                # 删除旧的外键字段
                print("注意：由于SQLite限制，需要手动重建DeviceApk表以完全迁移外键")
                print("请运行recreate_deviceapk_table()函数完成迁移")
            
            # 4. 将serialno设为主键（SQLite限制，需要重建表）
            print("注意：由于SQLite限制，需要手动重建Device表以完全迁移主键")
            print("请运行recreate_device_table()函数完成迁移")
        
        # 需要添加的字段
        fields_to_add = [
            ('debug_check_status', 'TEXT DEFAULT "UNKNOWN"'),
            ('debug_check_message', 'TEXT'),
            ('debug_check_time', 'DATETIME'),
            ('apps', 'TEXT'),
            ('name', 'TEXT'),
            ('model', 'TEXT'),
            ('manufacturer', 'TEXT'),
            ('api_level', 'INTEGER'),
            ('platform', 'TEXT DEFAULT "Android"'),
            ('brand', 'TEXT'),
            ('device', 'TEXT'),
            ('product', 'TEXT'),
            ('ip', 'TEXT'),
            ('screen_width', 'INTEGER'),
            ('screen_height', 'INTEGER')
        ]
        
        # 添加缺失的字段
        for field_name, field_type in fields_to_add:
            if field_name not in columns:
                print(f"添加字段: {field_name}")
                cursor.execute(f"ALTER TABLE device ADD COLUMN {field_name} {field_type}")
            else:
                print(f"字段已存在: {field_name}")
        
        conn.commit()
        conn.close()
        
        print("数据库迁移完成！")
        return True
        
    except Exception as e:
        print(f"迁移失败: {str(e)}")
        return False


def recreate_device_table():
    """重建Device表以完成主键迁移"""
    print("重建Device表以完成主键迁移...")
    
    db_path = get_db_path()
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    try:
        # 1. 创建临时表
        cursor.execute("""
            CREATE TABLE device_temp (
                serialno TEXT PRIMARY KEY,
                udid TEXT,
                user_id INTEGER,
                device_name TEXT DEFAULT 'Unknown Device',
                status TEXT DEFAULT 'offline',
                last_seen DATETIME,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                debug_check_status TEXT DEFAULT 'UNKNOWN',
                debug_check_message TEXT,
                debug_check_time DATETIME,
                apps TEXT,
                name TEXT,
                model TEXT,
                manufacturer TEXT,
                api_level INTEGER,
                platform TEXT DEFAULT 'Android',
                brand TEXT,
                device TEXT,
                product TEXT,
                ip TEXT,
                screen_width INTEGER,
                screen_height INTEGER,
                FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE SET NULL
            )
        """)
        
        # 2. 复制数据
        cursor.execute("""
            INSERT INTO device_temp (
                serialno, udid, user_id, device_name, status, last_seen, 
                created_at, updated_at, debug_check_status, debug_check_message,
                debug_check_time, apps, name, model, manufacturer, api_level,
                platform, brand, device, product, ip, screen_width, screen_height
            )
            SELECT 
                serialno, udid, user_id, device_name, status, last_seen,
                created_at, updated_at, debug_check_status, debug_check_message,
                debug_check_time, apps, name, model, manufacturer, api_level,
                platform, brand, device, product, ip, screen_width, screen_height
            FROM device
        """)
        
        # 3. 删除旧表
        cursor.execute("DROP TABLE device")
        
        # 4. 重命名临时表
        cursor.execute("ALTER TABLE device_temp RENAME TO device")
        
        conn.commit()
        print("Device表重建完成！")
        return True
        
    except Exception as e:
        print(f"重建Device表失败: {str(e)}")
        conn.rollback()
        return False
    finally:
        conn.close()


def recreate_deviceapk_table():
    """重建DeviceApk表以完成外键迁移"""
    print("重建DeviceApk表以完成外键迁移...")
    
    db_path = get_db_path()
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    try:
        # 1. 创建临时表
        cursor.execute("""
            CREATE TABLE deviceapk_temp (
                id INTEGER PRIMARY KEY,
                device_id INTEGER,
                apk_id INTEGER,
                installed_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (device_id) REFERENCES device (id) ON DELETE CASCADE,
                FOREIGN KEY (apk_id) REFERENCES apk (id) ON DELETE CASCADE,
                UNIQUE(device_id, apk_id)
            )
        """)
        
        # 2. 复制数据
        cursor.execute("""
            INSERT INTO deviceapk_temp (id, device_id, apk_id, installed_time)
            SELECT id, device_id, apk_id, installed_time
            FROM deviceapk
        """)
        
        # 3. 删除旧表
        cursor.execute("DROP TABLE deviceapk")
        
        # 4. 重命名临时表
        cursor.execute("ALTER TABLE deviceapk_temp RENAME TO deviceapk")
        
        conn.commit()
        print("DeviceApk表重建完成！")
        return True
        
    except Exception as e:
        print(f"重建DeviceApk表失败: {str(e)}")
        conn.rollback()
        return False
    finally:
        conn.close()

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="数据库迁移工具")
    parser.add_argument("--migrate", action="store_true", help="执行基本迁移（添加字段）")
    parser.add_argument("--recreate-device", action="store_true", help="重建Device表以完成主键迁移")
    parser.add_argument("--recreate-deviceapk", action="store_true", help="重建DeviceApk表以完成外键迁移")
    parser.add_argument("--all", action="store_true", help="执行完整迁移流程")
    
    args = parser.parse_args()
    
    if args.all:
        print("执行完整迁移流程...")
        if migrate_database():
            print("\n基本迁移完成，现在重建表...")
            if recreate_device_table():
                print("\nDevice表重建完成，现在重建DeviceApk表...")
                if recreate_deviceapk_table():
                    print("\n所有迁移完成！")
                else:
                    print("\nDeviceApk表重建失败")
            else:
                print("\nDevice表重建失败")
        else:
            print("\n基本迁移失败")
    elif args.migrate:
        migrate_database()
    elif args.recreate_device:
        recreate_device_table()
    elif args.recreate_deviceapk:
        recreate_deviceapk_table()
    else:
        print("请指定要执行的操作。使用 --help 查看帮助。")
        print("示例：")
        print("  python migrate_db.py --migrate          # 执行基本迁移")
        print("  python migrate_db.py --recreate-device  # 重建Device表")
        print("  python migrate_db.py --recreate-deviceapk  # 重建DeviceApk表")
        print("  python migrate_db.py --all              # 执行完整迁移流程")