"""Capture the current emulator screen and its visible accessibility tree."""
import argparse
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET
from PIL import Image

parser = argparse.ArgumentParser()
parser.add_argument('label')
parser.add_argument('--serial', default='emulator-5554')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1] / 'build/qa/ui'
root.mkdir(parents=True, exist_ok=True)
adb = str(Path(os.environ['LOCALAPPDATA']) / 'Android/Sdk/platform-tools/adb.exe')
def run(*command, **options):
    return subprocess.run([adb, '-s', args.serial, *command], check=True, **options)
remote = f'/sdcard/ilturno-{args.label}.xml'
dump = run('shell', 'uiautomator', 'dump', remote, capture_output=True)
if b'ERROR' in dump.stdout + dump.stderr:
    raise RuntimeError((dump.stdout + dump.stderr).decode('utf-8', errors='replace'))
xml = run('exec-out', 'cat', remote, capture_output=True).stdout
(root / f'{args.label}.xml').write_bytes(xml)
with (root / f'{args.label}.png').open('wb') as output:
    run('exec-out', 'screencap', '-p', stdout=output)
print('Screenshot:', str(root / f'{args.label}.png'), 'size:', Image.open(root / f'{args.label}.png').size)
for node in ET.fromstring(xml).iter('node'):
    if node.get('text') or node.get('content-desc'):
        print({key: node.get(key) for key in ['text', 'content-desc', 'bounds', 'checked', 'clickable']})
