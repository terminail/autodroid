#!/usr/bin/env python3
"""
测试XML特殊字符处理功能（不依赖设备）
"""

import sys
from pathlib import Path
import xml.etree.ElementTree as ET
import html


def test_xml_special_char_handling():
    """测试XML特殊字符处理"""
    
    print("=" * 60)
    print("🔬 测试XML特殊字符处理功能")
    print("=" * 60)
    
    # 测试用例：包含特殊字符的XML（修复格式）
    test_xml = """<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
<hierarchy rotation="0">
  <node index="0" text="" resource-id="com.tdx.androidCCZQ:id/outbox" class="android.widget.FrameLayout">
    <node index="1" text="川财APP首页" resource-id="com.tdx.androidCCZQ:id/title" class="android.widget.TextView"/>
    <node index="2" text="&#xe6ee;" resource-id="com.tdx.androidCCZQ:id/icon" class="android.widget.TextView"/>
    <node index="3" text="自选股" resource-id="com.tdx.androidCCZQ:id/tab1" class="android.widget.TextView"/>
    <node index="4" text="市场行情" resource-id="com.tdx.androidCCZQ:id/tab2" class="android.widget.TextView"/>
  </node>
</hierarchy>"""
    
    print("\n📝 测试XML内容:")
    print(test_xml)
    
    # 测试1: 解析XML
    print("\n🔍 测试1: 解析XML...")
    try:
        root = ET.fromstring(test_xml)
        print("✅ XML解析成功")
        print(f"   根节点: {root.tag}")
        
        # 查找所有节点
        nodes = list(root.iter())
        print(f"   节点总数: {len(nodes)}")
        
        # 检查特殊字符
        special_chars_found = []
        for node in nodes:
            text = node.get('text', '')
            if text:
                for i, char in enumerate(text):
                    if ord(char) > 127:
                        special_chars_found.append((text, char, ord(char)))
        
        if special_chars_found:
            print(f"\n✅ 发现特殊字符:")
            for text, char, code in special_chars_found:
                print(f"   文本: '{text}', 字符: '{char}', Unicode: U+{code:04X}")
        else:
            print("\nℹ️  未发现特殊字符（可能已被转义）")
        
    except Exception as e:
        print(f"❌ XML解析失败: {e}")
        return False
    
    # 测试2: 测试HTML实体解码
    print("\n🔍 测试2: HTML实体解码...")
    try:
        decoded_text = html.unescape("&#xe6ee;")
        print(f"   原始: '&#xe6ee;'")
        print(f"   解码后: '{decoded_text}'")
        print(f"   Unicode: U+{ord(decoded_text):04X}")
        print("✅ HTML实体解码成功")
    except Exception as e:
        print(f"❌ HTML实体解码失败: {e}")
    
    # 测试3: 测试多种编码方式
    print("\n🔍 测试3: 测试多种编码方式...")
    test_string = "川财APP首页&#xe6ee;"
    
    encodings = ['utf-8', 'latin-1', 'cp1252']
    for encoding in encodings:
        try:
            encoded = test_string.encode(encoding, errors='replace')
            decoded = encoded.decode(encoding, errors='replace')
            print(f"   {encoding}: '{decoded}'")
        except Exception as e:
            print(f"   {encoding}: 失败 - {e}")
    
    # 测试4: 保存和读取XML文件
    print("\n🔍 测试4: 保存和读取XML文件...")
    output_dir = Path(r"d:\git\autodroid\autodroid-trader-executor\tradescripts\dump-pages")
    output_dir.mkdir(parents=True, exist_ok=True)
    
    xml_file = output_dir / "test_special_chars.xml"
    
    # 使用不同编码保存
    for encoding in ['utf-8', 'utf-8-sig']:
        try:
            with open(xml_file, 'w', encoding=encoding, errors='replace') as f:
                f.write(test_xml)
            print(f"   ✓ 使用 {encoding} 保存成功")
            
            # 读取验证
            with open(xml_file, 'r', encoding=encoding, errors='replace') as f:
                content = f.read()
            
            # 验证内容
            if content == test_xml:
                print(f"   ✓ 使用 {encoding} 读取验证成功")
            else:
                print(f"   ⚠️  使用 {encoding} 读取内容不一致")
                print(f"      期望长度: {len(test_xml)}, 实际长度: {len(content)}")
                
        except Exception as e:
            print(f"   ✗ {encoding}: {e}")
    
    print(f"\n💾 测试文件已保存到: {xml_file}")
    
    return True


