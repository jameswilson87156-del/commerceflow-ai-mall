# README First Run

## Prerequisites

Java 17, Docker Desktop, Node 20+, Python 3.12+, Maven Wrapper, and PowerShell 5+.

## Start

```powershell
Copy-Item .env.example .env
./scripts/start-mysql.ps1
./scripts/start-redis.ps1
./mvnw.cmd -f apps/mall-api/pom.xml test
./mvnw.cmd -f apps/mall-api/pom.xml spring-boot:run
```

If port 8080 is already occupied, use `./mvnw.cmd -f apps/mall-api/pom.xml spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` and set `VITE_API_BASE=http://localhost:8081/api` for the frontends.

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

Start the admin web:

```powershell
Set-Location apps/admin-web
npm install
npm run dev
```

The UniApp folder contains a Vite web preview for the documented user flow. It can be opened with `npm install` and `npm run dev` under `apps/mobile-app`.

## Local Redis boundary

The P5 Showcase Redis container is intentionally published only on `127.0.0.1:${REDIS_PORT:-6380}`. It has no demo password and is for this local rate-limit demonstration only; do not expose it to a LAN or reuse this configuration for a production deployment.

`/actuator/health` keeps the Redis component visible. `/actuator/health/readiness` requires the Java process and MySQL, while allowing the documented `FAIL_OPEN` Showcase policy to keep an AI request available when the optional Redis limiter is temporarily down.
