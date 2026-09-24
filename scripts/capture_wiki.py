#!/usr/bin/env python3
"""Capture deepwoken.co game data from public SSR wiki pages (bypasses Cloudflare-gated /api/proxy)."""
import sys, os, json
# local module next to this script
from decode_nuxt import fetch, decode

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'data', 'raw')
os.makedirs(OUT, exist_ok=True)

ROUTES = {
    'talents':   'https://deepwoken.co/wiki/talent',
    'mantras':   'https://deepwoken.co/wiki/mantra',
    'weapons':   'https://deepwoken.co/wiki/weapon',
    'enchants':  'https://deepwoken.co/wiki/enchantment',
    'outfits':   'https://deepwoken.co/wiki/outfit',
    'enemies':   'https://deepwoken.co/wiki/enemy',
    'oaths':     'https://deepwoken.co/wiki/oath',
    'boons':     'https://deepwoken.co/wiki/boon',
    'flaws':     'https://deepwoken.co/wiki/flaw',
    'aspects':   'https://deepwoken.co/wiki/aspect',   # races
    'equipment': 'https://deepwoken.co/wiki/equipment',
}

for key, url in ROUTES.items():
    print(f"== {key}: {url}")
    try:
        html = fetch(url)
        d = decode(html)
        if d is None:
            print("  !! no __NUXT_DATA__ payload"); continue
        data = d.get('data', {})
        # find the list value
        lst = None
        for v in data.values():
            if isinstance(v, list):
                lst = v; break
        if lst is None:
            print("  !! no list in payload"); continue
        path = os.path.join(OUT, key + '.json')
        with open(path, 'w') as f:
            json.dump(lst, f, ensure_ascii=False, indent=1)
        print(f"  -> {path} ({len(lst)} items)")
    except Exception as e:
        print(f"  !! FAILED: {e}")