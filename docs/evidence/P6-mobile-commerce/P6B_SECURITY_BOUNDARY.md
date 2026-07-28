# P6B Security Boundary

- `userId=1` is a centralized demo-user configuration, not authentication.
- No password, token, API key, payment credential or real user data is used.
- The mobile API base and development proxy target are environment-configurable.
- CORS uses an explicit configurable allowlist; wildcard origin was not introduced.
- Cart update/delete operations are scoped by user ID and return a not-found error for another user's item.
- `Idempotency-Key` is retained for order submission; it is not a login or production identity mechanism.
- Local assets are copied from the existing project asset set; no network image dependency was added.
- This is not a production security boundary and does not implement real authentication or authorization.
