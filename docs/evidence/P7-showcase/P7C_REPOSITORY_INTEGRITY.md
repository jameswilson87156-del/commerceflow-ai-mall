# P7C Repository Integrity

`scripts/ci/verify_repository.py` is a lightweight current-tree gate. It checks README local links, canonical screenshot existence and PNG dimensions, required P3–P7 freeze evidence, forbidden tracked runtime/build paths, Windows absolute paths and personal LAN IPs in README, and a small set of obvious secret signatures.

It uses the Python standard library and Git's tracked-file list. It is not a complete Git-history scan, credential scanner, or legal audit; those wider claims remain outside P7C.
