#!/usr/bin/env python3
"""
Test script for AutoDroid Agent - demonstrates natural language to workplan execution.

This script shows how to use the AutoDroid Agent to:
1. Parse natural language instructions
2. Create and execute workplans
3. Handle different types of tasks (open app, login, search)
"""

import sys
import os
import json
import time

# Add current directory to path for imports
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from autodroid_agent import AutoDroidAgent


def test_natural_language_parsing():
    """Test natural language instruction parsing."""
    print("=== Testing Natural Language Parsing ===")
    
    agent = AutoDroidAgent()
    
    test_instructions = [
        "打开Autodroid Manager",
        "登录Autodroid Manager",
        "在淘宝搜索无线耳机",
        "打开美团",
        "执行登录测试"
    ]
    
    for instruction in test_instructions:
        print(f"\nInstruction: {instruction}")
        intent = agent.parse_natural_language(instruction)
        print(f"Parsed Intent:")
        print(f"  Action: {intent.action}")
        print(f"  Target App: {intent.target_app}")
        print(f"  Parameters: {intent.parameters}")
        print(f"  Workplan File: {intent.workplan_file}")


def test_login_with_workplan():
    """Test login functionality with custom workplan."""
    print("\n=== Testing Login with Custom Workplan ===")
    
    agent = AutoDroidAgent()
    
    # Create custom login workplan with specific coordinates
    custom_login_workplan = {
        "name": "Custom Login Test",
        "description": "Login to Autodroid Manager with custom coordinates",
        "app_package": "com.autodroid.manager",
        "credentials": {
            "username": "test@example.com",
            "password": "test123"
        },
        "steps": [
            {
                "step_id": "1",
                "description": "点击用户名输入框",
                "ui_elements": [
                    {
                        "name": "username_field",
                        "type": "input_field",
                        "identifier": {
                            "coordinates": {"x": 200, "y": 300}
                        },
                        "action": "tap",
                        "wait_time": 0.5
                    },
                    {
                        "name": "input_username",
                        "type": "text_input",
                        "action": "input",
                        "input_data": "test@example.com",
                        "wait_time": 1.0
                    }
                ],
                "expected_result": "用户名已输入"
            },
            {
                "step_id": "2",
                "description": "点击密码输入框",
                "ui_elements": [
                    {
                        "name": "password_field",
                        "type": "input_field",
                        "identifier": {
                            "coordinates": {"x": 400, "y": 300}
                        },
                        "action": "tap",
                        "wait_time": 0.5
                    },
                    {
                        "name": "input_password",
                        "type": "text_input",
                        "action": "input",
                        "input_data": "test123",
                        "wait_time": 1.0
                    }
                ],
                "expected_result": "密码已输入"
            },
            {
                "step_id": "3",
                "description": "点击登录按钮",
                "ui_elements": [
                    {
                        "name": "login_button",
                        "type": "button",
                        "identifier": {
                            "coordinates": {"x": 600, "y": 300}
                        },
                        "action": "tap",
                        "wait_time": 2.0
                    }
                ],
                "expected_result": "登录成功"
            }
        ]
    }
    
    # Test the agent with custom workplan
    print("Executing: 登录Autodroid Manager")
    result = agent.run("登录Autodroid Manager", custom_login_workplan)
    
    print(f"Result: {json.dumps(result, ensure_ascii=False, indent=2)}")
    
    return result


def test_search_functionality():
    """Test search functionality."""
    print("\n=== Testing Search Functionality ===")
    
    agent = AutoDroidAgent()
    
    # Test search instruction
    instruction = "在淘宝搜索无线耳机"
    print(f"Executing: {instruction}")
    
    result = agent.run(instruction)
    
    print(f"Result: {json.dumps(result, ensure_ascii=False, indent=2)}")
    
    return result


def test_workplan_file_detection():
    """Test workplan file detection."""
    print("\n=== Testing Workplan File Detection ===")
    
    agent = AutoDroidAgent()
    
    # Create a test workplan file
    test_workplan = {
        "name": "Test Workplan",
        "description": "Test workplan for file detection",
        "steps": []
    }
    
    workplan_file = "login_test.json"
    with open(workplan_file, 'w', encoding='utf-8') as f:
        json.dump(test_workplan, f, ensure_ascii=False, indent=2)
    
    # Test file detection
    instruction = "执行登录测试"
    intent = agent.parse_natural_language(instruction)
    
    print(f"Instruction: {instruction}")
    print(f"Detected workplan file: {intent.workplan_file}")
    
    # Cleanup
    if os.path.exists(workplan_file):
        os.remove(workplan_file)


def main():
    """Main test function."""
    print("AutoDroid Agent Test Suite")
    print("=" * 50)
    
    try:
        # Run tests
        test_natural_language_parsing()
        test_workplan_file_detection()
        
        # Note: The following tests require actual device connection
        # Uncomment to run with real device
        
        # test_login_with_workplan()
        # test_search_functionality()
        
        print("\n=== Test Summary ===")
        print("✓ Natural language parsing test completed")
        print("✓ Workplan file detection test completed")
        print("✓ Agent system is ready for use")
        
        print("\n=== Usage Examples ===")
        print("# Create agent")
        print("agent = AutoDroidAgent()")
        print()
        print("# Open app")
        print("result = agent.run('打开Autodroid Manager')")
        print()
        print("# Login with workplan")
        print("result = agent.run('登录Autodroid Manager', login_workplan)")
        print()
        print("# Search in app")
        print("result = agent.run('在淘宝搜索无线耳机')")
        print()
        print("# Execute specific workplan")
        print("result = agent.run('执行登录测试', custom_workplan)")
        
    except Exception as e:
        print(f"Test failed: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()