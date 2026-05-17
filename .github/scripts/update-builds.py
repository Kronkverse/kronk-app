#!/usr/bin/env python3
"""
update-builds.py upsert '<json>'   — add/update a build entry
update-builds.py remove '<slug>'   — remove a build entry
Regenerates /var/www/kronk.info/dev/builds.json from the entries/ dir.
"""
import os, json, sys
from datetime import datetime

DEV_DIR     = '/var/www/kronk.info/dev'
ENTRIES_DIR = os.path.join(DEV_DIR, 'entries')
BUILDS_JSON = os.path.join(DEV_DIR, 'builds.json')

os.makedirs(ENTRIES_DIR, exist_ok=True)

if len(sys.argv) >= 3:
    cmd = sys.argv[1]
    arg = sys.argv[2]
    if cmd == 'upsert':
        data = json.loads(arg)
        slug = data.get('slug') or data.get('branch', 'unknown').replace('/', '-')
        data['slug'] = slug
        with open(os.path.join(ENTRIES_DIR, f'{slug}.json'), 'w') as f:
            json.dump(data, f, indent=2)
        print(f'upserted {slug}')
    elif cmd == 'remove':
        slug = arg
        path = os.path.join(ENTRIES_DIR, f'{slug}.json')
        if os.path.exists(path):
            os.remove(path)
            print(f'removed {slug}')
        else:
            print(f'not found: {slug}')

CHANNEL_ORDER = {'production': 0, 'main': 1, 'development': 2}

builds = []
for fname in os.listdir(ENTRIES_DIR):
    if fname.endswith('.json'):
        with open(os.path.join(ENTRIES_DIR, fname)) as f:
            builds.append(json.load(f))

def sort_key(b):
    channel = b.get('channel', 'branch')
    order = CHANNEL_ORDER.get(channel, 3)
    try:
        ts = datetime.fromisoformat(b.get('buildTime', '').replace('Z', '+00:00')).timestamp()
    except Exception:
        ts = 0
    return (order, -ts)

builds.sort(key=sort_key)

with open(BUILDS_JSON, 'w') as f:
    json.dump(builds, f, indent=2)
print(f'builds.json: {len(builds)} build(s)')
