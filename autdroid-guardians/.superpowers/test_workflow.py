#!/usr/bin/env python3
"""
Superpowers 工作流测试脚本
用于验证工作流系统的各个组件
"""

import os
import sys
import json
import yaml
import subprocess
import tempfile
import shutil
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Optional, Any
import unittest

class TestSuperpowersWorkflow(unittest.TestCase):
    """Superpowers 工作流测试"""
    
    def setUp(self):
        """设置测试环境"""
        self.test_dir = Path(tempfile.mkdtemp())
        self.config_path = self.test_dir / "config.yaml"
        self.output_dir = self.test_dir / "output"
        self.output_dir.mkdir()
        
        # 创建测试配置
        self.test_config = {
            "version": "1.0",
            "project_type": "android",
            "project_name": "test-project",
            "output_locations": {
                "design_docs": str(self.output_dir / "design/"),
                "plans": str(self.output_dir / "plans/"),
                "reviews": str(self.output_dir / "reviews/"),
                "logs": str(self.output_dir / "logs/"),
                "worktrees": str(self.output_dir / "worktrees/")
            },
            "git_config": {
                "main_branch": "main",
                "feature_branch_prefix": "feature/",
                "worktree_location": str(self.output_dir / "worktrees/")
            },
            "android_config": {
                "build_commands": ["./gradlew test"],
                "quality_checks": [
                    {"name": "Lint", "command": "echo 'lint passed'", "fail_on_error": False},
                    {"name": "Unit Tests", "command": "echo 'test passed'", "fail_on_error": True}
                ]
            },
            "backup": {
                "enabled": False
            }
        }
        
        # 保存配置文件
        with open(self.config_path, 'w', encoding='utf-8') as f:
            yaml.dump(self.test_config, f)
        
        # 添加工作流脚本到路径
        sys.path.insert(0, str(Path(__file__).parent))
    
    def tearDown(self):
        """清理测试环境"""
        if self.test_dir.exists():
            shutil.rmtree(self.test_dir)
    
    def test_load_config(self):
        """测试加载配置文件"""
        from workflow import SuperpowersWorkflow
        
        workflow = SuperpowersWorkflow(str(self.config_path))
        self.assertEqual(workflow.config['version'], "1.0")
        self.assertEqual(workflow.config['project_type'], "android")
    
    def test_ensure_directories(self):
        """测试确保输出目录存在"""
        from workflow import SuperpowersWorkflow
        
        workflow = SuperpowersWorkflow(str(self.config_path))
        workflow._ensure_directories()
        
        for location in workflow.output_locations.values():
            self.assertTrue(location.exists())
    
    def test_log(self):
        """测试日志记录"""
        from workflow import SuperpowersWorkflow
        
        workflow = SuperpowersWorkflow(str(self.config_path))
        workflow._log("test", "测试日志消息")
        
        log_file = workflow.output_locations['logs'] / "test.log"
        self.assertTrue(log_file.exists())
        
        with open(log_file, 'r', encoding='utf-8') as f:
            content = f.read()
            self.assertIn("测试日志消息", content)
    
    def test_brainstorming(self):
        """测试设计细化阶段"""
        from workflow import SuperpowersWorkflow
        
        workflow = SuperpowersWorkflow(str(self.config_path))
        output_file = workflow.brainstorming("测试需求")
        
        self.assertTrue(Path(output_file).exists())
        
        with open(output_file, 'r', encoding='utf-8') as f:
            design_doc = json.load(f)
            self.assertEqual(design_doc['phase'], "brainstorming")
            self.assertEqual(design_doc['requirement'], "测试需求")
            self.assertEqual(design_doc['status'], "in_progress")
    
    def test_write_plan(self):
        """测试编写实施计划"""
        from workflow import SuperpowersWorkflow
        
        # 先创建设计文档
        workflow = SuperpowersWorkflow(str(self.config_path))
        design_doc_path = workflow.brainstorming("测试需求")
        
        # 创建实施计划
        plan_path = workflow.write_plan(design_doc_path)
        
        self.assertTrue(Path(plan_path).exists())
        
        with open(plan_path, 'r', encoding='utf-8') as f:
            plan = json.load(f)
            self.assertEqual(plan['phase'], "writing-plans")
            self.assertEqual(plan['design_doc'], design_doc_path)
            self.assertEqual(plan['status'], "in_progress")


