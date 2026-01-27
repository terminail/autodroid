#!/usr/bin/env python3
"""
将ONNX模型转换为MNN格式
"""

import MNN.tools.mnnconvert as mnnconvert
import sys

def convert_onnx_to_mnn(onnx_path, mnn_path):
    """转换ONNX到MNN格式"""
    print(f"开始转换: {onnx_path} -> {mnn_path}")
    
    try:
        # 创建参数对象
        class Args:
            def __init__(self):
                self.framework = 'ONNX'
                self.modelFile = onnx_path
                self.MNNModel = mnn_path
                self.bizCode = 'tinybert'
        
        args = Args()
        
        # 执行转换
        result = mnnconvert.convert(args)
        print(f"转换完成: {result}")
        return True
        
    except Exception as e:
        print(f"转换失败: {e}")
        return False

if __name__ == "__main__":
    # 转换INT8量化模型
    onnx_path = "tibresource/models/TinyBERT_General_4L_312D/onnx/model_int8.onnx"
    mnn_path = "tibresource/models/tinybert-int8.mnn"
    
    success = convert_onnx_to_mnn(onnx_path, mnn_path)
    
    if success:
        print("✅ ONNX到MNN转换成功！")
    else:
        print("❌ 转换失败，尝试其他方法...")
        
        # 尝试使用命令行工具
        import os
        print("尝试使用MNN命令行工具...")
        cmd = f"MNNConvert -f ONNX --modelFile {onnx_path} --MNNModel {mnn_path}"
        print(f"执行命令: {cmd}")
        os.system(cmd)