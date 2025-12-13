"""
Peewee数据库模型定义 - autodroid-analyzer版本
包含所有模块的数据库模型定义
"""

import os
from peewee import (
    CharField, IntegerField, DateTimeField, ForeignKeyField, 
    TextField, BooleanField, FloatField, AutoField, SqliteDatabase, Model
)
from datetime import datetime

# 获取数据库路径
def get_db_path():
    """获取数据库文件路径"""
    config_path = os.path.join(os.path.dirname(__file__), "..", "..", "config.yaml")
    if os.path.exists(config_path):
        import yaml
        try:
            # 使用UTF-8编码读取配置文件
            with open(config_path, 'r', encoding='utf-8') as f:
                config = yaml.safe_load(f)
            return config.get('database', {}).get('database_path', 'analyzer.db')
        except UnicodeDecodeError:
            # 如果UTF-8失败，尝试其他编码
            try:
                with open(config_path, 'r', encoding='gbk') as f:
                    config = yaml.safe_load(f)
                return config.get('database', {}).get('database_path', 'analyzer.db')
            except:
                return 'analyzer.db'
    return 'analyzer.db'

# 创建数据库连接
db = SqliteDatabase(get_db_path())

class BaseModel(Model):
    """基础模型类"""
    class Meta:
        database = db

# ========== APK模块数据库模型 ==========

class Apk(BaseModel):
    """APK数据库模型"""
    id = CharField(primary_key=True)  # APK唯一标识，用应用包名package_name表示
    app_name = CharField()  # 应用名称
    version_name = CharField(null=True)  # 版本名称
    version_code = IntegerField(null=True)  # 版本代码
    install_time = DateTimeField(null=True)  # 安装时间
    last_analyzed = DateTimeField(null=True)  # 最后分析时间
    total_operations = IntegerField(default=0)  # 总操作数
    total_screenshots = IntegerField(default=0)  # 总截屏数
    is_packed = BooleanField(default=False)  # 是否被加固
    packer_type = CharField(null=True)  # 加固类型
    packer_confidence = FloatField(default=0.0)  # 加固检测置信度
    packer_indicators = TextField(null=True)  # 加固特征指标
    packer_analysis_time = DateTimeField(null=True)  # 加固分析时间
    created_at = DateTimeField(default=datetime.now)  # 创建时间
    
    class Meta:
        table_name = 'apk'

# ========== 设备模块数据库模型 ==========

class Device(BaseModel):
    """设备信息模型"""
    id = CharField(primary_key=True)  # 设备ID
    device_name = CharField()  # 设备名称
    device_model = CharField(default='Unknown')  # 设备型号
    android_version = CharField()  # Android版本
    api_level = IntegerField(default=0)  # API级别
    is_connected = BooleanField(default=False)  # 是否连接
    connection_type = CharField(default='USB')  # 连接类型
    battery_level = IntegerField(default=0)  # 电池电量
    battery_status = CharField(default='Unknown')  # 电池状态
    is_charging = BooleanField(default=False)  # 是否正在充电
    created_at = DateTimeField(default=datetime.now)
    last_updated = DateTimeField(default=datetime.now)  # 最后更新时间
    last_connected = DateTimeField(null=True)  # 最后连接时间
    
    class Meta:
        table_name = 'device_info'

class DeviceConnectionLog(BaseModel):
    """设备连接日志模型"""
    id = CharField(primary_key=True)  # 日志ID
    device_id = CharField()  # 设备ID
    connection_type = CharField()  # 连接类型
    status = CharField()  # 连接状态
    timestamp = DateTimeField(default=datetime.now)  # 连接时间
    duration = IntegerField(null=True)  # 连接时长（秒）
    error_message = CharField(null=True)  # 错误信息
    
    class Meta:
        table_name = 'device_connection_log'

class DeviceApp(BaseModel):
    """设备应用信息模型"""
    id = CharField(primary_key=True)  # 记录ID
    device_id = CharField()  # 设备ID
    package_name = CharField()  # 应用包名
    app_name = CharField()  # 应用名称
    version_name = CharField(null=True)  # 版本名称
    version_code = IntegerField(null=True)  # 版本代码
    install_time = DateTimeField(null=True)  # 安装时间
    is_system_app = BooleanField(default=False)  # 是否系统应用
    
    class Meta:
        table_name = 'device_app_info'

