# P7C Baseline Audit

Baseline branch was `feat/p7-showcase-release` at `5e4bf08`. The worktree was clean and P7B's three commits were present before P7C started.

Findings:

- Root README still described Showcase V1, an optional OpenAI-compatible provider, and an obsolete manual startup path. It did not cover the P2–P7 real Showcase surface.
- Existing CI used Java 17, Python 3.12, Node 20, Redis, Maven/npm caches, and frontend builds, but omitted MySQL/Flyway smoke, Admin/Mobile tests, repository-integrity checks, and explicit provider safety.
- Admin/mobile package-lock files exist; Java uses Maven with the root wrapper and Java 17 enforcer; Python requirements are exact-version pins.
- The P7 canonical plan omitted the implemented P7B overview and promoted the Redis 429 image too prominently for README use.
- P7B evidence said four rate-limit headers and described final stop work in the future. Both statements required documentation-only correction.
