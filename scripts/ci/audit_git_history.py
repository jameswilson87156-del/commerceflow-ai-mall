"""Read-only history audit for release review; not a replacement for a credential scanner."""

from __future__ import annotations

import re
import subprocess
import sys
from collections import Counter
from pathlib import Path, PurePosixPath


ROOT = Path(__file__).resolve().parents[2]
FORBIDDEN_PARTS = {".env", "node_modules", "target", "dist", ".venv", "__pycache__", ".showcase"}
FORBIDDEN_SUFFIXES = {".pid", ".log", ".db", ".sqlite", ".sqlite3", ".zip", ".pem", ".key"}
SECRET_PATTERNS = {
    "github_token": re.compile(rb"\bgh[pousr]_[A-Za-z0-9]{20,}\b|\bgithub_pat_[A-Za-z0-9_]{20,}\b"),
    "api_key": re.compile(rb"\bsk-[A-Za-z0-9_-]{20,}\b|\bAKIA[0-9A-Z]{16}\b"),
    "private_key": re.compile(rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    "bearer_token": re.compile(rb"\bBearer\s+[A-Za-z0-9._-]{20,}\b", re.IGNORECASE),
    "openai_assignment": re.compile(rb"\bOPENAI_API_KEY[ \t]*[:=][ \t]*[^\s\"']+", re.IGNORECASE),
}
REDIS_ENV_ASSIGNMENT = re.compile(rb"(?m)^[ \t]*REDIS_PASSWORD[ \t]*=[ \t]*([^\r\n#]*)", re.IGNORECASE)
REDIS_SPRING_DEFAULT = re.compile(rb"\$\{REDIS_PASSWORD:([^}]*)\}", re.IGNORECASE)
WINDOWS_DRIVE = rb"[A-Za-z]" + rb":\\"
UNIX_HOME = rb"/" + rb"(?:Users|home)/"
ABSOLUTE_PATH = re.compile(rb"(?:" + WINDOWS_DRIVE + rb"(?:Users|workhome|Temp|Windows)\\|" + UNIX_HOME + rb")", re.IGNORECASE)


def git(*args: str) -> bytes:
    return subprocess.check_output(["git", *args], cwd=ROOT)


def commits() -> list[str]:
    return git("rev-list", "--all").decode().splitlines()


def tree_entries(commit: str):
    for entry in git("ls-tree", "-r", "-z", commit).split(b"\0"):
        if not entry:
            continue
        metadata, path = entry.split(b"\t", 1)
        mode, kind, object_id = metadata.split()
        if kind == b"blob":
            yield object_id.decode(), path.decode("utf-8", "surrogateescape")


def is_forbidden_path(path: str) -> bool:
    parsed = PurePosixPath(path)
    return any(part.lower() in FORBIDDEN_PARTS for part in parsed.parts) or parsed.suffix.lower() in FORBIDDEN_SUFFIXES


def main() -> int:
    findings: list[tuple[str, str, str]] = []
    seen_blobs: set[str] = set()
    scanned_commits = commits()
    scanned_paths = 0
    for commit in scanned_commits:
        for object_id, path in tree_entries(commit):
            scanned_paths += 1
            if is_forbidden_path(path):
                findings.append(("forbidden_history_path", commit, path))
            if object_id in seen_blobs:
                continue
            seen_blobs.add(object_id)
            content = git("cat-file", "-p", object_id)
            if b"\0" in content:
                continue
            for kind, pattern in SECRET_PATTERNS.items():
                if pattern.search(content):
                    findings.append((kind, commit, path))
            for match in REDIS_ENV_ASSIGNMENT.finditer(content):
                value = match.group(1).strip()
                if value and value not in {b'""', b"''"}:
                    findings.append(("redis_assignment", commit, path))
            for match in REDIS_SPRING_DEFAULT.finditer(content):
                if match.group(1).strip():
                    findings.append(("redis_assignment", commit, path))
            if ABSOLUTE_PATH.search(content):
                findings.append(("absolute_local_path", commit, path))
    counts = Counter(kind for kind, _, _ in findings)
    print(f"HISTORY_COMMITS={len(scanned_commits)}")
    print(f"HISTORY_TREE_ENTRIES={scanned_paths}")
    print(f"HISTORY_UNIQUE_BLOBS={len(seen_blobs)}")
    for kind, commit, path in findings:
        print(f"{kind}\t{commit}\t{path}")
    blocker_kinds = {"github_token", "api_key", "private_key", "bearer_token", "openai_assignment", "redis_assignment"}
    if any(kind in blocker_kinds for kind in counts):
        print("HISTORY_SECURITY_BLOCKED", file=sys.stderr)
        return 2
    print("HISTORY_SECURITY_NO_CREDENTIAL_BLOCKERS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