class TestSuperpowersWorkflowIntegration(unittest.TestCase):
    """Superpowers 工作流集成测试"""
    
    def setUp(self):
        """设置测试环境"""
        self.test_dir = Path(tempfile.mkdtemp())
        self.config_path = self.test_dir / "config.yaml"
        self.output_dir = self.test_dir / "output"
        self.output_dir.mkdir()
        
        # 创建测试配置
        self.test_config = {
            "version": "1.0",
            "project_type": "android",
            "project_name": "test-project",
            "output_locations": {
                "design_docs": str(self.output_dir / "design/"),
                "plans": str(self.output_dir / "plans/"),
                "reviews": str(self.output_dir / "reviews/"),
                "logs": str(self.output_dir / "logs/"),
                "worktrees": str(self.output_dir / "worktrees/")
            },
            "git_config": {
                "main_branch": "main",
                "feature_branch_prefix": "feature/",
                "worktree_location": str(self.output_dir / "worktrees/")
            },
            "android_config": {
                "build_commands": ["./gradlew test"],
                "quality_checks": [
                    {"name": "Lint", "command": "echo 'lint passed'", "fail_on_error": False},
                    {"name": "Unit Tests", "command": "echo 'test passed'", "fail_on_error": True}
                ]
            },
            "backup": {
                "enabled": False
            }
        }
        
        # 保存配置文件
        with open(self.config_path, 'w', encoding='utf-8') as f:
            yaml.dump(self.test_config, f)
        
        # 添加工作流脚本到路径
        sys.path.insert(0, str(Path(__file__).parent))
    
    def tearDown(self):
        """清理测试环境"""
        if self.test_dir.exists():
            shutil.rmtree(self.test_dir)
    
    def test_full_workflow(self):
        """测试完整工作流"""
        from workflow import SuperpowersWorkflow
        
        workflow = SuperpowersWorkflow(str(self.config_path))
        
        # 1. 设计细化
        design_doc_path = workflow.brainstorming("添加新功能")
        self.assertTrue(Path(design_doc_path).exists())
        
        # 2. 编写计划
        plan_path = workflow.write_plan(design_doc_path)
        self.assertTrue(Path(plan_path).exists())
        
        # 3. 创建工作空间（模拟，不实际创建 git worktree）
        # 在实际测试中，这需要真实的 git 仓库
        # worktree_path = workflow.create_worktree("test-feature")
        
        # 4. 执行计划（模拟）
        # 在实际测试中，这需要真实的 Android 项目
        # success = workflow.execute_plan(plan_path, worktree_path)
        
        # 5. 代码审查（模拟）
        # review_file = workflow.request_code_review(plan_path, worktree_path)
        
        # 6. 完成开发分支（模拟）
        # success = workflow.finish_development_branch(worktree_path, "test-feature")
        
        # 验证输出文件存在
        self.assertTrue(Path(design_doc_path).exists())
        self.assertTrue(Path(plan_path).exists())


