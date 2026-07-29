# P7 Showcase Runtime Plan

## Implemented P7B

The planned lifecycle is implemented in `scripts/showcase/`. It uses `.showcase/processes.json` and local logs (both ignored) to identify only owned listener processes. The default URLs are Admin `http://127.0.0.1:5174`, Mobile H5 `http://127.0.0.1:5173`, Java `http://127.0.0.1:8080`, and FastAPI `http://127.0.0.1:8000`; all ports are configurable. `status.ps1` distinguishes aggregate health from readiness, reports provider/rate-limit/Flyway facts, and does not start or stop services.

P7B will implement, not merely document, a controlled Windows PowerShell 5 lifecycle. P7A adds no scripts.

| Planned script | Responsibility | Safety rule |
| --- | --- | --- |
| `scripts/showcase/start.ps1` | Read `.env`, start Compose dependencies, wait health, start Java/FastAPI/admin/mobile with recorded ports and PIDs. | UTF-8, no ExecutionPolicy bypass, no global process kill. |
| `scripts/showcase/status.ps1` | Report expected process identity, PID, port listener, health endpoints, and log location. | Do not infer a random listener is CommerceFlow. |
| `scripts/showcase/stop.ps1` | Stop only PIDs started/recorded by the Showcase lifecycle and Compose services it owns. | Never stop unrelated processes. |
| `scripts/showcase/verify.ps1` | Call health, Mock AI, product/order reads, and builds/test commands selected for the release. | Fail with an actionable component/port message. |

## Runtime Order

1. Verify Java 17, Node 20, Python 3.12, Docker Desktop, and copied `.env`.
2. Start local MySQL 8.4 and Redis 8.0.2; wait for their Compose health checks.
3. Start Java on the configured local API port and wait for readiness/database migration.
4. Start FastAPI in Mock mode and wait for `/health`.
5. Start Vue admin and UniApp H5 with API/CORS origins matching the selected ports.
6. Record PIDs, ports, command lines, and log paths in an ignored local state file.

The design must support default local ports while resolving the current H5 CORS mismatch through one source of truth. It must not use real provider keys or expose Redis outside loopback.
