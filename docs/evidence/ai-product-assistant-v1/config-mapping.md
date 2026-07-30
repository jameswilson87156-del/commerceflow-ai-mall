# Provider configuration mapping

## Field-level precedence

Each field is resolved independently in this order:

1. `COMMERCEFLOW_AI_*` project override
2. `PORTFOLIO_AI_*` shared portfolio configuration
3. legacy `AI_PROVIDER_*` compatibility variables
4. Mock default or fail-closed validation when no valid configuration exists

Fields are provider, base URL, model, API key, protocol, and fallback enabled. Empty project variables are ignored rather than masking a lower layer. Mixed field origins are reported as `MIXED_LAYERED`; no configuration values are persisted.

## Presence-only preflight result

- Project-specific variables: not present.
- Shared portfolio provider, base URL, model, API key, protocol, and fallback variables: present.
- Legacy compatibility variables: not present.
- Real smoke effective source: `SHARED_PORTFOLIO`.
- Real smoke adapter: `CHAT_COMPLETIONS`.
- Real smoke protocol compatibility: `true`.

Values are intentionally omitted. The configured shared model, endpoint, provider identity, and credential are never written to evidence, logs, source, or Git.

## Failure behavior

Unknown providers, unsupported protocols, and incomplete real-provider configuration fail closed. `both` selects the Chat Completions adapter. `responses` is explicitly unsupported for this phase. Java fallback follows project, shared, legacy, then the safe `true` default.