# ========== 截屏模块数据库模型 ==========

class Screenshot(BaseModel):
    """截屏模型"""
    id = CharField(primary_key=True)  # 截屏唯一标识
    apk = ForeignKeyField(Apk, backref='screenshots', on_delete='CASCADE')  # 关联的APK
    timestamp = DateTimeField()  # 截屏时间戳
    file_path = CharField()  # 文件路径
    page_title = CharField(null=True)  # 页面标题
    analysis_status = CharField(default='pending')  # 分析状态
    created_at = DateTimeField(default=datetime.now)
    
    class Meta:
        table_name = 'screenshot'

class PageElement(BaseModel):
    """页面元素模型"""
    id = CharField(primary_key=True)  # 元素唯一标识
    screenshot = ForeignKeyField(Screenshot, backref='elements', on_delete='CASCADE')  # 关联的截屏
    element_type = CharField()  # 元素类型
    text_content = TextField(null=True)  # 文本内容
    bounds = TextField()  # 边界坐标（JSON格式存储）
    importance = IntegerField(default=3)  # 重要性评分(1-5)
    custom_tags = TextField(null=True)  # 自定义标签（JSON格式存储）
    created_at = DateTimeField(default=datetime.now)
    
    class Meta:
        table_name = 'page_element'

class ScreenshotAnalysisResult(BaseModel):
    """截屏分析结果模型"""
    id = CharField(primary_key=True)  # 分析结果ID
    screenshot = ForeignKeyField(Screenshot, backref='analysis_results', on_delete='CASCADE')  # 关联的截屏
    analysis_type = CharField()  # 分析类型
    result = TextField()  # 分析结果（JSON格式存储）
    confidence = FloatField(default=0.0)  # 置信度
    analysis_time = DateTimeField(default=datetime.now)
    
    class Meta:
        table_name = 'screenshot_analysis_result'

class PageStructure(BaseModel):
    """页面结构模型"""
    id = CharField(primary_key=True)  # 结构ID
    screenshot = ForeignKeyField(Screenshot, backref='structures', on_delete='CASCADE')  # 关联的截屏
    structure_type = CharField()  # 结构类型
    elements_count = IntegerField(default=0)  # 元素数量
    layout_info = TextField()  # 布局信息（JSON格式存储）
    created_at = DateTimeField(default=datetime.now)
    
    class Meta:
        table_name = 'page_structure'

# ========== 用户操作模块数据库模型 ==========

class UserOperation(BaseModel):
    """用户操作记录模型"""
    id = AutoField()  # 操作ID（自增主键）
    apk = ForeignKeyField(Apk, backref='operations', on_delete='CASCADE')  # 关联的APK
    timestamp = DateTimeField()  # 操作时间戳
    action_type = CharField()  # 操作类型
    target_element = TextField(null=True)  # 目标元素（JSON格式存储）
    input_text = TextField(null=True)  # 输入文本
    coordinates = TextField(null=True)  # 坐标信息（JSON格式存储）
    screenshot = ForeignKeyField(Screenshot, backref='operations', null=True, on_delete='SET NULL')  # 关联的截屏
    created_at = DateTimeField(default=datetime.now)
    
    class Meta:
        table_name = 'user_operation'

class OperationSequence(BaseModel):
    """操作序列模型"""
    id = CharField(primary_key=True)  # 序列ID
    apk = ForeignKeyField(Apk, backref='operation_sequences', on_delete='CASCADE')  # 关联的APK
    sequence_name = CharField()  # 序列名称
    description = TextField(null=True)  # 序列描述
    operations = TextField()  # 操作列表（JSON格式存储）
    total_steps = IntegerField(default=0)  # 总步骤数
    average_duration = FloatField(default=0.0)  # 平均执行时长
    created_at = DateTimeField(default=datetime.now)
    
    class Meta:
        table_name = 'operation_sequence'

class OperationPattern(BaseModel):
    """操作模式模型"""
    id = CharField(primary_key=True)  # 模式ID
    apk = ForeignKeyField(Apk, backref='operation_patterns', on_delete='CASCADE')  # 关联的APK
    pattern_type = CharField()  # 模式类型
    pattern_data = TextField()  # 模式数据（JSON格式存储）
    confidence = FloatField(default=0.0)  # 置信度
    frequency = IntegerField(default=0)  # 出现频率
    created_at = DateTimeField(default=datetime.now)
    
    class Meta:
        table_name = 'operation_pattern'

