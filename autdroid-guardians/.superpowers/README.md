# Superpowers 工作流系统

适用于 Android 开发项目的 AI 工作流管理系统，基于 Superpowers 框架理念。

## 概述

Superpowers 工作流系统为 Android 项目提供了一套完整的开发流程，包括设计细化、任务分解、测试驱动开发、代码审查等功能。该系统旨在帮助 AI 工具和开发者遵循最佳实践，提高代码质量和开发效率。

## 核心特性

- **设计细化**：通过问题澄清需求，探索替代方案
- **Git Worktree**：创建独立的工作空间，避免冲突
- **任务分解**：将工作分解为小任务（2-5分钟每个）
- **测试驱动开发**：强制执行 RED-GREEN-REFACTOR 循环
- **批量执行**：支持批量执行任务，设置人工检查点
- **代码审查**：自动运行代码质量检查
- **系统化调试**：4 阶段根因分析流程

## 安装

### 前置要求

- Python 3.7+（如果使用 Python 版本）
- PowerShell 5.1+（如果使用 PowerShell 版本）
- Git 2.0+
- Android SDK
- Gradle

### 安装步骤

1. **克隆项目**

```bash
git clone <repository-url>
cd autdroid-guardians
```

2. **安装依赖**

**Python 版本：**

```bash
pip install pyyaml
```

**PowerShell 版本：**

```powershell
Install-Module -Name PowerShell-Yaml -Scope CurrentUser
```

3. **验证安装**

**Python 版本：**

```bash
python .superpowers/workflow.py --help
```

**PowerShell 版本：**

```powershell
.\.superpowers\workflow.ps1 -?
```

## 快速开始

### 1. 设计细化

当你有一个新功能需求时，首先进行设计细化：

**Python 版本：**

```bash
python .superpowers/workflow.py brainstorm "添加新的报警触发方式"
```

**PowerShell 版本：**

```powershell
.\.superpowers\workflow.ps1 -Brainstorm "添加新的报警触发方式"
```

这将创建一个设计文档，包含：
- 需求描述
- 澄清问题
- 替代方案
- 推荐设计
- 实施计划概要

### 2. 创建工作空间

设计方案确认后，创建独立的工作空间：

**Python 版本：**

```bash
python .superpowers/workflow.py worktree "new-alarm-trigger"
```

**PowerShell 版本：**

```powershell
.\.superpowers\workflow.ps1 -Worktree "new-alarm-trigger"
```

这将：
- 创建新的 feature 分支
- 创建 git worktree
- 验证项目设置
- 运行测试基线

### 3. 编写实施计划

将工作分解为小任务：

**Python 版本：**

```bash
python .superpowers/workflow.py plan .superpowers/design/brainstorm_20260118_120000.json
```

**PowerShell 版本：**

```powershell
.\.superpowers\workflow.ps1 -Plan ".superpowers\design\brainstorm_20260118_120000.json"
```

每个任务应该：
- 可以在 2-5 分钟内完成
- 包含明确的文件路径
- 包含完整的代码示例
- 包含验证步骤

### 4. 执行计划

批量执行任务，设置检查点：

**Python 版本：**

```bash
python .superpowers/workflow.py execute \
  .superpowers/plans/writing-plans_20260118_130000.json \
  .superpowers/worktrees/new-alarm-trigger \
  --batch-size 3
```

**PowerShell 版本：**

```powershell
.\.superpowers\workflow.ps1 `
  -Execute ".superpowers\plans\writing-plans_20260118_130000.json" `
  -Worktree ".superpowers\worktrees\new-alarm-trigger" `
  -BatchSize 3
```

这将：
- 按 3 个任务一批执行
- 在每个检查点暂停
- 等待用户确认
- 继续下一批次

### 5. 代码审查

执行完成后，进行代码审查：

**Python 版本：**

```bash
python .superpowers/workflow.py review \
  .superpowers/plans/writing-plans_20260118_130000.json \
  .superpowers/worktrees/new-alarm-trigger
```

**PowerShell 版本：**

```powershell
.\.superpowers\workflow.ps1 `
  -Review ".superpowers\plans\writing-plans_20260118_130000.json" `
  -ReviewWorktree ".superpowers\worktrees\new-alarm-trigger"
```

这将：
- 运行代码质量检查（Lint、Detekt、测试等）
- 生成审查报告
- 标记关键问题

### 6. 完成开发分支

所有任务完成并通过审查后，完成开发分支：

**Python 版本：**

```bash
python .superpowers/workflow.py finish \
  .superpowers/worktrees/new-alarm-trigger \
  "new-alarm-trigger"
```

**PowerShell 版本：**

```powershell
.\.superpowers\workflow.ps1 `
  -Finish ".superpowers\worktrees\new-alarm-trigger" `
  -FinishFeature "new-alarm-trigger"
