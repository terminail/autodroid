# Superpowers 全局工作流系统

适用于 `d:/git/autodroid/` 下所有项目的全局工作流管理系统。

## 概述

Superpowers 全局工作流系统采用**混合架构**，结合了全局统一管理和项目定制的优势：

- **全局基础配置**：所有项目共享统一的工作流和最佳实践
- **项目特定配置**：每个项目可以覆盖特定配置以适应不同需求
- **自动项目检测**：自动识别项目类型（Android、Python、JavaScript、Web等）
- **统一脚本管理**：所有项目使用同一套脚本，减少维护成本

## 架构设计

```
d:/git/autodroid/
├── .superpowers/                    # 全局工作流系统
│   ├── config.yaml                  # 全局基础配置
│   ├── workflow.py                  # 全局工作流脚本
│   ├── workflow.ps1                 # 全局 PowerShell 脚本
│   ├── install.py                   # 项目安装脚本
│   ├── templates/                   # 项目模板
│   │   ├── android.yaml           # Android 项目配置
│   │   ├── python.yaml            # Python 项目配置
│   │   └── javascript.yaml        # JavaScript 项目配置
│   └── README.md                   # 本文档
│
├── autdroid-guardians/             # Android 项目
│   └── .superpowers/              # 项目特定配置（可选）
│       └── config.yaml             # 覆盖全局配置
│
├── autodroid-trader-aas/          # 另一个项目
│   └── .superpowers/              # 项目特定配置（可选）
│       └── config.yaml             # 覆盖全局配置
│
└── ...
```

## 快速开始

### 1. 安装到项目

在任意项目目录下运行安装脚本：

```bash
python d:/git/autodroid/.superpowers/install.py install
```

这将：
- 创建项目的 `.superpowers` 目录
- 复制配置文件
- 创建符号链接到全局脚本
- 创建输出目录

### 2. 验证安装

```bash
python d:/git/autodroid/.superpowers/install.py status
```

### 3. 使用工作流

安装后，可以在项目目录下使用工作流：

```bash
# 使用项目本地脚本
python .superpowers/workflow.py brainstorm "添加新功能"

# 或使用全局脚本
python d:/git/autodroid/.superpowers/workflow.py --project-root . brainstorm "添加新功能"
```

## 使用示例

### 示例 1：Android 项目

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

### 示例 2：Python 项目

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

### 示例 3：自动检测项目类型

```bash
# 不指定项目类型，自动检测
python d:/git/autodroid/.superpowers/install.py install

# 工作流会自动检测项目类型
python .superpowers/workflow.py brainstorm "添加新功能"
```

## 工作流阶段

1. **Brainstorming（设计细化）**
   - 通过问题澄清需求
   - 探索替代方案
   - 展示设计供验证

2. **Using Git Worktrees（使用 Git Worktrees）**
   - 创建独立的工作空间
   - 避免与其他开发冲突

3. **Writing Plans（编写计划）**
   - 将工作分解为小任务（2-5分钟每个）
   - 每个任务都有文件路径、代码示例、验证步骤

4. **Test-Driven Development（测试驱动开发）**
   - RED-GREEN-REFACTOR 循环
   - 强制执行 TDD 最佳实践

5. **Executing Plans（执行计划）**
   - 批量执行任务
   - 设置人工检查点

6. **Requesting Code Review（请求代码审查）**
   - 对照计划审查
   - 按严重程度报告问题

7. **Finishing a Development Branch（完成开发分支）**
   - 验证测试
   - 展示选项（合并/PR/保留/丢弃）
   - 清理 worktree

## 配置系统

### 配置优先级

配置按以下优先级合并（从低到高）：

1. **全局基础配置**：`d:/git/autodroid/.superpowers/config.yaml`
2. **项目模板配置**：`d:/git/autodroid/.superpowers/templates/{project_type}.yaml`
3. **项目特定配置**：`{project_root}/.superpowers/config.yaml`

高优先级配置会覆盖低优先级配置。

### 配置示例

#### 全局基础配置

```yaml
version: "1.0"
global: true

workflow:
  phases:
    - name: "brainstorming"
      description: "设计细化阶段"
      # ...

task_constraints:
  max_duration_minutes: 5
  max_files_per_task: 3
  prefer_small_tasks: true

tdd_rules:
  write_test_first: true
  watch_test_fail: true
  # ...

project_detection:
  android:
    patterns:
      - "build.gradle*"
      - "AndroidManifest.xml"
    config_template: "android.yaml"
  
  python:
    patterns:
      - "requirements.txt"
      - "setup.py"
    config_template: "python.yaml"
```

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
  
  quality_checks:
    - name: "Lint"
      command: "./gradlew lint"
      fail_on_error: false
```

## 项目类型支持

### Android 项目

**检测特征**：
- `build.gradle*`
- `settings.gradle*`
- `AndroidManifest.xml`

**配置模板**：`templates/android.yaml`

**特定功能**：
- Gradle 构建命令
- Lint、Detekt、KtLint 检查
- Android 权限检查
- 资源检查

### Python 项目

**检测特征**：
- `requirements.txt`
- `setup.py`
- `pyproject.toml`
- `Pipfile`

**配置模板**：`templates/python.yaml`

**特定功能**：
- pytest 测试
- MyPy 类型检查
- Flake8、Black、isort 代码质量
- PEP8 合规检查

### JavaScript 项目

**检测特征**：
- `package.json`
- `yarn.lock`
- `package-lock.json`

**配置模板**：`templates/javascript.yaml`

**特定功能**：
- npm/yarn 构建命令
- ESLint、Prettier 检查
- Jest 测试
- TypeScript 类型检查

### Web 项目

**检测特征**：
- `index.html`
- `webpack.config.js`
- `vite.config.js`

**配置模板**：`templates/web.yaml`

**特定功能**：
- Webpack/Vite 构建命令
- HTML/CSS/JavaScript 检查
- 浏览器测试

## 安装命令

### 安装到项目

```bash
# 基本安装（自动检测项目类型）
python d:/git/autodroid/.superpowers/install.py install

