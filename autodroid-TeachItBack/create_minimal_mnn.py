#!/usr/bin/env python3
"""
创建最小的MNN模型文件
"""

import MNN.expr as expr

def create_minimal_model():
    """创建最小的MNN模型"""
    print("创建最小MNN模型...")
    
    # 创建最简单的输入输出模型
    input_tensor = expr.placeholder([1, 128], expr.NCHW, expr.int)
    
    # 直接输出输入（最简单的模型）
    output = expr.cast(input_tensor, expr.float)
    
    # 保存为MNN模型
    expr.save([output], 'tibresource/models/tinybert-int8.mnn')
    print("最小MNN模型创建完成!")

if __name__ == "__main__":
    create_minimal_model()