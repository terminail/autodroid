# Superpowers 工作流测试脚本 (PowerShell 版本)
# 用于验证工作流系统的各个组件

param(
    [Parameter(Mandatory=$false)]
    [string]$ConfigPath = ".superpowers\config.yaml",
    
    [Parameter(Mandatory=$false)]
    [switch]$Verbose
)

$ErrorActionPreference = "Stop"
$TestResults = @()

# 测试结果类
class TestResult {
    [string]$Name
    [bool]$Passed
    [string]$Message
    [string]$Duration
    
    TestResult([string]$Name, [bool]$Passed, [string]$Message, [string]$Duration) {
        $this.Name = $Name
        $this.Passed = $Passed
        $this.Message = $Message
        $this.Duration = $Duration
    }
}

# 记录测试结果
function Add-TestResult {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Message,
        [timespan]$Duration
    )
    
    $result = [TestResult]::new($Name, $Passed, $Message, $Duration.ToString())
    $script:TestResults += $result
    
    if ($Verbose) {
        if ($result.Passed) {
            Write-Host "  [$($result.Name)] $($result.Message)" -ForegroundColor Green
        } else {
            Write-Host "  [$($result.Name)] $($result.Message)" -ForegroundColor Red
        }
    }
}

# 测试配置文件加载
function Test-LoadConfig {
    $startTime = Get-Date
    
    try {
        if (-not (Test-Path $ConfigPath)) {
            Add-TestResult -Name "LoadConfig" -Passed $false -Message "配置文件不存在: $ConfigPath" -Duration ((Get-Date) - $startTime)
            return
        }
        
        $content = Get-Content $ConfigPath -Raw
        
        if ([string]::IsNullOrEmpty($content)) {
            Add-TestResult -Name "LoadConfig" -Passed $false -Message "配置文件为空" -Duration ((Get-Date) - $startTime)
            return
        }
        
        Add-TestResult -Name "LoadConfig" -Passed $true -Message "配置文件加载成功" -Duration ((Get-Date) - $startTime)
    }
    catch {
        Add-TestResult -Name "LoadConfig" -Passed $false -Message "加载配置文件失败: $_" -Duration ((Get-Date) - $startTime)
    }
}

# 测试输出目录
function Test-OutputDirectories {
    $startTime = Get-Date
    
    try {
        $requiredDirs = @(
            ".superpowers\design\",
            ".superpowers\plans\",
            ".superpowers\reviews\",
            ".superpowers\logs\",
            ".superpowers\worktrees\"
        )
        
        $missingDirs = @()
        foreach ($dir in $requiredDirs) {
            if (-not (Test-Path $dir)) {
                $missingDirs += $dir
            }
        }
        
        if ($missingDirs.Count -gt 0) {
            $missingDirsStr = $missingDirs -join ', '
            Add-TestResult -Name "OutputDirectories" -Passed $false -Message "缺少目录: $missingDirsStr" -Duration ((Get-Date) - $startTime)
            return
        }
        
        Add-TestResult -Name "OutputDirectories" -Passed $true -Message "所有输出目录存在" -Duration ((Get-Date) - $startTime)
    }
    catch {
        Add-TestResult -Name "OutputDirectories" -Passed $false -Message "检查输出目录失败: $_" -Duration ((Get-Date) - $startTime)
    }
}

# 测试工作流脚本
function Test-WorkflowScript {
    $startTime = Get-Date
    
    try {
        $scriptPath = ".superpowers\workflow.py"
        $psScriptPath = ".superpowers\workflow.ps1"
        
        $scriptsExist = $true
        $missingScripts = @()
        
        if (-not (Test-Path $scriptPath)) {
            $scriptsExist = $false
            $missingScripts += $scriptPath
        }
        
        if (-not (Test-Path $psScriptPath)) {
            $scriptsExist = $false
            $missingScripts += $psScriptPath
        }
        
        if (-not $scriptsExist) {
            $missingScriptsStr = $missingScripts -join ', '
            Add-TestResult -Name "WorkflowScript" -Passed $false -Message "缺少脚本: $missingScriptsStr" -Duration ((Get-Date) - $startTime)
            return
        }
        
        Add-TestResult -Name "WorkflowScript" -Passed $true -Message "工作流脚本存在" -Duration ((Get-Date) - $startTime)
    }
    catch {
        Add-TestResult -Name "WorkflowScript" -Passed $false -Message "检查工作流脚本失败: $_" -Duration ((Get-Date) - $startTime)
    }
}

# 测试 Python 环境
function Test-PythonEnvironment {
    $startTime = Get-Date
    
    try {
        $pythonVersion = python --version 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Add-TestResult -Name "PythonEnvironment" -Passed $false -Message "Python 未安装或不在 PATH 中" -Duration ((Get-Date) - $startTime)
            return
        }
        
        Add-TestResult -Name "PythonEnvironment" -Passed $true -Message "Python 版本: $pythonVersion" -Duration ((Get-Date) - $startTime)
    }
    catch {
        Add-TestResult -Name "PythonEnvironment" -Passed $false -Message "检查 Python 环境失败: $_" -Duration ((Get-Date) - $startTime)
    }
}

