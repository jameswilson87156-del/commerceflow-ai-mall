# DeepSeek synthetic smoke — 2026-09-05

## Scope and boundary

This is a bounded, local verification of the CommerceFlow Java project's actual
DeepSeek OpenAI-compatible adapter. The request used local showcase product
facts only; it did not use customer data, payment data, or a public runtime.
The API key was injected into a temporary backend process only and is not
recorded here, in the frontend, in the repository, or in command output.

The reusable runner is [verify-deepseek-synthetic.ps1](../../scripts/local/verify-deepseek-synthetic.ps1).

## Result

- Provider: `deepseek`
- Model: `deepseek-chat`
- Runtime mode: `REAL_OPENAI_COMPATIBLE`
- Local fallback: disabled
- Endpoint path: `/api/v1/me/ai/customer-service/ask`
- Synthetic request: product `101`, SKU `10004`, SKU-code question
- Answer status: `ANSWERED`
- Fallback used: `false`
- Java-owned Evidence entries: `7`
- Trace steps: `6`
- Backend latency: recorded by the API and not used as a production SLO

## Safety checks

- No provider key was found in the temporary smoke logs.
- Temporary backend port was released after the run.
- The existing local MySQL/Redis Showcase data was not migrated or deleted.
- No order, payment, DNS, cloud host, or public deployment was changed.

## What this proves

The Java adapter can make a real DeepSeek request, validate the compatible
response, preserve the provider boundary, return Java-owned product Evidence,
and persist the response Trace for one synthetic local request.

## What this does not prove

This does not prove customer-facing model quality, production quota or cost,
high concurrency, OIDC acceptance, cloud backup/restore, public availability,
or Alibaba Cloud deployment.
