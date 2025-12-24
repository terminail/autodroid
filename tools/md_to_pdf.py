import re
from fpdf import FPDF
import textwrap
import os

class ChinesePDF(FPDF):
    def __init__(self):
        super().__init__()
        self.set_auto_page_break(auto=True, margin=15)
        self.left_margin = 15
        self.right_margin = 15
        self.top_margin = 20
        self.bottom_margin = 20
        
        # Add Chinese font support
        # Try to find a Chinese font on Windows
        font_paths = [
            r'C:\Windows\Fonts\simhei.ttf',
            r'C:\Windows\Fonts\simsun.ttc',
            r'C:\Windows\Fonts\msyh.ttc',
        ]
        
        self.chinese_font = None
        for font_path in font_paths:
            if os.path.exists(font_path):
                try:
                    # Add font for different styles
                    self.add_font('Chinese', '', font_path)
                    self.add_font('Chinese', 'B', font_path)
                    self.add_font('Chinese', 'I', font_path)
                    self.add_font('Chinese', 'BI', font_path)
                    self.chinese_font = 'Chinese'
                    break
                except:
                    continue
        
        if not self.chinese_font:
            print("Warning: Chinese font not found, text may not display correctly")
        
    def header(self):
        pass
        
    def footer(self):
        self.set_y(-15)
        if self.chinese_font:
            self.set_font(self.chinese_font, 'I', 8)
        else:
            self.set_font('Arial', 'I', 8)
        self.cell(0, 10, str(self.page_no()), 0, 0, 'C')

