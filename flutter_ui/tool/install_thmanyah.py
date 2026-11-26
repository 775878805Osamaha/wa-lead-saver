#!/usr/bin/env python3
"""Install user-owned Thmanyah Sans fonts locally for WA Lead Saver Flutter.

No network access. Never commit licensed OTF files to the public repository.
"""
from __future__ import annotations

import argparse
from pathlib import Path
import re
import sys
import zipfile

FACES = {
    "light": (300, "Light"),
    "regular": (400, "Regular"),
    "medium": (500, "Medium"),
    "bold": (700, "Bold"),
    "black": (900, "Black"),
}

def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path, help="Locally supplied licensed Thmanyah font ZIP")
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[1]
    pubspec = root / "pubspec.yaml"
    font_dir = root / "assets" / "fonts"

    if not args.archive.is_file():
        parser.error(f"File not found: {args.archive}")
    if not pubspec.is_file():
        parser.error("flutter_ui/pubspec.yaml does not exist")

    with zipfile.ZipFile(args.archive) as archive:
        entries = archive.namelist()
        selected: dict[str, str] = {}
        for weight in FACES:
            ending = f"thmanyahsans12_{weight}.otf"
            matches = [name for name in entries if name.lower().endswith(ending)]
            if len(matches) != 1:
                parser.error(f"Expected exactly one font face {ending}, found {len(matches)}")
            info = archive.getinfo(matches[0])
            if info.file_size < 10000 or info.file_size > 5_000_000:
                parser.error(f"Font file size is invalid: {ending}")
            selected[weight] = matches[0]
        font_dir.mkdir(parents=True, exist_ok=True)
        for weight, (_, title) in FACES.items():
            target = font_dir / f"ThmanyahSans-{title}.otf"
            target.write_bytes(archive.read(selected[weight]))
            target.chmod(0o600)

    raw = pubspec.read_text(encoding="utf-8")
    if not re.search(r"(?m)^flutter:\s*$", raw):
        parser.error("Expected Flutter configuration in pubspec.yaml")
    if re.search(r"(?m)^\s+- family: ThmanyahSans\s*$", raw):
        print("ThmanyahSans already registered in pubspec.yaml")
    else:
        font_manifest = "  fonts:\n    - family: ThmanyahSans\n      fonts:\n"
        for weight, title in FACES.values():
            font_manifest += (
                f"        - asset: assets/fonts/ThmanyahSans-{title}.otf\n"
                f"          weight: {weight}\n"
            )
        if re.search(r"(?m)^  fonts:\s*$", raw):
            parser.error("Existing flutter.fonts section; add ThmanyahSans manually to avoid overwrite")
        raw = raw.rstrip() + "\n" + font_manifest
        pubspec.write_text(raw, encoding="utf-8")
        print("Registered ThmanyahSans weights in the local pubspec.yaml")

    print("All five Thmanyah Sans faces installed LOCALLY. Do not publish font binaries.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
