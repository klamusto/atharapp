import re

def extract_outline(pdf_path):
    with open(pdf_path, 'rb') as f:
        content = f.read()
    
    matches = re.finditer(b'/Title', content)
    found = 0
    for m in matches:
        start = max(0, m.start() - 100)
        end = min(len(content), m.end() + 300)
        chunk = content[start:end]
        print(f"--- MATCH {found} ---")
        try:
            print(chunk[:400].decode('utf-8', errors='ignore'))
        except Exception as e:
            print(chunk[:400])
        print("\n")
        found += 1
        if found >= 20:
            break

extract_outline('/tmp/quran.pdf')