class UserBehavior(BaseModel):
    """用户行为分析模型"""
    id = CharField(primary_key=True)  # 行为ID
    apk = ForeignKeyField(Apk, backref='user_behaviors', on_delete='CASCADE')  # 关联的APK
    behavior_type = CharField()  # 行为类型
    behavior_data = TextField()  # 行为数据（JSON格式存储）
    analysis_result = TextField(null=True)  # 分析结果（JSON格式存储）
    created_at = DateTimeField(default=datetime.now)
    
    class Meta:
        table_name = 'user_behavior'

class OperationStatistics(BaseModel):
    """操作统计模型"""
    id = CharField(primary_key=True)  # 统计ID
    apk = ForeignKeyField(Apk, backref='operation_statistics', on_delete='CASCADE')  # 关联的APK
    statistic_type = CharField()  # 统计类型
    statistic_data = TextField()  # 统计数据（JSON格式存储）
    period_start = DateTimeField()  # 统计周期开始
    period_end = DateTimeField()  # 统计周期结束
    created_at = DateTimeField(default=datetime.now)
    
    class Meta:
        table_name = 'operation_statistics'

# ========== 分析模块数据库模型 ==========

class AnalysisResult(BaseModel):
    """分析结果模型"""
    id = CharField(primary_key=True)  # 结果ID
    apk = ForeignKeyField(Apk, backref='analysis_results', on_delete='CASCADE')  # 关联的APK
    analysis_type = CharField()  # 分析类型
    result_data = TextField()  # 结果数据（JSON格式存储）
    confidence = FloatField(default=0.0)  # 置信度
    analysis_time = DateTimeField(default=datetime.now)
    
    class Meta:
        table_name = 'analysis_result'

class AnalysisTask(BaseModel):
    """分析任务模型"""
    id = CharField(primary_key=True)  # 任务ID
    apk = ForeignKeyField(Apk, backref='analysis_tasks', on_delete='CASCADE')  # 关联的APK
    task_type = CharField()  # 任务类型
    status = CharField(default='pending')  # 任务状态
    parameters = TextField(null=True)  # 任务参数（JSON格式存储）
    result = TextField(null=True)  # 任务结果（JSON格式存储）
    created_at = DateTimeField(default=datetime.now)
    
    class Meta:
        table_name = 'analysis_task'

class AnalysisPattern(BaseModel):
    """分析模式模型"""
    id = CharField(primary_key=True)  # 模式ID
    apk = ForeignKeyField(Apk, backref='analysis_patterns', on_delete='CASCADE')  # 关联的APK
    pattern_type = CharField()  # 模式类型
    pattern_data = TextField()  # 模式数据（JSON格式存储）
    confidence = FloatField(default=0.0)  # 置信度
    created_at = DateTimeField(default=datetime.now)
    
    class Meta:
        table_name = 'analysis_pattern'

class AnalysisReport(BaseModel):
    """分析报告模型"""
    id = CharField(primary_key=True)  # 报告ID
    apk = ForeignKeyField(Apk, backref='analysis_reports', on_delete='CASCADE')  # 关联的APK
    report_type = CharField()  # 报告类型
    content = TextField()  # 报告内容（JSON格式存储）
    generated_at = DateTimeField(default=datetime.now)
    
    class Meta:
        table_name = 'analysis_report'

# 创建所有表
def create_tables():
    """创建所有数据库表"""
    with db:
        db.create_tables([
            # APK模块
            Apk,
            # 设备模块
            Device, DeviceConnectionLog, DeviceApp,
            # 截屏模块
            Screenshot, PageElement, ScreenshotAnalysisResult, PageStructure,
            # 用户操作模块
            UserOperation, OperationSequence, OperationPattern, UserBehavior, OperationStatistics,
            # 分析模块
            AnalysisResult, AnalysisTask, AnalysisPattern, AnalysisReport
        ])

# 初始化数据库
if __name__ == "__main__":
    create_tables()
    print("数据库表创建完成！")