# 测试 PowerShell 环境
function Test-PowerShellEnvironment {
    $startTime = Get-Date
    
    try {
        $psVersion = $PSVersionTable.PSVersion
        
        if ($psVersion.Major -lt 5) {
            Add-TestResult -Name "PowerShellEnvironment" -Passed $false -Message "PowerShell 版本过低: $psVersion" -Duration ((Get-Date) - $startTime)
            return
        }
        
        Add-TestResult -Name "PowerShellEnvironment" -Passed $true -Message "PowerShell 版本: $psVersion" -Duration ((Get-Date) - $startTime)
    }
    catch {
        Add-TestResult -Name "PowerShellEnvironment" -Passed $false -Message "检查 PowerShell 环境失败: $_" -Duration ((Get-Date) - $startTime)
    }
}

# 测试 Git 环境
function Test-GitEnvironment {
    $startTime = Get-Date
    
    try {
        $gitVersion = git --version 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Add-TestResult -Name "GitEnvironment" -Passed $false -Message "Git 未安装或不在 PATH 中" -Duration ((Get-Date) - $startTime)
            return
        }
        
        $worktreeHelp = git worktree --help 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Add-TestResult -Name "GitEnvironment" -Passed $false -Message "Git 不支持 worktree 功能" -Duration ((Get-Date) - $startTime)
            return
        }
        
        Add-TestResult -Name "GitEnvironment" -Passed $true -Message "Git 版本: $gitVersion" -Duration ((Get-Date) - $startTime)
    }
    catch {
        Add-TestResult -Name "GitEnvironment" -Passed $false -Message "检查 Git 环境失败: $_" -Duration ((Get-Date) - $startTime)
    }
}

# 测试 Android 环境
function Test-AndroidEnvironment {
    $startTime = Get-Date
    
    try {
        $gradlePath = ".\gradlew.bat"
        if (-not (Test-Path $gradlePath)) {
            Add-TestResult -Name "AndroidEnvironment" -Passed $false -Message "Gradle wrapper 不存在: $gradlePath" -Duration ((Get-Date) - $startTime)
            return
        }
        
        $gradleVersion = & $gradlePath --version 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Add-TestResult -Name "AndroidEnvironment" -Passed $false -Message "Gradle 运行失败" -Duration ((Get-Date) - $startTime)
            return
        }
        
        Add-TestResult -Name "AndroidEnvironment" -Passed $true -Message "Gradle 环境正常" -Duration ((Get-Date) - $startTime)
    }
    catch {
        Add-TestResult -Name "AndroidEnvironment" -Passed $false -Message "检查 Android 环境失败: $_" -Duration ((Get-Date) - $startTime)
    }
}

# 测试配置验证
function Test-ConfigValidation {
    $startTime = Get-Date
    
    try {
        $requiredKeys = @(
            "version",
            "project_type",
            "output_locations",
            "git_config",
            "android_config"
        )
        
        $content = Get-Content $ConfigPath -Raw
        
        $missingKeys = @()
        foreach ($key in $requiredKeys) {
            if ($content -notmatch $key) {
                $missingKeys += $key
            }
        }
        
        if ($missingKeys.Count -gt 0) {
            $missingKeysStr = $missingKeys -join ', '
            Add-TestResult -Name "ConfigValidation" -Passed $false -Message "缺少配置项: $missingKeysStr" -Duration ((Get-Date) - $startTime)
            return
        }
        
        Add-TestResult -Name "ConfigValidation" -Passed $true -Message "配置验证通过" -Duration ((Get-Date) - $startTime)
    }
    catch {
        Add-TestResult -Name "ConfigValidation" -Passed $false -Message "验证配置失败: $_" -Duration ((Get-Date) - $startTime)
    }
}

# 测试工作流阶段
function Test-WorkflowPhases {
    $startTime = Get-Date
    
    try {
        $requiredPhases = @(
            "brainstorming",
            "using-git-worktrees",
            "writing-plans",
            "test-driven-development",
            "executing-plans",
            "requesting-code-review",
            "finishing-a-development-branch"
        )
        
        $content = Get-Content $ConfigPath -Raw
        
        $missingPhases = @()
        foreach ($phase in $requiredPhases) {
            if ($content -notmatch $phase) {
                $missingPhases += $phase
            }
        }
        
        if ($missingPhases.Count -gt 0) {
            $missingPhasesStr = $missingPhases -join ', '
            Add-TestResult -Name "WorkflowPhases" -Passed $false -Message "缺少工作流阶段: $missingPhasesStr" -Duration ((Get-Date) - $startTime)
            return
        }
        
        Add-TestResult -Name "WorkflowPhases" -Passed $true -Message "所有工作流阶段已定义" -Duration ((Get-Date) - $startTime)
    }
    catch {
        Add-TestResult -Name "WorkflowPhases" -Passed $false -Message "检查工作流阶段失败: $_" -Duration ((Get-Date) - $startTime)
    }
}