def test_fingerprint_element_type():
    """测试FingerprintElement类型类"""
    
    print("\n" + "=" * 60)
    print("🔬 测试FingerprintElement类型类")
    print("=" * 60)
    
    try:
        from pydantic import BaseModel
        
        # 定义FingerprintElement（避免依赖page.py）
        class FingerprintElement(BaseModel):
            """指纹元素类型化类"""
            text: str = ""
            resource_id: str = ""
            class_name: str = ""
            bounds: str = ""
            content_desc: str = ""
            clickable: str = ""
            long_clickable: str = ""
            index: int = 0
        
        # 测试FingerprintElement
        print("\n📝 测试FingerprintElement...")
        fp_elem = FingerprintElement(
            text="川财APP首页",
            resource_id="com.tdx.androidCCZQ:id/title",
            class_name="android.widget.TextView",
            bounds="[0,100][1080,200]",
            content_desc="首页标题",
            clickable="true",
            long_clickable="false",
            index=1
        )
        print(f"✅ FingerprintElement创建成功")
        print(f"   text: '{fp_elem.text}'")
        print(f"   resource_id: '{fp_elem.resource_id}'")
        print(f"   index: {fp_elem.index}")
        
        # 测试序列化
        print("\n📝 测试序列化...")
        import json
        elem_dict = fp_elem.model_dump()
        print(f"✅ 序列化成功")
        print(f"   JSON: {json.dumps(elem_dict, ensure_ascii=False, indent=2)}")
        
        # 测试反序列化
        print("\n📝 测试反序列化...")
        fp_elem2 = FingerprintElement(**elem_dict)
        print(f"✅ 反序列化成功")
        print(f"   text: '{fp_elem2.text}'")
        print(f"   resource_id: '{fp_elem2.resource_id}'")
        
        return True
        
    except Exception as e:
        print(f"❌ 测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_xml_parsing_with_special_chars():
    """测试解析包含特殊字符的XML文件"""
    
    print("\n" + "=" * 60)
    print("🔬 测试解析包含特殊字符的XML文件")
    print("=" * 60)
    
    # 查找现有的XML文件
    xml_files = [
        Path(r"d:\git\autodroid\autodroid-trader-executor\app\src\main\assets\apks\com.tdx.androidCCZQ\wang-ge-jiao-yi\wang-ge-jiao-yi.xml"),
        Path(r"d:\git\autodroid\autodroid-trader-executor\app\src\main\assets\apks\com.tdx.androidCCZQ\general\home.xml"),
    ]
    
    success_count = 0
    for xml_file in xml_files:
        if not xml_file.exists():
            continue
        
        print(f"\n📄 解析文件: {xml_file.name}")
        
        # 尝试多种编码方式读取
        for encoding in ['utf-8', 'latin-1', 'cp1252']:
            try:
                with open(xml_file, 'r', encoding=encoding, errors='replace') as f:
                    content = f.read()
                
                print(f"   ✓ 使用 {encoding} 读取成功")
                print(f"   内容长度: {len(content)} 字符")
                
                # 检查特殊字符
                special_chars = []
                for i, char in enumerate(content):
                    if ord(char) > 127:
                        special_chars.append((i, char, ord(char)))
                
                if special_chars:
                    print(f"   发现 {len(special_chars)} 个特殊字符")
                    # 显示前5个
                    for idx, char, code in special_chars[:5]:
                        print(f"     位置 {idx}: '{char}' (U+{code:04X})")
                    if len(special_chars) > 5:
                        print(f"     ... 还有 {len(special_chars) - 5} 个")
                
                # 尝试解析XML
                try:
                    root = ET.fromstring(content)
                    print(f"   ✓ XML解析成功")
                    print(f"   根节点: {root.tag}")
                    
                    # 查找fingerprint元素（使用正确的命名空间）
                    AUTODROID_NS = "https://autodroid.example.com"
                    AUTODROID_FINGERPRINT = f"{{{AUTODROID_NS}}}fingerprint"
                    fingerprint_count = 0
                    for elem in root.iter():
                        if elem.get(AUTODROID_FINGERPRINT) == 'true':
                            fingerprint_count += 1
                            text = elem.get('text', '')
                            resource_id = elem.get('resource-id', '')
                            print(f"     - fingerprint元素: text='{text}', resource_id='{resource_id}'")
                    
                    if fingerprint_count > 0:
                        print(f"   ✓ 发现 {fingerprint_count} 个fingerprint元素")
                    else:
                        print(f"   ℹ️  未发现fingerprint元素")
                    
                    success_count += 1
                    break  # 成功解析，不需要尝试其他编码
                    
                except Exception as e:
                    print(f"   ⚠️  XML解析失败: {e}")
                    continue
                    
            except Exception as e:
                print(f"   ✗ {encoding}: {e}")
                continue
    
    return success_count > 0


if __name__ == "__main__":
    print("=" * 60)
    print("🧪 XML特殊字符处理测试")
    print("=" * 60)
    
    # 运行测试
    results = {}
    
    # 测试1: XML特殊字符处理
    results['xml_special_chars'] = test_xml_special_char_handling()
    
    # 测试2: FingerprintElement类型类
    results['fingerprint_element_type'] = test_fingerprint_element_type()
    
    # 测试3: 解析包含特殊字符的XML文件
    results['xml_file_parsing'] = test_xml_parsing_with_special_chars()
    
    # 总结结果
    print("\n" + "=" * 60)
    print("📊 测试结果总结")
    print("=" * 60)
    
    for test_name, result in results.items():
        status = "✅ 通过" if result else "❌ 失败"
        print(f"{status}: {test_name}")
    
    total_tests = len(results)
    passed_tests = sum(1 for result in results.values() if result)
    
    print(f"\n总计: {passed_tests}/{total_tests} 测试通过")
    
    if passed_tests == total_tests:
        print("\n🎉 所有测试通过！")
    else:
        print(f"\n⚠️  有 {total_tests - passed_tests} 个测试失败")
