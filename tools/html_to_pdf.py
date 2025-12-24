from playwright.sync_api import sync_playwright
import os

def html_to_pdf(html_file_path, pdf_output_path):
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()
        
        # Get absolute path for HTML file
        abs_html_path = os.path.abspath(html_file_path)
        
        # Load HTML file
        page.goto(f'file:///{abs_html_path}')
        
        # Generate PDF
        page.pdf(
            path=pdf_output_path,
            format='A4',
            print_background=True,
            margin={
                'top': '20px',
                'right': '20px',
                'bottom': '20px',
                'left': '20px'
            }
        )
        
        browser.close()
    
    print(f'PDF generated successfully: {pdf_output_path}')

if __name__ == '__main__':
    html_file = r'd:\git\autodroid\autodroid-trader-server\course_chose.html'
    pdf_file = r'd:\git\autodroid\autodroid-trader-server\course_chose.pdf'
    html_to_pdf(html_file, pdf_file)
