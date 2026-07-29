# P7 Configuration Matrix

| Variable / source | Module | Default | Sensitive | Showcase rule | Production boundary |
| --- | --- | --- | --- | --- |
| `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_PORT` | Compose / Java | `commerceflow`, demo user/password, `3307` | Password is demo-only but still keep local `.env` ignored. | Local MySQL only. | Replace with managed-secret configuration. |
| `MALL_API_PORT` | Java | `8080` | No | Single API port selected by runtime plan. | Deployment-owned binding. |
| `AI_SERVICE_PORT`, `AI_SERVICE_URL` | Python / Java | `8000`, loopback URL | No | Mock service only. | Authenticated service boundary required. |
| `AI_PROVIDER_MODE` | Python | `mock` | No | Must remain `mock` for canonical evidence. | Remote mode requires separately approved contract. |
| `AI_PROVIDER_API_KEY` | Python | blank | Yes | Never populate or commit. | Secret manager only. |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Java / Compose | localhost, `6380`, blank | Password if used | Loopback Redis; no password required for local demo. | Private network and secret management. |
| `AI_RATE_LIMIT_*` | Java | 5 / 60 seconds / FAIL_OPEN | HMAC salt is local-only | Visible Showcase policy. | Threat-model review; do not reuse local salt. |
| `COMMERCEFLOW_CORS_ALLOWED_ORIGINS` | Java | Vite defaults currently 5173-5175 | No | P7B must include actual admin/mobile ports. | Explicit allowlist. |
| `VITE_API_BASE`, `VITE_MOBILE_API_BASE_URL` | Vue / UniApp | local API or `/api` | No | Must point at local Java API. | Environment-specific public origin. |
| `VITE_DEMO_USER_ID` | UniApp | `1` | No | Demo identity, not login. | Replace with authentication/session design. |

`.env.example` is a template, not a secret store. P7D must scan the full candidate history and working tree before public release.
