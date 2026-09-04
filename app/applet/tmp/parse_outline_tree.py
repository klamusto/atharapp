import re

def parse_pdf_outline(pdf_path):
    with open(pdf_path, 'rb') as f:
        content = f.read()

    # 1. Map object numbers to sequential page numbers
    # We find all /Type /Page (or /Type/Page) objects.
    # Page objects look like: 'X Y obj \n << ... /Type /Page ... >>'
    # Or we can scan for 'obj' and check if it has '/Type /Page' or '/Type/Page' inside its dictionary.
    
    # Let's find all page objects:
    # A simple way is to find all occurrences of '/Type/Page' or '/Type /Page'
    # and find the object number preceding it.
    pages_obj_ids = []
    
    # We can search for all 'obj' definitions, but a regex for page objects is faster.
    # Page objects typically have '/Type/Page' or '/Type /Page'.
    # Let's search for objects and check their type.
    obj_pattern = re.compile(rb'(\d+)\s+(\d+)\s+obj')
    
    # Let's find all objects of type Page by scanning the file
    # for '/Type/Page' or '/Type /Page' and finding their object header.
    # To be extremely robust, let's find the object starts and parse their content up to 'endobj'.
    obj_starts = [m.start() for m in obj_pattern.finditer(content)]
    
    page_obj_to_num = {}
    page_count = 0
    
    # Let's find objects that contain b'/Type/Page' or b'/Type /Page'
    # and map them.
    for i in range(len(obj_starts)):
        start = obj_starts[i]
        end = obj_starts[i+1] if i+1 < len(obj_starts) else len(content)
        obj_header = obj_pattern.match(content, start)
        if obj_header:
            obj_id = int(obj_header.group(1))
            obj_content = content[start:end]
            # Check if this object is a page
            if b'/Type/Page' in obj_content or b'/Type /Page' in obj_content:
                page_count += 1
                page_obj_to_num[obj_id] = page_count

    print(f"Total page objects found: {len(page_obj_to_num)}")
    
    # 2. Parse all outline objects
    # Outline items contain: /Title (string) and /A (destination action) or /Dest
    # Let's find all objects and see if they have /Title and /Parent.
    outlines = []
    for i in range(len(obj_starts)):
        start = obj_starts[i]
        end = obj_starts[i+1] if i+1 < len(obj_starts) else len(content)
        obj_header = obj_pattern.match(content, start)
        if obj_header:
            obj_id = int(obj_header.group(1))
            obj_content = content[start:end]
            if b'/Title' in obj_content:
                # Extract Title
                title_match = re.search(rb'/Title\s*\((.*?)\)', obj_content, re.DOTALL)
                title = None
                if title_match:
                    title_bytes = title_match.group(1)
                    # Convert to string (might be UTF-16BE if starts with FEFF, or octal, etc.)
                    # Let's decode properly
                    if title_bytes.startswith(b'\xfe\xff'):
                        try:
                            title = title_bytes.decode('utf-16-be')
                        except:
                            pass
                    else:
                        # Try to parse octal escapes or decode utf-8
                        # But wait, looking at the previous output, some bytes were:
                        # \x00\xbb\x80\x07...
                        # Let's check what format the title is
                        title = title_bytes.decode('utf-8', errors='ignore')
                else:
                    # Hex string style: /Title <FEFF...>
                    title_match_hex = re.search(rb'/Title\s*<(.*?)>', obj_content, re.DOTALL)
                    if title_match_hex:
                        hex_bytes = title_match_hex.group(1).replace(b' ', b'').replace(b'\r', b'').replace(b'\n', b'')
                        try:
                            title_bytes = bytes.fromhex(hex_bytes.decode('ascii'))
                            if title_bytes.startswith(b'\xfe\xff'):
                                title = title_bytes[2:].decode('utf-16-be')
                            else:
                                title = title_bytes.decode('utf-8', errors='ignore')
                        except Exception as e:
                            print("Hex decode error:", e)
                
                if title:
                    # Now extract Destination Page Object ID
                    # Dest can be in /A with /D [X 0 R ...] or direct /Dest [X 0 R ...]
                    dest_page_id = None
                    dest_match = re.search(rb'/D\s*\[\s*(\d+)\s+\d+\s+R', obj_content)
                    if dest_match:
                        dest_page_id = int(dest_match.group(1))
                    else:
                        dest_match2 = re.search(rb'/Dest\s*\[\s*(\d+)\s+\d+\s+R', obj_content)
                        if dest_match2:
                            dest_page_id = int(dest_match2.group(1))
                    
                    page_num = page_obj_to_num.get(dest_page_id, None)
                    outlines.append({
                        "obj_id": obj_id,
                        "title": title,
                        "dest_page_obj_id": dest_page_id,
                        "page_number": page_num
                    })

    # Let's filter out outlines that didn't resolve to a page, or sort them
    for out in outlines[:50]:
        print(f"Title: {out['title']} -> Page Object: {out['dest_page_obj_id']} -> Page Number: {out['page_number']}")

parse_pdf_outline('/tmp/quran.pdf')
