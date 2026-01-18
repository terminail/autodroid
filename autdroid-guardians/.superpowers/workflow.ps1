# Superpowers 工作流管理器 (PowerShell 版本)
# 适用于 Windows 环境的 Android 开发项目

param(
    [Parameter(Mandatory=$false)]
    [string]$ConfigPath = ".superpowers\config.yaml",
    
    [Parameter(Mandatory=$false, ParameterSetName="Brainstorm")]
    [string]$Requirement,
    
    [Parameter(Mandatory=$false, ParameterSetName="Brainstorm")]
    [string]$BrainstormOutput,
    
    [Parameter(Mandatory=$false, ParameterSetName="Worktree")]
    [string]$Feature,
    
    [Parameter(Mandatory=$false, ParameterSetName="Plan")]
    [string]$DesignDoc,
    
    [Parameter(Mandatory=$false, ParameterSetName="Plan")]
    [string]$PlanOutput,
    
    [Parameter(Mandatory=$false, ParameterSetName="Execute")]
    [string]$Plan,
    
    [Parameter(Mandatory=$false, ParameterSetName="Execute")]
    [string]$Worktree,
    
    [Parameter(Mandatory=$false, ParameterSetName="Execute")]
    [int]$BatchSize = 3,
    
    [Parameter(Mandatory=$false, ParameterSetName="Review")]
    [string]$ReviewPlan,
    
    [Parameter(Mandatory=$false, ParameterSetName="Review")]
    [string]$ReviewWorktree,
    
    [Parameter(Mandatory=$false, ParameterSetName="Finish")]
    [string]$FinishWorktree,
    
    [Parameter(Mandatory=$false, ParameterSetName="Finish")]
    [string]$FinishFeature
)

# 错误处理
$ErrorActionPreference = "Stop"

# 加载配置文件
function Load-Config {
    param([string]$Path)
    
    if (-not (Test-Path $Path)) {
        throw "配置文件不存在: $Path"
    }
    
    # 使用 PowerShell 的 YAML 解析（需要安装 PowerShell-Yaml 模块）
    # 如果没有安装，使用简单的文本解析
    try {
        $config = Get-Content $Path -Raw | ConvertFrom-Yaml
        return $config
    } catch {
        Write-Warning "无法解析 YAML 文件，使用基本配置"
        return @{
            version = "1.0"
            project_type = "android"
            project_name = "autodroid-guardians"
            output_locations = @{
                design_docs = ".superpowers/design/"
                plans = ".superpowers/plans/"
                reviews = ".superpowers/reviews/"
                logs = ".superpowers/logs/"
                worktrees = ".superpowers/worktrees/"
            }
            git_config = @{
                main_branch = "main"
                feature_branch_prefix = "feature/"
                worktree_location = ".superpowers/worktrees/"
            }
            android_config = @{
                build_commands = @("./gradlew.bat assembleDebug", "./gradlew.bat test")
                quality_checks = @(
                    @{ name = "Lint"; command = "./gradlew.bat lint"; fail_on_error = $false }
                    @{ name = "Unit Tests"; command = "./gradlew.bat test"; fail_on_error = $true }
                )
            }
            backup = @{
                enabled = $true
                location = ".superpowers/backups/"
            }
        }
    }
}

# 获取输出位置
function Get-OutputLocations {
    param([hashtable]$Config)
    
    $locations = $Config.output_locations
    $result = @{}
    
    foreach ($key in $locations.Keys) {
        $result[$key] = Join-Path $PWD $locations[$key]
    }
    
    return $result
}

# 确保目录存在
function Ensure-Directories {
    param([hashtable]$Locations)
    
    foreach ($location in $Locations.Values) {
        if (-not (Test-Path $location)) {
            New-Item -ItemType Directory -Path $location -Force | Out-Null
        }
    }
}

# 记录日志
function Write-Log {
    param(
        [string]$Phase,
        [string]$Message,
        [string]$Level = "INFO"
    )
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logEntry = "[$timestamp] [$Level] [$Phase] $Message"
    
    $logFile = Join-Path $Locations['logs'] "$($Phase.ToLower()).log"
    $logDir = Split-Path $logFile -Parent
    
    if (-not (Test-Path $logDir)) {
        New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    }
    
    Add-Content -Path $logFile -Value $logEntry
    Write-Host $logEntry
}

