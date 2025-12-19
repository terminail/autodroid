#!/usr/bin/env python3
from PIL import Image, ImageDraw, ImageFont

# 设置参数
width = 48  # dp
height = 300  # dp
background_color = "#80CC0000"  # 半透明中国红
text_color = "#FFFFFF"  # 白色
text = "自动交计花"

# 转换为像素（假设320dpi）
pixel_width = int(width * 3.5)  # 近似转换
pixel_height = int(height * 3.5)

# 创建图片
image = Image.new("RGBA", (pixel_width, pixel_height), background_color)
draw = ImageDraw.Draw(image)

# 加载字体（使用系统默认字体）
try:
    font = ImageFont.truetype("simhei.ttf", int(pixel_width * 0.8))  # 80% of width
except IOError:
    font = ImageFont.load_default()

# 逐个绘制文字（竖排）
for i, char in enumerate(text):
    y = int(pixel_height * (i + 0.5) / len(text))  # 均匀分布
    x = int(pixel_width / 2)
    draw.text((x, y), char, font=font, fill=text_color, anchor="mm")

# 保存图片
image.save("d:\\git\\autodroid\\autodroid-trader-app\\vertical_text.png")
print("图片已生成：vertical_text.png")