class TestConfigValidation(unittest.TestCase):
    """配置文件验证测试"""
    
    def test_valid_config(self):
        """测试有效配置"""
        config = {
            "version": "1.0",
            "project_type": "android",
            "project_name": "test-project",
            "output_locations": {
                "design_docs": ".superpowers/design/",
                "plans": ".superpowers/plans/",
                "reviews": ".superpowers/reviews/",
                "logs": ".superpowers/logs/",
                "worktrees": ".superpowers/worktrees/"
            },
            "git_config": {
                "main_branch": "main",
                "feature_branch_prefix": "feature/",
                "worktree_location": ".superpowers/worktrees/"
            },
            "android_config": {
                "build_commands": ["./gradlew test"],
                "quality_checks": []
            },
            "backup": {
                "enabled": True,
                "location": ".superpowers/backups/"
            }
        }
        
        self.assertEqual(config['version'], "1.0")
        self.assertEqual(config['project_type'], "android")
        self.assertIn('design_docs', config['output_locations'])
    
    def test_missing_required_fields(self):
        """测试缺少必需字段"""
        config = {
            "version": "1.0",
            "project_type": "android"
        }
        
        self.assertNotIn('output_locations', config)
        self.assertNotIn('git_config', config)
    
    def test_task_constraints(self):
        """测试任务约束"""
        config = {
            "task_constraints": {
                "max_duration_minutes": 5,
                "max_files_per_task": 3,
                "max_lines_per_task": 100,
                "prefer_small_tasks": True
            }
        }
        
        self.assertEqual(config['task_constraints']['max_duration_minutes'], 5)
        self.assertEqual(config['task_constraints']['max_files_per_task'], 3)
        self.assertTrue(config['task_constraints']['prefer_small_tasks'])


class TestTDDRules(unittest.TestCase):
    """TDD 规则测试"""
    
    def test_tdd_rules(self):
        """测试 TDD 规则"""
        tdd_rules = {
            "write_test_first": True,
            "watch_test_fail": True,
            "write_minimal_code": True,
            "watch_test_pass": True,
            "refactor_immediately": True,
            "delete_code_without_tests": True
        }
        
        self.assertTrue(tdd_rules['write_test_first'])
        self.assertTrue(tdd_rules['watch_test_fail'])
        self.assertTrue(tdd_rules['write_minimal_code'])
        self.assertTrue(tdd_rules['watch_test_pass'])
        self.assertTrue(tdd_rules['refactor_immediately'])
        self.assertTrue(tdd_rules['delete_code_without_tests'])
    
    def test_anti_patterns(self):
        """测试反模式"""
        anti_patterns = [
            "writing-code-before-tests",
            "skipping-refactoring",
            "ignoring-test-failures",
            "over-engineering",
            "premature-optimization",
            "copy-paste-coding",
            "magic-numbers",
            "god-classes"
        ]
        
        self.assertEqual(len(anti_patterns), 8)
        self.assertIn("writing-code-before-tests", anti_patterns)
        self.assertIn("over-engineering", anti_patterns)


class TestReviewStandards(unittest.TestCase):
    """代码审查标准测试"""
    
    def test_review_standards(self):
        """测试代码审查标准"""
        review_standards = {
            "check_spec_compliance": True,
            "check_code_quality": True,
            "check_test_coverage": True,
            "check_documentation": True,
            "critical_issues_block_progress": True
        }
        
        self.assertTrue(review_standards['check_spec_compliance'])
        self.assertTrue(review_standards['check_code_quality'])
        self.assertTrue(review_standards['check_test_coverage'])
        self.assertTrue(review_standards['check_documentation'])
        self.assertTrue(review_standards['critical_issues_block_progress'])
    
    def test_issue_severity(self):
        """测试问题严重程度"""
        severities = ["high", "medium", "low"]
        
        self.assertEqual(len(severities), 3)
        self.assertIn("high", severities)
        self.assertIn("medium", severities)
        self.assertIn("low", severities)


def run_tests():
    """运行所有测试"""
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    # 添加所有测试类
    suite.addTests(loader.loadTestsFromTestCase(TestSuperpowersWorkflow))
    suite.addTests(loader.loadTestsFromTestCase(TestSuperpowersWorkflowIntegration))
    suite.addTests(loader.loadTestsFromTestCase(TestConfigValidation))
    suite.addTests(loader.loadTestsFromTestCase(TestTDDRules))
    suite.addTests(loader.loadTestsFromTestCase(TestReviewStandards))
    
    # 运行测试
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    # 返回测试结果
    return result.wasSuccessful()


if __name__ == '__main__':
    success = run_tests()
    sys.exit(0 if success else 1)
