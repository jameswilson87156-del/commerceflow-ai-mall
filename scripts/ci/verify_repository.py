"""Small tracked-file and README reference gate for the local Showcase repository."""

from __future__ import annotations

import re
import struct
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote


ROOT = Path(__file__).resolve().parents[2]
README = ROOT / "README.md"
ADMIN_SCREENSHOT = ROOT / "screenshots/v2/01-operations-overview-real.png"
MOBILE_SCREENSHOTS = (
    ROOT / "screenshots/v2/06-mobile-product-detail-final.png",
    ROOT / "screenshots/v2/06-mobile-order-detail-final.png",
    ROOT / "screenshots/v2/07-mobile-ai-customer-service-final.png",
)
FREEZE_RECORDS = (
    ROOT / "docs/evidence/P3-order-inventory/P3_FINAL_FREEZE.md",
    ROOT / "docs/evidence/P4-ai-customer-service/P4_FINAL_VISUAL_FREEZE.md",
    ROOT / "docs/evidence/P5-ai-rate-limit/P5_FINAL_FREEZE.md",
    ROOT / "docs/evidence/P6-mobile-commerce/P6_FINAL_FREEZE.md",
    ROOT / "docs/evidence/P7-showcase/P7B_TEST_RESULTS.md",
)
FORBIDDEN_SEGMENTS = {".env", "node_modules", "target", "dist", ".venv", "__pycache__", ".showcase"}
FORBIDDEN_SUFFIXES = {".pid", ".log", ".sqlite", ".sqlite3", ".db"}
SECRET_PATTERNS = (
    re.compile(r"\bsk-[A-Za-z0-9_-]{20,}\b"),
    re.compile(r"\bgh[pousr]_[A-Za-z0-9]{20,}\b"),
    re.compile(r"\bgithub_pat_[A-Za-z0-9_]{20,}\b"),
    re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    re.compile(r"\bAKIA[0-9A-Z]{16}\b"),
)
LINK = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")


def tracked_files() -> list[str]:
    output = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT)
    return [entry for entry in output.decode("utf-8").split("\0") if entry]


def png_size(path: Path) -> tuple[int, int]:
    header = path.read_bytes()[:24]
    if len(header) != 24 or header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        raise ValueError(f"not a PNG IHDR file: {path.relative_to(ROOT)}")
    return struct.unpack(">II", header[16:24])


def local_markdown_links(document: Path) -> list[Path]:
    links: list[Path] = []
    for raw_target in LINK.findall(document.read_text(encoding="utf-8")):
        target = raw_target.strip().strip("<>").split("#", 1)[0]
        if not target or re.match(r"(?:https?|mailto):", target, re.IGNORECASE):
            continue
        if target.lower().startswith("file:"):
            raise ValueError(f"README contains file URI: {raw_target}")
        links.append((document.parent / unquote(target)).resolve())
    return links


def main() -> int:
    failures: list[str] = []
    tracked = tracked_files()
    for name in tracked:
        path = Path(name)
        if any(part.lower() in FORBIDDEN_SEGMENTS for part in path.parts) or path.suffix.lower() in FORBIDDEN_SUFFIXES:
            failures.append(f"forbidden tracked runtime/build path: {name}")
    for path in (README, ADMIN_SCREENSHOT, *MOBILE_SCREENSHOTS, *FREEZE_RECORDS):
        if not path.exists():
            failures.append(f"required file is missing: {path.relative_to(ROOT)}")
    if README.exists():
        text = README.read_text(encoding="utf-8")
        if re.search(r"[A-Za-z]:\\", text):
            failures.append("README contains a Windows absolute path")
        if re.search(r"\b(?:10|172\.(?:1[6-9]|2\d|3[01])|192\.168)\.\d{1,3}\.\d{1,3}\b", text):
            failures.append("README contains a personal LAN IP")
        for link in local_markdown_links(README):
            if not link.exists():
                failures.append(f"README local link is missing: {link}")
    for document in (ROOT / "docs/architecture").glob("*.md"):
        for link in local_markdown_links(document):
            if not link.exists():
                failures.append(f"architecture local link is missing: {document.relative_to(ROOT)} -> {link}")
    if ADMIN_SCREENSHOT.exists() and png_size(ADMIN_SCREENSHOT) != (1920, 1080):
        failures.append("admin canonical screenshot must be 1920x1080")
    for path in MOBILE_SCREENSHOTS:
        if path.exists() and png_size(path) != (390, 844):
            failures.append(f"mobile canonical screenshot must be 390x844: {path.name}")
    for name in tracked:
        path = ROOT / name
        if path.suffix.lower() in {".md", ".yml", ".yaml", ".py", ".java", ".ts", ".vue", ".ps1", ".xml", ".properties"}:
            try:
                content = path.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                continue
            if any(pattern.search(content) for pattern in SECRET_PATTERNS):
                failures.append(f"obvious secret pattern in tracked text: {name}")
    if failures:
        print("REPOSITORY_INTEGRITY_FAILED", file=sys.stderr)
        print("\n".join(f"- {item}" for item in failures), file=sys.stderr)
        return 1
    print("REPOSITORY_INTEGRITY_OK")
    print("README/architecture links, canonical screenshot dimensions, tracked-file policy, freeze records, and obvious-key patterns passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
