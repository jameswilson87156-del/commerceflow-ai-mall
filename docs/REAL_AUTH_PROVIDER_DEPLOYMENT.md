# Real authentication, provider and staging deployment

> 2026-09-05 audit correction: overall **BLOCKED**. The current rerun is 91 Java tests with zero failures/errors/skips, Admin46/H5 36/Python11, application builds, configuration guards, schema-only MySQL checks and Compose static checks **LOCAL_PASS**. The older skipped-Redis summary below is historical. The local deployment scaffold issues (shared OIDC source in image stages, Demo Flyway seeds, container-only DB/Redis wiring and client/config isolation) were remediated; Docker registry access, real cloud resources/Secrets/OIDC, DNS/ICP/TLS and public acceptance remain **STAGING_PENDING** or **BLOCKED**. Follow [the independent audit plan](release/STAGING_AUDIT_PLAN_20260905.md) before any runbook deployment commands. No new live Provider request or cloud deployment was performed.

This is the current execution guide for CommerceFlow. It describes what is implemented and how to connect external services; it is not evidence that an IdP, an AI account or a public server has already been connected.

> Current evidence update (2026-09-05): the local real DeepSeek synthetic smoke and the local isolated OIDC/PKCE/API authorization smoke have passed. This does not change the remaining external staging and public-deployment gates. The copy-ready deployment handoff is [AI_DEPLOYMENT_AND_UPGRADE_PROMPT.md](release/AI_DEPLOYMENT_AND_UPGRADE_PROMPT.md).

## Implemented boundary

- The Java API has stateless OIDC resource-server mode for staging and production. Spring Security verifies the bearer JWT using the issuer discovery document and JWKS; an optional audience check is enabled by `COMMERCEFLOW_AUTH_AUDIENCE`.
- Consumer routes derive the user from the verified JWT and `user_account`; the client cannot choose a `userId` on `/api/v1/me/**`.
- Operator routes derive an operator actor from the verified JWT and the configured role claim. `COMMERCEFLOW_AUTH_OPERATOR_ROLES` controls which normalized roles can enter the operational boundary.
- Local Showcase mode remains explicit and separate. Staging/production profiles use OIDC, disable Demo identities, and disable legacy routes.
- The mobile H5 and admin Vue builds use Authorization Code + PKCE. They contain only a public OIDC client id. The AI API key is sent only by the Java server and is never compiled into either browser bundle.
- `deploy/staging/` includes MySQL, Redis, the Java API, the combined Nginx edge and an optional Caddy TLS profile. The real-provider staging path no longer starts the legacy Python placeholder service; the Java OpenAI-compatible adapter is the selected path.
- A staging/production startup guard rejects Demo identity modes, legacy routes, fallback-enabled AI, a non-`FAIL_CLOSED` rate-limit policy, placeholder database settings, unsafe rate-limit identity secrets, unsupported provider names, missing provider credentials and non-HTTPS CORS origins.
- SpringDoc and Swagger UI are disabled in staging/production. `/api/actuator/**` is deliberately not exposed by the edge; public health is the minimal edge `GET /health` route, while readiness remains inside the service network.
- The edge template applies a baseline CSP, frame/object blocking, MIME sniffing protection, referrer and permissions policies. TLS mode adds HSTS. Redis remains mandatory in staging because AI rate limiting is fail closed.

## OIDC registration

Use a public browser client with Authorization Code + PKCE (`S256`) and no client secret. Register both exact redirect URIs:

- `https://<admin-host>/`
- `https://<mobile-host>/`

The issuer must be the same issuer that appears in the JWT `iss` claim and must expose discovery/JWKS endpoints reachable from the Java container. Set the role claim to match the IdP. The resolver accepts the configured role claim plus common `groups`, Keycloak `realm_access.roles` and `scope` shapes; the operator boundary still requires one of `COMMERCEFLOW_AUTH_OPERATOR_ROLES`.

### Mapping a consumer identity

By default, `COMMERCEFLOW_AUTH_AUTO_PROVISION_USERS=false`. Before the first real consumer request, map the IdP subject to an active row in `user_account`. The stored key is the exact issuer plus a pipe plus the JWT subject:

```sql
UPDATE user_account
SET external_subject = 'https://id.example.com/realms/commerceflow|SUBJECT_FROM_IDP'
WHERE id = 1 AND status = 'ACTIVE';
```

Run this through the database migration/admin process with the actual issuer and subject; do not put a user subject into source control. If controlled auto-provisioning is desired, set `COMMERCEFLOW_AUTH_AUTO_PROVISION_USERS=true` only after reviewing username uniqueness and account lifecycle rules.

## AI provider choices

The Java adapter accepts the following OpenAI-compatible configurations:

| Provider | Base URL | Path | Example model |
| --- | --- | --- | --- |
| OpenAI | `https://api.openai.com/v1` | `/chat/completions` | provider account model |
| DeepSeek | `https://api.deepseek.com` | `/v1/chat/completions` | `deepseek-chat` |
| Compatible gateway | gateway base | gateway-specific path | gateway model |

