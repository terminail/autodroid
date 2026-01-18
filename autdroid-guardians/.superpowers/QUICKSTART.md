# Superpowers 工作流系统 - 快速开始指南

## 概述

Superpowers 工作流系统是为 Android 开发项目设计的 AI 工作流管理系统，基于 Superpowers 框架理念。它提供了一套完整的开发流程，包括设计细化、任务分解、测试驱动开发、代码审查等功能。

## 文件结构

```
.superpowers/
├── config.yaml                 # 配置文件
├── workflow.py                 # Python 版本工作流脚本
├── workflow.ps1                # PowerShell 版本工作流脚本
├── test_workflow.py            # Python 版本测试脚本
├── test_workflow.ps1           # PowerShell 版本测试脚本
├── README.md                   # 详细文档
├── .gitignore                  # Git 忽略文件
├── examples/                   # 示例文件
│   ├── brainstorming_example.json
│   └── writing-plans_example.json
├── design/                     # 设计文档输出目录
├── plans/                      # 实施计划输出目录
├── reviews/                    # 审查报告输出目录
├── logs/                       # 日志输出目录
└── worktrees/                  # Git worktree 目录
```

## 快速开始

### 1. 安装依赖

**Python 版本：**

```bash
pip install pyyaml
```

**PowerShell 版本：**

```powershell
Install-Module -Name PowerShell-Yaml -Scope CurrentUser
```

### 2. 验证安装

**Python 版本：**

```bash
python .superpowers/workflow.py --help
```

**PowerShell 版本：**

```powershell
.\.superpowers\workflow.ps1 -?
```

### 3. 运行测试

**Python 版本：**

```bash
python .superpowers/test_workflow.py
```

**PowerShell 版本：**

```powershell
powershell -ExecutionPolicy Bypass -File .superpowers\test_workflow.ps1
```

## 使用示例

### 示例 1：设计细化

当你有一个新功能需求时：

**Python 版本：**

```bash
python .superpowers/workflow.py brainstorm "添加新的报警触发方式"
```

**PowerShell 版本：**

```powershell
.\.superpowers\workflow.ps1 -Brainstorm "添加新的报警触发方式"
```

这将创建一个设计文档，包含需求描述、澄清问题、替代方案等。

### 示例 2：创建工作空间

设计方案确认后：

**Python 版本：**

```bash
python .superpowers/workflow.py worktree "new-alarm-trigger"
```

**PowerShell 版本：**

```powershell
.\.superpowers\workflow.ps1 -Worktree "new-alarm-trigger"
```

这将创建新的 feature 分支和 git worktree。

### 示例 3：编写实施计划

将工作分解为小任务：

**Python 版本：**

```bash
python .superpowers/workflow.py plan .superpowers/design/brainstorming_20260118_120000.json
```

**PowerShell 版本：**

```powershell
.\.superpowers\workflow.ps1 -Plan ".superpowers\design\brainstorming_20260118_120000.json"
```

### 示例 4：执行计划

批量执行任务：

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

### 示例 5：代码审查

执行完成后进行代码审查：

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

### 示例 6：完成开发分支

所有任务完成并通过审查后：

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

## 配置文件

配置文件位于 `.superpowers/config.yaml`，包含：

- **工作流阶段配置**：定义各个阶段的触发条件、动作、输出等
- **Android 项目配置**：构建命令、质量检查等
- **任务约束**：任务大小限制
- **TDD 规则**：测试驱动开发规则
- **代码审查标准**：审查标准配置
- **输出位置**：各类文件的输出目录

## 日志和输出

### 日志文件

所有日志文件存储在 `.superpowers/logs/` 目录：

- `brainstorming.log`：设计细化日志
- `using-git-worktrees.log`：worktree 创建日志
- `writing-plans.log`：计划编写日志
- `test-driven-development.log`：TDD 日志
- `executing-plans.log`：计划执行日志
- `requesting-code-review.log`：代码审查日志
- `finishing-a-development-branch.log`：分支完成日志

### 输出文件

- **设计文档**：`.superpowers/design/brainstorming_*.json`
- **实施计划**：`.superpowers/plans/writing-plans_*.json`
- **审查报告**：`.superpowers/reviews/requesting-code-review_*.json`
- **备份**：`.superpowers/backups/backup_*.tar.gz`

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

## 最佳实践

1. **任务分解**
   - 每个任务应该在 2-5 分钟内完成
   - 每个任务应该有明确的文件路径
   - 每个任务应该有完整的代码示例
   - 每个任务应该有验证步骤

2. **测试驱动开发**
   - 始终先编写测试
   - 确保测试失败后再编写实现
   - 编写最小代码使测试通过
   - 立即重构代码
   - 不要跳过任何步骤

3. **代码审查**
   - 对照实施计划审查
   - 检查代码质量
   - 检查测试覆盖率
   - 按严重程度报告问题
   - 关键问题阻止进度

4. **分支管理**
   - 使用 git worktree 创建独立工作空间
   - 每个功能使用单独的分支
   - 完成后及时合并或清理
   - 保持主分支干净

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

## 示例文件

项目包含示例文件，可以帮助你了解如何使用：

- `.superpowers/examples/brainstorming_example.json`：设计文档示例
- `.superpowers/examples/writing-plans_example.json`：实施计划示例

## 参考资源

- [Superpowers 原始项目](https://github.com/obra/superpowers)
- [Android 开发最佳实践](https://developer.android.com/)
- [测试驱动开发](https://en.wikipedia.org/wiki/Test-driven_development)
- [Git Worktree 文档](https://git-scm.com/docs/git-worktree)
- [.superpowers/README.md](.superpowers/README.md)：详细文档

## 支持

如有问题或建议，请：

1. 查看详细文档：[.superpowers/README.md](.superpowers/README.md)
2. 查看示例文件：`.superpowers/examples/`
3. 查看日志文件：`.superpowers/logs/`
4. 运行测试脚本：`python .superpowers/test_workflow.py`

## 许可证

MIT License
