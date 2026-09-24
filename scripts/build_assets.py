#!/usr/bin/env python3
"""Build the app's bundled data assets from data/raw/*.json.

- minifies JSON (compact separators)
- strips wiki-only fields (wiki, gifs, media, image, VOI)
- writes to android/app/src/main/assets/data/  (shipped in APK)
- writes to android/app/src/test/resources/data/ (used by unit tests)

Usage: python3 scripts/build_assets.py
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, "data", "raw")
ASSETS = os.path.join(ROOT, "android", "app", "src", "main", "assets", "data")
TEST_RES = os.path.join(ROOT, "android", "app", "src", "test", "resources", "data")

STRIP_KEYS = {"wiki", "gifs", "media", "image", "VOI"}


def clean(item):
    if isinstance(item, dict):
        return {k: clean(v) for k, v in item.items() if k not in STRIP_KEYS}
    if isinstance(item, list):
        return [clean(v) for v in item]
    return item


def main():
    os.makedirs(ASSETS, exist_ok=True)
    os.makedirs(TEST_RES, exist_ok=True)
    total = 0
    for name in sorted(os.listdir(RAW)):
        if not name.endswith(".json"):
            continue
        with open(os.path.join(RAW, name), encoding="utf-8") as f:
            data = json.load(f)
        cleaned = clean(data)
        out = json.dumps(cleaned, ensure_ascii=False, separators=(",", ":"))
        for dest in (ASSETS, TEST_RES):
            with open(os.path.join(dest, name), "w", encoding="utf-8") as f:
                f.write(out)
        total += len(out)
        print(f"{name}: {len(cleaned)} items, {len(out)} bytes")
    print(f"total assets: {total/1024:.0f} KiB")


if __name__ == "__main__":
    sys.exit(main())