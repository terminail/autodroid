#!/usr/bin/env python3
"""
创建占位符MNN模型文件
"""

import MNN.expr as expr

def create_dummy_model():
    """创建简单的占位符模型"""
    print("创建占位符MNN模型...")
    
    # 创建输入占位符
    input_ids = expr.placeholder([1, 128], expr.NCHW, expr.int)
    
    # 创建一个简单的线性层作为示例
    output = expr.linear(input_ids, 312, 768)
    
    # 保存为MNN模型
    expr.save([output], 'tibresource/models/tinybert-int8.mnn')
    print("占位符MNN模型创建完成!")

if __name__ == "__main__":
    create_dummy_model()