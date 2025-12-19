# Workscripts目录结构说明

本目录按照APK的package name组织测试脚本，每个APP拥有独立的测试环境。

## 目录规范

```
workscripts/
├── {package.name}/              # 按照APK包名命名的目录
│   ├── test_script.py          # 主测试脚本
│   ├── config.py               # APP专用配置文件
│   ├── templates/              # 图像识别模板
│   │   ├── login_button.png
│   │   ├── submit_button.png
│   │   └── *.png              # 其他控件截图
│   ├── screenshots/            # 测试截图保存
│   │   ├── success_*.png      # 成功截图
│   │   ├── fail_*.png         # 失败截图
│   │   └── debug_*.png        # 调试截图
│   └── README.md              # APP专用说明（可选）
├── shared/                     # 共享资源（可选）
├── common/                     # 通用工具函数（可选）
├── Dockerfile                  # Docker镜像构建
└── README.md                  # 本说明文档
```

## 使用说明

### 1. 为新APP创建测试环境

```bash
# 1. 创建APP目录（以包名命名）
mkdir com.yourcompany.yourapp
cd com.yourcompany.yourapp

# 2. 复制模板文件
cp ../com.example.app/test_script.py .
cp ../com.example.app/config.py .

# 3. 创建必要目录
mkdir -p templates screenshots

# 4. 修改配置文件
vim config.py  # 修改APP_PACKAGE等参数
```

### 2. 配置APP专用参数

在每个APP目录的`config.py`中配置：

```python
# APP基本信息
APP_PACKAGE = "com.yourcompany.yourapp"
APP_ACTIVITY = ".MainActivity"

# 测试数据
TEST_USERNAME = "your_test_user"
TEST_PASSWORD = "your_test_pass"

# 图像识别模板
LOGIN_BUTTON_TEMPLATE = "login_button.png"
SUBMIT_BUTTON_TEMPLATE = "submit_button.png"
```

### 3. 制作图像识别模板

```bash
# 1. 启动weditor
weditor

# 2. 连接设备并截图
# 3. 框选目标控件
# 4. 保存PNG图片到templates目录
# 5. 在config.py中引用模板文件名
```

### 4. 运行测试

```bash
# 进入APP目录
cd com.yourcompany.yourapp

# 本地运行
python test_script.py

# Docker运行
docker run --network host \
  -e PHONE_IP=192.168.1.100 \
  -e APP_PACKAGE=com.yourcompany.yourapp \
  -v $(pwd)/screenshots:/app/screenshots \
  -v $(pwd)/templates:/app/templates \
  android-auto-test:v1
```

## 目录命名规范

### Package Name规则
- 使用APP的实际包名（如：com.tencent.mm）
- 全部小写
- 使用点号分隔
- 避免特殊字符

### 文件命名规范
- **test_script.py**: 主测试脚本（固定名称）
- **config.py**: 配置文件（固定名称）
- **templates/*.png**: 图像模板（描述性名称）
- **screenshots/*.png**: 截图文件（时间戳+描述）

## 示例目录

当前已创建的示例目录：

```
com.example.app/          # 示例APP测试脚本
├── test_script.py       # 完整的测试脚本示例
├── config.py            # 配置文件示例
├── requirements.txt     # 依赖包列表
├── templates/           # 图像模板目录（空）
└── screenshots/         # 截图保存目录（空）

com.autodroid.manager/   # 自研APP测试目录（预留）
```

## 最佳实践

### 1. 每个APP独立目录
- 避免不同APP之间的配置冲突
- 便于版本管理和权限控制
- 支持不同APP使用不同测试框架

### 2. 图像模板管理
- 为每个关键控件制作模板
- 使用描述性文件名
- 定期更新模板（APP UI变更时）

### 3. 测试结果管理
- 按时间戳命名截图
- 分类保存成功/失败截图
- 定期清理旧截图

### 4. 配置分离
- 通用配置放在根目录
- APP专用配置放在各自目录
- 敏感信息使用环境变量

## 扩展功能

### 多环境支持
```
com.example.app/
├── dev/                   # 开发环境
├── test/                  # 测试环境
└── prod/                  # 生产环境
```

### 多版本支持
```
com.example.app/
├── v1.0.0/               # 版本1.0.0
├── v2.0.0/               # 版本2.0.0
└── latest/               # 最新版本
```

### 共享资源
```
shared/
├── common_functions.py    # 通用函数
├── device_utils.py       # 设备工具
└── image_utils.py        # 图像处理工具
```

## 注意事项

1. **包名准确性**: 确保使用正确的APP包名
2. **权限管理**: 不同团队可以拥有不同APP目录的访问权限
3. **版本控制**: 建议为每个APP测试脚本建立独立的Git仓库
4. **备份策略**: 重要的图像模板和配置文件需要备份
5. **文档维护**: 及时更新各APP的专用说明文档