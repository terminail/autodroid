明白了！我给你一个完整的 AI 使用指南，告诉你应该怎么说和发送什么 prompt：

## 🤖 AI 使用指南

### 阶段 1：设计细化

**发送给 AI 的 Prompt：**

```
我想要为 Guardian SDK 添加新的传感器触发报警功能。

请使用 Superpowers 工作流系统，从 "brainstorming" 阶段开始：
1. 提出澄清问题
2. 探索多种解决方案
3. 展示设计选项
4. 等待我确认

项目类型：Android
项目路径：d:/git/autodroid/autdroid-guardians
```

**AI 应该做什么：**
- 运行：`python .superpowers/workflow.py brainstorm "为 Guardian SDK 添加新的传感器触发报警功能"`
- 创建设计文档
- 提出澄清问题
- 探索替代方案

---

### 阶段 2：创建工作空间

**发送给 AI 的 Prompt：**

```
设计方案已确认，请继续到 "using-git-worktrees" 阶段：

功能名称：sensor-alarm-trigger
项目路径：d:/git/autodroid/autdroid-guardians

请：
1. 创建新的 feature 分支
2. 创建 git worktree
3. 验证项目设置
4. 运行测试基线
```

**AI 应该做什么：**
- 运行：`python .superpowers/workflow.py worktree "sensor-alarm-trigger"`
- 创建分支和 worktree
- 验证项目设置

---

### 阶段 3：编写实施计划

**发送给 AI 的 Prompt：**

```
工作空间已创建，请继续到 "writing-plans" 阶段：

设计文档：.superpowers/design/brainstorming_20260118_211758.json
项目路径：d:/git/autodroid/autdroid-guardians

请：
1. 将工作分解为小任务（2-5分钟每个）
2. 为每个任务指定文件路径
3. 编写完整的代码示例
4. 定义验证步骤

任务示例：
- 创建 SensorDetector 类
- 实现传感器事件监听
- 添加设置项到 GuardianActivity
- 编写单元测试
```

**AI 应该做什么：**
- 运行：`python .superpowers/workflow.py plan .superpowers/design/brainstorming_20260118_211758.json`
- 创建实施计划
- 分解任务为小块

---

### 阶段 4：执行计划（TDD）

**发送给 AI 的 Prompt：**

```
实施计划已批准，请继续到 "executing-plans" 阶段：

计划文件：.superpowers/plans/writing-plans_20260118_211758.json
工作空间：.superpowers/worktrees/sensor-alarm-trigger
批次大小：3

请：
1. 按 TDD 循环执行每个任务（RED-GREEN-REFACTOR）
2. 每 3 个任务暂停等待确认
3. 遵循 Android 最佳实践

TDD 循环：
- RED: 编写失败的测试
- GREEN: 编写最小代码使测试通过
- REFACTOR: 重构代码
```

**AI 应该做什么：**
- 运行：`python .superpowers/workflow.py execute .superpowers/plans/writing-plans_20260118_211758.json .superpowers/worktrees/sensor-alarm-trigger --batch-size 3`
- 执行 TDD 循环
- 在检查点暂停

---

### 阶段 5：代码审查

**发送给 AI 的 Prompt：**

```
任务执行完成，请继续到 "requesting-code-review" 阶段：

计划文件：.superpowers/plans/writing-plans_20260118_211758.json
工作空间：.superpowers/worktrees/sensor-alarm-trigger

请：
1. 运行 Android 代码质量检查（Lint、Detekt、KtLint）
2. 运行单元测试
3. 对照实施计划审查
4. 按严重程度报告问题
5. 关键问题阻止进度
```

**AI 应该做什么：**
- 运行：`python .superpowers/workflow.py review .superpowers/plans/writing-plans_20260118_211758.json .superpowers/worktrees/sensor-alarm-trigger`
- 运行质量检查
- 生成审查报告

---

### 阶段 6：完成开发分支

**发送给 AI 的 Prompt：**

```
代码审查通过，请继续到 "finishing-a-development-branch" 阶段：

工作空间：.superpowers/worktrees/sensor-alarm-trigger
功能名称：sensor-alarm-trigger

请：
1. 验证所有测试
2. 展示选项（合并/PR/保留/丢弃）
3. 清理 worktree
4. 我选择：合并到主分支
```

