# Troubleshooting

## MySQL is not healthy

Run `docker compose ps`, then `./scripts/start-mysql.ps1 -Reset` only when demo data can be recreated. Port `3307` is intentional to avoid common local MySQL conflicts.

## Maven cannot connect to MySQL

The test profile uses H2. The running application uses MySQL. Confirm the Compose healthcheck is healthy and the `.env` values match.

## AI answer is unavailable

The showcase defaults to Mock Provider. Check that Java can reach `http://localhost:8000/health` and that `AI_PROVIDER_MODE=mock` is set.

## Redis rate-limit protection is degraded

Run `./scripts/start-redis.ps1` and wait for the Redis healthcheck. The local Compose port is intentionally loopback-only at `127.0.0.1:6380` by default; it is not a LAN service and has no production password configuration.

With the Showcase `FAIL_OPEN` policy, a Redis outage leaves the AI endpoint available and reports `X-RateLimit-Mode: degraded` without inventing quota values. `/actuator/health` will show Redis as `DOWN`, while `/actuator/health/readiness` remains `UP` only when the Java process and MySQL are healthy. Restore Redis before relying on enforced quotas.

## Frontend shows no data

Start `mall-api`, run demo login, and confirm the browser can call `http://localhost:8080/api/products`.

## Windows line endings

Run scripts from PowerShell 5+ and keep `.gitattributes` in place. Do not commit `.env`, API keys, database files, or logs.