# 指定项目类型
python d:/git/autodroid/.superpowers/install.py install --project-type android

# 强制重新安装
python d:/git/autodroid/.superpowers/install.py install --force
```

### 卸载

```bash
python d:/git/autodroid/.superpowers/install.py uninstall
```

### 查看状态

```bash
python d:/git/autodroid/.superpowers/install.py status
```

## 工作流命令

### 基本使用

```bash
# 设计细化
python .superpowers/workflow.py brainstorm "需求描述"

# 创建工作空间
python .superpowers/workflow.py worktree "功能名称"

# 编写实施计划
python .superpowers/workflow.py plan <设计文档路径>

# 执行计划
python .superpowers/workflow.py execute <计划路径> <工作空间路径>

# 代码审查
python .superpowers/workflow.py review <计划路径> <工作空间路径>

# 完成开发分支
python .superpowers/workflow.py finish <工作空间路径> <功能名称>
```

### 指定项目根目录

```bash
# 使用全局脚本，指定项目根目录
python d:/git/autodroid/.superpowers/workflow.py \
  --project-root d:/git/autodroid/autdroid-guardians \
  brainstorm "需求描述"
```

### 指定项目类型

```bash
# 覆盖自动检测的项目类型
python .superpowers/workflow.py \
  --project-type android \
  brainstorm "需求描述"
```

## 优势

### 统一管理

✅ **一次更新，全局生效**：更新全局配置，所有项目自动受益  
✅ **共享脚本**：所有项目使用同一套脚本，减少维护成本  
✅ **统一标准**：所有项目遵循相同的工作流和最佳实践  

### 项目定制

✅ **灵活配置**：每个项目可以覆盖特定配置  
✅ **项目模板**：不同项目类型有专门的配置模板  
✅ **渐进式采用**：可以选择性地启用特定功能  

### 自动化

✅ **自动检测**：自动识别项目类型，无需手动配置  
✅ **智能合并**：配置自动合并，优先级清晰  
✅ **符号链接**：自动创建符号链接，避免文件重复  

## 最佳实践

### 1. 配置管理

- 将通用配置放在全局配置中
- 将项目特定配置放在项目配置中
- 使用项目模板快速开始新项目

### 2. 项目安装

- 每个项目只需安装一次
- 使用 `--force` 选项重新安装
- 使用 `status` 命令检查安装状态

### 3. 工作流使用

- 遵循工作流阶段的顺序
- 在每个检查点暂停并确认
- 使用 TDD 循环确保代码质量

### 4. 配置更新

- 更新全局配置时，所有项目自动受益
- 项目配置可以覆盖全局配置
- 定期检查配置是否需要更新

## 故障排除

### 常见问题

1. **找不到全局配置**

```
错误: 配置文件不存在: d:/git/autodroid/.superpowers/config.yaml
解决: 确保全局 Superpowers 目录存在
```

2. **项目类型检测失败**

```
错误: 无法检测项目类型
解决: 使用 --project-type 选项指定项目类型
```

3. **符号链接创建失败**

```
错误: 无法创建符号链接
解决: 脚本会自动回退到复制文件
```

4. **配置合并失败**

```
错误: 配置合并失败
解决: 检查配置文件格式是否正确
```

### 调试技巧

1. **查看安装状态**

```bash
python d:/git/autodroid/.superpowers/install.py status
```

2. **查看日志文件**

```bash
cat .superpowers/logs/brainstorming.log
```

3. **验证配置**

```bash
cat .superpowers/config.yaml
```

4. **检查符号链接**

```bash
ls -la .superpowers/workflow.py
```

## 与 AI 工具集成

### Claude Code

Claude Code 可以直接使用这个工作流系统：

1. **安装到项目**

```
python d:/git/autodroid/.superpowers/install.py install
```

2. **使用工作流**

```
/superpowers:brainstorm 添加新的报警触发方式
/superpowers:write-plan
/superpowers:execute-plan
```

### 其他 AI 工具

对于其他 AI 工具，可以：

1. **直接调用脚本**

```bash
python .superpowers/workflow.py brainstorm "需求描述"
```

2. **使用全局脚本**

```bash
python d:/git/autodroid/.superpowers/workflow.py \
  --project-root <项目路径> \
  brainstorm "需求描述"
```

3. **读取配置**

AI 工具可以读取 `.superpowers/config.yaml` 了解工作流配置

4. **遵循工作流阶段**

AI 工具应该按照工作流阶段顺序执行

## 贡献

欢迎贡献！请遵循以下步骤：

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

MIT License

## 联系方式

如有问题或建议，请创建 Issue 或 Pull Request。

## 参考资料

- [Superpowers 原始项目](https://github.com/obra/superpowers)
- [Android 开发最佳实践](https://developer.android.com/)
- [Python 开发最佳实践](https://docs.python.org/)
- [测试驱动开发](https://en.wikipedia.org/wiki/Test-driven_development)
- [Git Worktree 文档](https://git-scm.com/docs/git-worktree)
