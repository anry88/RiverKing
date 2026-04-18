#!/usr/bin/env python3
"""Validate Android version metadata against the backend update policy."""

from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
VERSION_FILE = ROOT / "mobile" / "android-app" / "version.properties"
POLICY_FILE = ROOT / "src" / "main" / "resources" / "android-update-policy.json"


def read_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(1)


def main() -> None:
    if not VERSION_FILE.is_file():
        fail(f"{VERSION_FILE.relative_to(ROOT)} not found")
    if not POLICY_FILE.is_file():
        fail(f"{POLICY_FILE.relative_to(ROOT)} not found")

    version = read_properties(VERSION_FILE)
    try:
        version_code = int(version["RIVERKING_VERSION_CODE"])
        version_name = version["RIVERKING_VERSION_NAME"]
    except KeyError as error:
        fail(f"missing {error.args[0]} in {VERSION_FILE.relative_to(ROOT)}")
    except ValueError:
        fail("RIVERKING_VERSION_CODE must be an integer")

    policy = json.loads(POLICY_FILE.read_text(encoding="utf-8"))
    latest_code = policy.get("latestVersionCode")
    latest_name = policy.get("latestVersionName")
    min_supported = policy.get("minSupportedVersionCode")
    require_headers = policy.get("requireVersionHeaders", False)

    if latest_code != version_code:
        fail(
            "android-update-policy.json latestVersionCode "
            f"({latest_code}) must match version.properties ({version_code})"
        )
    if latest_name != version_name:
        fail(
            "android-update-policy.json latestVersionName "
            f"({latest_name}) must match version.properties ({version_name})"
        )
    if not isinstance(min_supported, int):
        fail("android-update-policy.json minSupportedVersionCode must be an integer")
    if min_supported > latest_code:
        fail("minSupportedVersionCode cannot exceed latestVersionCode")
    if not isinstance(require_headers, bool):
        fail("android-update-policy.json requireVersionHeaders must be a boolean when present")

    notes = policy.get("releaseNotes", {})
    for locale in ("en", "ru"):
        localized = notes.get(locale)
        if not isinstance(localized, list) or not any(str(item).strip() for item in localized):
            fail(f"releaseNotes.{locale} must include at least one non-empty note")

    print(
        "Android update policy OK: "
        f"latest={latest_code} ({latest_name}), minSupported={min_supported}, "
        f"requireVersionHeaders={str(require_headers).lower()}"
    )


if __name__ == "__main__":
    main()
