#!/usr/bin/env python3
"""Resolve a named event manifest against the admin catalog; preview by default."""
import argparse
import json
import math
import os
from datetime import datetime, timezone
from pathlib import Path
from urllib.error import HTTPError
from urllib.parse import urlparse
from urllib.request import Request, urlopen


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--apply", action="store_true", help="Create the future event after validation")
    args = parser.parse_args()
    base = args.base_url.rstrip("/")
    destination = urlparse(base)
    if destination.scheme != "https" and not (
        destination.scheme == "http" and destination.hostname in {"localhost", "127.0.0.1", "::1"}
    ):
        parser.error("Use HTTPS, or a local HTTP test server")
    token = os.environ.get("ADMIN_API_TOKEN", "")
    if not token:
        parser.error("Set ADMIN_API_TOKEN in the environment")
    policy = json.loads((Path(__file__).resolve().parents[1] / "src/main/resources/android-update-policy.json").read_text())
    headers = {
        "Authorization": "Bearer " + token,
        "X-RiverKing-App-Version-Code": str(policy["latestVersionCode"]),
        "X-RiverKing-App-Version-Name": policy["latestVersionName"],
        "X-RiverKing-App-Channel": "direct",
    }

    def request(path, payload=None):
        data = None if payload is None else json.dumps(payload, ensure_ascii=False).encode()
        req = Request(base + path, data=data, headers={**headers, "Content-Type": "application/json"})
        with urlopen(req, timeout=30) as response:
            return json.load(response)

    def check_image(path):
        # Public assets do not need the admin credential.
        with urlopen(base + path, timeout=30) as response:
            if not response.headers.get_content_type().startswith("image/"):
                raise ValueError("Missing image: " + path)
            response.read(16)

    event = json.loads(args.manifest.read_text())
    catalog = request("/api/admin/catalog")
    fish_by_name = {}
    for fish in catalog["eventFish"]:
        name, rarity = fish["label"].rsplit(" (", 1)
        if name in fish_by_name:
            raise ValueError("Duplicate catalog name: " + name)
        fish_by_name[name] = (int(fish["id"]), rarity.rstrip(")"))
    selected = []
    names = set()
    for fish in event["fish"]:
        name = fish["name"]
        if name in names or not math.isfinite(fish["weight"]) or fish["weight"] <= 0:
            raise ValueError("Invalid or duplicate event fish: " + name)
        names.add(name)
        if name not in fish_by_name:
            raise ValueError("Catalog fish missing; deploy the content release first: " + name)
        fish_id, rarity = fish_by_name[name]
        if rarity != fish["rarity"]:
            raise ValueError("Catalog rarity differs: " + name)
        selected.append({"fishId": fish_id, "weight": fish["weight"]})
    if not selected:
        raise ValueError("Empty fish pool")
    start = datetime.fromisoformat(event["start"].replace("Z", "+00:00"))
    end = datetime.fromisoformat(event["end"].replace("Z", "+00:00"))
    if start.tzinfo is None or end.tzinfo is None or end <= start:
        raise ValueError("Invalid event dates")
    payload = {key: event[key] for key in ("nameRu", "nameEn", "imagePath", "castZone")}
    payload.update(startTime=int(start.timestamp() * 1000), endTime=int(end.timestamp() * 1000), fish=selected)
    for category in ("weightPrizes", "countPrizes", "fishPrizes"):
        config = event[category]
        if config["prizePlaces"] != len(config["prizes"]):
            raise ValueError("Prize places differ from reward count: " + category)
        payload[category] = {"prizePlaces": config["prizePlaces"], "prizesJson": json.dumps(config["prizes"], separators=(",", ":"))}
    check_image("/event-assets/" + event["imagePath"])
    for asset in event.get("requiredFishAssets", []):
        check_image("/app/assets/fish/" + asset + ".png")
    existing = request("/api/admin/events")
    def canonical(value):
        value = json.loads(json.dumps({key: value[key] for key in payload}))
        value["fish"] = sorted(value["fish"], key=lambda fish: fish["fishId"])
        for category in ("weightPrizes", "countPrizes", "fishPrizes"):
            value[category]["prizesJson"] = json.loads(value[category]["prizesJson"])
        return value

    for other in existing:
        same = other["nameRu"] == payload["nameRu"] and other["startTime"] == payload["startTime"]
        if same:
            if canonical(other) != canonical(payload):
                raise ValueError("Event already exists with different settings; inspect it in the admin app")
            print(f"Event {other['id']} already matches; nothing changed")
            return
        if other["startTime"] < payload["endTime"] and other["endTime"] > payload["startTime"]:
            raise ValueError("Event dates overlap: " + other["nameRu"])
    if start <= datetime.now(timezone.utc):
        raise ValueError("Refusing to create an event that has already started")
    print(f"Ready: {event['nameRu']}; {len(selected)} fish; {event['start']} — {event['end']}")
    if args.apply:
        created = request("/api/admin/events", payload)
        saved = next(event for event in request("/api/admin/events") if event["id"] == created["id"])
        if canonical(saved) != canonical(payload):
            raise ValueError("Saved event differs from submitted settings")
        print(f"Created and verified event {created['id']}")
    else:
        print("Preview only; pass --apply to create")


if __name__ == "__main__":
    try:
        main()
    except HTTPError as error:
        raise SystemExit(f"Admin API returned HTTP {error.code}: {error.read(1000).decode(errors='replace')}")
    except (ValueError, KeyError) as error:
        raise SystemExit(str(error))
