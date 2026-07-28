# P5B Security Evidence

- Vue never receives or displays the HMAC secret, raw IP, raw Redis key, Redis command, or Provider credential.
- The `.env.example` HMAC salt is a local Showcase default, not a production secret. Production must inject a distinct secret outside Git.
- Redis persistence is disabled and the rate-counter service has no volume.
- Redis stores only an HMAC identity hash, window timestamp, integer counter, and TTL. It stores no raw prompts, business facts, Evidence, Trace payloads, database credentials, or API keys.
- The normal Provider remains `commerceflow-mock`; validation used no real API key or external AI provider.
- Existing ignores exclude `.env`, keys/tokens, database files, `node_modules`, `target`, and `dist`; P5B will not stage them.
- FAIL_OPEN is disclosed as a local Showcase boundary, not a production abuse-prevention claim.