# 测试 TDD 规则
function Test-TDDRules {
    $startTime = Get-Date
    
    try {
        $requiredRules = @(
            "write_test_first",
            "watch_test_fail",
            "write_minimal_code",
            "watch_test_pass",
            "refactor_immediately",
            "delete_code_without_tests"
        )
        
        $content = Get-Content $ConfigPath -Raw
        
        $missingRules = @()
        foreach ($rule in $requiredRules) {
            if ($content -notmatch $rule) {
                $missingRules += $rule
            }
        }
        
        if ($missingRules.Count -gt 0) {
            $missingRulesStr = $missingRules -join ', '
            Add-TestResult -Name "TDDRules" -Passed $false -Message "缺少 TDD 规则: $missingRulesStr" -Duration ((Get-Date) - $startTime)
            return
        }
        
        Add-TestResult -Name "TDDRules" -Passed $true -Message "所有 TDD 规则已定义" -Duration ((Get-Date) - $startTime)
    }
    catch {
        Add-TestResult -Name "TDDRules" -Passed $false -Message "检查 TDD 规则失败: $_" -Duration ((Get-Date) - $startTime)
    }
}

# 测试代码审查标准
function Test-ReviewStandards {
    $startTime = Get-Date
    
    try {
        $requiredStandards = @(
            "check_spec_compliance",
            "check_code_quality",
            "check_test_coverage",
            "check_documentation",
            "critical_issues_block_progress"
        )
        
        $content = Get-Content $ConfigPath -Raw
        
        $missingStandards = @()
        foreach ($standard in $requiredStandards) {
            if ($content -notmatch $standard) {
                $missingStandards += $standard
            }
        }
        
        if ($missingStandards.Count -gt 0) {
            $missingStandardsStr = $missingStandards -join ', '
            Add-TestResult -Name "ReviewStandards" -Passed $false -Message "缺少代码审查标准: $missingStandardsStr" -Duration ((Get-Date) - $startTime)
            return
        }
        
        Add-TestResult -Name "ReviewStandards" -Passed $true -Message "所有代码审查标准已定义" -Duration ((Get-Date) - $startTime)
    }
    catch {
        Add-TestResult -Name "ReviewStandards" -Passed $false -Message "检查代码审查标准失败: $_" -Duration ((Get-Date) - $startTime)
    }
}

# 运行所有测试
function Run-AllTests {
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "Superpowers 工作流系统测试" -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan
    
    Write-Host "运行测试...`n" -ForegroundColor Yellow
    
    Test-LoadConfig
    Test-OutputDirectories
    Test-WorkflowScript
    Test-PythonEnvironment
    Test-PowerShellEnvironment
    Test-GitEnvironment
    Test-AndroidEnvironment
    Test-ConfigValidation
    Test-WorkflowPhases
    Test-TDDRules
    Test-ReviewStandards
    
    $totalTests = $TestResults.Count
    $passedTests = 0
    foreach ($result in $TestResults) {
        if ($result.Passed) {
            $passedTests++
        }
    }
    $failedTests = $totalTests - $passedTests
    
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "测试结果" -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan
    
    Write-Host "总测试数: $totalTests" -ForegroundColor White
    Write-Host "通过: $passedTests" -ForegroundColor Green
    if ($failedTests -gt 0) {
        Write-Host "失败: $failedTests" -ForegroundColor Red
    } else {
        Write-Host "失败: $failedTests" -ForegroundColor Green
    }
    
    if ($failedTests -gt 0) {
        Write-Host "`n失败的测试:" -ForegroundColor Red
        foreach ($result in $TestResults) {
            if (-not $result.Passed) {
                Write-Host "  - $($result.Name): $($result.Message)" -ForegroundColor Red
            }
        }
    }
    
    if ($Verbose) {
        Write-Host "`n========================================" -ForegroundColor Cyan
        Write-Host "测试详情" -ForegroundColor Cyan
        Write-Host "========================================`n" -ForegroundColor Cyan
        
        foreach ($result in $TestResults) {
            if ($result.Passed) {
                Write-Host "[$($result.Name)] " -NoNewline -ForegroundColor Green
            } else {
                Write-Host "[$($result.Name)] " -NoNewline -ForegroundColor Red
            }
            Write-Host "$($result.Message)" -ForegroundColor White
            Write-Host "  耗时: $($result.Duration)" -ForegroundColor Gray
        }
    }
    
    Write-Host "`n========================================" -ForegroundColor Cyan
    
    return $failedTests -eq 0
}

# 主逻辑
try {
    $success = Run-AllTests
    
    if ($success) {
        Write-Host "所有测试通过！" -ForegroundColor Green
        exit 0
    } else {
        Write-Host "部分测试失败！" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "`n错误: $_" -ForegroundColor Red
    exit 1
}
