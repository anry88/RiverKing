#!/usr/bin/env python3
"""Verify Android bundled assets stay in sync with webapp assets and UI mappings."""

from __future__ import annotations

from pathlib import Path
import re
import sys


SCRIPT_PATH = Path(__file__).resolve()
ANDROID_ROOT = SCRIPT_PATH.parents[1]
REPO_ROOT = SCRIPT_PATH.parents[3]
WEBAPP_ASSETS = REPO_ROOT / "src" / "main" / "resources" / "webapp" / "assets"
ANDROID_ASSETS = ANDROID_ROOT / "app" / "src" / "main" / "assets"
GAME_TABS = ANDROID_ROOT / "app" / "src" / "main" / "java" / "com" / "riverking" / "mobile" / "ui" / "GameTabs.kt"
TABLES = REPO_ROOT / "src" / "main" / "kotlin" / "db" / "Tables.kt"
I18N = REPO_ROOT / "src" / "main" / "kotlin" / "service" / "I18n.kt"
ACHIEVEMENTS = REPO_ROOT / "src" / "main" / "kotlin" / "service" / "AchievementService.kt"

CATEGORY_DIRS = ("fish", "shop", "baits", "backgrounds", "achievements", "menu", "rods")
ACHIEVEMENT_BASE_CODES = (
    "simple_fisher",
    "uncommon_fisher",
    "rare_fisher",
    "epic_fisher",
    "legendary_fisher",
    "mythic_fisher",
    "traveler",
    "trophy_hunter",
    "tournament_winner",
    "daily_rating_star",
    "koi_collector",
)
ACHIEVEMENT_LEVELS = ("grey", "bronze", "silver", "gold", "platinum")


def normalize_achievement_base(code: str) -> str:
    if code == "river_delta_all_fish":
        return "explorer_delta_river"
    if code.endswith("_all_fish"):
        return f"explorer_{code.removesuffix('_all_fish')}"
    return code


def check_category_sync(errors: list[str]) -> None:
    for category in CATEGORY_DIRS:
        web_dir = WEBAPP_ASSETS / category
        android_dir = ANDROID_ASSETS / category
        web_files = {path.with_suffix(".webp").name for path in web_dir.glob("*.png")}
        android_files = {path.name for path in android_dir.glob("*.webp")}

        missing_in_android = sorted(web_files - android_files)
        extra_in_android = sorted(android_files - web_files)

        if missing_in_android:
            errors.extend(f"{category}: missing Android asset {name}" for name in missing_in_android)
        if extra_in_android:
            errors.extend(f"{category}: extra Android asset {name}" for name in extra_in_android)


def fish_map_values_and_keys() -> tuple[set[str], set[str]]:
    text = GAME_TABS.read_text(encoding="utf-8")
    match = re.search(r"private val FISH_ASSET_MAP = mapOf\((.*?)\n\)", text, re.S)
    if not match:
        raise SystemExit("Could not locate FISH_ASSET_MAP in GameTabs.kt")
    block = match.group(1)
    values = set(re.findall(r'to\s+"(fish/[^"]+)"', block))
    keys = set(re.findall(r'"([^"]+)"\s+to\s+"fish/[^"]+"', block))
    return values, keys


def check_fish_assets(errors: list[str]) -> None:
    values, keys = fish_map_values_and_keys()
    for relative_path in sorted(values):
        if not (ANDROID_ASSETS / relative_path).is_file():
            errors.append(f"fish map points to missing asset {relative_path}")

    ru_names = set(re.findall(r'upsertFish\("([^"]+)"\s*,', TABLES.read_text(encoding="utf-8")))
    i18n_text = I18N.read_text(encoding="utf-8")
    fish_block = re.search(r"private val fish = mapOf\((.*?)\n\s*\)", i18n_text, re.S)
    if not fish_block:
        raise SystemExit("Could not locate fish map in I18n.kt")

    en_names = {
        en_name
        for ru_name, en_name in re.findall(r'"([^"]+)"\s+to\s+"([^"]+)"', fish_block.group(1))
        if ru_name in ru_names
    }

    missing_ru = sorted(ru_names - keys)
    missing_en = sorted(en_names - keys)
    if missing_ru:
        errors.extend(f"fish map missing RU key {name}" for name in missing_ru)
    if missing_en:
        errors.extend(f"fish map missing EN key {name}" for name in missing_en)


def check_achievement_assets(errors: list[str]) -> None:
    achievement_dir = ANDROID_ASSETS / "achievements"
    files = {path.name for path in achievement_dir.glob("*.webp")}

    location_codes = re.findall(r'LocationAchievementConfig\("([^"]+)"', ACHIEVEMENTS.read_text(encoding="utf-8"))
    expected = {
        f"{normalize_achievement_base(code)}_{level}.webp"
        for code in (*ACHIEVEMENT_BASE_CODES, *location_codes)
        for level in ACHIEVEMENT_LEVELS
    }

    missing = sorted(expected - files)
    if missing:
        errors.extend(f"achievement art missing {name}" for name in missing)


def main() -> int:
    errors: list[str] = []
    check_category_sync(errors)
    check_fish_assets(errors)
    check_achievement_assets(errors)

    if errors:
        print("Asset audit failed:")
        for error in errors:
            print(f" - {error}")
        return 1

    print("Asset audit passed.")
    print(f"Checked categories: {', '.join(CATEGORY_DIRS)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