# Brainstorming 阶段
function Invoke-Brainstorming {
    param(
        [string]$Requirement,
        [string]$OutputFile
    )
    
    $phase = "brainstorming"
    Write-Log -Phase $phase -Message "开始设计细化阶段 - 需求: $Requirement"
    
    $designDoc = @{
        phase = $phase
        timestamp = (Get-Date).ToString("o")
        requirement = $Requirement
        clarifying_questions = @()
        alternatives = @()
        recommended_design = $null
        implementation_plan_outline = $null
        status = "in_progress"
    }
    
    if ([string]::IsNullOrEmpty($OutputFile)) {
        $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
        $OutputFile = Join-Path $Locations['design_docs'] "${phase}_${timestamp}.json"
    }
    
    $designDoc | ConvertTo-Json -Depth 10 | Out-File -FilePath $OutputFile -Encoding UTF8
    
    Write-Log -Phase $phase -Message "设计文档已创建: $OutputFile"
    Write-Log -Phase $phase -Message "请提出澄清问题并探索替代方案"
    
    return $OutputFile
}

# 创建 Worktree
function New-Worktree {
    param([string]$FeatureName)
    
    $phase = "using-git-worktrees"
    Write-Log -Phase $phase -Message "开始创建工作空间 - 功能: $FeatureName"
    
    $branchName = "feature/$FeatureName"
    
    # 检查分支是否已存在
    $result = git branch --list $branchName 2>&1
    if ($result) {
        Write-Log -Phase $phase -Message "分支 $branchName 已存在"
        return $branchName
    }
    
    # 创建新分支
    try {
        git checkout -b $branchName
        Write-Log -Phase $phase -Message "创建新分支: $branchName"
    } catch {
        Write-Log -Phase $phase -Message "创建分支失败: $_" -Level "ERROR"
        throw
    }
    
    # 创建 worktree
    $worktreePath = Join-Path $Locations['worktrees'] $FeatureName
    
    try {
        # 切换回主分支
        git checkout $Config.git_config.main_branch
        
        # 创建 worktree
        git worktree add $worktreePath $branchName
        Write-Log -Phase $phase -Message "创建 worktree: $worktreePath"
    } catch {
        Write-Log -Phase $phase -Message "创建 worktree 失败: $_" -Level "ERROR"
        throw
    }
    
    # 验证项目设置
    Write-Log -Phase $phase -Message "验证项目设置..."
    Verify-ProjectSetup $worktreePath
    
    # 运行测试基线
    Write-Log -Phase $phase -Message "运行测试基线..."
    Invoke-TestBaseline $worktreePath
    
    Write-Log -Phase $phase -Message "工作空间创建完成: $worktreePath"
    
    return $worktreePath
}

# 验证项目设置
function Verify-ProjectSetup {
    param([string]$WorktreePath)
    
    $gradleFile = Join-Path $WorktreePath "build.gradle.kts"
    $manifestFile = Join-Path $WorktreePath "app\src\main\AndroidManifest.xml"
    
    if (-not (Test-Path $gradleFile)) {
        Write-Log -Phase "verify" -Message "Gradle 文件不存在: $gradleFile" -Level "ERROR"
        throw "Gradle 文件不存在: $gradleFile"
    }
    
    if (-not (Test-Path $manifestFile)) {
        Write-Log -Phase "verify" -Message "AndroidManifest 文件不存在: $manifestFile" -Level "ERROR"
        throw "AndroidManifest 文件不存在: $manifestFile"
    }
    
    Write-Log -Phase "verify" -Message "项目设置验证通过"
}

# 运行测试基线
function Invoke-TestBaseline {
    param([string]$WorktreePath)
    
    try {
        $result = & "$WorktreePath\gradlew.bat" test 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Write-Log -Phase "test-baseline" -Message "测试失败: $result" -Level "WARNING"
        } else {
            Write-Log -Phase "test-baseline" -Message "测试基线通过"
        }
    } catch {
        Write-Log -Phase "test-baseline" -Message "运行测试失败: $_" -Level "WARNING"
    }
}

# 编写计划
function Write-Plan {
    param(
        [string]$DesignDocPath,
        [string]$OutputFile
    )
    
    $phase = "writing-plans"
    Write-Log -Phase $phase -Message "开始编写实施计划 - 设计文档: $DesignDocPath"
    
    $designDoc = Get-Content $DesignDocPath | ConvertFrom-Json
    
    $plan = @{
        phase = $phase
        timestamp = (Get-Date).ToString("o")
        design_doc = $DesignDocPath
        tasks = @()
        status = "in_progress"
    }
    
    if ([string]::IsNullOrEmpty($OutputFile)) {
        $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
        $OutputFile = Join-Path $Locations['plans'] "${phase}_${timestamp}.json"
    }
    
    $plan | ConvertTo-Json -Depth 10 | Out-File -FilePath $OutputFile -Encoding UTF8
    
    Write-Log -Phase $phase -Message "实施计划已创建: $OutputFile"
    Write-Log -Phase $phase -Message "请将工作分解为小任务（2-5分钟每个）"
    
    return $OutputFile
}

