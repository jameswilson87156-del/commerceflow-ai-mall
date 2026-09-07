# README First Run

## Prerequisites

Java 17, Docker Desktop, Node 20+, Python 3.12+, Maven Wrapper, and PowerShell 5+.

## Start

For the shortest reproducible local run, prefer the controlled Showcase lifecycle script:

```powershell
Copy-Item .env.example .env
powershell -NoProfile -File .\scripts\showcase\start.ps1 -IncludeMobile
```

The script starts MySQL, Redis, Java, FastAPI Mock, Admin, and (with `-IncludeMobile`) the H5 preview. It fails closed when an expected port is occupied by an unknown process. If port 8080 is already occupied, set `$env:MALL_API_PORT = "8081"` in the current PowerShell session before running it. The script passes the selected API port to the Admin and Mobile dev servers.

The copied `.env` deliberately selects `DEMO` with explicit local consumer and Operator identities (`1` and `9001`). This is a Showcase fixture, not a login session or production RBAC. The mobile flow uses `/api/v1/me/...`; the Admin flow uses `/api/v1/operator/...`. To exercise a no-identity or staging-like boundary, use the corresponding Spring profile/configuration instead of adding a query parameter.

For a manual Java-only run, start the dependencies, run the tests, and then start the API:

```powershell
./scripts/start-mysql.ps1
./scripts/start-redis.ps1
./mvnw.cmd -f apps/mall-api/pom.xml test
./mvnw.cmd -f apps/mall-api/pom.xml spring-boot:run
```

In another terminal:

```powershell
./scripts/verify.ps1
```

Start the AI service in Mock mode:

```powershell
py -3 -m venv services/ai-service/.venv
services/ai-service/.venv/Scripts/pip install -r services/ai-service/requirements.txt
services/ai-service/.venv/Scripts/python -m uvicorn app.main:app --app-dir services/ai-service --port 8000
```

On Windows, use the `py -3` launcher shown above. Some installations do not provide a working `python` command because the Microsoft Store app alias is enabled instead.

Start the admin web:

```powershell
Set-Location apps/admin-web
npm install
npm run dev
```

The UniApp folder contains a Vite H5 preview for the documented user flow. It can be opened with `npm install` and `npm run dev` under `apps/mobile-app`; H5 is browser-verified at `390×844`, while non-H5 targets are compile-only evidence.

## Local Redis boundary

The P5 Showcase Redis container is intentionally published only on `127.0.0.1:${REDIS_PORT:-6380}`. It has no demo password and is for this local rate-limit demonstration only; do not expose it to a LAN or reuse this configuration for a production deployment.

`/actuator/health` keeps the Redis component visible. `/actuator/health/readiness` requires the Java process and MySQL, while allowing the documented `FAIL_OPEN` Showcase policy to keep an AI request available when the optional Redis limiter is temporarily down.
