#!/usr/bin/env python3
"""Convert webapp PNG assets to WebP for Android bundling."""
import os
import sys
from pathlib import Path
from PIL import Image

WEBAPP_ASSETS = Path(__file__).resolve().parent.parent.parent / "src" / "main" / "resources" / "webapp" / "assets"
ANDROID_ASSETS = Path(__file__).resolve().parent / "app" / "src" / "main" / "assets"

# Directories to convert (relative to webapp assets dir)
DIRS = ["rods", "backgrounds", "fish", "shop", "baits"]

# Quality settings per directory (backgrounds can be lower, rods need high quality)
QUALITY = {
    "rods": 85,
    "backgrounds": 75,
    "fish": 80,
    "shop": 80,
    "baits": 80,
}

def convert_dir(name: str):
    src_dir = WEBAPP_ASSETS / name
    dst_dir = ANDROID_ASSETS / name
    if not src_dir.is_dir():
        print(f"  SKIP {name}/ (not found)")
        return 0, 0, 0
    dst_dir.mkdir(parents=True, exist_ok=True)
    quality = QUALITY.get(name, 80)
    count = 0
    total_src = 0
    total_dst = 0
    for src_file in sorted(src_dir.glob("*.png")):
        dst_file = dst_dir / (src_file.stem + ".webp")
        src_size = src_file.stat().st_size
        try:
            img = Image.open(src_file)
            img.save(dst_file, "WEBP", quality=quality, method=4)
            dst_size = dst_file.stat().st_size
            ratio = dst_size / src_size * 100 if src_size > 0 else 0
            count += 1
            total_src += src_size
            total_dst += dst_size
        except Exception as e:
            print(f"  ERR  {src_file.name}: {e}")
    print(f"  {name}/: {count} files, {total_src/1024/1024:.1f} MB -> {total_dst/1024/1024:.1f} MB ({total_dst/total_src*100:.0f}%)")
    return count, total_src, total_dst

# Also copy the bobber/menu asset
def copy_menu_assets():
    src_dir = WEBAPP_ASSETS / "menu"
    dst_dir = ANDROID_ASSETS / "menu"
    if not src_dir.is_dir():
        return
    dst_dir.mkdir(parents=True, exist_ok=True)
    for src_file in sorted(src_dir.glob("*.png")):
        dst_file = dst_dir / (src_file.stem + ".webp")
        try:
            img = Image.open(src_file)
            img.save(dst_file, "WEBP", quality=85, method=4)
        except Exception as e:
            print(f"  ERR  menu/{src_file.name}: {e}")

def main():
    print(f"Source: {WEBAPP_ASSETS}")
    print(f"Target: {ANDROID_ASSETS}")
    print()
    grand_src = 0
    grand_dst = 0
    grand_count = 0
    for d in DIRS:
        c, s, ds = convert_dir(d)
        grand_count += c
        grand_src += s
        grand_dst += ds
    copy_menu_assets()
    print()
    print(f"Total: {grand_count} files, {grand_src/1024/1024:.1f} MB -> {grand_dst/1024/1024:.1f} MB ({grand_dst/grand_src*100:.0f}% of original)")

if __name__ == "__main__":
    main()