Set:

```text
COMMERCEFLOW_AI_PROVIDER=OPENAI_COMPATIBLE
COMMERCEFLOW_AI_BASE_URL=https://api.openai.com/v1
COMMERCEFLOW_AI_PATH=/chat/completions
COMMERCEFLOW_AI_MODEL=<model>
COMMERCEFLOW_AI_API_KEY=<server-side-secret>
COMMERCEFLOW_AI_FALLBACK_ENABLED=false
```

The application sends the question plus Java-owned product/SKU/inventory facts, validates the provider response, stores trace/evidence data, and preserves the customer-service boundary. With fallback disabled, an upstream error returns `PROVIDER_ERROR` and is recorded in the trace; it is not relabeled as a successful local answer. Local fallback remains available for Showcase by setting the explicit local mode and enabling it there.

The provider adapter has deterministic in-process HTTP tests and a bounded local real-provider verification. On 2026-09-05, the CommerceFlow synthetic smoke called DeepSeek through the Java OpenAI-compatible adapter with `fallbackUsed=false`, returned `ANSWERED`, preserved 7 Java-owned Evidence entries and 6 Trace steps, and released its temporary backend port. This proves one local synthetic success path only; it does not prove public availability, quota, cost, concurrency, model quality, or production SLO.

## Staging runbook

From the repository root:

```powershell
Copy-Item deploy/staging/.env.example deploy/staging/.env
# Replace every CHANGE_ME value; keep deploy/staging/.env untracked.
docker compose --env-file deploy/staging/.env -f deploy/staging/docker-compose.yml config -q
docker compose --env-file deploy/staging/.env -f deploy/staging/docker-compose.yml up -d --build
docker compose --env-file deploy/staging/.env -f deploy/staging/docker-compose.yml --profile tls up -d caddy
```

Before `--profile tls`, point both hostnames to this server, open ports 80/443, and confirm the ACME email. The first MySQL volume initialization runs Flyway migrations, including `V11__add_external_identity_mapping.sql`; do not reuse a staging volume as a production database.

The edge serves `/admin/` and `/mobile/` assets behind the configured hostnames and proxies `/api/` to the Java service. Verify `GET /health` at the edge and `GET /actuator/health/readiness` inside the service network. Redis is required in staging because the AI rate-limit policy is `FAIL_CLOSED`.

## Security release gate

Before an Alibaba Cloud or other public deployment:

1. Keep OIDC, external consumer/operator identity modes, legacy-route disablement and the AI provider's `fallback-enabled=false` settings from the staging profile. Verify anonymous `401`, consumer scope and operator `403` paths with real tokens.
2. Set a strong, independently generated `AI_RATE_LIMIT_IDENTITY_HASH_SECRET`; it is required because the Redis rate limiter stores an HMAC-derived identity rather than a raw token subject. Keep the failure policy `FAIL_CLOSED`.
3. Use only exact HTTPS browser origins in `COMMERCEFLOW_CORS_ALLOWED_ORIGINS`, keep `allowCredentials=false`, and replace every `.env` placeholder through an untracked runtime file or cloud secret manager.
4. Keep Swagger and public actuator proxying disabled. Do not bind MySQL or Redis to public ports. Complete WAF/firewall, certificate, backup/restore and multi-replica rate-limit reviews before opening public DNS.
5. Retain the CI dependency-audit and Compose interpolation gates; they catch new high-severity browser dependency findings and missing staged environment variables before deployment.

## Acceptance checks

1. `GET /api/v1/me/cart` without a bearer token returns 401.
2. A signed token without an operator role cannot access `/api/v1/operator/**` and returns 403/`OPERATOR_FORBIDDEN`.
3. A mapped consumer token can read and mutate only its own `/api/v1/me/**` resources; the request body has no user id field.
4. A real provider run returns provider metadata and trace steps; an invalid key with fallback disabled records `PROVIDER_ERROR` and does not return a fallback answer.
5. Order creation still requires its existing idempotency contract and inventory evidence remains server-derived.
6. Run Java, admin, mobile and Compose static checks before release.

## Current verification boundary

- Java regression: 89 tests passed, with zero failures/errors; five Redis-only integration tests were skipped when local Redis `127.0.0.1:6380` was unavailable. Maven build succeeded.
- Mobile: 36 tests passed; both Vite H5 and uni-app CLI production builds passed.
- Admin: 46 tests passed and production build passed.
- All three browser package production-dependency audits reported zero vulnerabilities.
- `docker compose --env-file deploy/staging/.env.example -f deploy/staging/docker-compose.yml config -q` passed in this workspace.
- Local isolated Keycloak OIDC/PKCE and CommerceFlow API authorization smoke passed for `mall-customer` and `mall-operator`; it was protocol-level evidence, not a public browser acceptance.
- No formal external IdP, SSH/cloud deployment credential, cloud backup/restore, DNS/TLS staging deployment or public deployment was available here. No commit or push was performed.
