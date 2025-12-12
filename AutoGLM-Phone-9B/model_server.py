#!/usr/bin/env python3
"""
AutoGLM-Phone-9B 模型服务
提供 OpenAI 兼容的 API 接口
"""

import os
import sys
import json
import torch
from pathlib import Path
from typing import Dict, Any, List

# 添加 Open-AutoGLM 路径
sys.path.append(str(Path(__file__).parent.parent / "Open-AutoGLM"))

try:
    from transformers import AutoTokenizer, AutoModelForCausalLM, AutoModel
    from flask import Flask, request, jsonify
    import yaml
except ImportError:
    print("正在安装依赖...")
    os.system("pip install transformers flask torch pyyaml")
    from transformers import AutoTokenizer, AutoModelForCausalLM, AutoModel
    from flask import Flask, request, jsonify
    import yaml

app = Flask(__name__)

class ModelServer:
    def __init__(self, model_path: str, host: str = "localhost", port: int = 8000):
        self.model_path = model_path
        self.host = host
        self.port = port
        self.model = None
        self.tokenizer = None
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        
        print(f"模型服务配置:")
        print(f"  模型路径: {model_path}")
        print(f"  服务地址: {host}:{port}")
        print(f"  设备: {self.device}")
    
    def load_model(self):
        """加载模型"""
        print("正在加载模型...")
        
        try:
            # 加载分词器
            self.tokenizer = AutoTokenizer.from_pretrained(
                self.model_path,
                trust_remote_code=True
            )
            
            # 使用 AutoModel 而不是 AutoModelForCausalLM，因为 AutoGLM-Phone-9B 使用特殊的配置类
            self.model = AutoModel.from_pretrained(
                self.model_path,
                torch_dtype=torch.float16 if self.device == "cuda" else torch.float32,
                device_map="auto" if self.device == "cuda" else None,
                trust_remote_code=True
            )
            
            if self.device == "cpu":
                self.model = self.model.to(self.device)
            
            print("✓ 模型加载成功！")
            return True
            
        except Exception as e:
            print(f"✗ 模型加载失败: {e}")
            return False
    
    def generate_response(self, messages: List[Dict[str, str]], 
                         max_tokens: int = 3000, 
                         temperature: float = 0.1) -> str:
        """生成响应"""
        try:
            # 构建输入文本
            if isinstance(messages, str):
                input_text = messages
            else:
                # 简化的消息格式处理
                input_text = ""
                for msg in messages:
                    if msg.get("role") == "user":
                        input_text = msg.get("content", "")
                        break
            
            if not input_text:
                return "请输入有效的问题"
            
            # 编码输入
            inputs = self.tokenizer(input_text, return_tensors="pt").to(self.device)
            
            # 生成响应
            with torch.no_grad():
                outputs = self.model.generate(
                    **inputs,
                    max_new_tokens=max_tokens,
                    temperature=temperature,
                    do_sample=True,
                    pad_token_id=self.tokenizer.eos_token_id
                )
            
            # 解码输出
            response = self.tokenizer.decode(outputs[0], skip_special_tokens=True)
            
            # 移除输入部分，只保留生成的内容
            if input_text in response:
                response = response.replace(input_text, "").strip()
            
            return response
            
        except Exception as e:
            print(f"生成响应时出错: {e}")
            return f"生成响应失败: {str(e)}"

# 创建全局模型服务实例
model_server = None

@app.route('/v1/chat/completions', methods=['POST'])
def chat_completions():
    """OpenAI 兼容的聊天完成接口"""
    try:
        data = request.get_json()
        
        messages = data.get('messages', [])
        max_tokens = data.get('max_tokens', 3000)
        temperature = data.get('temperature', 0.1)
        model = data.get('model', 'autoglm-phone-9b')
        
        # 生成响应
        response_text = model_server.generate_response(
            messages, max_tokens, temperature
        )
        
        # 构建 OpenAI 兼容的响应格式
        response = {
            "id": f"chatcmpl-{hash(str(messages))}",
            "object": "chat.completion",
            "created": int(torch.cuda.Event().elapsed_time(torch.cuda.Event()) / 1000) if torch.cuda.is_available() else 0,
            "model": model,
            "choices": [{
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": response_text
                },
                "finish_reason": "stop"
            }],
            "usage": {
                "prompt_tokens": len(str(messages)),
                "completion_tokens": len(response_text),
                "total_tokens": len(str(messages)) + len(response_text)
            }
        }
        
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
            "created": 0,
            "owned_by": "autoglm"
        }]
    })

@app.route('/health', methods=['GET'])
def health_check():
    """健康检查接口"""
    return jsonify({
        "status": "healthy",
        "model_loaded": model_server.model is not None,
        "device": model_server.device
    })

def main():
    global model_server
    
    # 默认模型路径
    model_path = "./autoglm-phone-9b"
    host = "localhost"
    port = 8000
    
    # 检查命令行参数
    if len(sys.argv) > 1:
        model_path = sys.argv[1]
    if len(sys.argv) > 2:
        host = sys.argv[2]
    if len(sys.argv) > 3:
        port = int(sys.argv[3])
    
    # 检查模型路径
    if not Path(model_path).exists():
        print(f"✗ 模型路径不存在: {model_path}")
        print("请先下载模型: python download_autoglm_model.py")
        return False
    
    # 创建并启动模型服务
    model_server = ModelServer(model_path, host, port)
    
    if not model_server.load_model():
        return False
    
    print(f"\n🚀 模型服务启动成功！")
    print(f"服务地址: http://{host}:{port}")
    print(f"API 端点:")
    print(f"  - POST /v1/chat/completions")
    print(f"  - GET  /v1/models")
    print(f"  - GET  /health")
    print(f"\n按 Ctrl+C 停止服务")
    
    try:
        app.run(host=host, port=port, debug=False)
    except KeyboardInterrupt:
        print("\n正在停止服务...")
        print("服务已停止")

if __name__ == "__main__":
    main()