**AI 应该做什么：**
- 运行：`python .superpowers/workflow.py finish .superpowers/worktrees/sensor-alarm-trigger "sensor-alarm-trigger"`
- 合并分支
- 清理 worktree

---

## 📋 完整工作流 Prompt 模板

### 初始 Prompt

```
我想要开发一个新功能。

项目信息：
- 项目类型：Android
- 项目路径：d:/git/autodroid/autdroid-guardians
- 功能描述：[你的功能描述]

请使用 Superpowers 工作流系统，从 "brainstorming" 阶段开始。
```

### 阶段转换 Prompt

```
当前阶段已完成，请继续到下一个阶段。

已完成阶段：[当前阶段名称]
下一步阶段：[下一个阶段名称]

相关信息：
- [阶段特定的信息]
```

### 检查点确认 Prompt

```
检查点已到达，请等待我的确认。

当前进度：
- 已完成任务：[任务数量]
- 总任务数：[总数量]
- 当前批次：[批次号]

请暂停并等待我的确认后再继续。
```

---

## 🎯 实际使用示例

### 示例 1：添加新功能

**发送给 AI：**

```
我想要为 Guardian SDK 添加摇动手机触发报警的功能。

项目信息：
- 项目类型：Android
- 项目路径：d:/git/autodroid/autdroid-guardians

请使用 Superpowers 工作流系统，从 "brainstorming" 阶段开始。
```

**AI 应该执行：**
1. 运行：`python .superpowers/workflow.py brainstorm "添加摇动手机触发报警功能"`
2. 创建设计文档
3. 提出问题：摇动强度阈值？持续时间？如何避免误触发？
4. 探索方案：使用加速度传感器、陀螺仪、组合传感器
5. 等待你确认

**你回复：**
```
我确认使用加速度传感器方案，阈值设为 15.0，持续时间为 1000ms。

请继续到 "using-git-worktrees" 阶段。
```

---

### 示例 2：修复 Bug

**发送给 AI：**

```
我需要修复一个 bug：报警触发后没有发送短信。

项目信息：
- 项目类型：Android
- 项目路径：d:/git/autodroid/autdroid-guardians
- Bug 描述：报警触发后，应该发送短信给监护人，但没有发送

请使用 Superpowers 工作流系统，从 "systematic-debugging" 阶段开始。
```

**AI 应该执行：**
1. 收集信息：查看日志、检查代码、复现问题
2. 形成假设：可能是权限问题、网络问题、代码逻辑问题
3. 验证假设：添加日志、测试不同场景
4. 修复和验证：修复问题、运行测试

---

## 📝 关键要点

### 1. 明确项目信息

每次都要告诉 AI：
- 项目类型（Android/Python/JavaScript/Web）
- 项目路径
- 功能描述或 Bug 描述

### 2. 明确阶段

告诉 AI 当前在哪个阶段，下一步要做什么：
```
请继续到 [阶段名称] 阶段
```

### 3. 明确确认点

在检查点明确告诉 AI 等待确认：
```
请暂停并等待我的确认后再继续。
```

### 4. 明确选择

在需要选择时明确告诉 AI：
```
我选择：[选项名称]
```

---

## 🔧 常用 Prompt 片段

### 开始新功能

```
我想要开发一个新功能：[功能描述]

项目信息：
- 项目类型：[Android/Python/JavaScript/Web]
- 项目路径：[项目路径]

请使用 Superpowers 工作流系统，从 "brainstorming" 阶段开始。
```

### 继续下一阶段

```
当前阶段已完成，请继续到 [下一阶段名称] 阶段。

相关信息：
- [阶段特定的信息]
```

### 确认设计

```
我确认使用 [方案名称] 方案。

具体要求：
- [要求1]
- [要求2]

请继续到下一阶段。
```

### 批准计划

```
我批准实施计划。

请继续到 "executing-plans" 阶段，批次大小设为 3。
```

### 检查点确认

```
检查点已到达，请暂停。

当前进度：[进度信息]

我确认继续下一批次。
```

### 选择合并选项

```
我选择：合并到主分支

请执行合并并清理 worktree。
```

---

这样你就可以清楚地告诉 AI 每一步该做什么了！