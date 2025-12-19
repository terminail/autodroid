# 第三方APP自动化测试脚本 (ADB-Based)

基于纯ADB命令的Python测试脚本，兼容新旧安卓WiFi连接方式，适配加固APP的控件定位。**已移除Appium依赖，完全基于ADB命令实现。**

## 目录结构

workscripts目录按照APK的package name组织：

```
workscripts/
├── com.example.app/              # 示例APP测试脚本
│   ├── test_script.py           # 主测试脚本
│   ├── config.py                # 配置文件
│   ├── templates/               # 图像识别模板
│   │   ├── login_button.png
│   │   ├── submit_button.png
│   │   └── *.png              # 其他控件截图
│   └── screenshots/             # 测试截图
├── com.autodroid.manager/       # 自研APP测试脚本（空目录）
├── sample-workscript.py         # Python格式简单测试脚本
├── Dockerfile                   # Docker镜像构建文件
└── README.md                    # 使用说明

## 功能特性

- ✅ **WiFi调试兼容**：支持新旧安卓设备的WiFi连接方式
- ✅ **加固APP适配**：控件ID + XPath + 图像识别 + 坐标定位四级降级方案
- ✅ **失败捕获**：自动截图保存失败现场
- ✅ **测试断言**：验证操作结果
- ✅ **容器化支持**：Docker容器一键运行
- ✅ **环境变量配置**：支持通过环境变量传递参数
- ✅ **纯ADB实现**：完全基于ADB命令，无需Appium
- ✅ **快速执行**：直接设备控制，无Appium服务器开销

## 快速开始

### 1. 环境准备

确保手机已开启WiFi调试：
- **安卓11+**：设置 → 开发者选项 → 无线调试 → 开启
- **安卓10及以下**：需先用USB连接，执行 `adb tcpip 5555` 开启WiFi调试

### 2. 本地运行测试

```bash
# 进入对应的APP目录（以com.example.app为例）
cd com.example.app

# 安装依赖（在主项目目录安装所有依赖）
cd ../..  # 回到autodroid-trader-server根目录
pip install -e .

# 回到APP目录
cd workscripts/com.example.app

# 配置参数（修改config.py中的IP和APP信息）
# 或直接设置环境变量
export PHONE_IP="192.168.1.100"
export APP_PACKAGE="com.example.app"

# 运行测试
python test_script.py
```

### 3. Docker容器运行

```bash
# 构建镜像（在workscripts根目录）
docker build -t android-auto-test:v1 .

# 运行测试（进入对应APP目录）
cd com.example.app
docker run --network host \
  -e PHONE_IP=192.168.1.100 \
  -e APP_PACKAGE=com.example.app \
  -v $(pwd)/screenshots:/app/screenshots \
  -v $(pwd)/templates:/app/templates \
  android-auto-test:v1

# 交互模式调试
docker run -it --network host \
  -e PHONE_IP=192.168.1.100 \
  -v $(pwd)/screenshots:/app/screenshots \
  -v $(pwd)/templates:/app/templates \
  android-auto-test:v1 /bin/bash

# 容器内手动执行
python test_script.py
```

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| PHONE_IP | 手机WiFi IP地址 | 192.168.1.100 |
| PHONE_PORT | WiFi调试端口 | 5555 |
| APP_PACKAGE | APP包名 | com.autodroid.manager |
| APP_ACTIVITY | APP主Activity | .MainActivity |
| TEST_USERNAME | 测试用户名 | testuser |
| TEST_PASSWORD | 测试密码 | testpass |
| TEST_INPUT_TEXT | 测试输入文本 | 自动化测试内容 |

### 配置文件

在每个APP目录下修改 `config.py` 文件可以自定义：
- 连接超时和重试配置
- 操作延迟范围（规避风控）
- 截图保存路径
- 日志配置
- 断言条件

## 加固APP适配方案

### 1. 控件ID定位（首选）
```python
# 适用于非加固APP
element = d(resourceId="com.xxx.xxx:id/button_submit")
element.click()
```

### 2. XPath定位（备选）
```python
# 适用于控件ID被隐藏的加固APP
element = d.xpath('//android.widget.Button[@text="提交"]')
if element.exists:
    element.click()
```

### 3. 图像识别（加固APP首选）
```python
# 需要提前截图保存模板图片
from airtest.core.api import *
if exists(Template("submit_button.png")):
    click(Template("submit_button.png"))
```

### 4. 坐标定位（最后备选）
```python
# 需要提前获取控件坐标
d.click(500, 800)  # x, y坐标
```

## 图像识别模板制作

1. **使用weditor获取控件截图**
   ```bash
   pip install weditor
   weditor
   # 浏览器打开 http://localhost:17310
   # 连接手机后截图，框选目标控件，保存为PNG图片
   ```

2. **模板图片放置位置**
   - 本地运行：放在项目目录下
   - Docker运行：放在挂载的templates目录

3. **模板命名规范**
   - 登录按钮：`login_button.png`
   - 提交按钮：`submit_button.png`
   - 确认按钮：`confirm_button.png`

## 测试结果

### 成功结果
```
✅ 成功连接手机：192.168.1.100:5555
✅ APP启动成功
✅ 自动化测试执行成功！
```

### 失败处理
- 自动截图保存失败现场
- 截图文件命名：`fail_screenshot_时间戳.png`
- 截图保存在 `/app/screenshots/` 目录

## 故障排查

| 问题 | 可能原因 | 解决方案 |
|------|----------|----------|
| 连接失败 | 手机未开启WiFi调试 | 检查开发者选项设置 |
| 控件定位失败 | APP加固或控件属性变化 | 使用图像识别或坐标定位 |
| 测试超时 | 网络延迟或APP响应慢 | 增加超时时间配置 |
| 截图失败 | 权限问题 | 检查Docker挂载权限 |

## 高级功能

### 多设备测试
```python
# 修改config.py支持多IP
PHONE_IPS = ["192.168.1.100", "192.168.1.101"]
for ip in PHONE_IPS:
    # 执行测试逻辑
```

### 随机延迟规避风控
```python
# 已内置随机延迟
time.sleep(random.uniform(0.5, 2.0))
```

### 自定义断言条件
```python
# 修改config.py中的断言配置
ASSERTION_CONFIG = {
    "success_indicators": ["登录成功", "主页面"],
    "failure_indicators": ["登录失败", "错误"]
}
```

## 更新日志

- v1.0.0: 初始版本，支持基本测试功能
- 后续将根据实际需求添加更多测试用例和适配方案