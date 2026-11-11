#!/usr/bin/env python3
"""Offline pre-publication checks; not a substitute for a real secret scanner."""
from pathlib import Path
import re
import sys
root = Path(__file__).resolve().parents[1]
required = ['README.md','CONTRIBUTORS.md','SECURITY.md','CHANGELOG.md','.gitignore',
            '.env.example','backend/pom.xml','database/01_schema.sql',
            'database/08_migrate_legacy.sql','scripts/check-engine.sh']
for name in required:
    if not (root/name).is_file():
        raise SystemExit(f'MISSING FILE: {name}')
forbidden_segments = {'target', '.idea', '.git', '__pycache__', '.vscode'}
forbidden_names = {'.env','application-local.properties'}
patterns = [re.compile(r'CREATE\s+LOGIN\s+.*?WITH\s+PASSWORD\s*=', re.I|re.S),
            re.compile(r'(?im)^\s*public\s+static\s+final\s+String\s+PASSWORD\s*=')]
files = 0
for f in root.rglob('*'):
    if not f.is_file(): continue
    files += 1
    rel = f.relative_to(root)
    if forbidden_segments.intersection(rel.parts) or f.name in forbidden_names or f.suffix in {'.class','.bak','.bacpac'}:
        raise SystemExit(f'UNWANTED FILE: {rel}')
    if f.suffix.lower() not in {'.java','.sql','.md','.txt','.properties','.yml','.yaml','.example'}:
        continue
    data = f.read_text(encoding='utf-8-sig',errors='replace')
    if f.suffix == '.sql' and patterns[0].search(data):
        raise SystemExit(f'SQL LOGIN PASSWORD SCRIPT: {rel}')
    if f.suffix == '.java' and patterns[1].search(data):
        raise SystemExit(f'HARDCODED DATABASE PASSWORD: {rel}')
print(f'PASS: {files} publishable repository files checked. Review with a dedicated secret scanner before public push.')
