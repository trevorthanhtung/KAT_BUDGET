import os
import re

res_dir = r"D:\02_PROJECTS\4_KAT WALLET\KATWallet\app\src\main\res\values\strings.xml"
src_dir = r"D:\02_PROJECTS\4_KAT WALLET\KATWallet\app\src\main"

with open(res_dir, 'r', encoding='utf-8') as f:
    content = f.read()

# find all strings
matches = re.findall(r'<string\s+name="([^"]+)"', content)

# read all file contents
all_text = ""
for root, dirs, files in os.walk(src_dir):
    for file in files:
        if file.endswith('.kt') or file.endswith('.java') or file.endswith('.xml'):
            filepath = os.path.join(root, file)
            if "res\\values" in filepath and "strings.xml" in filepath:
                continue
            with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
                all_text += f.read()

unused = []
for string_name in matches:
    if string_name == 'app_name':
        continue
    # look for R.string.string_name or @string/string_name
    pattern1 = f"R.string.{string_name}"
    pattern2 = f"@string/{string_name}"
    
    # We can also check if it's referenced dynamically but that's rare here.
    if pattern1 not in all_text and pattern2 not in all_text:
        unused.append(string_name)

if unused:
    print("UNUSED STRINGS FOUND:")
    for u in unused:
        print(u)
else:
    print("NO UNUSED STRINGS FOUND.")
