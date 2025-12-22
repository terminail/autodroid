"""
Peewee数据库模型定义
"""

import os
from peewee import (
    CharField, IntegerField, DateTimeField, ForeignKeyField, 
    TextField, DecimalField, CompositeKey, SqliteDatabase, Model, BooleanField
)
from datetime import datetime

# 获取数据库路径
def get_db_path():
    """获取数据库文件路径"""
    config_path = os.path.join(os.path.dirname(__file__), "..", "..", "config.yaml")
    if os.path.exists(config_path):
        import yaml
        with open(config_path, 'r', encoding='utf-8') as f:
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
    description = CharField(null=True)
    password_hash = CharField()
    role = CharField(default='user')
    last_login = DateTimeField(null=True)
    created_at = DateTimeField(default=datetime.now)



class Apk(BaseModel):
    """APK模型"""
    id = CharField(primary_key=True)  # 对应设计文档中的id字段
    package_name = CharField()  # 包名
    app_name = CharField()  # 应用名称
    name = CharField()  # 保留原有字段
    description = CharField(null=True)
    version = CharField()
    version_code = IntegerField()
    installed_time = IntegerField(null=True)  # 安装时间戳
    is_system = BooleanField(default=False)  # 是否为系统应用
    icon_path = CharField(null=True)  # 图标路径
    created_at = DateTimeField(default=datetime.now)

class Device(BaseModel):
    """设备模型"""
    serialno = CharField(primary_key=True)  # 设备序列号，与adb devices和Appium保持一致
    udid = CharField(null=True)  # 设备UDID，保留用于兼容性
    user = ForeignKeyField(User, backref='devices', null=True, on_delete='SET NULL')
    name = CharField(default='Unknown Device',null=True)  # 设备名称
    model = CharField(null=True)  # 设备型号
    manufacturer = CharField(null=True)  # 设备制造商
    android_version = CharField(null=True)  # Android版本
    api_level = IntegerField(null=True)  # API级别
    platform = CharField(default='Android',null=True)  # 平台
    brand = CharField(null=True)  # 品牌
    device = CharField(null=True)  # 设备
    product = CharField(null=True)  # 产品
    ip = CharField(null=True)  # IP地址
    screen_width = IntegerField(null=True)  # 屏幕宽度
    screen_height = IntegerField(null=True)  # 屏幕高度
    battery_level = IntegerField(default=50,null=True)  # 电池电量
    is_online = BooleanField(default=False)  # 是否在线
    connection_type = CharField(default='network',null=True)  # 连接类型
    usb_debug_enabled = BooleanField(default=False,null=True)  # USB调试是否开启
    wifi_debug_enabled = BooleanField(default=False,null=True)  # WiFi调试是否开启
    check_status = CharField(default='UNKNOWN',null=True)  # 设备检查状态：UNKNOWN, SUCCESS, FAILED
    check_message = CharField(null=True)  # 设备检查消息
    check_time = DateTimeField(null=True)  # 设备检查时间
    apps = TextField(null=True)  # 已安装的支持应用列表，JSON格式存储
    registered_at = DateTimeField(default=datetime.now)  # 注册时间
    created_at = DateTimeField(default=datetime.now)
    updated_at = DateTimeField(default=datetime.now)

class DeviceApk(BaseModel):
    """设备-APK关联模型（多对多关系）"""
    device = ForeignKeyField(Device, backref='device_apks', on_delete='CASCADE')
    apk = ForeignKeyField(Apk, backref='device_apks', on_delete='CASCADE')
    installed_time = DateTimeField(default=datetime.now)
    
    class Meta:
        primary_key = CompositeKey('device', 'apk')

class WorkScript(BaseModel):
    """工作脚本模型"""
    id = CharField(primary_key=True)
    apk = ForeignKeyField(Apk, backref='workscripts', on_delete='CASCADE')
    name = CharField()
    description = CharField(null=True)
    metadata = TextField()  # JSON格式存储
    script_path = CharField()  # Python脚本文件路径
    status = CharField(default='NEW')  # 状态：NEW、INPROGRESS、FAILED、OK
    created_at = DateTimeField(default=datetime.now)

class WorkPlan(BaseModel):
    """工作脚本计划模型"""
    id = CharField(primary_key=True)
    script = ForeignKeyField(WorkScript, backref='workplans', on_delete='CASCADE')
    user = ForeignKeyField(User, backref='workplans', null=True, on_delete='SET NULL')
    name = CharField()
    description = CharField(null=True)
    data = TextField()  # JSON格式存储，工作计划数据符合WorkScript的metadata要求
    status = CharField(default='NEW')  # 状态：NEW、INPROGRESS、FAILED、OK
    created_at = DateTimeField(default=datetime.now)
    started_at = DateTimeField(null=True)
    ended_at = DateTimeField(null=True)

class WorkReport(BaseModel):
    """工作脚本执行报告模型"""
    id = CharField(primary_key=True)
    user = ForeignKeyField(User, backref='workreports', on_delete='CASCADE')
    plan = ForeignKeyField(WorkPlan, backref='workreports', on_delete='CASCADE')
    name = CharField()
    description = CharField(null=True)
    execution_log = TextField()  # JSON格式存储
    result_data = TextField()  # JSON格式存储
    error_message = CharField(null=True)
    created_at = DateTimeField(default=datetime.now)

class Contract(BaseModel):
    """合约模型"""
    id = CharField(primary_key=True)
    symbol = CharField()
    name = CharField()
    description = CharField(null=True)
    type = CharField()  # 类型：股票、ETF、期权等
    exchange = CharField()  # 交易所 - 必填字段
    price = DecimalField()  # 当前价格
    created_at = DateTimeField(default=datetime.now)

class Order(BaseModel):
    """订单模型"""
    id = CharField(primary_key=True)
    plan = ForeignKeyField(WorkPlan, backref='orders', null=True, on_delete='SET NULL')
    contract = ForeignKeyField(Contract, backref='orders', null=True, on_delete='SET NULL')
    name = CharField()
    description = CharField(null=True)
    order_type = CharField()  # 订单类型：委托单(Entrusted)、成交单(Executed)
    amount = DecimalField()
    contract_shares = IntegerField(default=0)  # 合约股数
    contract_fee = DecimalField(default=0.00)  # 手续费
    contract_profit_loss = DecimalField(default=0.00)  # 利润损失
    created_at = DateTimeField(default=datetime.now)

# 创建所有表
def create_tables():
    """创建所有数据库表"""
    with db:
        db.create_tables([
            User, Device, Apk, DeviceApk, 
            WorkScript, WorkPlan, WorkReport, Contract, Order
        ])

# 初始化数据库
if __name__ == "__main__":
    create_tables()
    print("数据库表创建完成！")