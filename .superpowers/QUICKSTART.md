# Superpowers 全局工作流系统 - 快速开始

## 概述

Superpowers 全局工作流系统适用于 `d:/git/autodroid/` 下的所有项目，采用混合架构：全局基础配置 + 项目特定定制。

## 一分钟快速开始

### 1. 安装到项目

在任意项目目录下运行：

```bash
python d:/git/autodroid/.superpowers/install.py install
```

### 2. 使用工作流

```bash
# 设计细化
python .superpowers/workflow.py brainstorm "添加新功能"

# 创建工作空间
python .superpowers/workflow.py worktree "new-feature"

# 编写计划
python .superpowers/workflow.py plan .superpowers/design/brainstorming_*.json

# 执行计划
python .superpowers/workflow.py execute .superpowers/plans/*.json .superpowers/worktrees/*

# 代码审查
python .superpowers/workflow.py review .superpowers/plans/*.json .superpowers/worktrees/*

# 完成分支
python .superpowers/workflow.py finish .superpowers/worktrees/* "new-feature"
```

## 支持的项目类型

系统会自动检测项目类型：

### Android 项目

**检测特征**：
- `build.gradle*`
- `settings.gradle*`
- `AndroidManifest.xml`

**配置模板**：`templates/android.yaml`

### Python 项目

**检测特征**：
- `requirements.txt`
- `setup.py`
- `pyproject.toml`

**配置模板**：`templates/python.yaml`

### JavaScript 项目

**检测特征**：
- `package.json`
- `yarn.lock`
- `package-lock.json`

**配置模板**：`templates/javascript.yaml`

### Web 项目

**检测特征**：
- `index.html`
- `webpack.config.js`
- `vite.config.js`

**配置模板**：`templates/web.yaml`

## 安装选项

### 基本安装（自动检测）

```bash
python d:/git/autodroid/.superpowers/install.py install
```

### 指定项目类型

```bash
# Android 项目
python d:/git/autodroid/.superpowers/install.py install --project-type android

# Python 项目
python d:/git/autodroid/.superpowers/install.py install --project-type python

# JavaScript 项目
python d:/git/autodroid/.superpowers/install.py install --project-type javascript
```

### 强制重新安装

```bash
python d:/git/autodroid/.superpowers/install.py install --force
```

## 使用选项

### 使用项目本地脚本

```bash
python .superpowers/workflow.py brainstorm "需求描述"
```

### 使用全局脚本

```bash
python d:/git/autodroid/.superpowers/workflow.py \
  --project-root d:/git/autodroid/autdroid-guardians \
  brainstorm "需求描述"
```

### 指定项目类型

```bash
python .superpowers/workflow.py \
  --project-type android \
  brainstorm "需求描述"
```

## 工作流阶段

1. **Brainstorming** - 设计细化
2. **Using Git Worktrees** - 创建独立工作空间
3. **Writing Plans** - 任务分解
4. **Test-Driven Development** - TDD 循环
5. **Executing Plans** - 批量执行
6. **Requesting Code Review** - 代码审查
7. **Finishing a Development Branch** - 完成分支

## 配置系统

### 配置优先级

1. **全局基础配置**：`d:/git/autodroid/.superpowers/config.yaml`
2. **项目模板配置**：`d:/git/autodroid/.superpowers/templates/{project_type}.yaml`
3. **项目特定配置**：`{project_root}/.superpowers/config.yaml`

### 配置示例

#### 项目特定配置

```yaml
# 继承全局配置
extends: "../config.yaml"

# 项目类型
project_type: "android"

# Android 特定配置
android_config:
  build_commands:
    - "./gradlew test"
    - "./gradlew lint"
```

## 管理命令

### 查看安装状态

```bash
python d:/git/autodroid/.superpowers/install.py status
```

### 卸载

```bash
python d:/git/autodroid/.superpowers/install.py uninstall
```

## 常见问题

### 1. 找不到全局配置

**问题**：`配置文件不存在: d:/git/autodroid/.superpowers/config.yaml`

**解决**：确保全局 Superpowers 目录存在

### 2. 项目类型检测失败

**问题**：无法检测项目类型

**解决**：使用 `--project-type` 选项指定项目类型

```bash
python d:/git/autodroid/.superpowers/install.py install --project-type android
```

### 3. 符号链接创建失败

**问题**：无法创建符号链接

**解决**：脚本会自动回退到复制文件，不影响使用

## 实际示例

### Android 项目示例

```bash
# 进入 Android 项目
cd d:/git/autodroid/autdroid-guardians

# 安装 Superpowers
python d:/git/autodroid/.superpowers/install.py install --project-type android

# 使用工作流
python .superpowers/workflow.py brainstorm "添加新的报警触发方式"
python .superpowers/workflow.py worktree "new-alarm-trigger"
python .superpowers/workflow.py plan .superpowers/design/brainstorming_*.json
python .superpowers/workflow.py execute .superpowers/plans/*.json .superpowers/worktrees/*
```

### Python 项目示例

```bash
# 进入 Python 项目
cd d:/git/autodroid/some-python-project

# 安装 Superpowers
python d:/git/autodroid/.superpowers/install.py install --project-type python

# 使用工作流
python .superpowers/workflow.py brainstorm "添加新功能"
python .superpowers/workflow.py worktree "new-feature"
python .superpowers/workflow.py plan .superpowers/design/brainstorming_*.json
python .superpowers/workflow.py execute .superpowers/plans/*.json .superpowers/worktrees/*
```

## 优势

### 统一管理

✅ 一次更新，全局生效  
✅ 共享脚本，减少维护成本  
✅ 统一标准，所有项目遵循相同的工作流

### 项目定制

✅ 灵活配置，每个项目可以覆盖特定配置  
✅ 项目模板，不同项目类型有专门的配置模板  
✅ 渐进式采用，可以选择性地启用特定功能

### 自动化

✅ 自动检测项目类型，无需手动配置  
✅ 智能合并配置，优先级清晰  
✅ 符号链接，自动创建，避免文件重复

## 下一步

- 查看详细文档：[README.md](README.md)
- 查看项目模板：`templates/` 目录
- 查看配置示例：`config.yaml`

## 获取帮助

```bash
# 查看安装脚本帮助
python d:/git/autodroid/.superpowers/install.py --help

# 查看工作流脚本帮助
python .superpowers/workflow.py --help
```

## 参考资料

- [Superpowers 原始项目](https://github.com/obra/superpowers)
- [Android 开发最佳实践](https://developer.android.com/)
- [Python 开发最佳实践](https://docs.python.org/)
- [测试驱动开发](https://en.wikipedia.org/wiki/Test-driven_development)
