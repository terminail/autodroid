#!/usr/bin/env python3
"""
AutoGLM-Phone-9B 模拟模型服务
用于测试集成流程，提供模拟响应
"""

from flask import Flask, request, jsonify
import json
import random
from datetime import datetime

app = Flask(__name__)

# 模拟UI元素识别响应
MOCK_UI_RESPONSES = [
    {
        "elements": [
            {
                "type": "button",
                "text": "登录",
                "coordinates": {"x": 540, "y": 1200},
                "confidence": 0.95,
                "action": "click"
            },
            {
                "type": "input",
                "hint": "请输入用户名",
                "coordinates": {"x": 540, "y": 600},
                "confidence": 0.92,
                "action": "input_text"
            },
            {
                "type": "input", 
                "hint": "请输入密码",
                "coordinates": {"x": 540, "y": 800},
                "confidence": 0.90,
                "action": "input_text"
            }
        ],
        "analysis": "检测到登录界面，包含用户名输入框、密码输入框和登录按钮",
        "next_action": "点击用户名输入框"
    },
    {
        "elements": [
            {
                "type": "button",
                "text": "Sign In",
                "coordinates": {"x": 550, "y": 1150},
                "confidence": 0.88,
                "action": "click"
            }
        ],
        "analysis": "检测到英文登录界面",
        "next_action": "输入用户名"
    },
    {
        "elements": [
            {
                "type": "text",
                "text": "登录成功",
                "coordinates": {"x": 540, "y": 300},
                "confidence": 0.96,
                "action": "verify"
            }
        ],
        "analysis": "登录成功，检测到成功提示",
        "next_action": "验证登录结果"
    }
]

# 模拟登录测试结果
MOCK_LOGIN_RESULTS = [
    {
        "status": "success",
        "message": "登录成功",
        "screenshot_path": "/tmp/screenshots/login_success.png",
        "ai_analysis": {
            "confidence": 0.95,
            "elements_detected": 3,
            "execution_time": 2.5,
            "issues": []
        }
    },
    {
        "status": "failure",
        "message": "密码错误",
        "screenshot_path": "/tmp/screenshots/login_failed.png",
        "ai_analysis": {
            "confidence": 0.88,
            "elements_detected": 2,
            "execution_time": 1.8,
            "issues": ["密码输入框检测到错误提示"]
        }
    }
]

@app.route('/v1/chat/completions', methods=['POST'])
def chat_completions():
    """模拟聊天完成接口"""
    try:
        data = request.get_json()
        messages = data.get('messages', [])
        
        # 获取用户输入
        user_input = ""
        for msg in messages:
            if msg.get("role") == "user":
                user_input = msg.get("content", "")
                break
        
        print(f"收到请求: {user_input}")
        
        # 根据输入内容生成模拟响应
        if "UI" in user_input or "界面" in user_input or "element" in user_input.lower():
            response_data = random.choice(MOCK_UI_RESPONSES)
            response_text = f"AI分析结果: {json.dumps(response_data, ensure_ascii=False, indent=2)}"
        
        elif "登录" in user_input or "login" in user_input.lower():
            result = random.choice(MOCK_LOGIN_RESULTS)
            response_text = f"登录测试结果: {json.dumps(result, ensure_ascii=False, indent=2)}"
        
        elif "测试" in user_input or "test" in user_input.lower():
            response_text = f"测试执行完成: {json.dumps(random.choice(MOCK_LOGIN_RESULTS), ensure_ascii=False, indent=2)}"
        
        else:
            response_text = f"模拟AI响应: 已处理请求 '{user_input[:50]}...'"
        
        # 构建 OpenAI 兼容的响应
        response = {
            "id": f"chatcmpl-{datetime.now().strftime('%Y%m%d%H%M%S')}",
            "object": "chat.completion",
            "created": int(datetime.now().timestamp()),
            "model": "autoglm-phone-9b",
            "choices": [{
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": response_text
                },
                "finish_reason": "stop"
            }],
            "usage": {
                "prompt_tokens": len(user_input),
                "completion_tokens": len(response_text),
                "total_tokens": len(user_input) + len(response_text)
            }
        }
        
        print(f"返回响应: {response_text[:100]}...")
        return jsonify(response)
        
    except Exception as e:
        return jsonify({
            "error": {
                "message": str(e),
                "type": "internal_error",
                "code": "internal_error"
            }
        }), 500

@app.route('/v1/models', methods=['GET'])
def list_models():
    """列出可用模型"""
    return jsonify({
        "object": "list",
        "data": [{
            "id": "autoglm-phone-9b",
            "object": "model",
            "created": int(datetime.now().timestamp()),
            "owned_by": "autoglm"
        }]
    })

@app.route('/health', methods=['GET'])
def health_check():
    """健康检查接口"""
    return jsonify({
        "status": "healthy",
        "model_loaded": True,
        "device": "mock",
        "service": "AutoGLM-Phone-9B Mock Server"
    })

@app.route('/ui/analyze', methods=['POST'])
def ui_analyze():
    """UI分析接口（自定义）"""
    try:
        data = request.get_json()
        screenshot = data.get('screenshot')
        
        # 返回模拟的UI分析结果
        response = {
            "status": "success",
            "analysis": random.choice(MOCK_UI_RESPONSES),
            "processing_time": random.uniform(0.5, 2.0),
            "confidence": random.uniform(0.85, 0.98)
        }
        
        return jsonify(response)
        
    except Exception as e:
        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

def main():
    """主函数"""
    host = "localhost"
    port = 8000
    
    print("🚀 AutoGLM-Phone-9B 模拟模型服务启动")
    print("=" * 50)
    print(f"服务地址: http://{host}:{port}")
    print("API 端点:")
    print(f"  - POST /v1/chat/completions")
    print(f"  - GET  /v1/models")
    print(f"  - GET  /health")
    print(f"  - POST /ui/analyze")
    print("\n📋 模拟功能:")
    print("  - UI元素识别和分析")
    print("  - 登录流程测试")
    print("  - 智能操作决策")
    print("\n⚠️  注意：这是模拟服务，用于测试集成流程")
    print("按 Ctrl+C 停止服务")
    
    try:
        app.run(host=host, port=port, debug=False)
    except KeyboardInterrupt:
        print("\n正在停止服务...")
        print("服务已停止")

if __name__ == "__main__":
    main()