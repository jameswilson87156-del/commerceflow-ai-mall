# Troubleshooting

## MySQL is not healthy

Run `docker compose ps`, then `./scripts/start-mysql.ps1 -Reset` only when demo data can be recreated. Port `3307` is intentional to avoid common local MySQL conflicts.

## Maven cannot connect to MySQL

The test profile uses H2. The running application uses MySQL. Confirm the Compose healthcheck is healthy and the `.env` values match.

## AI answer is unavailable

The showcase defaults to Mock Provider. Check that Java can reach `http://127.0.0.1:8000/health` and that `AI_PROVIDER_MODE=mock` is set.

## Redis rate-limit protection is degraded

Run `./scripts/start-redis.ps1` and wait for the Redis healthcheck. The local Compose port is intentionally loopback-only at `127.0.0.1:6380` by default; it is not a LAN service and has no production password configuration.

With the Showcase `FAIL_OPEN` policy, a Redis outage leaves the AI endpoint available and reports `X-RateLimit-Mode: degraded` without inventing quota values. `/actuator/health` will show Redis as `DOWN`, while `/actuator/health/readiness` remains `UP` only when the Java process and MySQL are healthy. Restore Redis before relying on enforced quotas.

## Frontend shows no data

Start `mall-api` with the explicit local Demo configuration from `.env.example` or the `demo` profile, then confirm the browser can call `http://127.0.0.1:8080/api/products` (or the port selected by `$env:MALL_API_PORT`) and `GET /api/v1/me`. The controlled Showcase start script synchronizes `VITE_API_BASE` with that selected port, including when an ignored local `.env.local` contains an older API URL. `POST /api/auth/demo-login` is legacy compatibility only; it does not turn an arbitrary request into a trusted production session.

## Port 8080 is occupied

Do not stop an unknown listener just to make the demo start. Inspect it with `Get-NetTCPConnection -State Listen -LocalPort 8080`, then run the Showcase on another API port:

```powershell
$env:MALL_API_PORT = "8081"
powershell -NoProfile -File .\scripts\showcase\start.ps1 -IncludeMobile
```

The lifecycle scripts record only processes they started and skip foreign or stale PIDs during shutdown.

## Windows line endings

Run scripts from PowerShell 5+ and keep `.gitattributes` in place. Do not commit `.env`, API keys, database files, or logs.
