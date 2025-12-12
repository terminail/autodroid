# APK 分析工具 - Quick Start

## 简介
`list_apks.py` 是一个用于从连接的 Android 设备上获取已安装用户 APK 信息的工具，包括基本信息和加固信息。

## 快速开始

### 1. 准备工作
确保满足以下条件：
- 已安装 Python 3.7+
- 已安装 ADB 并添加到系统 PATH
- Android 设备已通过 USB 连接并启用 USB 调试

### 2. 安装依赖
```bash
pip install pyyaml
```

### 3. 基本使用
```bash
cd 'd:/git/autodroid/autodroid-analyzer'; conda activate liugejiao; python core/apk/list_apks.py
```

### 4. 功能说明
运行脚本后，工具将：
1. 自动检测所有连接的 Android 设备
2. 获取设备上已安装的用户 APK 列表
3. 收集每个 APK 的基本信息（包名、应用名、版本等）
4. 可选进行 APK 加固检测
5. 将结果保存到数据库和/或导出到文件

### 5. 配置选项
所有配置都在 `../config.yaml` 文件中，主要配置项：
- `apk.analyze_packer`: 是否进行 APK 加固检测（默认：true）
- `apk.save_basic_info`: 是否保存基础信息（默认：true）
- `apk.extract_apk`: 是否提取 APK 文件到本地（默认：true）

### 6. 输出结果
- 数据库文件：`analyzer.db`
- 导出文件：`analysis_output/reports/apk_list.json`

## 注意事项
- 确保设备已授权 USB 调试
- 首次运行可能需要较长时间进行加固检测
- 结果会自动保存，无需额外操作