#!/usr/bin/env python3
"""Run from the repository root: python3 tests/check-ux-locale.py."""
import json,re,subprocess
from pathlib import Path
p=Path('src/main/resources/assets/litematica/lang/en_us.json')
def pairs(xs):
 d={}
 for k,v in xs:
  assert k not in d,k
  d[k]=v
 return d
x=json.loads(p.read_text(),object_pairs_hook=pairs)
old=json.loads(subprocess.check_output(['git','show','HEAD:'+str(p)],text=True))
assert old.keys() <= x.keys()
for k,v in old.items():
 if x[k]!=v:
  assert re.findall(r'%(?:\d+\$)?[sdf]',v)==re.findall(r'%(?:\d+\$)?[sdf]',x[k]),k
missing=[]
for p in Path('src/main/java').rglob('*.java'):
 for k in re.findall(r'"(litematica\.[\w.]+)"',p.read_text()):
  if ('.ux.' in k or '.quick_place.' in k or '.baby.' in k or '.placement_editor.' in k) and not k.endswith(('.', '_')) and k not in x: missing.append((str(p),k))
assert not missing,missing
print('PASS: locale JSON, original keys/placeholders, new literal references')