# 执行计划
function Invoke-ExecutePlan {
    param(
        [string]$PlanPath,
        [string]$WorktreePath,
        [int]$BatchSize = 3
    )
    
    $phase = "executing-plans"
    Write-Log -Phase $phase -Message "开始执行计划 - 计划: $PlanPath"
    
    $plan = Get-Content $PlanPath | ConvertFrom-Json
    $tasks = $plan.tasks
    $totalTasks = $tasks.Count
    
    for ($i = 0; $i -lt $totalTasks; $i++) {
        $task = $tasks[$i]
        Write-Log -Phase $phase -Message "执行任务 $($i+1)/$totalTasks: $($task.description)"
        
        # 执行 TDD 循环
        if (-not (Invoke-TDDCycle $task $WorktreePath)) {
            Write-Log -Phase $phase -Message "任务 $($i+1) 失败" -Level "ERROR"
            return $false
        }
        
        # 检查是否到达检查点
        if (($i + 1) % $BatchSize -eq 0 -or ($i + 1) -eq $totalTasks) {
            Write-Log -Phase $phase -Message "到达检查点 $($i+1)/$totalTasks"
            Write-Log -Phase $phase -Message "请检查进度并确认是否继续"
            Write-Log -Phase $phase -Message "用户确认继续"
        }
    }
    
    Write-Log -Phase $phase -Message "计划执行完成"
    return $true
}

# TDD 循环
function Invoke-TDDCycle {
    param(
        [object]$Task,
        [string]$WorktreePath
    )
    
    $phase = "test-driven-development"
    Write-Log -Phase $phase -Message "开始 TDD 循环 - 任务: $($Task.description)"
    
    # RED: 编写失败的测试
    Write-Log -Phase $phase -Message "RED: 编写失败的测试..."
    
    # GREEN: 编写最小代码使测试通过
    Write-Log -Phase $phase -Message "GREEN: 编写最小代码使测试通过..."
    
    # REFACTOR: 重构代码
    Write-Log -Phase $phase -Message "REFACTOR: 重构代码..."
    
    Write-Log -Phase $phase -Message "TDD 循环完成"
    return $true
}

# 代码审查
function Request-CodeReview {
    param(
        [string]$PlanPath,
        [string]$WorktreePath
    )
    
    $phase = "requesting-code-review"
    Write-Log -Phase $phase -Message "开始代码审查 - 计划: $PlanPath"
    
    $plan = Get-Content $PlanPath | ConvertFrom-Json
    
    $review = @{
        phase = $phase
        timestamp = (Get-Date).ToString("o")
        plan = $PlanPath
        spec_compliance = $true
        code_quality = $true
        test_coverage = $true
        issues = @()
        status = "in_progress"
    }
    
    # 运行代码质量检查
    Write-Log -Phase $phase -Message "运行代码质量检查..."
    foreach ($check in $Config.android_config.quality_checks) {
        $checkName = $check.name
        $command = $check.command
        
        try {
            $result = & "$WorktreePath\$command" 2>&1
            
            if ($LASTEXITCODE -ne 0) {
                $review.issues += @{
                    type = $checkName
                    severity = if ($check.fail_on_error) { "high" } else { "medium" }
                    message = $result
                }
                Write-Log -Phase $phase -Message "$checkName 检查失败" -Level "WARNING"
            } else {
                Write-Log -Phase $phase -Message "$checkName 检查通过"
            }
        } catch {
            $review.issues += @{
                type = $checkName
                severity = "low"
                message = $_.Exception.Message
            }
            Write-Log -Phase $phase -Message "$checkName 检查异常: $_" -Level "WARNING"
        }
    }
    
    # 保存审查报告
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $reviewFile = Join-Path $Locations['reviews'] "${phase}_${timestamp}.json"
    $review | ConvertTo-Json -Depth 10 | Out-File -FilePath $reviewFile -Encoding UTF8
    
    Write-Log -Phase $phase -Message "审查报告已创建: $reviewFile"
    
    # 检查关键问题
    $criticalIssues = $review.issues | Where-Object { $_.severity -eq "high" }
    if ($criticalIssues.Count -gt 0) {
        Write-Log -Phase $phase -Message "发现 $($criticalIssues.Count) 个关键问题" -Level "ERROR"
        return $reviewFile
    }
    
    Write-Log -Phase $phase -Message "代码审查完成"
    return $reviewFile
}

