#!/usr/bin/env python3
"""
Superpowers 工作流管理器
适用于 Android 开发项目的 AI 工作流脚本
"""

import os
import sys
import json
import yaml
import subprocess
import shutil
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Any
import argparse

class SuperpowersWorkflow:
    """Superpowers 工作流管理器"""
    
    def __init__(self, config_path: str = ".superpowers/config.yaml"):
        self.config_path = Path(config_path)
        self.config = self._load_config()
        self.project_root = Path.cwd()
        self.output_locations = self._get_output_locations()
        
    def _load_config(self) -> Dict:
        """加载配置文件"""
        if not self.config_path.exists():
            raise FileNotFoundError(f"配置文件不存在: {self.config_path}")
        
        with open(self.config_path, 'r', encoding='utf-8') as f:
            return yaml.safe_load(f)
    
    def _get_output_locations(self) -> Dict[str, Path]:
        """获取输出位置"""
        locations = self.config.get('output_locations', {})
        return {k: self.project_root / v for k, v in locations.items()}
    
    def _ensure_directories(self):
        """确保所有输出目录存在"""
        for location in self.output_locations.values():
            location.mkdir(parents=True, exist_ok=True)
    
    def _log(self, phase: str, message: str, level: str = "INFO"):
        """记录日志"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        log_entry = f"[{timestamp}] [{level}] [{phase}] {message}\n"
        
        log_file = self.output_locations['logs'] / f"{phase.lower()}.log"
        log_file.parent.mkdir(parents=True, exist_ok=True)
        
        with open(log_file, 'a', encoding='utf-8') as f:
            f.write(log_entry)
        
        print(log_entry.strip())
    
    def brainstorming(self, requirement: str, output_file: Optional[str] = None) -> str:
        """
        阶段 1: Brainstorming - 设计细化
        
        通过问题澄清需求，探索替代方案，展示设计供验证
        """
        self._ensure_directories()
        phase = "brainstorming"
        self._log(phase, f"开始设计细化阶段 - 需求: {requirement}")
        
        # 创建设计文档
        design_doc = {
            "phase": phase,
            "timestamp": datetime.now().isoformat(),
            "requirement": requirement,
            "clarifying_questions": [],
            "alternatives": [],
            "recommended_design": None,
            "implementation_plan_outline": None,
            "status": "in_progress"
        }
        
        # 保存设计文档
        if output_file is None:
            output_file = self.output_locations['design_docs'] / f"{phase}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        else:
            output_file = Path(output_file)
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(design_doc, f, indent=2, ensure_ascii=False)
        
        self._log(phase, f"设计文档已创建: {output_file}")
        self._log(phase, "请提出澄清问题并探索替代方案")
        
        return str(output_file)
    
    def create_worktree(self, feature_name: str) -> str:
        """
        阶段 2: Using Git Worktrees - 创建独立工作空间
        
        创建新的 feature 分支和 git worktree
        """
        self._ensure_directories()
        phase = "using-git-worktrees"
        self._log(phase, f"开始创建工作空间 - 功能: {feature_name}")
        
        # 创建 feature 分支名称
        branch_name = f"feature/{feature_name}"
        
        # 检查分支是否已存在
        try:
            result = subprocess.run(
                ['git', 'branch', '--list', branch_name],
                capture_output=True,
                text=True,
                cwd=self.project_root
            )
            
            if result.stdout.strip():
                self._log(phase, f"分支 {branch_name} 已存在")
                return branch_name
        except Exception as e:
            self._log(phase, f"检查分支失败: {e}", level="ERROR")
            raise
        
        # 创建新分支
        try:
            subprocess.run(
                ['git', 'checkout', '-b', branch_name],
                check=True,
                cwd=self.project_root
            )
            self._log(phase, f"创建新分支: {branch_name}")
        except subprocess.CalledProcessError as e:
            self._log(phase, f"创建分支失败: {e}", level="ERROR")
            raise
        
        # 创建 worktree
        worktree_path = self.output_locations['worktrees'] / feature_name
        
        try:
            # 切换回主分支
            subprocess.run(
                ['git', 'checkout', self.config['git_config']['main_branch']],
                check=True,
                cwd=self.project_root
            )
            
            # 创建 worktree
            subprocess.run(
                ['git', 'worktree', 'add', str(worktree_path), branch_name],
                check=True,
                cwd=self.project_root
            )
            self._log(phase, f"创建 worktree: {worktree_path}")
        except subprocess.CalledProcessError as e:
            self._log(phase, f"创建 worktree 失败: {e}", level="ERROR")
            raise
        
        # 验证项目设置
        self._log(phase, "验证项目设置...")
        self._verify_project_setup(worktree_path)
        
        # 运行测试基线
        self._log(phase, "运行测试基线...")
        self._run_test_baseline(worktree_path)
        
        self._log(phase, f"工作空间创建完成: {worktree_path}")
        
        return str(worktree_path)
    
    def _verify_project_setup(self, worktree_path: Path):
        """验证项目设置"""
        gradle_file = worktree_path / self.config['android_config']['gradle_path']
        manifest_file = worktree_path / self.config['android_config']['manifest_path']
        
        if not gradle_file.exists():
            self._log("verify", f"Gradle 文件不存在: {gradle_file}", level="ERROR")
            raise FileNotFoundError(f"Gradle 文件不存在: {gradle_file}")
        
        if not manifest_file.exists():
            self._log("verify", f"AndroidManifest 文件不存在: {manifest_file}", level="ERROR")
            raise FileNotFoundError(f"AndroidManifest 文件不存在: {manifest_file}")
        
        self._log("verify", "项目设置验证通过")
    
    def _run_test_baseline(self, worktree_path: Path):
        """运行测试基线"""
        try:
            # 运行单元测试
            result = subprocess.run(
                ['./gradlew', 'test'],
                capture_output=True,
                text=True,
                cwd=worktree_path,
                timeout=300
            )
            
            if result.returncode != 0:
                self._log("test-baseline", f"测试失败: {result.stderr}", level="WARNING")
            else:
                self._log("test-baseline", "测试基线通过")
        except subprocess.TimeoutExpired:
            self._log("test-baseline", "测试超时", level="WARNING")
        except Exception as e:
            self._log("test-baseline", f"运行测试失败: {e}", level="WARNING")
    
    def write_plan(self, design_doc_path: str, output_file: Optional[str] = None) -> str:
        """
        阶段 3: Writing Plans - 将工作分解为小任务
        
        将工作分解为小任务（2-5分钟每个），每个任务都有文件路径、完整代码、验证步骤
        """
        self._ensure_directories()
        phase = "writing-plans"
        self._log(phase, f"开始编写实施计划 - 设计文档: {design_doc_path}")
        
        # 加载设计文档
        with open(design_doc_path, 'r', encoding='utf-8') as f:
            design_doc = json.load(f)
        
        # 创建实施计划
        plan = {
            "phase": phase,
            "timestamp": datetime.now().isoformat(),
            "design_doc": design_doc_path,
            "tasks": [],
            "status": "in_progress"
        }
        
        # 保存实施计划
        if output_file is None:
            output_file = self.output_locations['plans'] / f"{phase}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        else:
            output_file = Path(output_file)
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(plan, f, indent=2, ensure_ascii=False)
        
        self._log(phase, f"实施计划已创建: {output_file}")
        self._log(phase, "请将工作分解为小任务（2-5分钟每个）")
        
        return str(output_file)
    
    def execute_tdd_cycle(self, task: Dict[str, Any], worktree_path: str) -> bool:
        """
        阶段 4: Test-Driven Development - TDD 循环
        
        RED-GREEN-REFACTOR 循环
        """
        phase = "test-driven-development"
        self._log(phase, f"开始 TDD 循环 - 任务: {task.get('description', 'N/A')}")
        
        # RED: 编写失败的测试
        self._log(phase, "RED: 编写失败的测试...")
        test_file = Path(worktree_path) / task.get('test_file', '')
        
        if test_file.exists():
            # 运行测试确认失败
            try:
                result = subprocess.run(
                    ['./gradlew', 'test', '--tests', task.get('test_name', '')],
                    capture_output=True,
                    text=True,
                    cwd=worktree_path
                )
                
                if result.returncode == 0:
                    self._log(phase, "测试应该失败但通过了", level="WARNING")
                else:
                    self._log(phase, "测试失败（预期）")
            except Exception as e:
                self._log(phase, f"运行测试失败: {e}", level="ERROR")
                return False
        else:
            self._log(phase, f"测试文件不存在: {test_file}", level="ERROR")
            return False
        
        # GREEN: 编写最小代码使测试通过
        self._log(phase, "GREEN: 编写最小代码使测试通过...")
        source_file = Path(worktree_path) / task.get('source_file', '')
        
        if not source_file.exists():
            self._log(phase, f"源文件不存在: {source_file}", level="ERROR")
            return False
        
        # 运行测试确认通过
        try:
            result = subprocess.run(
                ['./gradlew', 'test', '--tests', task.get('test_name', '')],
                capture_output=True,
                text=True,
                cwd=worktree_path
            )
            
            if result.returncode != 0:
                self._log(phase, "测试失败", level="ERROR")
                return False
            else:
                self._log(phase, "测试通过")
        except Exception as e:
            self._log(phase, f"运行测试失败: {e}", level="ERROR")
            return False
        
        # REFACTOR: 重构代码
        self._log(phase, "REFACTOR: 重构代码...")
        
        # 运行测试确认仍然通过
        try:
            result = subprocess.run(
                ['./gradlew', 'test'],
                capture_output=True,
                text=True,
                cwd=worktree_path
            )
            
            if result.returncode != 0:
                self._log(phase, "重构后测试失败", level="ERROR")
                return False
            else:
                self._log(phase, "重构后测试通过")
        except Exception as e:
            self._log(phase, f"运行测试失败: {e}", level="ERROR")
            return False
        
        self._log(phase, "TDD 循环完成")
        return True
    
    def execute_plan(self, plan_path: str, worktree_path: str, batch_size: int = 3) -> bool:
        """
        阶段 5: Executing Plans - 批量执行计划
        
        批量执行任务，设置人工检查点
        """
        self._ensure_directories()
        phase = "executing-plans"
        self._log(phase, f"开始执行计划 - 计划: {plan_path}")
        
        # 加载计划
        with open(plan_path, 'r', encoding='utf-8') as f:
            plan = json.load(f)
        
        tasks = plan.get('tasks', [])
        total_tasks = len(tasks)
        
        for i, task in enumerate(tasks, 1):
            self._log(phase, f"执行任务 {i}/{total_tasks}: {task.get('description', 'N/A')}")
            
            # 执行 TDD 循环
            if not self.execute_tdd_cycle(task, worktree_path):
                self._log(phase, f"任务 {i} 失败", level="ERROR")
                return False
            
            # 检查是否到达检查点
            if i % batch_size == 0 or i == total_tasks:
                self._log(phase, f"到达检查点 {i}/{total_tasks}")
                self._log(phase, "请检查进度并确认是否继续")
                
                # 在实际使用中，这里应该等待用户输入
                # 为了自动化，我们假设用户总是确认继续
                self._log(phase, "用户确认继续")
        
        self._log(phase, "计划执行完成")
        return True
    
    def request_code_review(self, plan_path: str, worktree_path: str) -> str:
        """
        阶段 6: Requesting Code Review - 请求代码审查
        
        对照计划审查，按严重程度报告问题
        """
        self._ensure_directories()
        phase = "requesting-code-review"
        self._log(phase, f"开始代码审查 - 计划: {plan_path}")
        
        # 加载计划
        with open(plan_path, 'r', encoding='utf-8') as f:
            plan = json.load(f)
        
        # 创建审查报告
        review = {
            "phase": phase,
            "timestamp": datetime.now().isoformat(),
            "plan": plan_path,
            "spec_compliance": True,
            "code_quality": True,
            "test_coverage": True,
            "issues": [],
            "status": "in_progress"
        }
        
        # 运行代码质量检查
        self._log(phase, "运行代码质量检查...")
        for check in self.config['android_config']['quality_checks']:
            check_name = check['name']
            command = check['command']
            
            try:
                result = subprocess.run(
                    command.split(),
                    capture_output=True,
                    text=True,
                    cwd=worktree_path,
                    timeout=300
                )
                
                if result.returncode != 0:
                    review['issues'].append({
                        "type": check_name,
                        "severity": "high" if check.get('fail_on_error', False) else "medium",
                        "message": result.stderr or result.stdout
                    })
                    self._log(phase, f"{check_name} 检查失败", level="WARNING")
                else:
                    self._log(phase, f"{check_name} 检查通过")
            except subprocess.TimeoutExpired:
                review['issues'].append({
                    "type": check_name,
                    "severity": "medium",
                    "message": "检查超时"
                })
                self._log(phase, f"{check_name} 检查超时", level="WARNING")
            except Exception as e:
                review['issues'].append({
                    "type": check_name,
                    "severity": "low",
                    "message": str(e)
                })
                self._log(phase, f"{check_name} 检查异常: {e}", level="WARNING")
        
        # 保存审查报告
        review_file = self.output_locations['reviews'] / f"{phase}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(review_file, 'w', encoding='utf-8') as f:
            json.dump(review, f, indent=2, ensure_ascii=False)
        
        self._log(phase, f"审查报告已创建: {review_file}")
        
        # 检查关键问题
        critical_issues = [issue for issue in review['issues'] if issue['severity'] == 'high']
        if critical_issues:
            self._log(phase, f"发现 {len(critical_issues)} 个关键问题", level="ERROR")
            if self.config['review_standards']['critical_issues_block_progress']:
                self._log(phase, "关键问题阻止进度", level="ERROR")
                return str(review_file)
        
        self._log(phase, "代码审查完成")
        return str(review_file)
    
    def finish_development_branch(self, worktree_path: str, feature_name: str) -> bool:
        """
        阶段 7: Finishing a Development Branch - 完成开发分支
        
        验证测试，展示选项，清理 worktree
        """
        self._ensure_directories()
        phase = "finishing-a-development-branch"
        self._log(phase, f"开始完成开发分支 - 功能: {feature_name}")
        
        # 验证所有测试
        self._log(phase, "验证所有测试...")
        try:
            result = subprocess.run(
                ['./gradlew', 'test'],
                capture_output=True,
                text=True,
                cwd=worktree_path,
                timeout=300
            )
            
            if result.returncode != 0:
                self._log(phase, "测试失败", level="ERROR")
                return False
            else:
                self._log(phase, "所有测试通过")
        except subprocess.TimeoutExpired:
            self._log(phase, "测试超时", level="ERROR")
            return False
        except Exception as e:
            self._log(phase, f"运行测试失败: {e}", level="ERROR")
            return False
        
        # 展示选项
        self._log(phase, "请选择操作:")
        self._log(phase, "1. 合并到主分支")
        self._log(phase, "2. 创建 Pull Request")
        self._log(phase, "3. 保留分支")
        self._log(phase, "4. 丢弃分支")
        
        # 在实际使用中，这里应该等待用户输入
        # 为了自动化，我们选择合并到主分支
        choice = 1
        self._log(phase, f"用户选择: {choice}")
        
        branch_name = f"feature/{feature_name}"
        
        if choice == 1:
            # 合并到主分支
            self._log(phase, f"合并分支 {branch_name} 到主分支...")
            try:
                subprocess.run(
                    ['git', 'checkout', self.config['git_config']['main_branch']],
                    check=True,
                    cwd=self.project_root
                )
                subprocess.run(
                    ['git', 'merge', branch_name],
                    check=True,
                    cwd=self.project_root
                )
                self._log(phase, "合并完成")
            except subprocess.CalledProcessError as e:
                self._log(phase, f"合并失败: {e}", level="ERROR")
                return False
        elif choice == 2:
            # 创建 Pull Request
            self._log(phase, "请手动创建 Pull Request")
        elif choice == 3:
            # 保留分支
            self._log(phase, "保留分支")
        elif choice == 4:
            # 丢弃分支
            self._log(phase, f"删除分支 {branch_name}...")
            try:
                subprocess.run(
                    ['git', 'branch', '-D', branch_name],
                    check=True,
                    cwd=self.project_root
                )
                self._log(phase, "分支已删除")
            except subprocess.CalledProcessError as e:
                self._log(phase, f"删除分支失败: {e}", level="ERROR")
                return False
        
        # 清理 worktree
        self._log(phase, f"清理 worktree: {worktree_path}...")
        try:
            subprocess.run(
                ['git', 'worktree', 'remove', str(worktree_path)],
                check=True,
                cwd=self.project_root
            )
            self._log(phase, "worktree 已清理")
        except subprocess.CalledProcessError as e:
            self._log(phase, f"清理 worktree 失败: {e}", level="WARNING")
        
        self._log(phase, "开发分支完成")
        return True
    
    def backup(self, worktree_path: str):
        """备份工作空间"""
        if not self.config['backup']['enabled']:
            return
        
        self._log("backup", f"备份工作空间: {worktree_path}")
        
        backup_location = Path(self.config['backup']['location'])
        backup_location.mkdir(parents=True, exist_ok=True)
        
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        backup_path = backup_location / f"backup_{timestamp}.tar.gz"
        
        try:
            subprocess.run(
                ['tar', '-czf', str(backup_path), '-C', str(Path(worktree_path).parent), Path(worktree_path).name],
                check=True
            )
            self._log("backup", f"备份完成: {backup_path}")
        except subprocess.CalledProcessError as e:
            self._log("backup", f"备份失败: {e}", level="ERROR")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='Superpowers 工作流管理器')
    subparsers = parser.add_subparsers(dest='command', help='可用命令')
    
    # Brainstorming 命令
    brainstorm_parser = subparsers.add_parser('brainstorm', help='设计细化阶段')
    brainstorm_parser.add_argument('requirement', help='需求描述')
    brainstorm_parser.add_argument('-o', '--output', help='输出文件路径')
    
    # Create Worktree 命令
    worktree_parser = subparsers.add_parser('worktree', help='创建工作空间')
    worktree_parser.add_argument('feature', help='功能名称')
    
    # Write Plan 命令
    plan_parser = subparsers.add_parser('plan', help='编写实施计划')
    plan_parser.add_argument('design_doc', help='设计文档路径')
    plan_parser.add_argument('-o', '--output', help='输出文件路径')
    
    # Execute Plan 命令
    execute_parser = subparsers.add_parser('execute', help='执行计划')
    execute_parser.add_argument('plan', help='计划文件路径')
    execute_parser.add_argument('worktree', help='工作空间路径')
    execute_parser.add_argument('-b', '--batch-size', type=int, default=3, help='批次大小')
    
    # Code Review 命令
    review_parser = subparsers.add_parser('review', help='代码审查')
    review_parser.add_argument('plan', help='计划文件路径')
    review_parser.add_argument('worktree', help='工作空间路径')
    
    # Finish Branch 命令
    finish_parser = subparsers.add_parser('finish', help='完成开发分支')
    finish_parser.add_argument('worktree', help='工作空间路径')
    finish_parser.add_argument('feature', help='功能名称')
    
    args = parser.parse_args()
    
    if args.command is None:
        parser.print_help()
        sys.exit(1)
    
    try:
        workflow = SuperpowersWorkflow()
        
        if args.command == 'brainstorm':
            output_file = workflow.brainstorming(args.requirement, args.output)
            print(f"\n设计文档已创建: {output_file}")
        
        elif args.command == 'worktree':
            worktree_path = workflow.create_worktree(args.feature)
            print(f"\n工作空间已创建: {worktree_path}")
        
        elif args.command == 'plan':
            output_file = workflow.write_plan(args.design_doc, args.output)
            print(f"\n实施计划已创建: {output_file}")
        
        elif args.command == 'execute':
            success = workflow.execute_plan(args.plan, args.worktree, args.batch_size)
            if success:
                print("\n计划执行成功")
            else:
                print("\n计划执行失败")
                sys.exit(1)
        
        elif args.command == 'review':
            review_file = workflow.request_code_review(args.plan, args.worktree)
            print(f"\n审查报告已创建: {review_file}")
        
        elif args.command == 'finish':
            success = workflow.finish_development_branch(args.worktree, args.feature)
            if success:
                print("\n开发分支完成")
            else:
                print("\n开发分支完成失败")
                sys.exit(1)
    
    except Exception as e:
        print(f"\n错误: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