```

这将：
- 验证所有测试
- 展示选项（合并/PR/保留/丢弃）
- 清理 worktree

## 工作流阶段详解

### 1. Brainstorming（设计细化）

**目的**：通过问题澄清需求，探索替代方案，展示设计供验证

**触发条件**：
- 用户提出新功能需求
- 用户要求添加新特性
- 用户需要设计建议

**输出**：
- 设计文档（JSON 格式）
- 技术方案
- 实施计划概要

**完成标准**：
- 用户明确同意设计方案
- 设计文档已保存

**示例**：

```json
{
  "phase": "brainstorming",
  "timestamp": "2026-01-18T12:00:00",
  "requirement": "添加新的报警触发方式",
  "clarifying_questions": [
    "新的触发方式是什么？",
    "需要哪些权限？",
    "如何与现有系统集成？"
  ],
  "alternatives": [
    {
      "name": "方案A：使用传感器",
      "pros": ["响应快速", "无需用户操作"],
      "cons": ["耗电", "可能误触发"]
    },
    {
      "name": "方案B：使用定时器",
      "pros": ["精确控制", "低耗电"],
      "cons": ["不够灵活"]
    }
  ],
  "recommended_design": "方案A",
  "implementation_plan_outline": [
    "添加传感器监听服务",
    "实现触发逻辑",
    "更新UI设置",
    "添加测试"
  ],
  "status": "approved"
}
```

### 2. Using Git Worktrees（使用 Git Worktrees）

**目的**：创建独立的工作空间，避免与其他开发冲突

**触发条件**：
- 设计方案已批准
- 开始实施新功能

**输出**：
- 独立的工作目录
- 干净的测试基线

**完成标准**：
- worktree 创建成功
- 所有测试通过

**优势**：
- 可以同时处理多个功能
- 避免分支切换的开销
- 保持主分支干净

### 3. Writing Plans（编写计划）

**目的**：将工作分解为小任务（2-5分钟每个）

**触发条件**：
- worktree 已创建
- 设计方案已确认

**输出**：
- 详细的实施计划
- 任务清单

**完成标准**：
- 所有任务已定义
- 每个任务都有验证步骤
- 用户批准计划

**任务示例**：

```json
{
  "phase": "writing-plans",
  "timestamp": "2026-01-18T13:00:00",
  "design_doc": ".superpowers/design/brainstorm_20260118_120000.json",
  "tasks": [
    {
      "id": 1,
      "description": "创建传感器监听服务类",
      "estimated_minutes": 3,
      "files": [
        "src/main/java/com/autodroid/guardiansdk/service/SensorListenerService.kt"
      ],
      "test_file": "src/test/java/com/autodroid/guardiansdk/service/SensorListenerServiceTest.kt",
      "test_name": "SensorListenerServiceTest.testInitialization",
      "verification_steps": [
        "服务类已创建",
        "测试文件已创建",
        "测试通过"
      ]
    },
    {
      "id": 2,
      "description": "实现传感器事件监听",
      "estimated_minutes": 4,
      "files": [
        "src/main/java/com/autodroid/guardiansdk/service/SensorListenerService.kt"
      ],
      "test_file": "src/test/java/com/autodroid/guardiansdk/service/SensorListenerServiceTest.kt",
      "test_name": "SensorListenerServiceTest.testSensorEvent",
      "verification_steps": [
        "传感器事件监听已实现",
        "测试通过"
      ]
    }
  ],
  "status": "approved"
}
```

### 4. Test-Driven Development（测试驱动开发）

**目的**：强制执行 RED-GREEN-REFACTOR 循环

**触发条件**：
- 开始实施任务
- 需要编写新功能

**输出**：
- 通过的测试
- 实现的功能代码

**完成标准**：
- 所有测试通过
- 代码已重构
- 无测试反模式

**TDD 循环**：

1. **RED**：编写失败的测试
   - 编写测试代码
   - 运行测试确认失败

2. **GREEN**：编写最小代码使测试通过
   - 编写最小实现代码
   - 运行测试确认通过

3. **REFACTOR**：重构代码
   - 改进代码结构
   - 运行测试确认仍然通过

**反模式检测**：
- 在测试之前编写代码
- 跳过重构
- 忽略测试失败
- 过度工程化

### 5. Executing Plans（执行计划）

**目的**：批量执行任务，设置人工检查点

**触发条件**：
- 实施计划已批准
- 开始批量执行

**输出**：
- 已实现的功能
- 进度报告

**完成标准**：
- 所有任务完成
- 用户确认每个检查点

**检查点设置**：
- 默认每 3 个任务一个检查点
- 可配置批次大小
- 关键更改后自动检查

### 6. Requesting Code Review（请求代码审查）

**目的**：对照计划审查，按严重程度报告问题

**触发条件**：
- 任务完成
- 功能实现完成
- 达到检查点

**输出**：
- 审查报告
- 问题清单

**完成标准**：
- 审查完成
- 关键问题已解决

**审查标准**：
- 规范合规性检查
- 代码质量检查
- 测试覆盖率检查
- 文档检查

**问题严重程度**：
- **High**：阻止进度，必须修复
- **Medium**：建议修复，但不阻止
- **Low**：可选修复

### 7. Finishing a Development Branch（完成开发分支）

**目的**：验证测试，展示选项，清理 worktree

**触发条件**：
- 所有任务完成
- 所有测试通过
- 代码审查通过

**输出**：
- 合并的代码
- 清理的工作空间

**完成标准**：
- 测试通过
- 用户选择操作
- worktree 已清理

**选项**：
1. **合并到主分支**：直接合并代码
2. **创建 Pull Request**：创建 PR 供审查
3. **保留分支**：保留分支供后续使用
4. **丢弃分支**：删除分支和更改

## 配置文件

配置文件位于 `.superpowers/config.yaml`，包含以下配置：

### 工作流阶段配置

```yaml
workflow:
  phases:
    - name: "brainstorming"
      description: "设计细化阶段"
      triggers: [...]
      actions: [...]
      outputs: [...]
      completion_criteria: [...]