def parse_markdown_to_pdf(md_file_path, pdf_output_path):
    pdf = ChinesePDF()
    pdf.add_page()
    
    # Read markdown file
    with open(md_file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    lines = content.split('\n')
    
    i = 0
    in_code_block = False
    code_content = []
    
    while i < len(lines):
        line = lines[i]
        
        # Skip empty lines
        if not line.strip() and not in_code_block:
            i += 1
            continue
        
        # Code block detection
        if line.strip().startswith('```'):
            if not in_code_block:
                in_code_block = True
                code_content = []
            else:
                in_code_block = False
                # Add code block
                if pdf.chinese_font:
                    pdf.set_font(pdf.chinese_font, '', 8)
                else:
                    pdf.set_font('Courier', '', 9)
                pdf.set_fill_color(245, 245, 245)
                for code_line in code_content:
                    pdf.cell(0, 5, code_line, ln=True, fill=True)
                pdf.ln(2)
            i += 1
            continue
        
        if in_code_block:
            code_content.append(line)
            i += 1
            continue
        
        # Table detection
        if '|' in line and i + 1 < len(lines) and '|' in lines[i + 1] and '-' in lines[i + 1]:
            table_data = []
            # Parse header
            header = [cell.strip() for cell in line.split('|')[1:-1]]
            table_data.append(header)
            i += 1
            # Skip separator line
            i += 1
            # Parse rows
            while i < len(lines) and '|' in lines[i]:
                row = [cell.strip() for cell in lines[i].split('|')[1:-1]]
                table_data.append(row)
                i += 1
            
            # Create table
            if table_data:
                col_width = (pdf.w - pdf.left_margin - pdf.right_margin) / len(table_data[0])
                
                # Header row
                pdf.set_fill_color(52, 152, 219)
                pdf.set_text_color(255, 255, 255)
                if pdf.chinese_font:
                    pdf.set_font(pdf.chinese_font, 'B', 8)
                else:
                    pdf.set_font('Arial', 'B', 9)
                for cell in table_data[0]:
                    pdf.cell(col_width, 7, cell, border=1, fill=True)
                pdf.ln()
                
                # Data rows
                pdf.set_text_color(0, 0, 0)
                if pdf.chinese_font:
                    pdf.set_font(pdf.chinese_font, '', 7)
                else:
                    pdf.set_font('Arial', '', 8)
                for row in table_data[1:]:
                    fill = False
                    pdf.set_fill_color(249, 249, 249)
                    for cell in row:
                        # Wrap text if too long
                        wrapped = textwrap.wrap(cell, width=int(col_width / 2.5))
                        if not wrapped:
                            wrapped = ['']
                        pdf.cell(col_width, 7, wrapped[0], border=1, fill=fill)
                        for wrap_line in wrapped[1:]:
                            pdf.ln()
                            pdf.cell(col_width, 7, wrap_line, border=1, fill=fill)
                    pdf.ln()
                    fill = not fill
                pdf.ln(5)
            continue
        
        # Heading 1
        if line.startswith('# '):
            text = line[2:].strip()
            if pdf.chinese_font:
                pdf.set_font(pdf.chinese_font, 'B', 14)
            else:
                pdf.set_font('Arial', 'B', 18)
            pdf.set_text_color(44, 62, 80)
            pdf.cell(0, 10, text, ln=True)
            pdf.line(pdf.left_margin, pdf.get_y(), pdf.w - pdf.right_margin, pdf.get_y())
            pdf.ln(5)
            i += 1
            continue
        
        # Heading 2
        if line.startswith('## '):
            text = line[3:].strip()
            if pdf.chinese_font:
                pdf.set_font(pdf.chinese_font, 'B', 12)
            else:
                pdf.set_font('Arial', 'B', 14)
            pdf.set_text_color(52, 73, 94)
            pdf.cell(0, 8, text, ln=True)
            pdf.ln(3)
            i += 1
            continue
        
        # Heading 3
        if line.startswith('### '):
            text = line[4:].strip()
            if pdf.chinese_font:
                pdf.set_font(pdf.chinese_font, 'B', 10)
            else:
                pdf.set_font('Arial', 'B', 12)
            pdf.set_text_color(93, 109, 126)
            pdf.cell(0, 7, text, ln=True)
            pdf.ln(2)
            i += 1
            continue
        
        # Heading 4
        if line.startswith('#### '):
            text = line[5:].strip()
            if pdf.chinese_font:
                pdf.set_font(pdf.chinese_font, 'B', 9)
            else:
                pdf.set_font('Arial', 'B', 11)
            pdf.set_text_color(127, 140, 141)
            pdf.cell(0, 6, text, ln=True)
            pdf.ln(2)
            i += 1
            continue
        
        # Bold text
        line = re.sub(r'\*\*(.+?)\*\*', r'\1', line)
        # Italic text
        line = re.sub(r'\*(.+?)\*', r'\1', line)
        
        # List items
        if line.strip().startswith('- ') or line.strip().startswith('* '):
            text = line.strip()[2:]
            if pdf.chinese_font:
                pdf.set_font(pdf.chinese_font, '', 8)
            else:
                pdf.set_font('Arial', '', 10)
            pdf.cell(10, 5, chr(149), ln=False)
            pdf.multi_cell(0, 5, text)
            i += 1
            continue
        
        if re.match(r'^\d+\.\s', line.strip()):
            text = re.sub(r'^\d+\.\s', '', line.strip())
            if pdf.chinese_font:
                pdf.set_font(pdf.chinese_font, '', 8)
            else:
                pdf.set_font('Arial', '', 10)
            pdf.multi_cell(0, 5, text)
            i += 1
            continue
        
        # Horizontal rule
        if line.strip() == '---':
            pdf.ln(3)
            pdf.line(pdf.left_margin, pdf.get_y(), pdf.w - pdf.right_margin, pdf.get_y())
            pdf.ln(3)
            i += 1
            continue
        
        # Regular paragraph
        if line.strip():
            if pdf.chinese_font:
                pdf.set_font(pdf.chinese_font, '', 8)
            else:
                pdf.set_font('Arial', '', 10)
            pdf.multi_cell(0, 5, line)
            pdf.ln(2)
        
        i += 1
    
    # Save PDF
    pdf.output(pdf_output_path)
    print(f'PDF generated successfully: {pdf_output_path}')

if __name__ == '__main__':
    md_file = r'd:\git\autodroid\autodroid-trader-server\course_chose.md'
    pdf_file = r'd:\git\autodroid\autodroid-trader-server\course_chose.pdf'
    parse_markdown_to_pdf(md_file, pdf_file)
