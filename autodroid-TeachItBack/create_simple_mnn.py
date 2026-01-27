#!/usr/bin/env python3
"""
创建简单的MNN模型文件
"""

import MNN.expr as expr

def create_simple_model():
    """创建简单的MNN模型"""
    print("创建简单MNN模型...")
    
    # 创建输入占位符
    input_ids = expr.placeholder([1, 128], expr.NCHW, expr.int)
    
    # 创建一个简单的全连接层（使用matmul和bias_add）
    weight = expr.const([[0.1] * 768] * 312, [312, 768], expr.NCHW)
    bias = expr.const([0.0] * 768, [768], expr.NCHW)
    
    # 矩阵乘法
    output = expr.matmul(input_ids, weight)
    # 添加偏置
    output = expr.bias_add(output, bias)
    
    # 保存为MNN模型
    expr.save([output], 'tibresource/models/tinybert-int8.mnn')
    print("简单MNN模型创建完成!")

if __name__ == "__main__":
    create_simple_model()