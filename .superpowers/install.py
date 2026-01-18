#!/usr/bin/env python3
"""
Superpowers 项目安装脚本
将 Superpowers 工作流系统安装到项目中
"""

import os
import sys
import shutil
from pathlib import Path
from typing import Optional
import argparse

class SuperpowersInstaller:
    """Superpowers 安装器"""
    
    def __init__(self, global_superpowers_path: Optional[Path] = None):
        self.project_root = Path.cwd()
        self.project_superpowers_path = self.project_root / ".superpowers"
        
        if global_superpowers_path is None:
            # 查找全局 Superpowers 目录
            global_superpowers_path = self._find_global_superpowers()
        
        self.global_superpowers_path = global_superpowers_path
    
    def _find_global_superpowers(self) -> Path:
        """查找全局 Superpowers 目录"""
        # 从当前目录向上查找
        current = self.project_root
        while current.parent != current:
            global_path = current / ".superpowers"
            if global_path.exists() and (global_path / "config.yaml").exists():
                return global_path
            current = current.parent
        
        # 如果没找到，使用默认位置
        return Path.home() / ".superpowers"
    
    def install(self, project_type: Optional[str] = None, force: bool = False):
        """安装 Superpowers 到项目"""
        print(f"正在安装 Superpowers 到项目: {self.project_root}")
        print(f"全局 Superpowers 路径: {self.global_superpowers_path}")
        
        # 检查是否已安装
        if self.project_superpowers_path.exists():
            if not force:
                print(f"\n项目已安装 Superpowers: {self.project_superpowers_path}")
                print("使用 --force 选项强制重新安装")
                return False
            
            print("\n强制重新安装...")
            shutil.rmtree(self.project_superpowers_path)
        
        # 创建项目 Superpowers 目录
        self.project_superpowers_path.mkdir(parents=True, exist_ok=True)
        
        # 复制配置文件
        print("\n复制配置文件...")
        self._copy_config(project_type)
        
        # 创建符号链接到全局脚本
        print("\n创建符号链接...")
        self._create_symlinks()
        
        # 创建输出目录
        print("\n创建输出目录...")
        self._create_output_directories()
        
        # 创建 .gitignore
        print("\n创建 .gitignore...")
        self._create_gitignore()
        
        print(f"\n✅ Superpowers 安装成功！")
        print(f"\n项目 Superpowers 路径: {self.project_superpowers_path}")
        print(f"使用方法:")
        print(f"  python {self.project_superpowers_path / 'workflow.py'} --help")
        print(f"  或")
        print(f"  python {self.global_superpowers_path / 'workflow.py'} --project-root {self.project_root}")
        
        return True
    
    def _copy_config(self, project_type: Optional[str]):
        """复制配置文件"""
        # 如果指定了项目类型，复制对应的模板
        if project_type:
            template_path = self.global_superpowers_path / "templates" / f"{project_type}.yaml"
            if template_path.exists():
                config_path = self.project_superpowers_path / "config.yaml"
                shutil.copy2(template_path, config_path)
                print(f"  ✓ 复制 {project_type} 配置模板")
                return
        
        # 否则创建最小配置文件
        config_path = self.project_superpowers_path / "config.yaml"
        config_content = f"""# Superpowers 项目配置
# 继承自全局配置: {self.global_superpowers_path / 'config.yaml'}

# 项目类型（可选，将自动检测）
# project_type: "android"

# 项目特定配置（可选，覆盖全局配置）
# android_config:
#   build_commands:
#     - "./gradlew test"
"""
        
        with open(config_path, 'w', encoding='utf-8') as f:
            f.write(config_content)
        
        print(f"  ✓ 创建项目配置文件")
    
    def _create_symlinks(self):
        """创建符号链接到全局脚本"""
        # 创建符号链接到全局 workflow.py
        workflow_link = self.project_superpowers_path / "workflow.py"
        global_workflow = self.global_superpowers_path / "workflow.py"
        
        if global_workflow.exists():
            try:
                if workflow_link.exists():
                    workflow_link.unlink()
                workflow_link.symlink_to(global_workflow)
                print(f"  ✓ 创建 workflow.py 符号链接")
            except OSError:
                # 如果不支持符号链接，复制文件
                shutil.copy2(global_workflow, workflow_link)
                print(f"  ✓ 复制 workflow.py")
        
        # 创建符号链接到全局 PowerShell 脚本
        workflow_ps1_link = self.project_superpowers_path / "workflow.ps1"
        global_workflow_ps1 = self.global_superpowers_path / "workflow.ps1"
        
        if global_workflow_ps1.exists():
            try:
                if workflow_ps1_link.exists():
                    workflow_ps1_link.unlink()
                workflow_ps1_link.symlink_to(global_workflow_ps1)
                print(f"  ✓ 创建 workflow.ps1 符号链接")
            except OSError:
                # 如果不支持符号链接，复制文件
                shutil.copy2(global_workflow_ps1, workflow_ps1_link)
                print(f"  ✓ 复制 workflow.ps1")
    
    def _create_output_directories(self):
        """创建输出目录"""
        directories = [
            "design",
            "plans",
            "reviews",
            "logs",
            "worktrees",
            "backups"
        ]
        
        for directory in directories:
            dir_path = self.project_superpowers_path / directory
            dir_path.mkdir(parents=True, exist_ok=True)
            print(f"  ✓ 创建 {directory}/ 目录")
    
    def _create_gitignore(self):
        """创建 .gitignore"""
        gitignore_path = self.project_superpowers_path / ".gitignore"
        gitignore_content = """# 日志文件
*.log

# 临时文件
*.tmp
*.temp

# 备份文件
*.bak
*.backup

# 工作树目录
worktrees/

# 备份目录
backups/

# Python 缓存
__pycache__/
*.py[cod]
*$py.class
*.so

# 测试输出
.pytest_cache/
.coverage
htmlcov/

# IDE 文件
.vscode/
.idea/
*.swp
*.swo
*~

# 操作系统文件
.DS_Store
Thumbs.db
"""
        
        with open(gitignore_path, 'w', encoding='utf-8') as f:
            f.write(gitignore_content)
        
        print(f"  ✓ 创建 .gitignore")
    
    def uninstall(self):
        """从项目中卸载 Superpowers"""
        print(f"正在从项目中卸载 Superpowers: {self.project_root}")
        
        if not self.project_superpowers_path.exists():
            print(f"\n项目未安装 Superpowers")
            return False
        
        # 删除项目 Superpowers 目录
        shutil.rmtree(self.project_superpowers_path)
        
        print(f"\n✅ Superpowers 卸载成功！")
        print(f"已删除: {self.project_superpowers_path}")
        
        return True
    
    def status(self):
        """显示安装状态"""
        print(f"项目根目录: {self.project_root}")
        print(f"全局 Superpowers: {self.global_superpowers_path}")
        print(f"项目 Superpowers: {self.project_superpowers_path}")
        
        if self.project_superpowers_path.exists():
            print(f"\n状态: ✅ 已安装")
            
            # 显示配置信息
            config_path = self.project_superpowers_path / "config.yaml"
            if config_path.exists():
                print(f"\n配置文件: {config_path}")
                with open(config_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    print(content[:500] if len(content) > 500 else content)
        else:
            print(f"\n状态: ❌ 未安装")
            
            # 检测项目类型
            print(f"\n检测项目类型:")
            self._detect_project_type()
    
    def _detect_project_type(self):
        """检测项目类型"""
        project_types = {
            "android": ["build.gradle*", "settings.gradle*", "AndroidManifest.xml"],
            "python": ["requirements.txt", "setup.py", "pyproject.toml", "Pipfile"],
            "javascript": ["package.json", "yarn.lock", "package-lock.json"],
            "web": ["index.html", "webpack.config.js", "vite.config.js"]
        }
        
        for project_type, patterns in project_types.items():
            for pattern in patterns:
                if (self.project_root / pattern).exists():
                    print(f"  ✓ {project_type} (检测到 {pattern})")
                    return
        
        print(f"  ? 未知类型")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='Superpowers 项目安装器')
    parser.add_argument('--global-path', help='全局 Superpowers 路径')
    parser.add_argument('--project-type', help='指定项目类型（自动检测）')
    parser.add_argument('--force', action='store_true', help='强制重新安装')
    
    subparsers = parser.add_subparsers(dest='command', help='可用命令')
    
    # Install 命令
    install_parser = subparsers.add_parser('install', help='安装 Superpowers 到项目')
    
    # Uninstall 命令
    uninstall_parser = subparsers.add_parser('uninstall', help='从项目中卸载 Superpowers')
    
    # Status 命令
    status_parser = subparsers.add_parser('status', help='显示安装状态')
    
    args = parser.parse_args()
    
    if args.command is None:
        parser.print_help()
        sys.exit(1)
    
    try:
        installer = SuperpowersInstaller(global_superpowers_path=args.global_path)
        
        if args.command == 'install':
            installer.install(project_type=args.project_type, force=args.force)
        
        elif args.command == 'uninstall':
            installer.uninstall()
        
        elif args.command == 'status':
            installer.status()
    
    except Exception as e:
        print(f"\n错误: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
