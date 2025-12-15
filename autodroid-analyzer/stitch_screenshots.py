#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
安卓截图拼接工具
使用通用算法将多个可滚动界面的截图拼接成一张长图，自动去除重复部分
"""

import cv2
import numpy as np
import os
import argparse
from pathlib import Path
from typing import List, Tuple


def load_images(image_paths: List[str]) -> List[np.ndarray]:
    """加载图像文件"""
    images = []
    for path in image_paths:
        img = cv2.imread(path)
        if img is not None:
            images.append(img)
        else:
            print(f"警告: 无法加载图像 {path}")
    return images


def detect_region_by_pixel_difference(img: np.ndarray, direction: str = 'top_bottom', 
                                     max_check_height: int = 1500, diff_threshold: int = 0) -> int:
    """
    基于像素差异的区域检测
    
    参数:
        img: 输入图像
        direction: 扫描方向 'top_bottom' 或 'bottom_top'
        max_check_height: 最大检查高度
        diff_threshold: 像素差异阈值
    
    返回:
        需要裁剪的高度（对于top_bottom是头部高度，对于bottom_top是底部高度）
    """
    h, w = img.shape[:2]
    
    # 转换为灰度图
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    
    if direction == 'top_bottom':
        # 从顶部向下扫描
        reference_row = gray[0:1, :]  # 第一行作为参考
        
        for row in range(1, min(max_check_height, h)):
            current_row = gray[row:row+1, :]
            avg_diff = np.mean(np.abs(reference_row.astype(float) - current_row.astype(float)))
            
            if avg_diff > diff_threshold:
                return row  # 返回头部需要裁剪的高度
        
        return min(max_check_height, h)
    
    elif direction == 'bottom_top':
        # 从底部向上扫描
        reference_row = gray[h-1:h, :]  # 最后一行作为参考
        
        for row_offset in range(1, min(max_check_height, h)):
            row = h - 1 - row_offset  # 从底部向上扫描
            current_row = gray[row:row+1, :]
            avg_diff = np.mean(np.abs(reference_row.astype(float) - current_row.astype(float)))
            
            if avg_diff > diff_threshold:
                return row_offset  # 返回底部需要裁剪的高度
        
        return min(max_check_height, h)
    
    else:
        raise ValueError("direction 必须是 'top_bottom' 或 'bottom_top'")


def stitch_images_complete(images: List[np.ndarray]) -> np.ndarray:
    """
    完整的拼接算法：同时检测头部和底部重复区域
    策略：
    1. 第一张图片头部完全保留，其他图片裁剪头部
    2. 最后一张图片底部完全保留，其他图片裁剪底部
    """
    if not images:
        raise ValueError("没有有效的图像可供拼接")
    
    if len(images) == 1:
        return images[0]
    
    print("开始检测头部和底部重复区域...")
    
    # 检测每张图片的头部和底部区域
    head_heights = []
    bottom_heights = []
    
    for i, img in enumerate(images):
        print(f"检测第 {i+1} 张图片...")
        
        # 检测头部区域（从顶部向下扫描）
        head_height = detect_region_by_pixel_difference(img, direction='top_bottom')
        head_heights.append(head_height)
        
        # 检测底部区域（从底部向上扫描）
        bottom_height = detect_region_by_pixel_difference(img, direction='bottom_top')
        bottom_heights.append(bottom_height)
    
    print("开始裁剪和拼接...")
    
    # 构建每张图片的实际使用区域
    used_regions = []
    
    for i in range(len(images)):
        h = images[i].shape[0]
        
        # 头部裁剪策略
        if i == 0:
            # 第一张图片头部完全保留
            start_row = 0
        else:
            # 其他图片裁剪头部区域
            start_row = head_heights[i]
        
        # 底部裁剪策略
        if i == len(images) - 1:
            # 最后一张图片底部完全保留
            end_row = h
        else:
            # 其他图片裁剪底部区域
            end_row = h - bottom_heights[i]
        
        used_regions.append((start_row, end_row))
        print(f"第 {i+1} 张图片: 使用区域 [{start_row}:{end_row}] ({end_row - start_row} 行)")
    
    # 计算最终图像的尺寸
    total_height = sum(end - start for start, end in used_regions)
    max_width = max(img.shape[1] for img in images)
    
    print(f"总高度: {total_height}px")
    print(f"最大宽度: {max_width}px")
    
    # 创建结果图像
    result = np.zeros((total_height, max_width, 3), dtype=np.uint8)
    
    # 逐个添加处理后的图片
    current_y = 0
    for i in range(len(images)):
        start_row, end_row = used_regions[i]
        if start_row < end_row:  # 确保有内容要复制
            img_part = images[i][start_row:end_row]
            part_h, part_w = img_part.shape[:2]
            if part_h > 0 and part_w > 0:  # 确保有有效内容
                result[current_y:current_y+part_h, :part_w] = img_part
                current_y += part_h
    
    return result


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='安卓截图拼接工具')
    parser.add_argument('input_files', nargs='+', help='输入图像文件路径')
    parser.add_argument('-o', '--output', default='stitched_result.png', help='输出文件路径')
    parser.add_argument('--sort', action='store_true', help='按文件名排序图像')
    
    args = parser.parse_args()
    
    # 处理输入文件
    image_paths = args.input_files
    if args.sort:
        image_paths = sorted(image_paths)
    
    print(f"加载 {len(image_paths)} 张图片...")
    images = load_images(image_paths)
    
    if len(images) < 1:
        print("错误: 没有有效的图像文件")
        return
    
    print(f"开始拼接 {len(images)} 张图片...")
    
    # 使用完整算法进行拼接
    result = stitch_images_complete(images)
    
    # 保存结果
    success = cv2.imwrite(args.output, result)
    
    if success:
        print(f"拼接完成! 结果已保存到: {args.output}")
        print(f"结果图像尺寸: {result.shape[1]} x {result.shape[0]} pixels")
    else:
        print("保存失败!")


if __name__ == "__main__":
    main()