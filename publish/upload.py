#!/usr/bin/env python3
"""
VartaPack Publish Script
Uploads mod JARs to Modrinth and CurseForge via their APIs.

Usage:
    python3 publish/upload.py --version 0.1.0-beta --changelog "Initial beta release"
    python3 publish/upload.py --version 0.1.0-beta --changelog "..." --platform modrinth
    python3 publish/upload.py --version 0.1.0-beta --changelog "..." --loader fabric
"""

import argparse
import json
import sys
import os
import requests
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent


def load_json(path):
    with open(path) as f:
        return json.load(f)


def get_jar_path(config, loader, version):
    template = config["jar_paths"][loader]
    mc_version = config["mc_version"]
    jar_name = template.format(version=version, mc_version=mc_version)
    return PROJECT_ROOT / jar_name


def upload_modrinth(config, secrets, version, changelog, loader):
    token = secrets["modrinth_token"]
    mc = config["modrinth"]

    jar_path = get_jar_path(config, loader, version)
    if not jar_path.exists():
        print(f"[ERROR] JAR not found: {jar_path}")
        return False

    loaders = mc[f"loaders_{loader}"]
    version_number = f"{version}+{loader}"

    data = {
        "name": f"VartaPack {version} ({loader.capitalize()})",
        "version_number": version_number,
        "changelog": changelog,
        "dependencies": [],
        "game_versions": mc["game_versions"],
        "version_type": mc["version_type"],
        "loaders": loaders,
        "featured": mc["featured"],
        "project_id": mc["project_id"],
        "file_parts": ["file"],
        "primary_file": "file"
    }

    resp = requests.post(
        "https://api.modrinth.com/v2/version",
        headers={"Authorization": token},
        data={"data": json.dumps(data)},
        files={"file": (jar_path.name, open(jar_path, "rb"), "application/java-archive")}
    )

    if resp.status_code == 200:
        version_id = resp.json()["id"]
        patch = requests.patch(
            f"https://api.modrinth.com/v2/version/{version_id}",
            headers={"Authorization": token},
            json={
                "client_side": mc.get("client_side", "required"),
                "server_side": mc.get("server_side", "unsupported")
            }
        )
        if patch.status_code not in (200, 204):
            print(f"[WARN] Modrinth {loader}: could not set environment ({patch.status_code})")
        print(f"[OK] Modrinth {loader}: uploaded {jar_path.name}")
        return True
    else:
        print(f"[ERROR] Modrinth {loader}: {resp.status_code} - {resp.text}")
        return False


def upload_curseforge(config, secrets, version, changelog, loader):
    token = secrets["curseforge_token"]
    cf = config["curseforge"]

    jar_path = get_jar_path(config, loader, version)
    if not jar_path.exists():
        print(f"[ERROR] JAR not found: {jar_path}")
        return False

    game_versions = cf["game_versions"] + cf[f"loaders_{loader}"]

    metadata = {
        "changelog": changelog,
        "changelogType": "markdown",
        "displayName": f"VartaPack {version} ({loader.capitalize()})",
        "gameVersions": game_versions,
        "releaseType": cf["release_type"]
    }

    resp = requests.post(
        f"https://minecraft.curseforge.com/api/projects/{cf['project_id']}/upload-file",
        headers={"X-Api-Token": token},
        data={"metadata": json.dumps(metadata)},
        files={"file": (jar_path.name, open(jar_path, "rb"), "application/java-archive")}
    )

    if resp.status_code == 200:
        print(f"[OK] CurseForge {loader}: uploaded {jar_path.name}")
        return True
    else:
        print(f"[ERROR] CurseForge {loader}: {resp.status_code} - {resp.text}")
        return False


def main():
    parser = argparse.ArgumentParser(description="Upload VartaPack to Modrinth/CurseForge")
    parser.add_argument("--version", required=True, help="Mod version (e.g. 0.1.0-beta)")
    parser.add_argument("--changelog", required=True, help="Changelog text (markdown)")
    parser.add_argument("--platform", choices=["modrinth", "curseforge", "both"], default="both")
    parser.add_argument("--loader", choices=["fabric", "neoforge", "both"], default="both")
    parser.add_argument("--changelog-neoforge", default=None, help="Separate changelog for NeoForge (optional)")
    args = parser.parse_args()

    secrets = load_json(SCRIPT_DIR / "secrets.json")
    config = load_json(SCRIPT_DIR / "config.json")

    loaders = ["fabric", "neoforge"] if args.loader == "both" else [args.loader]
    platforms = ["modrinth", "curseforge"] if args.platform == "both" else [args.platform]

    success = True
    for loader in loaders:
        changelog = args.changelog
        if loader == "neoforge" and args.changelog_neoforge:
            changelog = args.changelog_neoforge

        for platform in platforms:
            if platform == "modrinth":
                if not upload_modrinth(config, secrets, args.version, changelog, loader):
                    success = False
            elif platform == "curseforge":
                if not upload_curseforge(config, secrets, args.version, changelog, loader):
                    success = False

    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
