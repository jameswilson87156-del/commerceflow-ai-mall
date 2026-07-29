# P7 Git Release Strategy

## Audit

- The remote currently has frozen feature branches but no `main`; remote `HEAD` still points to `feat/p3-order-inventory-evidence`.
- Historical P3-P6 branches are evidence and must remain unchanged.
- P7A creates no PR, tag, release, or remote change.

## Candidate Release Path

1. Create `feat/p7-showcase-release` from `feat/p6-mobile-final-freeze` after P7A review.
2. Complete P7B: truthful operations overview if approved, read-only APIs only, and runtime lifecycle.
3. Complete P7C: CI, README, architecture/configuration docs, canonical screenshot validation.
4. Complete P7D: security/history scan and final empty-runtime verification.
5. Open a PR to a newly established `main` only after the candidate is clean and reviewed.
6. Merge with squash only if the final history remains understandable; retain frozen phase branches regardless.
7. Tag the merged commit `showcase-v1.0.0`.
8. Create release notes that link evidence, explicit boundaries, provenance, and ownership gaps.

Only the future P7D release owner may create `main`, a PR, tag, or public release. No force push, rebase, or branch deletion is part of this strategy.