```

### Android 项目配置

```yaml
android_config:
  build_commands:
    - "./gradlew assembleDebug"
    - "./gradlew test"
    - "./gradlew connectedAndroidTest"
    - "./gradlew lint"
    - "./gradlew detekt"
  
  quality_checks:
    - name: "Lint"
      command: "./gradlew lint"
      fail_on_error: false
    - name: "Unit Tests"
      command: "./gradlew test"
      fail_on_error: true
```

### 任务约束

```yaml
task_constraints:
  max_duration_minutes: 5
  max_files_per_task: 3
  max_lines_per_task: 100
  prefer_small_tasks: true
```

### TDD 规则

```yaml
tdd_rules:
  write_test_first: true
  watch_test_fail: true
  write_minimal_code: true
  watch_test_pass: true
  refactor_immediately: true
  delete_code_without_tests: true
```

## 日志和输出

### 日志位置

所有日志文件存储在 `.superpowers/logs/` 目录：

- `brainstorming.log`：设计细化日志
- `using-git-worktrees.log`：worktree 创建日志
- `writing-plans.log`：计划编写日志
- `test-driven-development.log`：TDD 日志
- `executing-plans.log`：计划执行日志
- `requesting-code-review.log`：代码审查日志
- `finishing-a-development-branch.log`：分支完成日志

### 输出文件

- **设计文档**：`.superpowers/design/brainstorm_*.json`
- **实施计划**：`.superpowers/plans/writing-plans_*.json`
- **审查报告**：`.superpowers/reviews/requesting-code-review_*.json`
- **备份**：`.superpowers/backups/backup_*.tar.gz`

## 最佳实践

### 1. 任务分解

- 每个任务应该在 2-5 分钟内完成
- 每个任务应该有明确的文件路径
- 每个任务应该有完整的代码示例
- 每个任务应该有验证步骤

### 2. 测试驱动开发

- 始终先编写测试
- 确保测试失败后再编写实现
- 编写最小代码使测试通过
- 立即重构代码
- 不要跳过任何步骤

### 3. 代码审查

- 对照实施计划审查
- 检查代码质量
- 检查测试覆盖率
- 按严重程度报告问题
- 关键问题阻止进度

### 4. 分支管理

- 使用 git worktree 创建独立工作空间
- 每个功能使用单独的分支
- 完成后及时合并或清理
- 保持主分支干净

### 5. 备份和恢复

- 定期备份工作空间
- 保留最近的几个备份
- 在关键操作前备份
- 确保可以快速恢复

## 故障排除

### 常见问题

1. **配置文件不存在**

```
错误: 配置文件不存在: .superpowers/config.yaml
解决: 确保配置文件存在，路径正确
```

2. **Git worktree 创建失败**

```
错误: 创建 worktree 失败
解决: 检查 Git 版本，确保支持 worktree 功能
```

3. **测试失败**

```
错误: 测试失败
解决: 检查测试代码和实现代码，确保 TDD 循环正确执行
```

4. **代码质量检查失败**

```
错误: Lint 检查失败
解决: 修复 Lint 报告的问题，或调整配置
```

### 调试技巧

1. **查看日志文件**

```bash
cat .superpowers/logs/brainstorming.log
```

2. **运行单个命令**

```bash
python .superpowers/workflow.py brainstorm "测试需求" --help
```

3. **检查配置**

```bash
cat .superpowers/config.yaml
```

4. **验证环境**

```bash
python --version
git --version
./gradlew --version
```

## 与 AI 工具集成

### Claude Code

Claude Code 可以直接使用这个工作流系统：

1. **安装插件**

```
/plugin marketplace add obra/superpowers-marketplace
/plugin install superpowers@superpowers-marketplace
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

2. **使用配置文件**

AI 工具可以读取 `.superpowers/config.yaml` 了解工作流配置

3. **遵循工作流阶段**

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
- [测试驱动开发](https://en.wikipedia.org/wiki/Test-driven_development)
- [Git Worktree 文档](https://git-scm.com/docs/git-worktree)
