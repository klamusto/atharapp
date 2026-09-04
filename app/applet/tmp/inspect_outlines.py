def inspect_outlines(pdf_path):
    with open(pdf_path, 'rb') as f:
        content = f.read()
    
    # Search for /Outlines reference
    idx = content.find(b'/Outlines')
    if idx != -1:
        print("Found /Outlines at:", idx)
        print(content[idx:idx+200])
    else:
        print("No /Outlines found!")

inspect_outlines('/tmp/quran.pdf')
