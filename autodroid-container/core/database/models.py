"""
Peewee数据库模型定义
"""

import os
from peewee import *
from datetime import datetime

# 获取数据库路径
def get_db_path():
    """获取数据库文件路径"""
    config_path = os.path.join(os.path.dirname(__file__), "..", "..", "config.yaml")
    if os.path.exists(config_path):
        import yaml
        with open(config_path, 'r') as f:
            config = yaml.safe_load(f)
        return config.get('database', {}).get('sqlite_path', 'users.db')
    return 'users.db'

# 创建数据库连接
db = SqliteDatabase(get_db_path())

class BaseModel(Model):
    """基础模型类"""
    class Meta:
        database = db

class User(BaseModel):
    """用户模型"""
    id = CharField(primary_key=True)
    email = CharField(unique=True)
    name = CharField()
    password_hash = CharField()
    role = CharField(default='user')
    last_login = DateTimeField(null=True)
    created_at = DateTimeField(default=datetime.now)

class Session(BaseModel):
    """会话模型"""
    id = CharField(primary_key=True)
    user = ForeignKeyField(User, backref='sessions', on_delete='CASCADE')
    token = CharField()
    expires_at = DateTimeField()
    created_at = DateTimeField(default=datetime.now)

class Apk(BaseModel):
    """APK模型"""
    package_name = CharField(primary_key=True)
    app_name = CharField()
    version = CharField()
    version_code = IntegerField()
    installed_time = IntegerField()
    is_system = BooleanField(default=False)
    icon_path = CharField(null=True)
    created_at = DateTimeField(default=datetime.now)
    updated_at = DateTimeField(default=datetime.now)

class Device(BaseModel):
    """设备模型"""
    udid = CharField(primary_key=True)
    device_name = CharField()
    android_version = CharField()
    battery_level = IntegerField(default=50)
    is_online = BooleanField(default=True)
    connection_type = CharField(default='network')
    user = ForeignKeyField(User, backref='devices', null=True, on_delete='SET NULL')
    created_at = DateTimeField(default=datetime.now)
    updated_at = DateTimeField(default=datetime.now)

class DeviceApk(BaseModel):
    """设备-APK关联模型（多对多关系）"""
    device = ForeignKeyField(Device, backref='device_apks', on_delete='CASCADE')
    apk = ForeignKeyField(Apk, backref='device_apks', on_delete='CASCADE')
    installed_time = DateTimeField(default=datetime.now)
    
    class Meta:
        primary_key = CompositeKey('device', 'apk')

class Workflow(BaseModel):
    """工作流模型"""
    id = CharField(primary_key=True)
    name = CharField()
    description = CharField(null=True)
    config_yaml = TextField()
    created_by = ForeignKeyField(User, backref='workflows', null=True, on_delete='SET NULL')
    created_at = DateTimeField(default=datetime.now)
    updated_at = DateTimeField(default=datetime.now)

class DeviceWorkflowPlan(BaseModel):
    """设备工作流计划模型"""
    id = CharField(primary_key=True)
    workflow = ForeignKeyField(Workflow, backref='device_plans', on_delete='CASCADE')
    device = ForeignKeyField(Device, backref='workflow_plans', on_delete='CASCADE')
    enabled = BooleanField(default=True)
    schedule_json = TextField()
    priority = IntegerField(default=0)
    created_at = DateTimeField(default=datetime.now)
    updated_at = DateTimeField(default=datetime.now)

class WorkflowExecution(BaseModel):
    """工作流执行记录模型"""
    id = CharField(primary_key=True)
    workflow = ForeignKeyField(Workflow, backref='executions', on_delete='CASCADE')
    device = ForeignKeyField(Device, backref='workflow_executions', on_delete='CASCADE')
    status = CharField()
    start_time = DateTimeField()
    end_time = DateTimeField(null=True)
    result_json = TextField(null=True)
    error_message = CharField(null=True)
    created_at = DateTimeField(default=datetime.now)

# 创建所有表
def create_tables():
    """创建所有数据库表"""
    with db:
        db.create_tables([
            User, Session, Apk, Device, DeviceApk, 
            Workflow, DeviceWorkflowPlan, WorkflowExecution
        ])

# 初始化数据库
if __name__ == "__main__":
    create_tables()
    print("数据库表创建完成！")