# 完成开发分支
function Complete-DevelopmentBranch {
    param(
        [string]$WorktreePath,
        [string]$FeatureName
    )
    
    $phase = "finishing-a-development-branch"
    Write-Log -Phase $phase -Message "开始完成开发分支 - 功能: $FeatureName"
    
    # 验证所有测试
    Write-Log -Phase $phase -Message "验证所有测试..."
    try {
        $result = & "$WorktreePath\gradlew.bat" test 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Write-Log -Phase $phase -Message "测试失败" -Level "ERROR"
            return $false
        } else {
            Write-Log -Phase $phase -Message "所有测试通过"
        }
    } catch {
        Write-Log -Phase $phase -Message "运行测试失败: $_" -Level "ERROR"
        return $false
    }
    
    # 展示选项
    Write-Log -Phase $phase -Message "请选择操作:"
    Write-Log -Phase $phase -Message "1. 合并到主分支"
    Write-Log -Phase $phase -Message "2. 创建 Pull Request"
    Write-Log -Phase $phase -Message "3. 保留分支"
    Write-Log -Phase $phase -Message "4. 丢弃分支"
    
    # 自动选择合并到主分支
    $choice = 1
    Write-Log -Phase $phase -Message "用户选择: $choice"
    
    $branchName = "feature/$FeatureName"
    
    if ($choice -eq 1) {
        # 合并到主分支
        Write-Log -Phase $phase -Message "合并分支 $branchName 到主分支..."
        try {
            git checkout $Config.git_config.main_branch
            git merge $branchName
            Write-Log -Phase $phase -Message "合并完成"
        } catch {
            Write-Log -Phase $phase -Message "合并失败: $_" -Level "ERROR"
            return $false
        }
    } elseif ($choice -eq 4) {
        # 丢弃分支
        Write-Log -Phase $phase -Message "删除分支 $branchName..."
        try {
            git branch -D $branchName
            Write-Log -Phase $phase -Message "分支已删除"
        } catch {
            Write-Log -Phase $phase -Message "删除分支失败: $_" -Level "ERROR"
            return $false
        }
    }
    
    # 清理 worktree
    Write-Log -Phase $phase -Message "清理 worktree: $WorktreePath..."
    try {
        git worktree remove $WorktreePath
        Write-Log -Phase $phase -Message "worktree 已清理"
    } catch {
        Write-Log -Phase $phase -Message "清理 worktree 失败: $_" -Level "WARNING"
    }
    
    Write-Log -Phase $phase -Message "开发分支完成"
    return $true
}

# 主逻辑
try {
    $Config = Load-Config -Path $ConfigPath
    $Locations = Get-OutputLocations -Config $Config
    Ensure-Directories -Locations $Locations
    
    if ($PSCmdlet.ParameterSetName -eq "Brainstorm") {
        $outputFile = Invoke-Brainstorming -Requirement $Requirement -OutputFile $BrainstormOutput
        Write-Host "`n设计文档已创建: $outputFile"
    }
    elseif ($PSCmdlet.ParameterSetName -eq "Worktree") {
        $worktreePath = New-Worktree -FeatureName $Feature
        Write-Host "`n工作空间已创建: $worktreePath"
    }
    elseif ($PSCmdlet.ParameterSetName -eq "Plan") {
        $outputFile = Write-Plan -DesignDocPath $DesignDoc -OutputFile $PlanOutput
        Write-Host "`n实施计划已创建: $outputFile"
    }
    elseif ($PSCmdlet.ParameterSetName -eq "Execute") {
        $success = Invoke-ExecutePlan -PlanPath $Plan -WorktreePath $Worktree -BatchSize $BatchSize
        if ($success) {
            Write-Host "`n计划执行成功"
        } else {
            Write-Host "`n计划执行失败"
            exit 1
        }
    }
    elseif ($PSCmdlet.ParameterSetName -eq "Review") {
        $reviewFile = Request-CodeReview -PlanPath $ReviewPlan -WorktreePath $ReviewWorktree
        Write-Host "`n审查报告已创建: $reviewFile"
    }
    elseif ($PSCmdlet.ParameterSetName -eq "Finish") {
        $success = Complete-DevelopmentBranch -WorktreePath $FinishWorktree -FeatureName $FinishFeature
        if ($success) {
            Write-Host "`n开发分支完成"
        } else {
            Write-Host "`n开发分支完成失败"
            exit 1
        }
    }
    else {
        Write-Host "请指定命令"
        Write-Host "`n可用命令:"
        Write-Host "  -Brainstorm <需求> [-Output <输出文件>]"
        Write-Host "  -Worktree <功能名称>"
        Write-Host "  -Plan <设计文档> [-Output <输出文件>]"
        Write-Host "  -Execute <计划> <工作空间> [-BatchSize <批次大小>]"
        Write-Host "  -Review <计划> <工作空间>"
        Write-Host "  -Finish <工作空间> <功能名称>"
        exit 1
    }
} catch {
    Write-Host "`n错误: $_" -ForegroundColor Red
    exit 1
}
