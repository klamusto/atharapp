import re

def extract_outline(pdf_path):
    with open(pdf_path, 'rb') as f:
        content = f.read()
    
    # Let's search for objects containing /Title and /Dest or /A
    # Usually outline items look like: << /Title ... /Dest ... >> or similar
    # Since they are dictionaries, let's search for /Title
    # We can search for /Title followed by text, and try to find /Dest or /Page references.
    
    # Let's find all occurrences of /Title
    matches = re.finditer(b'/Title', content)
    found = 0
    for m in matches:
        start = max(0, m.start() - 100)
        end = min(len(content), m.end() + 300)
        chunk = content[start:end]
        print(f"--- MATCH {found} ---")
        print(chunk[:400])
        print("\n")
        found += 1
        if found >= 20:
            break

extract_outline('/tmp/quran.pdf')
