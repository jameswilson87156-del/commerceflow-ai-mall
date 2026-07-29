# P7C CI Implementation

`.github/workflows/ci.yml` contains five jobs: `repository-integrity`, `java-backend`, `python-ai-service`, `admin-web`, and `mobile-app`.

- Integrity uses standard-library Python only and never claims a history-wide security audit.
- Java uses Temurin 17, MySQL 8.4 and Redis 8.0.2 services, Maven cache, the full test suite, and a separate real-MySQL Flyway overview smoke.
- Python uses Python 3.12 with the pinned requirements and `AI_PROVIDER_MODE=MOCK`.
- Admin and Mobile use Node 20, their respective lock files, test commands, and production/Uni compilation commands.
- CI has read-only repository permission. It does not call external AI, read a real key, deploy, publish images, create releases, or write Git history.

This is a workflow definition and local structural validation. P7C deliberately does not create a temporary remote branch or PR merely to execute CI.
